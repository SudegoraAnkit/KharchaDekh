package com.ankitsudegora.util

import android.content.Context
import android.os.Build

object GeminiNanoParser {

    /**
     * Evaluates whether the device has native on-device AICore / Gemini Nano hardware capabilities.
     * Checks for flagship devices running Android 15/16+ where AICore services are bundled.
     */
    fun isAICoreAvailable(context: Context): Boolean {
        // Pixel 8 Pro, Pixel 9 Series, Samsung S24/S25, and other modern flagship device models support AICore
        val model = Build.MODEL.lowercase()
        val isSupportedFlagship = model.contains("pixel 8 pro") || 
                model.contains("pixel 9") || 
                model.contains("pixel 10") ||
                model.contains("samsung s24") || 
                model.contains("samsung s25") ||
                model.contains("sm-s92") || 
                model.contains("sm-s93")
        
        return Build.VERSION.SDK_INT >= 35 && isSupportedFlagship
    }

    /**
     * Simulates on-device local Gemini Nano prompt execution.
     * Uses zero-telemetry NLP token heuristics to parse text on-device safely.
     */
    suspend fun parseTransactionNotification(smsBody: String): ParsedTransaction? {
        val cleaned = smsBody.replace(Regex("\\s+"), " ").trim()
        
        // 1. Extract Amount
        // Look for Rs, INR, USD, EUR, etc. followed by amount
        val amountRegex = Regex("(?i)(?:rs\\.?|inr|usd|eur|¥|jpy|yan|amount)\\s*([\\d,]+\\.?\\d*)")
        val amountMatch = amountRegex.find(cleaned)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        // 2. Classify Transaction Type
        var type = "DEBIT"
        if (cleaned.contains("credited", ignoreCase = true) || 
            cleaned.contains("received", ignoreCase = true) || 
            cleaned.contains("refund", ignoreCase = true) ||
            cleaned.contains("deposited", ignoreCase = true)
        ) {
            type = "CREDIT"
        }

        // 3. Extract Merchant/Payee name using NLP heuristics
        var merchant = "Unknown Merchant"
        if (type == "DEBIT") {
            // Find common debit payee markers
            val patterns = listOf(
                Regex("(?i)spent\\s+at\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)"),
                Regex("(?i)paid\\s+to\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)"),
                Regex("(?i)transfer\\s+to\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)"),
                Regex("(?i)txn\\s+at\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)")
            )
            for (pattern in patterns) {
                val match = pattern.find(cleaned)
                if (match != null) {
                    val candidate = match.groupValues[1].trim()
                    if (candidate.isNotEmpty() && !candidate.contains("rs", ignoreCase = true)) {
                        merchant = candidate
                        break
                    }
                }
            }
        } else {
            // Find common credit sender markers
            val patterns = listOf(
                Regex("(?i)received\\s+from\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)"),
                Regex("(?i)credited\\s+by\\s+([^,.]+?)(\\s+on|\\s+via|\\s+using|\\s+for|\\s+ref|\\.)")
            )
            for (pattern in patterns) {
                val match = pattern.find(cleaned)
                if (match != null) {
                    val candidate = match.groupValues[1].trim()
                    if (candidate.isNotEmpty()) {
                        merchant = candidate
                        break
                    }
                }
            }
        }

        // Clean merchant name from VPA symbols or junk tokens
        merchant = SemanticTransactionParser.cleanVpa(merchant)

        // 4. Extract reference number
        val refRegex = Regex("(?i)(?:ref|txn|reference|utr)\\s*(?:no|num|id)?\\.?\\s*([\\da-z]+)", RegexOption.IGNORE_CASE)
        val refMatch = refRegex.find(cleaned)
        val refNumber = refMatch?.groupValues?.get(1)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            refNumber = refNumber,
            sender = "GeminiNano"
        )
    }
}
