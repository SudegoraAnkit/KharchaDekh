package com.ankitsudegora.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.components.getCurrencySymbol
import com.ankitsudegora.ui.components.WhatChangedDetailSheet
import com.ankitsudegora.ui.components.WeeklyRecapStoryModal
import com.ankitsudegora.ui.components.WeeklyRecapManager
import com.ankitsudegora.ui.theme.*
import com.ankitsudegora.viewmodel.AnalyticsState
import com.ankitsudegora.viewmodel.ForecastAllowance
import com.ankitsudegora.viewmodel.TimeboxFilter
import java.text.SimpleDateFormat
import java.util.*

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
    allTransactions: List<TransactionWithCategory> = emptyList(),
    primaryCurrency: String = "INR"
) {
    val currencySymbol = remember(primaryCurrency) { getCurrencySymbol(primaryCurrency) }
    val cal = remember { Calendar.getInstance() }
    val currentMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    val currentDay = remember { cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
    val currentMonthShort = remember { SimpleDateFormat("MMM", Locale.getDefault()).format(Date()) }
    val totalDaysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val daysRemaining = remember { (totalDaysInMonth - currentDay).coerceAtLeast(1) }

    // Real dynamic monthly spending computations
    val curMonth = cal.get(Calendar.MONTH)
    val curYear = cal.get(Calendar.YEAR)
    val lastMonth = if (curMonth == 0) 11 else curMonth - 1
    val lastMonthYear = if (curMonth == 0) curYear - 1 else curYear

    val thisMonthTxns = remember(allTransactions, curMonth, curYear) {
        allTransactions.filter { txn ->
            txn.transaction.type == "DEBIT" && run {
                val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
                tCal.get(Calendar.MONTH) == curMonth && tCal.get(Calendar.YEAR) == curYear
            }
        }
    }

    val lastMonthTxns = remember(allTransactions, lastMonth, lastMonthYear) {
        allTransactions.filter { txn ->
            txn.transaction.type == "DEBIT" && run {
                val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
                tCal.get(Calendar.MONTH) == lastMonth && tCal.get(Calendar.YEAR) == lastMonthYear
            }
        }
    }

    val thisYearTxns = remember(allTransactions, curYear) {
        allTransactions.filter { txn ->
            txn.transaction.type == "DEBIT" && run {
                val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
                tCal.get(Calendar.YEAR) == curYear
            }
        }
    }
    val userBudget = if (monthlyIncome > 0) monthlyIncome else 30000.0
    val yearTotalSpent = remember(thisYearTxns) { thisYearTxns.sumOf { it.transaction.amount } }
    val yearBudget = userBudget * 12

    var showWhatChangedSheet by remember { mutableStateOf(false) }
    var showWeeklyStoryModal by remember { mutableStateOf(false) }
    var touchedDay by remember { mutableStateOf<Int?>(null) }

    val weeklyRecapData = remember(allTransactions) {
        WeeklyRecapManager.computeRecapData(allTransactions)
    }

    val paymentModeStats = remember(thisMonthTxns) {
        val total = thisMonthTxns.sumOf { it.transaction.amount }.coerceAtLeast(1.0)
        val upi = thisMonthTxns.filter { it.transaction.paymentMethod.equals("UPI", ignoreCase = true) }.sumOf { it.transaction.amount }
        val card = thisMonthTxns.filter { it.transaction.paymentMethod.equals("CARD", ignoreCase = true) || it.category?.name?.contains("CreditCard", ignoreCase = true) == true }.sumOf { it.transaction.amount }
        val cash = thisMonthTxns.filter { it.transaction.paymentMethod.equals("CASH", ignoreCase = true) }.sumOf { it.transaction.amount }
        Triple(((upi / total) * 100).toInt(), ((card / total) * 100).toInt(), ((cash / total) * 100).toInt())
    }

    val avgTicketSize = remember(thisMonthTxns) {
        if (thisMonthTxns.isNotEmpty()) (thisMonthTxns.sumOf { it.transaction.amount } / thisMonthTxns.size).toInt() else 0
    }

    val monthlyYearSpends = remember(thisYearTxns) {
        val map = (0..11).associateWith { 0.0 }.toMutableMap()
        thisYearTxns.forEach { txn ->
            val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
            val m = tCal.get(Calendar.MONTH)
            map[m] = (map[m] ?: 0.0) + txn.transaction.amount
        }
        map
    }

    val totalSpent = remember(thisMonthTxns, analytics.totalExpense) {
        val sum = thisMonthTxns.sumOf { it.transaction.amount }
        if (sum > 0) sum else analytics.totalExpense
    }
    val lastMonthTotalSpent = remember(lastMonthTxns) { lastMonthTxns.sumOf { it.transaction.amount } }

    val remainingBudget = (userBudget - totalSpent).coerceAtLeast(0.0)
    val progressPct = ((totalSpent / userBudget) * 100).toInt()
    val dailyLimit = remainingBudget / daysRemaining
    val currentDailyAvg = totalSpent / currentDay
    val expectedDailyPace = userBudget / totalDaysInMonth
    val isPaceOnTrack = currentDailyAvg <= expectedDailyPace
    val paceDifference = kotlin.math.abs((expectedDailyPace * currentDay) - totalSpent)

    // Month-over-Month Delta %
    val momPct = remember(totalSpent, lastMonthTotalSpent) {
        if (lastMonthTotalSpent > 0) {
            val delta = ((totalSpent - lastMonthTotalSpent) / lastMonthTotalSpent) * 100
            delta.toInt()
        } else 0
    }

    // Daily spending trend data for the line chart (1 to totalDaysInMonth)
    val dailySpendMap = remember(thisMonthTxns, totalDaysInMonth) {
        val map = mutableMapOf<Int, Double>()
        (1..totalDaysInMonth).forEach { map[it] = 0.0 }
        thisMonthTxns.forEach { txn ->
            val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
            val day = tCal.get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0.0) + txn.transaction.amount
        }
        map
    }

    val peakDayEntry = remember(dailySpendMap) {
        val nonZero = dailySpendMap.entries.filter { it.key <= currentDay && it.value > 0 }
        if (nonZero.isNotEmpty()) nonZero.maxByOrNull { it.value } else null
    }

    // Top categories and changes vs last month with real daily cumulative series for sparklines
    val categoryChanges = remember(thisMonthTxns, lastMonthTxns, categories, totalDaysInMonth) {
        val thisCatSpends = thisMonthTxns.groupBy { it.transaction.categoryId ?: -1L }
            .mapValues { it.value.sumOf { txn -> txn.transaction.amount } }
        val lastCatSpends = lastMonthTxns.groupBy { it.transaction.categoryId ?: -1L }
            .mapValues { it.value.sumOf { txn -> txn.transaction.amount } }
        val catMap = categories.associateBy { it.id }

        thisCatSpends.entries.sortedByDescending { it.value }.take(3).map { (catId, thisAmt) ->
            val lastAmt = lastCatSpends[catId] ?: 0.0
            val cat = catMap[catId]
            val pctDelta = if (lastAmt > 0) (((thisAmt - lastAmt) / lastAmt) * 100).toInt() else if (thisAmt > 0) 100 else 0
            val diff = thisAmt - lastAmt

            // Real daily cumulative spend series for sparkline
            val dailySeries = DoubleArray(totalDaysInMonth) { 0.0 }
            thisMonthTxns.filter { (it.transaction.categoryId ?: -1L) == catId }.forEach { txn ->
                val d = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }.get(Calendar.DAY_OF_MONTH)
                if (d in 1..totalDaysInMonth) {
                    dailySeries[d - 1] += txn.transaction.amount
                }
            }

            CategoryDelta(
                name = cat?.name ?: "Expense",
                amount = thisAmt,
                pctDelta = pctDelta,
                diff = diff,
                iconName = cat?.iconResName ?: "category",
                isIncrease = diff >= 0,
                sparklineData = dailySeries.toList()
            )
        }
    }

    // Small purchases analysis (< ₹150)
    val smallPurchases = remember(thisMonthTxns) {
        val list = thisMonthTxns.filter { it.transaction.amount < 150.0 }
        val sum = list.sumOf { it.transaction.amount }
        val pctOfTotal = if (totalSpent > 0) ((sum / totalSpent) * 100).toInt() else 0
        Triple(list.size, sum, pctOfTotal)
    }

    // Weekend vs Weekday analysis
    val weekendStats = remember(thisMonthTxns) {
        var weekendSum = 0.0
        var weekdaySum = 0.0
        var weekendDays = 0
        var weekdayDays = 0
        val seenDays = mutableSetOf<String>()
        thisMonthTxns.forEach { txn ->
            val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
            val dow = tCal.get(Calendar.DAY_OF_WEEK)
            val dayKey = "${tCal.get(Calendar.YEAR)}-${tCal.get(Calendar.DAY_OF_YEAR)}"
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendSum += txn.transaction.amount
                if (seenDays.add("W$dayKey")) weekendDays++
            } else {
                weekdaySum += txn.transaction.amount
                if (seenDays.add("D$dayKey")) weekdayDays++
            }
        }
        val avgWeekend = if (weekendDays > 0) weekendSum / weekendDays else 0.0
        val avgWeekday = if (weekdayDays > 0) weekdaySum / weekdayDays else 1.0
        if (avgWeekday > 0) (((avgWeekend - avgWeekday) / avgWeekday) * 100).toInt() else 0
    }

    var selectedTab by remember { mutableStateOf("MONTH") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Month/Year Switcher (Matches Mockup)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Insights",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = DarkOnSurface
                        )
                        Text("✨", fontSize = 20.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onFilterSelected(TimeboxFilter.MONTHLY) }
                    ) {
                        Text(
                            text = currentMonthYear,
                            style = MaterialTheme.typography.labelMedium,
                            color = DarkOnSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Month / Year Toggle Segment
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == "MONTH") BrandLime else Color.Transparent)
                                .clickable { selectedTab = "MONTH" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Month",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == "MONTH") DarkBackground else DarkOnSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == "YEAR") BrandLime else Color.Transparent)
                                .clickable { selectedTab = "YEAR" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Year",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == "YEAR") DarkBackground else DarkOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Budget & Remaining Hero Card (Matches Mockup)
        item {
            val displaySpent = if (selectedTab == "YEAR") yearTotalSpent else totalSpent
            val displayBudget = if (selectedTab == "YEAR") yearBudget else userBudget
            val displayRemaining = (displayBudget - displaySpent).coerceAtLeast(0.0)
            val displayProgressPct = if (displayBudget > 0) ((displaySpent / displayBudget) * 100).toInt() else 0

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (selectedTab == "YEAR") "Year-to-Date Spent" else "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = "$currencySymbol${"%,.0f".format(displaySpent)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = DarkOnSurface
                            )
                            Text(
                                text = "of $currencySymbol${"%,.0f".format(displayBudget)} ${if (selectedTab == "YEAR") "annual" else "monthly"} budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = "$currencySymbol${"%,.0f".format(displayRemaining)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = BrandLime
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant
                            ) {
                                Text(
                                    text = if (selectedTab == "YEAR") "12 months cap" else "$currencySymbol${"%,.0f".format(dailyLimit)} / day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = DarkOnSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Progress Bar
                    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = (displayProgressPct / 100f).coerceIn(0f, 1f),
                        animationSpec = androidx.compose.animation.core.spring(),
                        label = "budget_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (displayProgressPct > 100) Color(0xFFEF4444) else BrandLime,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
        }

        // 3. Dynamic Pace Card (Green "On Track" vs Red "Spending is High")
        item {
            val paceCardColor = if (isPaceOnTrack) BrandLimeContainer.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            val paceBorderColor = if (isPaceOnTrack) BrandLime.copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
            val paceTextColor = if (isPaceOnTrack) BrandLime else Color(0xFFEF4444)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = paceCardColor),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, paceBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isPaceOnTrack) "🎉" else "⚠️",
                            fontSize = 24.sp
                        )
                        Column {
                            Text(
                                text = if (isPaceOnTrack) "You're on track!" else "Spending is high",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = if (isPaceOnTrack) {
                                    "You're spending $currencySymbol${"%,.0f".format(paceDifference)} less than your expected pace."
                                } else {
                                    "You're exceeding your expected daily budget by $currencySymbol${"%,.0f".format(paceDifference)}."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (momPct <= 0) "↓ ${kotlin.math.abs(momPct)}%" else "↑ ${momPct}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = paceTextColor
                        )
                        Text(
                            text = "vs last month",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = DarkOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. "What changed this month?" Section (Matches Mockup)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What changed this month?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "View all >",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandLime,
                        modifier = Modifier.clickable { showWhatChangedSheet = true }
                    )
                }

                if (categoryChanges.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add transactions to see category changes vs last month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categoryChanges.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.width(135.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (item.isIncrease) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(item.iconName),
                                            contentDescription = item.name,
                                            tint = if (item.isIncrease) Color(0xFFEF4444) else Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DarkOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "$currencySymbol${"%,.0f".format(item.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = DarkOnSurface
                                    )

                                    Text(
                                        text = if (item.isIncrease) "↑ ${item.pctDelta}%" else "↓ ${kotlin.math.abs(item.pctDelta)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (item.isIncrease) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )

                                    Text(
                                        text = if (item.isIncrease) "$currencySymbol${"%,.0f".format(item.diff)} more" else "$currencySymbol${"%,.0f".format(kotlin.math.abs(item.diff))} less",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = DarkOnSurfaceVariant
                                    )

                                    // Real Mini Sparkline Canvas
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    ) {
                                        val path = Path()
                                        val w = size.width
                                        val h = size.height
                                        val data = item.sparklineData
                                        if (data.isNotEmpty() && data.any { it > 0 }) {
                                            val maxVal = data.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                                            data.forEachIndexed { idx, v ->
                                                val x = (idx.toFloat() / (data.size.coerceAtLeast(2) - 1)) * w
                                                val y = h - ((v / maxVal) * (h * 0.8f)).toFloat() - (h * 0.1f)
                                                if (idx == 0) path.moveTo(x, y)
                                                else {
                                                    val prevX = ((idx - 1).toFloat() / (data.size.coerceAtLeast(2) - 1)) * w
                                                    val prevY = h - ((data[idx - 1] / maxVal) * (h * 0.8f)).toFloat() - (h * 0.1f)
                                                    val cx = (prevX + x) / 2f
                                                    path.cubicTo(cx, prevY, cx, y, x, y)
                                                }
                                            }
                                        } else {
                                            path.moveTo(0f, if (item.isIncrease) h * 0.8f else h * 0.2f)
                                            path.cubicTo(
                                                w * 0.3f, if (item.isIncrease) h * 0.7f else h * 0.4f,
                                                w * 0.6f, if (item.isIncrease) h * 0.3f else h * 0.6f,
                                                w, if (item.isIncrease) h * 0.1f else h * 0.9f
                                            )
                                        }
                                        drawPath(
                                            path = path,
                                            color = if (item.isIncrease) Color(0xFFEF4444) else Color(0xFF10B981),
                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Spending Overview Interactive Bezier Line Chart (Supports Month & Year)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTab == "YEAR") "Yearly spending trend" else "Spending overview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Text(
                            text = if (selectedTab == "YEAR") "$curYear" else "View trend >",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandLime
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (selectedTab == "YEAR") "Monthly Average" else "Average per day",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = if (selectedTab == "YEAR") {
                                    "$currencySymbol${"%,.0f".format(yearTotalSpent / 12)}"
                                } else {
                                    "$currencySymbol${"%,.0f".format(currentDailyAvg)}"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("vs last month", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            val lastMonthDailyAvg = if (lastMonthTotalSpent > 0 && totalDaysInMonth > 0) lastMonthTotalSpent / totalDaysInMonth else currentDailyAvg
                            val dailyDelta = kotlin.math.abs(currentDailyAvg - lastMonthDailyAvg)
                            val isDown = currentDailyAvg <= lastMonthDailyAvg
                            Text(
                                text = if (isDown) "↓ $currencySymbol${"%,.0f".format(dailyDelta)}" else "↑ $currencySymbol${"%,.0f".format(dailyDelta)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDown) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    //                     // Canvas Spending Chart with Axis and Gradient Fill (Matches Insights.jpeg)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val maxDisplayVal = if (selectedTab == "YEAR") {
                            (monthlyYearSpends.values.maxOrNull() ?: 1000.0).coerceAtLeast(1000.0)
                        } else {
                            (dailySpendMap.values.maxOrNull() ?: 1000.0).coerceAtLeast(1000.0)
                        }

                        // Left Y-Axis Scale (Matches 3k, 2k, 1k, 0 in Mock)
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (maxDisplayVal >= 1000) "${(maxDisplayVal / 1000).toInt()}k" else "${maxDisplayVal.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = if (maxDisplayVal >= 1000) "${(maxDisplayVal * 0.66 / 1000).toInt()}k" else "${(maxDisplayVal * 0.66).toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = if (maxDisplayVal >= 1000) "${(maxDisplayVal * 0.33 / 1000).toInt()}k" else "${(maxDisplayVal * 0.33).toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = DarkOnSurfaceVariant
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = DarkOnSurfaceVariant
                            )
                        }

                        // Chart Canvas with Gradient Fill & Peak Marker
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val daysToDraw = (1..currentDay).toList()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(selectedTab) {
                                        detectTapGestures { offset ->
                                            if (selectedTab == "MONTH" && daysToDraw.isNotEmpty()) {
                                                val tappedFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                                val tappedDay = (1 + (tappedFraction * (currentDay - 1))).toInt().coerceIn(1, currentDay)
                                                touchedDay = tappedDay
                                            }
                                        }
                                    }
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw subtle horizontal grid lines
                                listOf(0.1f, 0.4f, 0.7f, 0.95f).forEach { frac ->
                                    drawLine(
                                        color = Color(0xFF334155).copy(alpha = 0.35f),
                                        start = Offset(0f, h * frac),
                                        end = Offset(w, h * frac),
                                        strokeWidth = 1f
                                    )
                                }

                                if (selectedTab == "YEAR") {
                                    // Draw 12-month year curve with gradient fill
                                    val path = Path()
                                    val fillPath = Path()
                                    (0..11).forEach { mIdx ->
                                        val spend = monthlyYearSpends[mIdx] ?: 0.0
                                        val x = (mIdx.toFloat() / 11f) * w
                                        val y = h - ((spend / maxDisplayVal) * (h * 0.82f)).toFloat() - 10f
                                        if (mIdx == 0) {
                                            path.moveTo(x, y)
                                            fillPath.moveTo(x, y)
                                        } else {
                                            val prevSpend = monthlyYearSpends[mIdx - 1] ?: 0.0
                                            val prevX = ((mIdx - 1).toFloat() / 11f) * w
                                            val prevY = h - ((prevSpend / maxDisplayVal) * (h * 0.82f)).toFloat() - 10f
                                            val cx = (prevX + x) / 2f
                                            path.cubicTo(cx, prevY, cx, y, x, y)
                                            fillPath.cubicTo(cx, prevY, cx, y, x, y)
                                        }
                                    }
                                    fillPath.lineTo(w, h)
                                    fillPath.lineTo(0f, h)
                                    fillPath.close()

                                    // Gradient fill
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(BrandLime.copy(alpha = 0.25f), Color.Transparent),
                                            startY = 0f,
                                            endY = h
                                        )
                                    )
                                    // Stroke line
                                    drawPath(
                                        path = path,
                                        color = BrandLime,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                } else {
                                    // Draw Monthly day-wise curve with gradient fill
                                    val path = Path()
                                    val fillPath = Path()
                                    if (daysToDraw.isNotEmpty()) {
                                        daysToDraw.forEachIndexed { index, day ->
                                            val spend = dailySpendMap[day] ?: 0.0
                                            val x = (index.toFloat() / (daysToDraw.size.coerceAtLeast(2) - 1)) * w
                                            val y = h - ((spend / maxDisplayVal) * (h * 0.82f)).toFloat() - 10f

                                            if (index == 0) {
                                                path.moveTo(x, y)
                                                fillPath.moveTo(x, y)
                                            } else {
                                                val prevDay = daysToDraw[index - 1]
                                                val prevSpend = dailySpendMap[prevDay] ?: 0.0
                                                val prevX = ((index - 1).toFloat() / (daysToDraw.size.coerceAtLeast(2) - 1)) * w
                                                val prevY = h - ((prevSpend / maxDisplayVal) * (h * 0.82f)).toFloat() - 10f
                                                val cx = (prevX + x) / 2f
                                                path.cubicTo(cx, prevY, cx, y, x, y)
                                                fillPath.cubicTo(cx, prevY, cx, y, x, y)
                                            }
                                        }

                                        fillPath.lineTo(w, h)
                                        fillPath.lineTo(0f, h)
                                        fillPath.close()

                                        // Area Gradient Fill
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(BrandLime.copy(alpha = 0.25f), Color.Transparent),
                                                startY = 0f,
                                                endY = h
                                            )
                                        )
                                        // Line Stroke
                                        drawPath(
                                            path = path,
                                            color = BrandLime,
                                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                        )

                                        // Interactive Highlight or Peak Point
                                        val activeMarkerDay = touchedDay ?: peakDayEntry?.key
                                        if (activeMarkerDay != null && activeMarkerDay in daysToDraw) {
                                            val activeIdx = daysToDraw.indexOf(activeMarkerDay)
                                            if (activeIdx >= 0) {
                                                val activeSpend = dailySpendMap[activeMarkerDay] ?: 0.0
                                                val markX = (activeIdx.toFloat() / (daysToDraw.size.coerceAtLeast(2) - 1)) * w
                                                val markY = h - ((activeSpend / maxDisplayVal) * (h * 0.82f)).toFloat() - 10f

                                                if (touchedDay != null) {
                                                    drawLine(
                                                        color = BrandLime.copy(alpha = 0.45f),
                                                        start = Offset(markX, 0f),
                                                        end = Offset(markX, h),
                                                        strokeWidth = 1.5.dp.toPx()
                                                    )
                                                }

                                                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(markX, markY))
                                                drawCircle(color = BrandLime, radius = 4.dp.toPx(), center = Offset(markX, markY))
                                                drawCircle(color = DarkBackground, radius = 2.dp.toPx(), center = Offset(markX, markY))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom X-Axis Milestones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectedTab == "YEAR") {
                            listOf("Jan", "Mar", "Jun", "Sep", "Dec").forEach { m ->
                                Text(m, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                            }
                        } else {
                            listOf("1 $currentMonthShort", "8 $currentMonthShort", "15 $currentMonthShort", "22 $currentMonthShort", "${totalDaysInMonth.coerceAtLeast(28)} $currentMonthShort").forEach { d ->
                                Text(d, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                            }
                        }
                    }

                    // Dynamic spending callout banner (supports tap & peak)
                    if (selectedTab == "MONTH") {
                        val activeDay = touchedDay ?: peakDayEntry?.key ?: currentDay
                        val activeSpend = dailySpendMap[activeDay] ?: 0.0
                        val isPeak = touchedDay == null || activeDay == peakDayEntry?.key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(if (isPeak) "🔥" else "📍", fontSize = 12.sp)
                                    Text(
                                        text = if (isPeak && peakDayEntry != null) "Spending peaked on ${peakDayEntry.key} $currentMonthShort – $currencySymbol${"%,.0f".format(peakDayEntry.value)}"
                                        else "$activeDay $currentMonthShort spend – $currencySymbol${"%,.0f".format(activeSpend)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkOnSurface
                                    )
                                }
                                if (touchedDay != null && touchedDay != peakDayEntry?.key) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandLime,
                                        modifier = Modifier.clickable { touchedDay = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Budgets Needing Attention Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budgets needing attention",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "Manage Budgets",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandLime,
                        modifier = Modifier.clickable { onNavigateToCategories() }
                    )
                }

                val attentionCategories = remember(categories, monthlyCategorySpends) {
                    categories.mapNotNull { cat ->
                        val cap = cat.budgetLimit ?: return@mapNotNull null
                        if (cap <= 0) return@mapNotNull null
                        val spent = monthlyCategorySpends[cat.id] ?: 0.0
                        val pct = ((spent / cap) * 100).toInt()
                        if (pct >= 80) Triple(cat, spent, cap) else null
                    }.sortedByDescending { it.second / it.third }
                }

                attentionCategories.forEach { (cat, spent, cap) ->
                    val pct = ((spent / cap.coerceAtLeast(1.0)) * 100).toInt()
                    val isOver = spent > cap

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isOver) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(cat.iconResName),
                                            contentDescription = cat.name,
                                            tint = if (isOver) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DarkOnSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol${"%,.0f".format(spent)} / $currencySymbol${"%,.0f".format(cap)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = DarkOnSurface
                                        )
                                        Text(
                                            text = if (isOver) "Over by $currencySymbol${"%,.0f".format(spent - cap)}" else "$currencySymbol${"%,.0f".format(cap - spent)} remaining",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = if (isOver) Color(0xFFEF4444) else Color(0xFF10B981)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isOver) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = if (isOver) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Colored progress bar
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = if (isOver) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 7. Spending Patterns (Small purchases & Weekend bar chart)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your spending patterns",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkOnSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Small purchases
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("☕ Small purchases", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                            Text("${smallPurchases.first} purchases under $currencySymbol 150", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                            Text("$currencySymbol${"%,.0f".format(smallPurchases.second)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = DarkOnSurface)
                            Text("${smallPurchases.third}% of total spending", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BrandLime)
                        }
                    }

                    // Card 2: Weekend Spending with Bar Chart
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📅 Weekend spending", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                            Text("You spend $weekendStats% more on weekends", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)

                            // 7-day bar chart — computed from real transactions
                            val dayOfWeekBars = remember(thisMonthTxns) {
                                val dayTotals = DoubleArray(7) { 0.0 }
                                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                                thisMonthTxns.forEach { txn ->
                                    val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
                                    val idx = when (tCal.get(Calendar.DAY_OF_WEEK)) {
                                        Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
                                        Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
                                        Calendar.SUNDAY -> 6; else -> 0
                                    }
                                    dayTotals[idx] += txn.transaction.amount
                                }
                                val maxDay = dayTotals.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                                dayLabels.mapIndexed { idx, label ->
                                    Triple(label, (dayTotals[idx] / maxDay).toFloat().coerceIn(0.05f, 1f), idx >= 5)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                dayOfWeekBars.forEach { (dayLabel, heightRatio, isWeekend) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height((22 * heightRatio.coerceAtLeast(0.18f)).dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isWeekend) BrandLime else Color(0xFF475569))
                                        )
                                        Text(
                                            text = dayLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 7.5.sp,
                                                fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isWeekend) BrandLime else DarkOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 2 of Spending Patterns (Payment Methods & Average Ticket)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 3: Payment Split
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡ Payment Modes", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                            Text("UPI ${paymentModeStats.first}% • Card ${paymentModeStats.second}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                            Text(if (paymentModeStats.first >= 50) "UPI Preferred" else "Card Preferred", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF38BDF8))
                            Text("${thisMonthTxns.size} transactions tracked", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                        }
                    }

                    // Card 4: Ticket Size
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🎟️ Average Ticket", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                            Text("Average spend / transaction", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnSurfaceVariant)
                            Text("$currencySymbol${"%,.0f".format(avgTicketSize.toDouble())}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFA855F7))
                            Text("Consistent pace", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BrandLime)
                        }
                    }
                }
            }
        }

        // 8. Smart Payment & Financial Facts Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Text(
                            text = "Smart Financial Facts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("•", color = BrandLime, fontWeight = FontWeight.Bold)
                            Text(
                                text = "UPI is your primary payment mode (${paymentModeStats.first}% of spends), ensuring instant settlements and zero transaction charges.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = DarkOnSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("•", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            Text(
                                text = "Your average spend per transaction this month is $currencySymbol${"%,.0f".format(avgTicketSize.toDouble())} across ${thisMonthTxns.size} expenses.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = DarkOnSurfaceVariant
                            )
                        }
                        if (smallPurchases.first > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${smallPurchases.first} micro-purchases under $currencySymbol 150 accumulated to $currencySymbol${"%,.0f".format(smallPurchases.second)} (${smallPurchases.third}% of budget).",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = DarkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 9. Weekly Recap Archive Card
        if (weeklyRecapData != null) {
            item {
                Surface(
                    onClick = { showWeeklyStoryModal = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F231D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandLime.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BrandLime.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 16.sp)
                            }
                            Column {
                                Text(
                                    text = "Watch Weekly Recap Story",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "Previous week summary (${weeklyRecapData.dateRangeLabel})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkOnSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = BrandLime, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }

    if (showWhatChangedSheet) {
        WhatChangedDetailSheet(
            categoryChanges = categoryChanges,
            currencySymbol = currencySymbol,
            onDismiss = { showWhatChangedSheet = false }
        )
    }

    if (showWeeklyStoryModal && weeklyRecapData != null) {
        WeeklyRecapStoryModal(
            data = weeklyRecapData,
            currencySymbol = currencySymbol,
            onDismiss = { showWeeklyStoryModal = false }
        )
    }
}

data class CategoryDelta(
    val name: String,
    val amount: Double,
    val pctDelta: Int,
    val diff: Double,
    val iconName: String,
    val isIncrease: Boolean,
    val sparklineData: List<Double> = emptyList()
)
