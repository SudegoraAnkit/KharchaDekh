package com.ankitsudegora

import androidx.compose.ui.test.junit4.createComposeRule
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.ui.screens.EnrichmentScreen
import com.ankitsudegora.ui.theme.KharchaDekhTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnrichmentScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testEnrichmentScreenRenders() {
        val categories = listOf(
            Category(id = 1, name = "Food", iconResName = "restaurant"),
            Category(id = 2, name = "Others", iconResName = "category")
        )
        val txn = Transaction(
            id = 1,
            amount = 100.0,
            type = "DEBIT",
            merchant = "Test Merchant",
            categoryId = 1,
            paymentMethod = "CASH",
            isPending = false,
            source = "MANUAL",
            timestamp = System.currentTimeMillis()
        )
        composeTestRule.setContent {
            KharchaDekhTheme {
                EnrichmentScreen(
                    transactionId = 1,
                    categories = categories,
                    onGetTransaction = { txn },
                    onFinalizeTransaction = { _, _, _, _, _, _, _ -> },
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
