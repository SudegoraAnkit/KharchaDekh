package com.ankitsudegora.domain.usecase

import java.util.Calendar

data class SpendingPaceResult(
    val dailyPace: Double,
    val targetDailyBudget: Double,
    val isOnTrack: Boolean,
    val paceDifferencePct: Int,
    val daysRemaining: Int
)

/**
 * Pure Kotlin UseCase for calculating dynamic daily burn rate vs monthly allowance.
 */
class CalculateSpendingPaceUseCase {

    operator fun invoke(
        totalSpentThisCycle: Double,
        budgetLimit: Double,
        calendar: Calendar = Calendar.getInstance()
    ): SpendingPaceResult {
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = (maxDays - currentDay).coerceAtLeast(0)

        val dailyPace = totalSpentThisCycle / currentDay
        val targetDailyBudget = if (maxDays > 0 && budgetLimit > 0) budgetLimit / maxDays else 0.0

        val isOnTrack = dailyPace <= targetDailyBudget || targetDailyBudget == 0.0
        val diffPct = if (targetDailyBudget > 0.0) {
            (((dailyPace - targetDailyBudget) / targetDailyBudget) * 100).toInt()
        } else 0

        return SpendingPaceResult(
            dailyPace = dailyPace,
            targetDailyBudget = targetDailyBudget,
            isOnTrack = isOnTrack,
            paceDifferencePct = diffPct,
            daysRemaining = daysRemaining
        )
    }
}
