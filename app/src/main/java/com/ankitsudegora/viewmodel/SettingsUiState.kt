package com.ankitsudegora.viewmodel

/**
 * Immutable MVI UI State for Settings Screen.
 */
data class SettingsUiState(
    val userName: String = "User",
    val primaryCurrency: String = "INR",
    val monthlyIncome: Double = 0.0,
    val billingCycleStartDay: Int = 1,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 30,
    val isMultiCurrencyEnabled: Boolean = false,
    val autoBackupNight: Boolean = false,
    val appVersion: String = "2.0.0.1 (Build 23)",
    val isLoading: Boolean = false
)

/**
 * Sealed MVI UI Actions for Settings Screen.
 */
sealed interface SettingsUiAction {
    data class OnUpdateUserName(val name: String) : SettingsUiAction
    data class OnUpdateMonthlyIncome(val income: Double) : SettingsUiAction
    data class OnUpdateBillingCycleStartDay(val day: Int) : SettingsUiAction
    data class OnUpdateReminderTime(val hour: Int, val minute: Int) : SettingsUiAction
    data class OnToggleMultiCurrency(val enabled: Boolean) : SettingsUiAction
    data class OnToggleAutoBackup(val enabled: Boolean) : SettingsUiAction
    data class OnSelectPrimaryCurrency(val currency: String) : SettingsUiAction
}
