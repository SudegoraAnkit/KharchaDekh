package com.ankitsudegora.ui.screens

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.components.getCurrencySymbol
import com.ankitsudegora.ui.components.CategorySpendingDetailSheet
import com.ankitsudegora.ui.components.WeeklyRecapStoryModal
import com.ankitsudegora.ui.components.WeeklyRecapManager
import com.ankitsudegora.ui.theme.*
import com.ankitsudegora.viewmodel.AnalyticsState
import com.ankitsudegora.viewmodel.TimeboxFilter
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ankitsudegora.R
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat


data class CategorySpendProgress(
    val name: String,
    val amount: Double,
    val pct: Int,
    val iconName: String
)

@Composable
fun DashboardScreen(
    userName: String,
    analytics: AnalyticsState,
    pendingTransactions: List<TransactionWithCategory>,
    allTransactions: List<TransactionWithCategory>,
    categories: List<com.ankitsudegora.data.Category>,
    selectedFilter: TimeboxFilter,
    onFilterSelected: (TimeboxFilter) -> Unit,
    onEnrichTransaction: (Long) -> Unit,
    onDeleteTransaction: (com.ankitsudegora.data.Transaction) -> Unit,
    onNavigateToCategories: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    monthlyIncome: Double,
    savingsTargetPct: Int,
    spendingTargetPct: Int,
    monthlyCategorySpends: Map<Long, Double>,
    forecastAllowance: com.ankitsudegora.viewmodel.ForecastAllowance,
    onSearchClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    billingCycleStartDay: Int,
    onExportCsvCalendar: (List<TransactionWithCategory>) -> Unit,
    onExportPdfCalendar: (List<TransactionWithCategory>) -> Unit,
    primaryCurrency: String,
    onConvertAmount: (Double, String) -> Double,
    onAddExpenseClicked: (() -> Unit)? = null,
    onNavigateToActivity: (() -> Unit)? = null,
    onNavigateToLists: (() -> Unit)? = null,
    onNavigateToInsights: (() -> Unit)? = null,
    onScanReceiptClicked: (() -> Unit)? = null,
    onAddIncomeClicked: (() -> Unit)? = null
) {
    val currencySymbol = remember(primaryCurrency) { getCurrencySymbol(primaryCurrency) }
    var isBalanceHidden by remember { mutableStateOf(false) }

    val currentGreeting = remember(userName) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val salutation = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
        val namePart = if (userName.isNotBlank()) ", $userName" else ""
        val emoji = if (hour in 5..20) "👋" else "🌙"
        "$salutation$namePart $emoji"
    }

    val currentMonthYear = remember {
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }

    val initials = remember(userName) {
        userName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "U" }
    }

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
    var isPreviousCycleExpanded by remember { mutableStateOf(false) }

    val nonPendingTxns = remember(allTransactions) {
        allTransactions.filter { !it.transaction.isPending }
    }
    val currentCycleTxns = remember(nonPendingTxns, currentBillingCycleStart) {
        nonPendingTxns.filter { it.transaction.timestamp >= currentBillingCycleStart }
    }
    val previousCycleTxns = remember(nonPendingTxns, currentBillingCycleStart) {
        nonPendingTxns.filter { it.transaction.timestamp < currentBillingCycleStart }
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val todayTxns = remember(nonPendingTxns, todayStart) {
        nonPendingTxns.filter { it.transaction.timestamp >= todayStart }
    }
    val todayTotalSpent = remember(todayTxns) {
        todayTxns.filter { it.transaction.type == "DEBIT" }.sumOf { it.transaction.amount }
    }

    val context = LocalContext.current
    var showCategoryDetailSheet by remember { mutableStateOf(false) }
    var showWeeklyStoryModal by remember { mutableStateOf(false) }

    val weeklyRecapData = remember(allTransactions) {
        WeeklyRecapManager.computeRecapData(allTransactions)
    }
    val showWeeklyBanner = remember(weeklyRecapData, showWeeklyStoryModal) {
        weeklyRecapData != null && WeeklyRecapManager.isWeeklyRecapAvailable() &&
                !WeeklyRecapManager.hasViewedRecap(context, weeklyRecapData.weekId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "KharchaDekh Logo",
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = "KHARCHADEKH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = BrandLime
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentGreeting,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkOnSurface
                    )
                )
                Text(
                    text = "Track. Control. Save more.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification Bell with pending indicator badge
                Box {
                    IconButton(
                        onClick = {
                            if (pendingTransactions.isNotEmpty()) {
                                onEnrichTransaction(pendingTransactions.first().transaction.id)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (pendingTransactions.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = if (pendingTransactions.isNotEmpty()) BrandLime else DarkOnSurface
                        )
                    }
                    if (pendingTransactions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pendingTransactions.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("dashboard_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = DarkOnSurface
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_feed"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Month Overview Hero Card (Matches Mockup)
            item {
                val budgetCap = if (monthlyIncome > 0.0) monthlyIncome * (spendingTargetPct / 100.0) else 0.0
                val totalSpent = analytics.totalExpense
                val isOverBudget = budgetCap > 0.0 && totalSpent > budgetCap
                val budgetRemaining = if (budgetCap > 0.0) (budgetCap - totalSpent).coerceAtLeast(0.0) else 0.0
                val progressPct = if (budgetCap > 0.0) ((totalSpent / budgetCap) * 100).toInt().coerceAtLeast(0) else 0

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("metrics_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Month Header with Eye toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$currentMonthYear Overview",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurfaceVariant
                                )
                                IconButton(
                                    onClick = { isBalanceHidden = !isBalanceHidden },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Balance",
                                        tint = DarkOnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable { onFilterSelected(TimeboxFilter.MONTHLY) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = currentMonthYear.split(" ").firstOrNull() ?: "Month",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkOnSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = DarkOnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Hero Expense Amount Display
                        Column {
                            Text(
                                text = "Total Outflow",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isBalanceHidden) "₹ ••••••" else "$currencySymbol${"%,.2f".format(analytics.totalExpense)}",
                                    style = MoneyTypography.HeroAmount.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = DarkOnSurface
                                )
                            }
                        }

                        // Budget Limit / Over-Budget Warning Strip
                        if (monthlyIncome > 0) {
                            val isOverBudget = analytics.totalExpense > monthlyIncome
                            val pctSpent = ((analytics.totalExpense / monthlyIncome) * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOverBudget) Color(0xFFEF4444).copy(alpha = 0.15f) else DarkSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isOverBudget) Color(0xFFEF4444) else BrandLime,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isOverBudget) "Over budget by $currencySymbol${"%,.0f".format(analytics.totalExpense - monthlyIncome)}" else "$pctSpent% of budget used",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isOverBudget) Color(0xFFEF4444) else DarkOnSurface
                                        )
                                    }
                                    Text(
                                        text = "$currencySymbol${"%,.0f".format(monthlyIncome)} Limit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOnSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        // 4 Key Stats Chips (Daily Avg, Highest Day, Transactions, Savings)
                        val dayOfMonth = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
                        val currentMonthShort = remember { SimpleDateFormat("MMM", Locale.getDefault()).format(Date()) }
                        val dailyAvg = analytics.totalExpense / dayOfMonth

                        // Accurate whole month/cycle peak day calculation
                        val currentMonthTxns = remember(allTransactions, currentBillingCycleStart) {
                            allTransactions.filter { txn ->
                                txn.transaction.type == "DEBIT" && !txn.transaction.isPending &&
                                txn.transaction.timestamp >= currentBillingCycleStart
                            }
                        }
                        val peakDayEntry = remember(currentMonthTxns) {
                            val map = mutableMapOf<Int, Double>()
                            currentMonthTxns.forEach { txn ->
                                val d = java.util.Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }.get(java.util.Calendar.DAY_OF_MONTH)
                                map[d] = (map[d] ?: 0.0) + txn.transaction.amount
                            }
                            map.entries.maxByOrNull { it.value }
                        }

                        val totalTxnCount = currentCycleTxns.size
                        val computedSavings = (monthlyIncome - analytics.totalExpense).coerceAtLeast(0.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 1. Daily Avg
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.height(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF10B981), modifier = Modifier.size(9.dp))
                                    }
                                    Text(
                                        text = "Daily Avg",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = DarkOnSurfaceVariant,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = if (isBalanceHidden) "•••" else "$currencySymbol${"%,.0f".format(dailyAvg)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = DarkOnSurface,
                                    softWrap = false
                                )
                                Text(
                                    text = "per day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = DarkOnSurfaceVariant,
                                    softWrap = false
                                )
                            }

                            // 2. Highest Day
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.height(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFFEF4444), modifier = Modifier.size(9.dp))
                                    }
                                    Text(
                                        text = "Highest Day",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = DarkOnSurfaceVariant,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = if (isBalanceHidden) "•••" else if (peakDayEntry != null && peakDayEntry.value > 0) "$currencySymbol${"%,.0f".format(peakDayEntry.value)}" else "$currencySymbol${"%,.0f".format(dailyAvg)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = DarkOnSurface,
                                    softWrap = false
                                )
                                Text(
                                    text = if (peakDayEntry != null && peakDayEntry.value > 0) "${peakDayEntry.key} $currentMonthShort" else "Peak spend",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = DarkOnSurfaceVariant,
                                    softWrap = false
                                )
                            }

                            // 3. Transactions Count
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.height(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(9.dp))
                                    }
                                    Text(
                                        text = "Transactions",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = DarkOnSurfaceVariant,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = "$totalTxnCount",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = DarkOnSurface,
                                    softWrap = false
                                )
                                Text(
                                    text = "This month",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = DarkOnSurfaceVariant,
                                    softWrap = false
                                )
                            }

                            // 4. Savings
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.height(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Savings, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(9.dp))
                                    }
                                    Text(
                                        text = "Savings",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = DarkOnSurfaceVariant,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = if (isBalanceHidden) "•••" else "$currencySymbol${"%,.0f".format(computedSavings)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = BrandLime,
                                    softWrap = false
                                )
                                Text(
                                    text = "Target reached",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = DarkOnSurfaceVariant,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 1.5 Weekly Recap Flash Story Card (YouTube Analytics Style)
            if (showWeeklyBanner && weeklyRecapData != null) {
                item {
                    Card(
                        onClick = { showWeeklyStoryModal = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F231D)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandLime.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_recap_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BrandLime.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✨", fontSize = 18.sp)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "WEEKLY RECAP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                letterSpacing = 1.sp
                                            ),
                                            color = BrandLime
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(BrandLime)
                                        )
                                    }
                                    Text(
                                        text = "Your Week in Review is ready!",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DarkOnSurface
                                    )
                                    Text(
                                        text = "See your spending & investments recap ➔",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOnSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Story",
                                tint = BrandLime,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // 2. Pending Review Alerts Card (Action First)
            if (pendingTransactions.isNotEmpty()) {
                item {
                    Card(
                        onClick = { onEnrichTransaction(pendingTransactions.first().transaction.id) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandLime.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_alert_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${pendingTransactions.size} expenses need your review",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "15 sec",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkOnSurfaceVariant
                                )
                            }

                            // Preview rows
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pendingTransactions.take(2).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkSurfaceVariant)
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                                    .background(BrandLime.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getIconVector(item.category?.iconResName ?: "restaurant"),
                                                    contentDescription = null,
                                                    tint = BrandLime,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "$currencySymbol${item.transaction.amount}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = DarkOnSurface
                                                )
                                                Text(
                                                    text = item.transaction.merchant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DarkOnSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkSurfaceElevated)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = item.category?.name ?: "Expense",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = DarkOnSurface
                                                )
                                            }
                                            Text(
                                                text = "98%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandLime
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { onEnrichTransaction(pendingTransactions.first().transaction.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF97316), Color(0xFFEF4444))
                                        )
                                    ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Review Now →",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. Today's Summary & Activity Feed
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "$currencySymbol${"%,.2f".format(todayTotalSpent)}",
                        style = MoneyTypography.LargeAmount,
                        color = DarkOnSurface
                    )
                }
            }

            if (todayTxns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = BrandLime.copy(alpha = 0.8f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No expenses logged today",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the + button below to log cash or shopping.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(todayTxns, key = { "today_${it.transaction.id}" }) { item ->
                    TransactionListItem(
                        item = item,
                        onEditClicked = { onEnrichTransaction(item.transaction.id) },
                        onDeleteClicked = { onDeleteTransaction(item.transaction) },
                        primaryCurrency = primaryCurrency,
                        onConvertAmount = onConvertAmount
                    )
                }
            }

            // 4. Quick Actions Row (Matches Mockup)
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
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Add Expense
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onAddExpenseClicked?.invoke() }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(BrandLime.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = BrandLime, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Add\nExpense", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = DarkOnSurface)
                            }

                            // 2. Scan Receipt
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onScanReceiptClicked?.invoke() }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Scan\nReceipt", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = DarkOnSurface)
                            }

                            // 3. Add Income
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onAddIncomeClicked?.invoke() }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Add\nIncome", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = DarkOnSurface)
                            }

                            // 4. Review Expenses
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (pendingTransactions.isNotEmpty()) {
                                            onEnrichTransaction(pendingTransactions.first().transaction.id)
                                        }
                                    }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                                    if (pendingTransactions.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${pendingTransactions.size}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.White)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Review\nExpenses", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = DarkOnSurface)
                            }

                            // 5. Add to List
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onNavigateToLists?.invoke() }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, null, tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Add to\nList", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = DarkOnSurface)
                            }
                        }
                    }
                }
            }

            // 5. Spending by Category Section (Matches Mockup)
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
                                text = "Spending by Category",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = "View All >",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandLime,
                                modifier = Modifier.clickable { showCategoryDetailSheet = true }
                            )
                        }

                        val computedMonthlyCatSpends = remember(allTransactions, monthlyCategorySpends) {
                            if (monthlyCategorySpends.isNotEmpty()) monthlyCategorySpends else {
                                val cal = Calendar.getInstance()
                                val curMonth = cal.get(Calendar.MONTH)
                                val curYear = cal.get(Calendar.YEAR)
                                allTransactions.filter { txn ->
                                    txn.transaction.type == "DEBIT" && run {
                                        val tCal = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
                                        tCal.get(Calendar.MONTH) == curMonth && tCal.get(Calendar.YEAR) == curYear
                                    }
                                }.groupBy { it.transaction.categoryId ?: -1L }
                                    .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }
                            }
                        }

                        val totalSpentForCats = remember(computedMonthlyCatSpends, analytics.totalExpense) {
                            val sum = computedMonthlyCatSpends.values.sum()
                            if (sum > 0) sum else (if (analytics.totalExpense > 0) analytics.totalExpense else 1.0)
                        }

                        val topCatEntries = remember(computedMonthlyCatSpends, categories, totalSpentForCats) {
                            val catMap = categories.associateBy { it.id }
                            computedMonthlyCatSpends.entries
                                .filter { it.value > 0 }
                                .sortedByDescending { it.value }
                                .take(4)
                                .map { entry ->
                                    val cat = catMap[entry.key]
                                    val pct = ((entry.value / totalSpentForCats) * 100).toInt().coerceIn(1, 100)
                                    val icon = cat?.iconResName ?: "category"
                                    CategorySpendProgress(cat?.name ?: "Expense", entry.value, pct, icon)
                                }
                        }

                        if (topCatEntries.isEmpty()) {
                            Text(
                                text = "No category spending recorded this month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceVariant
                            )
                        } else {
                            val colorsList = listOf(Color(0xFF06B6D4), Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF8B5CF6))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                topCatEntries.forEachIndexed { idx, item ->
                                    val col = colorsList.getOrElse(idx) { BrandLime }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(col.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getIconVector(item.iconName),
                                                    contentDescription = item.name,
                                                    tint = col,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = DarkOnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = if (isBalanceHidden) "••••" else "$currencySymbol${"%,.0f".format(item.amount)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = DarkOnSurfaceVariant
                                        )

                                        Text(
                                            text = "${item.pct}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = col
                                        )

                                        // Horizontal colored progress bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(DarkSurfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = (item.pct / 100f).coerceIn(0.05f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(col)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Privacy & Offline Footer (Matches Mockup)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "100% Private & Secure",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "Your data stays on your device.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = DarkOnSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Offline First",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandLime
                            )
                        }
                    }
                }
            }
        }

        if (showCategoryDetailSheet) {
            CategorySpendingDetailSheet(
                allTransactions = allTransactions,
                categories = categories,
                currencySymbol = currencySymbol,
                onDismiss = { showCategoryDetailSheet = false }
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
}

@Composable
fun BudgetTrackingSection(
    categories: List<com.ankitsudegora.data.Category>,
    monthlyCategorySpends: Map<Long, Double>,
    onNavigateToCategories: () -> Unit,
    currencySymbol: String
) {
    val budgetedCategories = categories.filter { it.budgetLimit != null && it.budgetLimit > 0.0 }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
                    color = DarkOnSurface
                )
                
                TextButton(onClick = onNavigateToCategories) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandLime
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
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Set monthly spending limits for categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        TextButton(onClick = onNavigateToCategories) {
                            Text(
                                text = "Set Category Limits",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandLime
                            )
                        }
                    }
                }
            } else {
                budgetedCategories.forEach { category ->
                    val spentVal = monthlyCategorySpends[category.id] ?: 0.0
                    val limit = category.budgetLimit ?: 0.0
                    val ratio = (spentVal / limit).coerceIn(0.0, 1.0).toFloat()
                    val percent = (spentVal / limit * 100).toInt()
                    
                    val isExceeded = spentVal >= limit
                    val isClose = spentVal >= 0.8 * limit && spentVal < limit
                    
                    val catColor = CategoryColors.getCategoryColor(category.name)
                    val progressColor = when {
                        isExceeded -> DarkError
                        isClose -> Color(0xFFF59E0B) // Amber
                        else -> BrandLime
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_row_${category.id}")
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
                                        .background(catColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(category.iconResName),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                            }
                            
                            Text(
                                text = "$currencySymbol${"%,.0f".format(spentVal)} / $currencySymbol${"%,.0f".format(limit)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = DarkOnSurfaceVariant
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = DarkSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$percent% spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                            
                            if (isExceeded) {
                                Text(
                                    text = "Over limit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkError
                                )
                            } else if (isClose) {
                                Text(
                                    text = "Near limit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF59E0B)
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
fun MetricsSummarySection(
    analytics: AnalyticsState,
    categories: List<com.ankitsudegora.data.Category>,
    monthlyIncome: Double,
    savingsTargetPct: Int,
    spendingTargetPct: Int,
    selectedFilter: TimeboxFilter,
    onFilterSelected: ((TimeboxFilter) -> Unit)? = null,
    monthlyCategorySpends: Map<Long, Double>,
    forecastAllowance: com.ankitsudegora.viewmodel.ForecastAllowance,
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Savings Target
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
                                    text = "SAVINGS GOAL ($savingsTargetPct%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = DarkOnSurfaceVariant
                                )
                                Text(
                                    text = if (isSavingsMet) "On Track" else "Under Goal",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = savingsColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${"%,.0f".format(currentSaved)} saved of $currencySymbol${"%,.0f".format(targetSavings)} goal",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (currentSaved / targetSavings).coerceIn(0.0, 1.0).toFloat() },
                                color = savingsColor,
                                trackColor = DarkSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    // Spending Cap
                    val isOverSpent = monthlySpent > spendingCap
                    val capLeft = (spendingCap - monthlySpent).coerceAtLeast(0.0)
                    val capColor = if (isOverSpent) DarkError else BrandLime

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

            // Segmented distribution bar
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

            // Breakdown list
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
fun TransactionListItem(
    item: TransactionWithCategory,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    primaryCurrency: String = "INR",
    onConvertAmount: ((Double, String) -> Double)? = null
) {
    val isDebit = item.transaction.type == "DEBIT"
    val catColor = CategoryColors.getCategoryColor(item.category?.name)

    Card(
        onClick = onEditClicked,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${item.transaction.id}")
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
                modifier = Modifier.weight(1.5f)
            ) {
                // Category Icon with comfortable rounded background badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = item.category?.iconResName ?: "category"
                    Icon(
                        imageVector = getIconVector(icon),
                        contentDescription = item.category?.name ?: "Expense",
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val subCatLabel = if (!item.transaction.subCategory.isNullOrBlank()) " (${item.transaction.subCategory})" else ""
                    Text(
                        text = (item.category?.name ?: "Uncategorized") + subCatLabel + 
                                if (!item.transaction.notes.isNullOrBlank()) " • " + item.transaction.notes else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Format Time
                    val date = java.util.Date(item.transaction.timestamp)
                    val format = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                    Text(
                        text = "${format.format(date)} • ${item.transaction.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                val sign = if (isDebit) "-" else "+"
                val amountColor = if (isDebit) DarkOnSurface else BrandLime
                
                val primarySymbol = getCurrencySymbol(primaryCurrency)
                val amountText = if (item.transaction.currency.uppercase() != primaryCurrency.uppercase() && onConvertAmount != null) {
                    val converted = onConvertAmount(item.transaction.amount, item.transaction.currency)
                    "$sign $primarySymbol${"%,.2f".format(converted)}"
                } else {
                    "$sign ${getCurrencySymbol(item.transaction.currency)}${"%,.2f".format(item.transaction.amount)}"
                }

                Text(
                    text = amountText,
                    style = MoneyTypography.ListItemAmount,
                    color = amountColor,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = DarkError.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SafeToSpendCard(
    allowance: com.ankitsudegora.viewmodel.ForecastAllowance,
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
                    // Daily Allowance
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

                    // Weekly Allowance
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
private fun CycleHeader(
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

