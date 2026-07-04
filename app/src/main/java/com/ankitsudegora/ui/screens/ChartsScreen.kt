package com.ankitsudegora.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.viewmodel.AnalyticsState
import com.ankitsudegora.viewmodel.CategoryUsage
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyCashFlow(
    val monthLabel: String,
    val inflow: Double,
    val outflow: Double
)

data class CategoryTrendPoint(
    val cycleLabel: String,
    val amount: Double
)

// Dynamic Date Calculations
fun getBillingCycleRange(startDay: Int, cycleOffset: Int, now: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    if (currentDay >= startDay) {
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
    } else {
        calendar.add(Calendar.MONTH, -1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDays))
    }
    
    val startOfCurrent = calendar.timeInMillis
    
    calendar.timeInMillis = startOfCurrent
    calendar.add(Calendar.MONTH, cycleOffset)
    val maxDaysStart = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDaysStart))
    val startOfTarget = calendar.timeInMillis
    
    calendar.timeInMillis = startOfTarget
    calendar.add(Calendar.MONTH, 1)
    val maxDaysEnd = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDaysEnd))
    val endOfTarget = calendar.timeInMillis
    
    return Pair(startOfTarget, endOfTarget)
}

fun formatCycleRange(startMs: Long, endMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val startStr = sdf.format(Date(startMs))
    val endInclusive = Date(endMs - 24 * 60 * 60 * 1000)
    val endStr = sdf.format(endInclusive)
    return "$startStr - $endStr"
}

fun getCycleLabel(cycleOffset: Int): String {
    return when (cycleOffset) {
        0 -> "Current Cycle"
        -1 -> "Previous Cycle"
        -2 -> "2 Cycles Ago"
        -3 -> "3 Cycles Ago"
        -4 -> "4 Cycles Ago"
        -5 -> "5 Cycles Ago"
        else -> "All Time"
    }
}

@Composable
fun ChartsScreen(
    analytics: AnalyticsState,
    allTransactions: List<TransactionWithCategory>,
    billingCycleStartDay: Int
) {
    var selectedCycleOffset by remember { mutableStateOf(0) } // 0 = current, -1 = prev, ..., -100 = All Time
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) } // null = all inflow/outflow
    
    val animationProgress = remember { Animatable(0f) }
    
    // Reset and restart animation on cycle or filter change
    LaunchedEffect(selectedCycleOffset, selectedCategoryFilter) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val now = remember { System.currentTimeMillis() }
    
    // 1. Calculate the active date range dynamically
    val cycleRange = remember(selectedCycleOffset, billingCycleStartDay) {
        if (selectedCycleOffset == -100) {
            Pair(0L, Long.MAX_VALUE)
        } else {
            getBillingCycleRange(billingCycleStartDay, selectedCycleOffset, now)
        }
    }
    
    // 2. Filter transactions in the active cycle
    val cycleTxns = remember(allTransactions, cycleRange, selectedCycleOffset) {
        if (selectedCycleOffset == -100) {
            allTransactions.filter { !it.transaction.isPending }
        } else {
            allTransactions.filter { item ->
                !item.transaction.isPending && 
                item.transaction.timestamp >= cycleRange.first && 
                item.transaction.timestamp < cycleRange.second
            }
        }
    }

    // 3. Compute active cycle metrics
    val cycleInflow = remember(cycleTxns) {
        cycleTxns.filter { it.transaction.type == "CREDIT" }.sumOf { it.transaction.amount }
    }
    
    val cycleOutflow = remember(cycleTxns) {
        cycleTxns.filter { 
            it.transaction.type == "DEBIT" && 
            it.category?.name != "CreditCard Payment" 
        }.sumOf { it.transaction.amount }
    }

    val cycleBreakdown = remember(cycleTxns, cycleOutflow) {
        val categoryMap = mutableMapOf<Category, Double>()
        cycleTxns.filter { 
            it.transaction.type == "DEBIT" && 
            it.category?.name != "CreditCard Payment" 
        }.forEach { item ->
            val cat = item.category ?: Category(name = "Uncategorized", iconResName = "category")
            categoryMap[cat] = categoryMap.getOrDefault(cat, 0.0) + item.transaction.amount
        }
        categoryMap.map { (cat, amount) ->
            CategoryUsage(
                category = cat,
                amount = amount,
                percentage = if (cycleOutflow > 0) ((amount / cycleOutflow) * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }
    }

    // 4. Calculate trend chart values (last 6 billing cycles)
    val monthlyData = remember(allTransactions, billingCycleStartDay) {
        val result = mutableListOf<MonthlyCashFlow>()
        for (i in 5 downTo 0) {
            val range = getBillingCycleRange(billingCycleStartDay, -i, now)
            val txnsInCycle = allTransactions.filter { item ->
                !item.transaction.isPending &&
                item.transaction.timestamp >= range.first &&
                item.transaction.timestamp < range.second
            }
            var inflow = 0.0
            var outflow = 0.0
            txnsInCycle.forEach { item ->
                val amt = item.transaction.amount
                if (item.transaction.type == "CREDIT") {
                    inflow += amt
                } else if (item.transaction.type == "DEBIT" && item.category?.name != "CreditCard Payment") {
                    outflow += amt
                }
            }
            val cal = Calendar.getInstance().apply { timeInMillis = range.first }
            val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            result.add(MonthlyCashFlow(monthLabel, inflow, outflow))
        }
        result
    }

    // 5. Calculate monthly category trend values if a specific category is filtered
    val categoryTrendData = remember(allTransactions, selectedCategoryFilter, billingCycleStartDay) {
        val result = mutableListOf<CategoryTrendPoint>()
        val cat = selectedCategoryFilter ?: return@remember emptyList<CategoryTrendPoint>()
        for (i in 5 downTo 0) {
            val range = getBillingCycleRange(billingCycleStartDay, -i, now)
            val totalInCycle = allTransactions.filter { item ->
                !item.transaction.isPending &&
                item.transaction.timestamp >= range.first &&
                item.transaction.timestamp < range.second &&
                item.transaction.type == "DEBIT" &&
                item.category?.id == cat.id
            }.sumOf { it.transaction.amount }
            
            val cal = Calendar.getInstance().apply { timeInMillis = range.first }
            val label = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            result.add(CategoryTrendPoint(label, totalInCycle))
        }
        result
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics & Trends",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Cycle Picker Card
        item {
            CycleSelectorCard(
                selectedOffset = selectedCycleOffset,
                onOffsetChanged = { selectedCycleOffset = it },
                billingCycleStartDay = billingCycleStartDay,
                cycleRange = cycleRange,
                now = now
            )
        }

        // Summary Card
        item {
            CycleSummaryCard(
                inflow = cycleInflow,
                outflow = cycleOutflow,
                offset = selectedCycleOffset
            )
        }

        // 1. Doughnut Chart Card
        item {
            DoughnutChartCard(
                outflow = cycleOutflow,
                breakdown = cycleBreakdown,
                cycleTxns = cycleTxns,
                selectedOffset = selectedCycleOffset,
                billingCycleStartDay = billingCycleStartDay,
                now = now,
                allTransactions = allTransactions,
                animationProgress = animationProgress.value
            )
        }

        // 2. Inflow vs Outflow / Category Trend Card
        item {
            CustomCashFlowTrendCard(
                monthlyData = monthlyData,
                categoryTrendData = categoryTrendData,
                selectedCategory = selectedCategoryFilter,
                onCategorySelected = { selectedCategoryFilter = it },
                allCategories = allTransactions.mapNotNull { it.category }.distinctBy { it.id },
                animationProgress = animationProgress.value
            )
        }
    }
}

@Composable
fun CycleSelectorCard(
    selectedOffset: Int,
    onOffsetChanged: (Int) -> Unit,
    billingCycleStartDay: Int,
    cycleRange: Pair<Long, Long>,
    now: Long
) {
    var expandedMenu by remember { mutableStateOf(false) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (selectedOffset == -100) {
                        onOffsetChanged(0)
                    } else if (selectedOffset > -5) {
                        onOffsetChanged(selectedOffset - 1)
                    }
                },
                enabled = selectedOffset != -100 && selectedOffset > -5
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Cycle"
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expandedMenu = true }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = getCycleLabel(selectedOffset),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = if (selectedOffset == -100) "All recorded transactions" else formatCycleRange(cycleRange.first, cycleRange.second),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    for (offset in 0 downTo -5) {
                        val range = getBillingCycleRange(billingCycleStartDay, offset, now)
                        val rangeLabel = formatCycleRange(range.first, range.second)
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(getCycleLabel(offset), fontWeight = FontWeight.Bold)
                                    Text(rangeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = {
                                onOffsetChanged(offset)
                                expandedMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("All Time", fontWeight = FontWeight.Bold) },
                        onClick = {
                            onOffsetChanged(-100)
                            expandedMenu = false
                        }
                    )
                }
            }

            IconButton(
                onClick = {
                    if (selectedOffset < 0) {
                        onOffsetChanged(selectedOffset + 1)
                    }
                },
                enabled = selectedOffset < 0
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Cycle"
                )
            }
        }
    }
}

@Composable
fun CycleSummaryCard(
    inflow: Double,
    outflow: Double,
    offset: Int
) {
    val netBalance = inflow - outflow
    val isSaving = netBalance >= 0.0
    val savedColor = if (isSaving) Color(0xFF4CAF50) else Color(0xFFEF5350)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = borderStroke(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Inflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%,.0f".format(inflow)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CAF50)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Outflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%,.0f".format(outflow)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF5350)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (offset == -100) "Net Saved" else "Cycle Net",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (isSaving) "+" else ""}₹${"%,.0f".format(netBalance)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = savedColor
                )
            }
        }
    }
}

@Composable
fun DoughnutChartCard(
    outflow: Double,
    breakdown: List<CategoryUsage>,
    cycleTxns: List<TransactionWithCategory>,
    selectedOffset: Int,
    billingCycleStartDay: Int,
    now: Long,
    allTransactions: List<TransactionWithCategory>,
    animationProgress: Float
) {
    val chartColors = listOf(
        Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
        Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFFFFCA28),
        Color(0xFF78909C), Color(0xFF26C6DA), Color(0xFF9CCC65), Color(0xFF5C6BC0)
    )
    
    var expandedCategoryName by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Expense Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (outflow <= 0 || breakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No outflow transactions to display",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        breakdown.forEachIndexed { index, item ->
                            if (item.percentage > 0) {
                                val sweepAngle = item.percentage * 3.6f * animationProgress
                                val color = chartColors[index % chartColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 45f, cap = StrokeCap.Round),
                                    size = Size(size.width - 45f, size.height - 45f),
                                    topLeft = Offset(22.5f, 22.5f)
                                )
                                startAngle += item.percentage * 3.6f
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Outflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${"%,.0f".format(outflow)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive breakdown list representation
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    breakdown.forEachIndexed { index, usage ->
                        val color = chartColors[index % chartColors.size]
                        val isExpanded = expandedCategoryName == usage.category.name
                        
                        CategoryRowItem(
                            usage = usage,
                            color = color,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCategoryName = if (isExpanded) null else usage.category.name
                            },
                            cycleTxns = cycleTxns,
                            billingCycleStartDay = billingCycleStartDay,
                            now = now,
                            allTransactions = allTransactions
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRowItem(
    usage: CategoryUsage,
    color: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cycleTxns: List<TransactionWithCategory>,
    billingCycleStartDay: Int,
    now: Long,
    allTransactions: List<TransactionWithCategory>
) {
    val categoryTxns = remember(cycleTxns, usage.category) {
        cycleTxns.filter { 
            it.transaction.type == "DEBIT" && 
            it.category?.id == usage.category.id 
        }.sortedByDescending { it.transaction.timestamp }
    }
    
    val txnCount = categoryTxns.size
    val averageSpend = if (txnCount > 0) usage.amount / txnCount else 0.0

    // Gather last 6 cycles data for this category for the micro-trend chart
    val categoryHistory = remember(allTransactions, usage.category, billingCycleStartDay) {
        val trendPoints = mutableListOf<Double>()
        for (i in 5 downTo 0) {
            val range = getBillingCycleRange(billingCycleStartDay, -i, now)
            val amt = allTransactions.filter { item ->
                !item.transaction.isPending &&
                item.transaction.timestamp >= range.first &&
                item.transaction.timestamp < range.second &&
                item.transaction.type == "DEBIT" &&
                item.category?.id == usage.category.id
            }.sumOf { it.transaction.amount }
            trendPoints.add(amt)
        }
        trendPoints
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = borderStroke(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = usage.category.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${"%,.0f".format(usage.amount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${usage.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Quick Analytics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$txnCount times", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg. Expense size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("₹${"%,.0f".format(averageSpend)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Mini Category Trend Chart
                    val maxVal = remember(categoryHistory) {
                        val max = categoryHistory.maxOrNull() ?: 1.0
                        if (max > 0) max else 1.0
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Spend Trend (Last 6 Cycles)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            categoryHistory.forEachIndexed { i, value ->
                                val pctHeight = (value / maxVal).toFloat()
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .fillMaxHeight(pctHeight.coerceIn(0.08f, 1f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }

                    // Individual Transaction Entries
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Itemized History",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (categoryTxns.isEmpty()) {
                            Text(
                                text = "No itemized records found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            val sdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                            categoryTxns.take(5).forEach { txn ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = txn.transaction.merchant.ifEmpty { "Cash Expense" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = sdf.format(Date(txn.transaction.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Text(
                                        text = "₹${"%,.0f".format(txn.transaction.amount)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (categoryTxns.size > 5) {
                                Text(
                                    text = "Showing recent 5 entries • and ${categoryTxns.size - 5} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
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
fun CustomCashFlowTrendCard(
    monthlyData: List<MonthlyCashFlow>,
    categoryTrendData: List<CategoryTrendPoint>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    allCategories: List<Category>,
    animationProgress: Float
) {
    val density = LocalDensity.current
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val textLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    
    val greenColor = Color(0xFF4CAF50)
    val redColor = Color(0xFFEF5350)
    val primaryColor = MaterialTheme.colorScheme.primary
    
    var expandedDropdown by remember { mutableStateOf(false) }

    val maxVal = remember(monthlyData, categoryTrendData, selectedCategory) {
        val maxAmount = if (selectedCategory == null) {
            monthlyData.maxOfOrNull { maxOf(it.inflow, it.outflow) } ?: 0.0
        } else {
            categoryTrendData.maxOfOrNull { it.amount } ?: 0.0
        }
        if (maxAmount > 0.0) maxAmount else 10000.0
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == null) "Cash Flow Trend" else "${selectedCategory.name} Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Trend Filter Action Dropdown
                Box {
                    IconButton(onClick = { expandedDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter trend chart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Cash Flow", fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                onCategorySelected(null)
                                expandedDropdown = false
                            }
                        )
                        HorizontalDivider()
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    onCategorySelected(cat)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCategory == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenColor))
                        Text("Inflow (Credits)", style = MaterialTheme.typography.bodySmall, color = textLabelColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(redColor))
                        Text("Outflow (Debits)", style = MaterialTheme.typography.bodySmall, color = textLabelColor)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Text("Monthly Spends", style = MaterialTheme.typography.bodySmall, color = textLabelColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val yLabelWidth = with(density) { 36.dp.toPx() }
                    val xLabelHeight = with(density) { 24.dp.toPx() }
                    
                    val plotWidth = canvasWidth - yLabelWidth
                    val plotHeight = canvasHeight - xLabelHeight

                    // Draw horizontal grid lines
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val y = i * (plotHeight / gridLinesCount)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(yLabelWidth, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 2f
                        )
                    }

                    // Draw columns
                    if (selectedCategory == null) {
                        val columnsCount = monthlyData.size
                        val colWidth = plotWidth / columnsCount
                        val barWidth = colWidth * 0.3f

                        monthlyData.forEachIndexed { index, data ->
                            val centerX = yLabelWidth + (index * colWidth) + (colWidth / 2)
                            
                            val inflowHeight = (data.inflow / maxVal) * plotHeight * animationProgress
                            val outflowHeight = (data.outflow / maxVal) * plotHeight * animationProgress

                            val inflowTop = plotHeight - inflowHeight
                            drawRect(
                                brush = Brush.verticalGradient(listOf(greenColor, greenColor.copy(alpha = 0.7f))),
                                topLeft = Offset(centerX - barWidth - 4f, inflowTop.toFloat()),
                                size = Size(barWidth, inflowHeight.toFloat())
                            )

                            val outflowTop = plotHeight - outflowHeight
                            drawRect(
                                brush = Brush.verticalGradient(listOf(redColor, redColor.copy(alpha = 0.7f))),
                                topLeft = Offset(centerX + 4f, outflowTop.toFloat()),
                                size = Size(barWidth, outflowHeight.toFloat())
                            )
                        }
                    } else {
                        val columnsCount = categoryTrendData.size
                        val colWidth = plotWidth / columnsCount
                        val barWidth = colWidth * 0.4f

                        categoryTrendData.forEachIndexed { index, data ->
                            val centerX = yLabelWidth + (index * colWidth) + (colWidth / 2)
                            val spendHeight = (data.amount / maxVal) * plotHeight * animationProgress
                            val spendTop = plotHeight - spendHeight

                            drawRect(
                                brush = Brush.verticalGradient(listOf(
                                    primaryColor, 
                                    primaryColor.copy(alpha = 0.7f)
                                )),
                                topLeft = Offset(centerX - (barWidth / 2), spendTop.toFloat()),
                                size = Size(barWidth, spendHeight.toFloat())
                            )
                        }
                    }
                }

                // Overlay month text labels cleanly
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .padding(start = 36.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val labels = if (selectedCategory == null) monthlyData.map { it.monthLabel } else categoryTrendData.map { it.cycleLabel }
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = textLabelColor,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Global theme helper
@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
)
