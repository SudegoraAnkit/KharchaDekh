package com.ankitsudegora.util

import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory

object CategoryClassifier {

    /**
     * Predicts and recommends a category for a given merchant name.
     * Looks at historical classification trends first, then falls back to keyword heuristics.
     */
    fun recommendCategory(
        merchant: String,
        allTransactions: List<TransactionWithCategory>,
        allCategories: List<Category>
    ): Category? {
        if (merchant.isBlank()) return null
        val cleanMerchant = merchant.trim().lowercase()

        // 1. Analyze historical transactions for this merchant name
        val matchingTxns = allTransactions.filter { 
            it.transaction.merchant.trim().lowercase() == cleanMerchant && 
            it.category != null 
        }
        
        if (matchingTxns.isNotEmpty()) {
            // Find the most frequent category for this merchant name
            val bestCategory = matchingTxns
                .groupBy { it.category!! }
                .maxByOrNull { it.value.size }
                ?.key
            if (bestCategory != null) return bestCategory
        }

        // 2. Fallback: Keyword Heuristics
        val matchKeywords = mapOf(
            "Food & Dining" to listOf("starbucks", "swiggy", "zomato", "restaurant", "food", "cafe", "dine", "pizza", "burger", "mcdonald", "kfc", "eats", "bakery", "dhaba"),
            "Groceries" to listOf("grocery", "groceries", "dmart", "store", "supermarket", "mart", "blinkit", "zepto", "bigbasket", "milk", "vegetables", "fruits", "provision"),
            "Rent & Maintenance" to listOf("rent", "maintenance", "landlord", "broker", "apartment", "society", "plumber", "electrician"),
            "Fuel & Travel" to listOf("uber", "ola", "metro", "auto", "train", "cab", "travel", "fuel", "petrol", "shell", "hpcl", "bpcl", "flight", "irctc", "bus", "railway", "transit"),
            "Shopping" to listOf("shopping", "amazon", "flipkart", "myntra", "meesho", "clothing", "apparel", "shoes", "mall", "electronics"),
            "Bills & Utilities" to listOf("electricity", "power", "recharge", "airtel", "water", "gas", "jio", "vi", "bsnl", "broadband", "wifi", "bill", "invoice", "insurance", "premium"),
            "Entertainment" to listOf("netflix", "spotify", "youtube", "amazon prime", "movie", "cinema", "theatre", "bookmyshow", "hotstar", "zee5", "subscription", "gaming", "playstation", "xbox", "steam"),
            "Health & Medical" to listOf("medical", "medicine", "pharmacy", "hospital", "doctor", "clinic", "health", "dental", "pharmeasy", "apollo", "1mg", "diagnostics"),
            "EMI & Loans" to listOf("emi", "loan", "mortgage", "hdfc bank", "icici bank", "sbi bank", "axis bank", "repayment", "finance")
        )

        for ((catName, keywords) in matchKeywords) {
            if (keywords.any { cleanMerchant.contains(it) }) {
                val found = allCategories.find { it.name.equals(catName, ignoreCase = true) }
                if (found != null) return found
            }
        }

        // Default fallback to "Others" if nothing matches
        // But if that's not found, return the first available category
        return allCategories.find { it.name.equals("Others", ignoreCase = true) } ?: allCategories.firstOrNull()
    }
}
