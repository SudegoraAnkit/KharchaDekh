package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
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
import com.ankitsudegora.ui.components.getCurrencySymbol
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
    onConvertAmount: (Double, String) -> Double,
    onNavigateToCategories: () -> Unit = {}
) {
    val currencySymbol = remember(primaryCurrency) { getCurrencySymbol(primaryCurrency) }
    var showExportFilterModal by remember { mutableStateOf(false) }

    val cal = remember { java.util.Calendar.getInstance() }
    val thisMonth = remember { cal.get(java.util.Calendar.MONTH) }
    val thisYear = remember { cal.get(java.util.Calendar.YEAR) }

    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, DEBIT, CREDIT, TRANSFER, REFUND
    var collapsedDays by remember { mutableStateOf(setOf<String>()) }

    val nonPendingTxns = remember(allTransactions) {
        allTransactions.filter { !it.transaction.isPending }
    }

    // Filtered by type
    val filteredTxns = remember(nonPendingTxns, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "DEBIT" -> nonPendingTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            "CREDIT" -> nonPendingTxns.filter { it.transaction.type.equals("CREDIT", ignoreCase = true) }
            "TRANSFER" -> nonPendingTxns.filter { it.category?.name?.contains("Transfer", ignoreCase = true) == true || it.transaction.merchant.contains("Transfer", ignoreCase = true) }
            "REFUND" -> nonPendingTxns.filter { it.category?.name?.contains("Refund", ignoreCase = true) == true || it.transaction.merchant.contains("Refund", ignoreCase = true) }
            else -> nonPendingTxns
        }
    }

    // This month vs last month transactions for dynamic MoM metrics
    val (thisMonthTxns, lastMonthTxns) = remember(nonPendingTxns, thisMonth, thisYear) {
        val thisM = mutableListOf<TransactionWithCategory>()
        val lastM = mutableListOf<TransactionWithCategory>()
        val lastMonthIdx = if (thisMonth == 0) 11 else thisMonth - 1
        val lastMonthYear = if (thisMonth == 0) thisYear - 1 else thisYear

        nonPendingTxns.forEach { txn ->
            val tCal = java.util.Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
            val m = tCal.get(java.util.Calendar.MONTH)
            val y = tCal.get(java.util.Calendar.YEAR)
            if (m == thisMonth && y == thisYear) thisM.add(txn)
            else if (m == lastMonthIdx && y == lastMonthYear) lastM.add(txn)
        }
        Pair(thisM, lastM)
    }

    // Compute 4 Metrics
    val totalSpent = remember(thisMonthTxns) {
        thisMonthTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            .sumOf { onConvertAmount(it.transaction.amount, it.transaction.currency) }
    }
    val lastMonthSpent = remember(lastMonthTxns) {
        lastMonthTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            .sumOf { onConvertAmount(it.transaction.amount, it.transaction.currency) }
    }
    val momSpentPct = remember(totalSpent, lastMonthSpent) {
        if (lastMonthSpent > 0) (((totalSpent - lastMonthSpent) / lastMonthSpent) * 100).toInt() else 0
    }

    val totalIncome = remember(thisMonthTxns) {
        thisMonthTxns.filter { it.transaction.type.equals("CREDIT", ignoreCase = true) }
            .sumOf { onConvertAmount(it.transaction.amount, it.transaction.currency) }
    }
    val lastMonthIncome = remember(lastMonthTxns) {
        lastMonthTxns.filter { it.transaction.type.equals("CREDIT", ignoreCase = true) }
            .sumOf { onConvertAmount(it.transaction.amount, it.transaction.currency) }
    }
    val momIncomePct = remember(totalIncome, lastMonthIncome) {
        if (lastMonthIncome > 0) (((totalIncome - lastMonthIncome) / lastMonthIncome) * 100).toInt() else 0
    }

    val netBalance = totalIncome - totalSpent
    val totalCount = thisMonthTxns.size

    // Group transactions by day
    val groupedByDay = remember(filteredTxns) {
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(yesterdayCal.time)
        val dayFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

        filteredTxns
            .sortedByDescending { it.transaction.timestamp }
            .groupBy {
                val txnDate = java.util.Date(it.transaction.timestamp)
                val txnDay = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(txnDate)
                when (txnDay) {
                    todayStr -> "Today"
                    yesterdayStr -> "Yesterday"
                    else -> dayFormat.format(txnDate)
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header (Matches Activity.jpeg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            ),
                            color = DarkOnSurface
                        )
                        Text("📈", fontSize = 20.sp)
                    }
                    Text(
                        text = "Track all your transactions in one place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("activity_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DarkOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Filter / Export Action
                    IconButton(
                        onClick = { showExportFilterModal = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = DarkOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Category Grid Icon Button (Green square as in Mockup)
                    IconButton(
                        onClick = onNavigateToCategories,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandLime)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Categories",
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 1. 4 Top Metric Summary Cards (Matches Activity.jpeg 1:1)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Total Spent
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "$currencySymbol${"%,.0f".format(totalSpent)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = DarkOnSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("This Month", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                                Text(
                                    text = if (momSpentPct >= 0) "↗ ${momSpentPct}% vs last month" else "↓ ${kotlin.math.abs(momSpentPct)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = if (momSpentPct >= 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Card 2: Total Income
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "$currencySymbol${"%,.0f".format(totalIncome)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = DarkOnSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("This Month", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                                Text(
                                    text = if (momIncomePct >= 0) "↑ ${momIncomePct}% vs last month" else "↓ ${kotlin.math.abs(momIncomePct)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = if (momIncomePct >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 3: Net Balance
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "$currencySymbol${"%,.0f".format(netBalance)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (netBalance >= 0) BrandLime else Color(0xFFEF4444)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                                Text("👛", fontSize = 11.sp)
                            }
                        }
                    }

                    // Card 4: Transactions
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Transactions", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = DarkOnSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("This Month", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                                Text("🧾", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Filter Pills Row with Animated Color Selection (Matches Activity.jpeg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterItems = listOf(
                    Triple("ALL", "All", null),
                    Triple("DEBIT", "Expense", "↑"),
                    Triple("CREDIT", "Income", "↓"),
                    Triple("TRANSFER", "Transfer", "⇄"),
                    Triple("REFUND", "Refund", "↺")
                )
                filterItems.forEach { (filterKey, label, symbol) ->
                    val isSel = selectedTypeFilter == filterKey
                    val animatedBg by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSel) BrandLime else DarkSurface,
                        label = "pill_bg"
                    )
                    val animatedText by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSel) DarkBackground else DarkOnSurfaceVariant,
                        label = "pill_text"
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = animatedBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSel) BrandLime else DarkBorder
                        ),
                        modifier = Modifier.clickable { selectedTypeFilter = filterKey }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (symbol != null) {
                                Text(
                                    text = symbol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedText
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = animatedText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Day-Wise Collapsible Ledger Groups (Matches Activity.jpeg 1:1)
            if (filteredTxns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching the filter tab or add a new transaction.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedByDay.forEach { (dayLabel, dayTxns) ->
                        val daySpent = dayTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
                            .sumOf { onConvertAmount(it.transaction.amount, it.transaction.currency) }
                        val isCollapsed = collapsedDays.contains(dayLabel)

                        item(key = "header_$dayLabel") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        collapsedDays = if (isCollapsed) {
                                            collapsedDays - dayLabel
                                        } else {
                                            collapsedDays + dayLabel
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = DarkOnSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (daySpent > 0) {
                                            Text(
                                                text = "₹${"%,.2f".format(daySpent)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = DarkOnSurface
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Expand/Collapse",
                                            tint = DarkOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isCollapsed) {
                            items(dayTxns, key = { "item_${it.transaction.id}" }) { item ->
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

    // Modal Sheet for Export & Filters
    if (showExportFilterModal) {
        AlertDialog(
            onDismissRequest = { showExportFilterModal = false },
            title = { Text("Export Ledger Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Download the complete ledger records for analysis or tax reporting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showExportFilterModal = false
                            onExportCsv()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export as CSV Spreadsheet", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showExportFilterModal = false
                            onExportPdf()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export as PDF Report", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFilterModal = false }) {
                    Text("Close", color = DarkOnSurfaceVariant)
                }
            },
            containerColor = DarkSurface
        )
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