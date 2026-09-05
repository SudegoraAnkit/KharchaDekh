package com.ankitsudegora.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getCurrencySymbol
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.theme.*
import com.ankitsudegora.viewmodel.AnalyticsState
import com.ankitsudegora.viewmodel.ForecastAllowance
import com.ankitsudegora.viewmodel.TimeboxFilter

@Composable
fun MetricsSummarySection(
    analytics: AnalyticsState,
    categories: List<Category>,
    monthlyIncome: Double,
    savingsTargetPct: Int,
    spendingTargetPct: Int,
    selectedFilter: TimeboxFilter,
    onFilterSelected: ((TimeboxFilter) -> Unit)? = null,
    monthlyCategorySpends: Map<Long, Double>,
    forecastAllowance: ForecastAllowance,
    currencySymbol: String
) {
    val totalExpense = analytics.totalExpense
    val totalActualSpent = monthlyCategorySpends.values.sum()
    val monthlySpent = if (monthlyIncome > 0.0) forecastAllowance.discretionarySpent else totalActualSpent

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("metrics_card")
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            // Timebox filter chips row inside Hero Card
            if (onFilterSelected != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeboxFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandLimeContainer else DarkSurfaceVariant)
                                .clickable { onFilterSelected(filter) }
                                .padding(vertical = 6.dp)
                                .testTag("filter_${filter.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (filter) {
                                    TimeboxFilter.DAILY -> "Daily"
                                    TimeboxFilter.WEEKLY -> "Weekly"
                                    TimeboxFilter.MONTHLY -> "Monthly"
                                    TimeboxFilter.YEARLY -> "Yearly"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) BrandOnLimeContainer else DarkOnSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = when (selectedFilter) {
                    TimeboxFilter.DAILY -> "Spent today"
                    TimeboxFilter.WEEKLY -> "Spent this week"
                    TimeboxFilter.MONTHLY -> "Spent this month"
                    TimeboxFilter.YEARLY -> "Spent this year"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = DarkOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Large Hero Financial Typography
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = currencySymbol,
                    style = MoneyTypography.HeroAmount.copy(color = BrandLime)
                )
                Text(
                    text = "%,.2f".format(totalExpense),
                    style = MoneyTypography.HeroAmount.copy(color = DarkOnSurface)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (monthlyIncome > 0.0) {
                val targetSavings = monthlyIncome * (savingsTargetPct / 100.0)
                val spendingCap = forecastAllowance.discretionarySpendingCap
                val currentSaved = monthlyIncome - totalActualSpent
                val isSavingsMet = currentSaved >= targetSavings
                val savingsColor = if (isSavingsMet) BrandLime else DarkError

                val capLeft = (spendingCap - monthlySpent).coerceAtLeast(0.0)
                val isOverSpent = monthlySpent > spendingCap
                val capColor = if (isOverSpent) DarkError else BrandLime

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Spending Cap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SPENDING CAP ($spendingTargetPct%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = DarkOnSurfaceVariant
                                )
                                Text(
                                    text = if (isOverSpent) "Exceeded" else "$currencySymbol${"%,.0f".format(capLeft)} Left",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = capColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(monthlySpent)} spent of $currencySymbol${"%,.0f".format(spendingCap)} limit",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (monthlySpent / spendingCap).coerceIn(0.0, 1.0).toFloat() },
                                color = capColor,
                                trackColor = DarkSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val totalCategoryBudget = categories.filter { it.budgetLimit != null }.sumOf { it.budgetLimit!! }
                    val actualBudgetLimit = if (totalCategoryBudget > 0.0) totalCategoryBudget else 25000.0
                    val budgetLeft = (actualBudgetLimit - monthlySpent).coerceAtLeast(0.0)

                    // Budget Left
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "BUDGET LEFT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = BrandLime
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(budgetLeft)}",
                                style = MoneyTypography.LargeAmount.copy(color = DarkOnSurface)
                            )
                        }
                    }

                    // Daily Avg
                    val dailyAvg = if (monthlySpent > 0) (monthlySpent / 30.0) else 0.0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "DAILY AVG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = DarkOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(dailyAvg)}",
                                style = MoneyTypography.LargeAmount.copy(color = DarkOnSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(analytics: AnalyticsState, currencySymbol: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkOnSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                analytics.categoryBreakdown.forEach { item ->
                    if (item.percentage > 0) {
                        val col = CategoryColors.getCategoryColor(item.category.name)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(item.percentage.coerceAtLeast(1f))
                                .background(col)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            analytics.categoryBreakdown.forEach { usage ->
                val color = CategoryColors.getCategoryColor(usage.category.name)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = usage.category.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = DarkOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "$currencySymbol${usage.amount.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Text(
                            text = "${usage.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingReviewCard(
    item: TransactionWithCategory,
    onVerifyClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    primaryCurrency: String = "INR",
    onConvertAmount: ((Double, String) -> Double)? = null
) {
    Card(
        onClick = onVerifyClicked,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandLimeContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_item_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BrandLimeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.transaction.source == "RECURRING") Icons.Default.Autorenew else Icons.Default.Sms,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    val amountText = if (item.transaction.currency.uppercase() != primaryCurrency.uppercase() && onConvertAmount != null) {
                        val converted = onConvertAmount(item.transaction.amount, item.transaction.currency)
                        "${getCurrencySymbol(primaryCurrency)}${"%,.2f".format(converted)} (${getCurrencySymbol(item.transaction.currency)}${item.transaction.amount})"
                    } else {
                        "${getCurrencySymbol(item.transaction.currency)}${item.transaction.amount}"
                    }
                    Text(
                        text = "$amountText • ${item.transaction.merchant}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    
                    val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    val timeStr = timeFormat.format(java.util.Date(item.transaction.timestamp))
                    Text(
                        text = if (item.transaction.source == "RECURRING") "Recurring Trigger • $timeStr" else "SMS: ${item.transaction.smsSenderId ?: "Alert"} • $timeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = DarkError.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onVerifyClicked,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }
    }
}




@Composable
fun SafeToSpendCard(
    allowance: ForecastAllowance,
    modifier: Modifier = Modifier,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("safe_to_spend_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (allowance.isOverspent) DarkErrorContainer else BrandLimeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allowance.isOverspent) Icons.Default.Warning else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (allowance.isOverspent) DarkError else BrandLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Safe to Spend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (allowance.isOverspent) DarkErrorContainer else BrandLimeContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (allowance.isOverspent) "Overspent" else "${allowance.remainingDays} days left",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (allowance.isOverspent) DarkOnErrorContainer else BrandOnLimeContainer
                    )
                }
            }

            if (allowance.isOverspent) {
                Text(
                    text = "You have exceeded your planned spending. Consider reducing non-essential expenses for the remaining ${allowance.remainingDays} days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkError
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DAILY LIMIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = BrandLime
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(allowance.dailyAllowance)}",
                                style = MoneyTypography.LargeAmount.copy(color = DarkOnSurface)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WEEKLY LIMIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = DarkOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(allowance.weeklyAllowance)}",
                                style = MoneyTypography.LargeAmount.copy(color = DarkOnSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CycleHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkOnSurface
                )
                Box(
                    modifier = Modifier
                        .background(color = BrandLimeContainer, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandOnLimeContainer
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = BrandLime
            )
        }
    }
}
