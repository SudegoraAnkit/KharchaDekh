package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.theme.*
import com.ankitsudegora.viewmodel.AnalyticsState
import com.ankitsudegora.viewmodel.ForecastAllowance
import com.ankitsudegora.viewmodel.TimeboxFilter

@Composable
fun InsightsScreen(
    analytics: AnalyticsState,
    categories: List<Category>,
    monthlyCategorySpends: Map<Long, Double>,
    forecastAllowance: ForecastAllowance,
    monthlyIncome: Double,
    selectedFilter: TimeboxFilter,
    onFilterSelected: (TimeboxFilter) -> Unit,
    onNavigateToCategories: () -> Unit,
    primaryCurrency: String = "INR"
) {
    val currencySymbol = remember(primaryCurrency) { getCurrencySymbol(primaryCurrency) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "Spending patterns, forecasts & budgets",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }

                // Timebox Filter Chips in Insights
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(TimeboxFilter.MONTHLY, TimeboxFilter.YEARLY).forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = { onFilterSelected(filter) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandLimeContainer else DarkSurfaceVariant,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = if (filter == TimeboxFilter.MONTHLY) "Month" else "Year",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) BrandOnLimeContainer else DarkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Safe-to-Spend Allowance
                if (monthlyIncome > 0.0) {
                    item {
                        SafeToSpendCard(forecastAllowance, currencySymbol = currencySymbol)
                    }
                }

                // Category Spending Breakdown
                if (analytics.categoryBreakdown.isNotEmpty()) {
                    item {
                        CategoryBreakdownSection(analytics, currencySymbol = currencySymbol)
                    }
                }

                // Budget Tracking Indicators
                item {
                    BudgetTrackingSection(
                        categories = categories,
                        monthlyCategorySpends = monthlyCategorySpends,
                        onNavigateToCategories = onNavigateToCategories,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}
