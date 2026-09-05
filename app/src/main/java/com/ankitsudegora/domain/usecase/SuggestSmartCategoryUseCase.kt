package com.ankitsudegora.domain.usecase

import com.ankitsudegora.data.TransactionWithCategory
import java.util.Calendar

/**
 * Pure Kotlin Domain UseCase for contextual category and merchant learning.
 * Encapsulates lunch hour heuristics (1-2 PM weekdays) and historical merchant matching.
 */
class SuggestSmartCategoryUseCase {

    operator fun invoke(
        amount: Double,
        merchant: String?,
        allTransactions: List<TransactionWithCategory>,
        calendar: Calendar = Calendar.getInstance()
    ): Pair<String, Int> {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekday = dow in Calendar.MONDAY..Calendar.FRIDAY

        // 1. Contextual Time-of-Day heuristic: 1-2 PM Weekday Lunch (₹50 - ₹300)
        if (isWeekday && hour in 13..14 && amount in 50.0..300.0) {
            return Pair("Food & Dining", 95)
        }

        // 2. Historical merchant match memory
        if (!merchant.isNullOrBlank()) {
            val normalized = merchant.trim().lowercase()
            val matchedTxn = allTransactions.firstOrNull {
                it.transaction.merchant.trim().lowercase() == normalized && it.category != null
            }
            if (matchedTxn?.category != null) {
                return Pair(matchedTxn.category.name, 90)
            }
        }

        // 3. Amount based heuristics
        return when {
            amount in 15.0..250.0 && hour in 8..10 -> Pair("Food & Dining", 85) // Breakfast / Chai
            amount in 500.0..3000.0 && (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) -> Pair("Entertainment", 75)
            else -> Pair("Others", 50)
        }
    }
}
