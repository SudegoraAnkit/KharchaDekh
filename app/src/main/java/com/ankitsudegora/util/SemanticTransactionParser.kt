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

    fun cleanVpa(vpa: String): String {
        var name = vpa.trim()
        name = name.split("/")[0].split("-")[0].split("@")[0].split(".")[0].trim()
        return name.replace(Regex("[^a-zA-Z0-9\\s]+$"), "").trim()
    }

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

        // Reject billing, due, statement, or invoice notifications (not finalized transactions)
        if (cleanText.contains("due date") || cleanText.contains("due on") || 
            cleanText.contains("to be paid") || cleanText.contains("bill generated") ||
            cleanText.contains("amount due") || cleanText.contains("minimum due") ||
            cleanText.contains("overdue") || cleanText.contains("statement of") ||
            cleanText.contains("statement for") || (cleanText.contains("generated") && cleanText.contains("bill"))) {
            return null
        }

        // Reject promotional, offer, cashback, or discount messages
        if (cleanText.contains("offer") || cleanText.contains("cashback") || cleanText.contains("coupon") || 
            cleanText.contains("promo") || cleanText.contains("voucher") || cleanText.contains("discount") || 
            cleanText.contains("scratch card") || cleanText.contains("win cash")) {
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

        // 5. Extract Merchant using semantic patterns
        var merchant = ""
        val cleanTextForMerchant = text.lowercase(Locale.ROOT)

        // Helper list of generic bank/account terms to reject as merchants
        val ignoredTerms = setOf(
            "a/c", "acct", "account", "bank", "card", "debit", "credit", "upi", "vpa", "xx", "no.", "number",
            "saving", "savings", "current", "wallet", "cashback", "alert", "otp", "notification"
        )

        fun cleanAndValidateMerchant(candidate: String): String? {
            var name = candidate.trim()
            if (name.isBlank()) return null
            
            // Clean common VPA/reference suffixes
            name = cleanVpa(name)
            
            val lowerName = name.lowercase(Locale.ROOT)
            
            // If it consists entirely of digits, it's not a merchant name (e.g. account numbers or ref numbers)
            if (name.all { it.isDigit() }) return null
            
            // If it starts with generic terms like "a/c", "account", or matches ignored terms, reject
            val words = lowerName.split(" ").filter { it.isNotBlank() }
            if (words.isEmpty()) return null
            if (words[0] in ignoredTerms || (words.size > 1 && words[0] == "your" && words[1] in ignoredTerms)) {
                return null
            }
            
            // If the name is too short or is an account mask (e.g. xx1234)
            if (lowerName.contains(Regex("xx+\\d+")) || lowerName.contains(Regex("x{3,}"))) {
                return null
            }

            return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        if (type == "DEBIT") {
            // Priority patterns for debit
            val patterns = listOf(
                Regex("(?:paid|sent|transfer(?:red)?|txn|pay|payment)\\s+to\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE),
                Regex("at\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE),
                Regex("(?:vpa|to\\s+vpa)\\s+([a-zA-Z0-9\\s.@]{3,35})", RegexOption.IGNORE_CASE),
                Regex("to\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE) // Fallback generic "to"
            )
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val candidate = match.groupValues[1]
                    val cleaned = cleanAndValidateMerchant(candidate)
                    if (cleaned != null) {
                        merchant = cleaned
                        break
                    }
                }
            }
        } else { // CREDIT
            // Priority patterns for credit
            val patterns = listOf(
                Regex("(?:received|credited|transfer(?:red)?|remittance|refund)\\s+from\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE),
                Regex("from\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE),
                Regex("credited\\s+by\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE),
                Regex("by\\s+([a-zA-Z0-9\\s.]{3,30})", RegexOption.IGNORE_CASE) // Fallback generic "by"
            )
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val candidate = match.groupValues[1]
                    val cleaned = cleanAndValidateMerchant(candidate)
                    if (cleaned != null) {
                        merchant = cleaned
                        break
                    }
                }
            }
        }

        // If no merchant was extracted cleanly, fallback to the sender ID/Title (excluding bank name prefixes)
        if (merchant.isBlank()) {
            merchant = title.ifBlank { "Unknown Merchant" }
            if (merchant.contains(Regex("[a-zA-Z]{2}-[a-zA-Z]+", RegexOption.IGNORE_CASE))) {
                val parts = merchant.split("-")
                if (parts.size > 1) {
                    merchant = parts[1].replace(Regex("bk$|bank$", RegexOption.IGNORE_CASE), "")
                }
            }
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