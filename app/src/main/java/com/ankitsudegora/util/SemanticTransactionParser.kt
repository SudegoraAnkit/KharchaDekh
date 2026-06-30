package com.ankitsudegora.util

import java.util.Locale

enum class ClassificationResult {
    TRANSACTION,
    SPAM,
    UNCERTAIN
}

interface SemanticTextClassifier {
    fun classify(text: String): ClassificationResult
}

class TokenWeightClassifier : SemanticTextClassifier {
    private val tokenWeights = mapOf(
        // Transactional indicators (Positive weights)
        "spent" to 2.5,
        "debited" to 2.5,
        "withdrawn" to 2.5,
        "paid" to 2.0,
        "sent" to 2.0,
        "transferred" to 2.0,
        "credited" to 2.5,
        "received" to 2.5,
        "added" to 1.5,
        "deposited" to 2.0,
        "refunded" to 2.0,
        "reversed" to 2.0,
        "txn" to 1.5,
        "vpa" to 1.5,
        "ref" to 1.0,
        "utr" to 1.0,
        "acct" to 1.0,
        "a/c" to 1.0,

        // Spam/Promotional/OTP/Failure indicators (Negative weights)
        "preapproved" to -3.5,
        "pre-approved" to -3.5,
        "loan" to -3.0,
        "limit" to -2.5,
        "otp" to -4.0,
        "verification" to -3.5,
        "one-time" to -3.5,
        "win" to -2.5,
        "cashback" to -1.5, // Often part of marketing, handled carefully
        "chance" to -2.0,
        "congratulations" to -3.0,
        "apply" to -2.5,
        "eligible" to -2.5,
        "avail" to -2.0,
        "offer" to -2.0,
        "will" to -2.0, // e.g. "will be debited" (future conditional)
        "declined" to -4.0,
        "failed" to -4.0,
        "rejected" to -4.0,
        "reversed-alert" to -2.0
    )

    override fun classify(text: String): ClassificationResult {
        val cleanText = text.lowercase(Locale.ROOT)
        // Split by non-alphanumeric boundaries
        val tokens = cleanText.split(Regex("[^a-zA-Z0-9/_-]")).filter { it.isNotBlank() }

        var totalScore = 0.0
        var hasPositiveIndicator = false
        
        for (token in tokens) {
            val weight = tokenWeights[token]
            if (weight != null) {
                totalScore += weight
                if (weight > 0) {
                    hasPositiveIndicator = true
                }
            }
        }

        // Contextual adjustments
        if (cleanText.contains("will be debited") || cleanText.contains("will be credited")) {
            totalScore -= 4.0
        }
        if (cleanText.contains("pre-approved") || cleanText.contains("pre approved")) {
            totalScore -= 4.0
        }

        return when {
            totalScore < -1.5 -> ClassificationResult.SPAM
            totalScore > 1.8 && hasPositiveIndicator -> ClassificationResult.TRANSACTION
            else -> ClassificationResult.UNCERTAIN
        }
    }
}

data class ParsedTransaction(
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val merchant: String,
    val refNumber: String?,
    val sender: String
)

object SemanticTransactionParser {
    private val classifier: SemanticTextClassifier = TokenWeightClassifier()

    fun parse(title: String, text: String): ParsedTransaction? {
        val cleanText = text.lowercase(Locale.ROOT)

        // 1. Primary Token-Weight Classification
        val classification = classifier.classify(text)
        if (classification == ClassificationResult.SPAM) {
            return null // Drop promotional/OTP/failed items immediately
        }

        // 2. Strict Heuristic Regex Fallback / Verification
        // Reject OTP, Verification, or password alerts
        if (cleanText.contains("otp") || cleanText.contains("one time password") || cleanText.contains("verification code")) {
            return null
        }
        
        // Reject failed/declined transactions
        if (cleanText.contains("declined") || cleanText.contains("failed") || cleanText.contains("rejected")) {
            return null
        }

        // Verify it contains a transactional intent
        val isDebit = cleanText.contains("debited") || cleanText.contains("withdrawn") ||
                cleanText.contains("spent") || cleanText.contains("paid") || 
                cleanText.contains("sent") || cleanText.contains("txn to") ||
                cleanText.contains("deducted")
        val isCredit = cleanText.contains("credited") || cleanText.contains("received") || 
                cleanText.contains("added") || cleanText.contains("deposited") ||
                cleanText.contains("refunded") || cleanText.contains("reversed")

        if (!isDebit && !isCredit) {
            return null
        }

        val type = if (isDebit) "DEBIT" else "CREDIT"

        // 3. Extract Amount
        val amountRegex = Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(text) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // 4. Extract UTR / Reference Number
        var refNumber: String? = null
        
        // Match 12-digit Indian UPI/UTR references (most common)
        val upiRefRegex = Regex("\\b\\d{12}\\b")
        val upiMatch = upiRefRegex.find(text)
        if (upiMatch != null) {
            refNumber = upiMatch.value
        } else {
            // Fallback for standard alpha-numeric codes following ref/utr/txn keywords
            val refRegex = Regex("\\b(?:ref|utr|txn|id)[:\\s#\\-]*([a-zA-Z0-9]{6,16})", RegexOption.IGNORE_CASE)
            val refMatch = refRegex.find(text)
            if (refMatch != null) {
                refNumber = refMatch.groupValues[1]
            }
        }

        // 5. Extract Merchant
        val merchantRegex = Regex("(?:at|to|from|vpa|into|thru)\\s+([a-zA-Z0-9\\s.]{3,20})", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(text)
        var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: ""
        
        // Clean up merchant strings that contain garbage words or only digits
        if (merchant.isBlank() || merchant.all { it.isDigit() } || merchant.lowercase(Locale.ROOT) == "your") {
            merchant = title.ifBlank { "Unknown Merchant" }
        } else {
            // Trim trailing punctuation or details
            merchant = merchant.split("/")[0].split(" ")[0].trim()
            merchant = merchant.replace(Regex("[^a-zA-Z0-9]+$"), "") // Strip trailing punctuation (like dots or commas)
            merchant = merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        val senderName = title.ifBlank { "Alert" }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            refNumber = refNumber,
            sender = senderName
        )
    }
}
