package com.ankitsudegora.domain.usecase

data class NetBalanceResult(
    val netBalance: Double,
    val effectiveBudget: Double,
    val isOverBudget: Boolean,
    val statusSubtitle: String
)

/**
 * Pure Kotlin UseCase for unified ledger net balance and budget remaining calculations.
 */
class CalculateNetBalanceUseCase {

    operator fun invoke(
        monthlyIncomeBudget: Double,
        categoryBudgetsSum: Double,
        cycleSpent: Double,
        cycleIncome: Double,
        currencySymbol: String = "₹"
    ): NetBalanceResult {
        val effectiveBudget = if (monthlyIncomeBudget > 0.0) monthlyIncomeBudget else categoryBudgetsSum

        val netBalance = if (effectiveBudget > 0.0) {
            effectiveBudget - cycleSpent
        } else if (cycleIncome > 0.0) {
            cycleIncome - cycleSpent
        } else {
            -cycleSpent
        }

        val isOver = netBalance < 0
        val statusSubtitle = if (effectiveBudget > 0.0) {
            if (netBalance >= 0) "Remaining of $currencySymbol${"%,.0f".format(effectiveBudget)}"
            else "Over by $currencySymbol${"%,.0f".format(kotlin.math.abs(netBalance))}"
        } else if (cycleIncome > 0.0) {
            if (netBalance >= 0) "+$currencySymbol${"%,.0f".format(netBalance)} saved"
            else "-$currencySymbol${"%,.0f".format(kotlin.math.abs(netBalance))} over"
        } else {
            "Net Outflow"
        }

        return NetBalanceResult(
            netBalance = netBalance,
            effectiveBudget = effectiveBudget,
            isOverBudget = isOver,
            statusSubtitle = statusSubtitle
        )
    }
}
