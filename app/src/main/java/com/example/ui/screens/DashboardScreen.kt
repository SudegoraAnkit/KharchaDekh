package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionWithCategory
import com.example.ui.components.getIconVector
import com.example.viewmodel.AnalyticsState
import com.example.viewmodel.TimeboxFilter

@Composable
fun DashboardScreen(
    userName: String,
    analytics: AnalyticsState,
    pendingTransactions: List<TransactionWithCategory>,
    allTransactions: List<TransactionWithCategory>,
    categories: List<com.example.data.Category>,
    selectedFilter: TimeboxFilter,
    onFilterSelected: (TimeboxFilter) -> Unit,
    onEnrichTransaction: (Long) -> Unit,
    onDeleteTransaction: (com.example.data.Transaction) -> Unit,
    onNavigateToCategories: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    val initials = remember(userName) {
        userName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "U" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_feed"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header or brief greeting
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KHARCHADEKH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Namaste, $userName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingTransactions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW ALERT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Time-box filters row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeboxFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    TimeboxFilter.DAILY -> "Daily"
                                    TimeboxFilter.WEEKLY -> "Weekly"
                                    TimeboxFilter.MONTHLY -> "Monthly"
                                    TimeboxFilter.YEARLY -> "Yearly"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_${filter.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Metrics Summary Card
        item {
            MetricsSummarySection(analytics, categories)
        }

        // Breakdown Chart
        if (analytics.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownSection(analytics)
            }
        }

        // Budget tracking progress indicators
        item {
            BudgetTrackingSection(
                categories = categories,
                analytics = analytics,
                onNavigateToCategories = onNavigateToCategories
            )
        }

        // Outstanding Pending Section banner / horizontal scroll list, or inline alerts
        if (pendingTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Action Required (SMS Alerts to Finalize)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(pendingTransactions, key = { "pending_${it.transaction.id}" }) { item ->
                PendingReviewCard(
                    item = item,
                    onVerifyClicked = { onEnrichTransaction(item.transaction.id) },
                    onDeleteClicked = { onDeleteTransaction(item.transaction) }
                )
            }
        }

        // Recent Transaction History
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Export Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        val nonPendingTxns = allTransactions.filter { !it.transaction.isPending }
        if (nonPendingTxns.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "No transactions",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No finalized transactions found for this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(nonPendingTxns, key = { "item_${it.transaction.id}" }) { item ->
                TransactionListItem(
                    item = item,
                    onEditClicked = { onEnrichTransaction(item.transaction.id) },
                    onDeleteClicked = { onDeleteTransaction(item.transaction) }
                )
            }
        }
    }
}

@Composable
fun BudgetTrackingSection(
    categories: List<com.example.data.Category>,
    analytics: AnalyticsState,
    onNavigateToCategories: () -> Unit
) {
    val budgetedCategories = categories.filter { it.budgetLimit != null && it.budgetLimit > 0.0 }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_tracking_section")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(onClick = onNavigateToCategories) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (budgetedCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Track your monthly spends with limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        TextButton(
                            onClick = onNavigateToCategories
                        ) {
                            Text(
                                text = "Tap here to set spending budgets",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                budgetedCategories.forEach { category ->
                    // Find spent amount
                    val spentUsage = analytics.categoryBreakdown.find { it.category.id == category.id }
                    val spentVal = spentUsage?.amount ?: 0.0
                    val limit = category.budgetLimit ?: 0.0
                    val ratio = (spentVal / limit).coerceIn(0.0, 1.0).toFloat()
                    val percent = (spentVal / limit * 100).toInt()
                    
                    val isExceeded = spentVal >= limit
                    val isClose = spentVal >= 0.8 * limit && spentVal < limit
                    
                    val progressColor = when {
                        isExceeded -> MaterialTheme.colorScheme.error
                        isClose -> Color(0xFFF59E0B) // Amber
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("budget_row_${category.id}")
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(progressColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(category.iconResName),
                                        contentDescription = null,
                                        tint = progressColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${"%,.0f".format(spentVal)} / ₹${"%,.0f".format(limit)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.12f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$percent% of monthly budget spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            if (isExceeded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Over budget!",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else if (isClose) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Close to limit",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsSummarySection(analytics: AnalyticsState, categories: List<com.example.data.Category>) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("metrics_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))) {
            // Decorative background circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .size(110.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
            )

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Spent this month",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "%,.2f".format(analytics.totalExpense),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Budget Left
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "BUDGET LEFT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val totalCategoryBudget = categories.filter { it.budgetLimit != null }.sumOf { it.budgetLimit!! }
                            val actualBudgetLimit = if (totalCategoryBudget > 0.0) totalCategoryBudget else 25000.0
                            val budgetLeft = (actualBudgetLimit - analytics.totalExpense).coerceAtLeast(0.0)
                            Text(
                                text = "₹${"%,.0f".format(budgetLeft)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Daily Avg
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "DAILY AVG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dailyAvg = if (analytics.totalExpense > 0) (analytics.totalExpense / 30.0) else 0.0
                            Text(
                                text = "₹${"%,.0f".format(dailyAvg)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(analytics: AnalyticsState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful scannable colored horizontal segmented distribution bar representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                // Color palette array
                val barColors = listOf(
                    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
                    Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFFFFCA28),
                    Color(0xFF78909C), Color(0xFF26C6DA), Color(0xFF9CCC65), Color(0xFF5C6BC0)
                )

                analytics.categoryBreakdown.forEachIndexed { index, item ->
                    if (item.percentage > 0) {
                        val col = barColors[index % barColors.size]
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(item.percentage.coerceAtLeast(1f))
                                .background(col)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // breakdown list details
            analytics.categoryBreakdown.forEachIndexed { index, usage ->
                val barColors = listOf(
                    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
                    Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFFFFCA28),
                    Color(0xFF78909C), Color(0xFF26C6DA), Color(0xFF9CCC65), Color(0xFF5C6BC0)
                )
                val color = barColors[index % barColors.size]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = getIconVector(usage.category.iconResName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = usage.category.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "₹${usage.amount.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${usage.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
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
    onDeleteClicked: () -> Unit
) {
    Card(
        onClick = onVerifyClicked,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
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
                // White Rounded Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS Alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${item.transaction.amount}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "at ${item.transaction.merchant}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    val timeStr = timeFormat.format(java.util.Date(item.transaction.timestamp))
                    Text(
                        text = "SMS: ${item.transaction.smsSenderId ?: "Alert"} • $timeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onVerifyClicked,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text(
                        text = "Categorize",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    item: TransactionWithCategory,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val isDebit = item.transaction.type == "DEBIT"
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDebit) {
        MaterialTheme.colorScheme.surface
    } else {
        if (isDark) Color(0xFF0F2D24) else Color(0xFFE6F9F6)
    }

    Card(
        onClick = onEditClicked,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${item.transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.5f)
            ) {
                // Category Icon with comfortable rounded background badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDebit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = item.category?.iconResName ?: "category"
                    Icon(
                        imageVector = getIconVector(icon),
                        contentDescription = item.category?.name ?: "Expense",
                        tint = if (isDebit) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = (item.category?.name ?: "Uncategorized") + 
                                if (!item.transaction.notes.isNullOrBlank()) " • " + item.transaction.notes else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Format Time
                    val date = java.util.Date(item.transaction.timestamp)
                    val format = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                    Text(
                        text = "${format.format(date)} via ${item.transaction.paymentMethod} (${item.transaction.source})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                val sign = if (isDebit) "-" else "+"
                val amountColor = if (isDebit) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    if (isDark) Color(0xFF4ADE80) else Color(0xFF0F766E)
                }
                
                Text(
                    text = "$sign ₹${"%,.2f".format(item.transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = amountColor,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
