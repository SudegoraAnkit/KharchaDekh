package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.theme.*

@Composable
fun ActivityScreen(
    allTransactions: List<TransactionWithCategory>,
    categories: List<Category>,
    billingCycleStartDay: Int,
    onEnrichTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onSearchClicked: () -> Unit,
    primaryCurrency: String,
    onConvertAmount: (Double, String) -> Double
) {
    val currencySymbol = remember(primaryCurrency) { getCurrencySymbol(primaryCurrency) }

    val currentBillingCycleStart = remember(billingCycleStartDay) {
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        if (currentDay >= billingCycleStartDay) {
            val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, billingCycleStartDay.coerceAtMost(maxDays))
        } else {
            calendar.add(java.util.Calendar.MONTH, -1)
            val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, billingCycleStartDay.coerceAtMost(maxDays))
        }
        calendar.timeInMillis
    }

    var isCurrentCycleExpanded by remember { mutableStateOf(true) }
    var isPreviousCycleExpanded by remember { mutableStateOf(true) }

    val nonPendingTxns = remember(allTransactions) {
        allTransactions.filter { !it.transaction.isPending }
    }
    val currentCycleTxns = remember(nonPendingTxns, currentBillingCycleStart) {
        nonPendingTxns.filter { it.transaction.timestamp >= currentBillingCycleStart }
    }
    val previousCycleTxns = remember(nonPendingTxns, currentBillingCycleStart) {
        nonPendingTxns.filter { it.transaction.timestamp < currentBillingCycleStart }
    }

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
                        text = "Activity",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "${nonPendingTxns.size} recorded ledger entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("activity_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DarkOnSurface
                        )
                    }

                    IconButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Export CSV",
                            tint = BrandLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("export_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = BrandLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (nonPendingTxns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recorded transactions yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Captured bank alerts and logged expenses will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (currentCycleTxns.isNotEmpty()) {
                        item {
                            ActivityCycleHeader(
                                title = "Current Billing Cycle",
                                count = currentCycleTxns.size,
                                isExpanded = isCurrentCycleExpanded,
                                onToggle = { isCurrentCycleExpanded = !isCurrentCycleExpanded }
                            )
                        }
                        if (isCurrentCycleExpanded) {
                            items(currentCycleTxns, key = { "item_curr_${it.transaction.id}" }) { item ->
                                TransactionListItem(
                                    item = item,
                                    onEditClicked = { onEnrichTransaction(item.transaction.id) },
                                    onDeleteClicked = { onDeleteTransaction(item.transaction) },
                                    primaryCurrency = primaryCurrency,
                                    onConvertAmount = onConvertAmount
                                )
                            }
                        }
                    }

                    if (previousCycleTxns.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            ActivityCycleHeader(
                                title = "Previous Transactions",
                                count = previousCycleTxns.size,
                                isExpanded = isPreviousCycleExpanded,
                                onToggle = { isPreviousCycleExpanded = !isPreviousCycleExpanded }
                            )
                        }
                        if (isPreviousCycleExpanded) {
                            items(previousCycleTxns, key = { "item_prev_${it.transaction.id}" }) { item ->
                                TransactionListItem(
                                    item = item,
                                    onEditClicked = { onEnrichTransaction(item.transaction.id) },
                                    onDeleteClicked = { onDeleteTransaction(item.transaction) },
                                    primaryCurrency = primaryCurrency,
                                    onConvertAmount = onConvertAmount
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCycleHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() },
        color = DarkSurfaceElevated
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandLimeContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = BrandOnLimeContainer
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = DarkOnSurfaceVariant
            )
        }
    }
}