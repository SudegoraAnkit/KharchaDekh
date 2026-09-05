package com.ankitsudegora.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class WeeklyRecapData(
    val weekId: String, // e.g. "2026_W34"
    val dateRangeLabel: String, // e.g. "18 Aug – 24 Aug"
    val totalSpent: Double,
    val prevWeekSpent: Double,
    val wowDeltaPct: Int,
    val isSpendReduced: Boolean,
    val totalTxnCount: Int,
    val dailyAvg: Double,
    val totalInvested: Double,
    val totalCreditCardSpent: Double,
    val topCategoryName: String,
    val topCategoryAmount: Double,
    val topCategoryPct: Int,
    val topMerchant: String?,
    val topMerchantAmount: Double,
    val zeroSpendDaysCount: Int,
    val weekendSpendPct: Int
)

object WeeklyRecapManager {
    private const val PREFS_NAME = "weekly_recap_prefs"
    private const val KEY_LAST_VIEWED_WEEK = "last_viewed_week_id"

    fun isWeeklyRecapAvailable(referenceCal: Calendar = Calendar.getInstance()): Boolean {
        val dayOfWeek = referenceCal.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = referenceCal.get(Calendar.HOUR_OF_DAY)
        // Available from Monday 12:00 PM through the rest of the week
        return if (dayOfWeek == Calendar.MONDAY) {
            hourOfDay >= 12
        } else {
            true // Tue, Wed, Thu, Fri, Sat, Sun
        }
    }

    fun hasViewedRecap(context: Context, weekId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_VIEWED_WEEK, "") == weekId
    }

    fun markRecapAsViewed(context: Context, weekId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_VIEWED_WEEK, weekId).apply()
    }

    fun computeRecapData(allTransactions: List<TransactionWithCategory>, referenceTimestamp: Long = System.currentTimeMillis()): WeeklyRecapData? {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceTimestamp }
        cal.firstDayOfWeek = Calendar.MONDAY

        // Target: Previous Week (Monday 00:00:00 to Sunday 23:59:59)
        val targetWeekCal = Calendar.getInstance().apply {
            timeInMillis = referenceTimestamp
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.WEEK_OF_YEAR, -1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevWeekStart = targetWeekCal.timeInMillis
        val weekNumber = targetWeekCal.get(Calendar.WEEK_OF_YEAR)
        val year = targetWeekCal.get(Calendar.YEAR)
        val weekId = "${year}_W${weekNumber}"

        targetWeekCal.add(Calendar.DAY_OF_YEAR, 6)
        targetWeekCal.set(Calendar.HOUR_OF_DAY, 23)
        targetWeekCal.set(Calendar.MINUTE, 59)
        targetWeekCal.set(Calendar.SECOND, 59)
        val prevWeekEnd = targetWeekCal.timeInMillis

        // Prior week (2 weeks ago) for comparison
        val priorWeekStart = prevWeekStart - (7L * 24 * 60 * 60 * 1000)
        val priorWeekEnd = prevWeekStart - 1

        val df = SimpleDateFormat("d MMM", Locale.getDefault())
        val dateRangeLabel = "${df.format(Date(prevWeekStart))} – ${df.format(Date(prevWeekEnd))}"

        val nonPending = allTransactions.filter { !it.transaction.isPending }
        val weekTxns = nonPending.filter { it.transaction.timestamp in prevWeekStart..prevWeekEnd }
        val priorWeekTxns = nonPending.filter { it.transaction.timestamp in priorWeekStart..priorWeekEnd }

        val debitTxns = weekTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val totalSpent = debitTxns.sumOf { it.transaction.amount }

        val priorDebitTxns = priorWeekTxns.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val priorSpent = priorDebitTxns.sumOf { it.transaction.amount }

        val wowDeltaPct = if (priorSpent > 0) {
            (((totalSpent - priorSpent) / priorSpent) * 100).toInt()
        } else 0
        val isSpendReduced = totalSpent <= priorSpent

        val totalTxnCount = weekTxns.size
        val dailyAvg = totalSpent / 7.0

        // Investments
        val totalInvested = weekTxns.filter {
            val cat = it.category?.name?.lowercase() ?: ""
            val m = it.transaction.merchant.lowercase()
            cat.contains("invest") || cat.contains("sip") || cat.contains("mutual") || cat.contains("stock") ||
                    m.contains("groww") || m.contains("zerodha") || m.contains("sip")
        }.sumOf { it.transaction.amount }

        // Credit Card spends
        val totalCreditCardSpent = debitTxns.filter {
            it.transaction.paymentMethod.equals("CARD", ignoreCase = true) ||
                    it.category?.name?.contains("CreditCard", ignoreCase = true) == true
        }.sumOf { it.transaction.amount }

        // Top Category
        val catSpendMap = mutableMapOf<String, Double>()
        debitTxns.forEach { txn ->
            val catName = txn.category?.name ?: "General"
            catSpendMap[catName] = (catSpendMap[catName] ?: 0.0) + txn.transaction.amount
        }
        val topCatEntry = catSpendMap.maxByOrNull { it.value }
        val topCategoryName = topCatEntry?.key ?: "Expenses"
        val topCategoryAmount = topCatEntry?.value ?: 0.0
        val topCategoryPct = if (totalSpent > 0) ((topCategoryAmount / totalSpent) * 100).toInt() else 0

        // Top Merchant
        val topMerchantEntry = debitTxns.maxByOrNull { it.transaction.amount }
        val topMerchant = topMerchantEntry?.transaction?.merchant
        val topMerchantAmount = topMerchantEntry?.transaction?.amount ?: 0.0

        // Zero Spend Days count (days in Mon..Sun with 0 debit spend)
        val daySpendMap = mutableMapOf<Int, Double>()
        debitTxns.forEach { txn ->
            val c = Calendar.getInstance().apply { timeInMillis = txn.transaction.timestamp }
            val day = c.get(Calendar.DAY_OF_WEEK)
            daySpendMap[day] = (daySpendMap[day] ?: 0.0) + txn.transaction.amount
        }
        val zeroSpendDaysCount = (1..7).count { (daySpendMap[it] ?: 0.0) == 0.0 }

        // Weekend spend %
        val satSpend = daySpendMap[Calendar.SATURDAY] ?: 0.0
        val sunSpend = daySpendMap[Calendar.SUNDAY] ?: 0.0
        val weekendTotal = satSpend + sunSpend
        val weekendSpendPct = if (totalSpent > 0) ((weekendTotal / totalSpent) * 100).toInt() else 0

        return WeeklyRecapData(
            weekId = weekId,
            dateRangeLabel = dateRangeLabel,
            totalSpent = totalSpent,
            prevWeekSpent = priorSpent,
            wowDeltaPct = wowDeltaPct,
            isSpendReduced = isSpendReduced,
            totalTxnCount = totalTxnCount,
            dailyAvg = dailyAvg,
            totalInvested = totalInvested,
            totalCreditCardSpent = totalCreditCardSpent,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            topCategoryPct = topCategoryPct,
            topMerchant = topMerchant,
            topMerchantAmount = topMerchantAmount,
            zeroSpendDaysCount = zeroSpendDaysCount,
            weekendSpendPct = weekendSpendPct
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyRecapStoryModal(
    data: WeeklyRecapData,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalSlides = 5
    var currentSlide by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    // Auto-advance timer (5 seconds per slide)
    LaunchedEffect(currentSlide, isPaused) {
        if (!isPaused) {
            delay(5000)
            if (currentSlide < totalSlides - 1) {
                currentSlide++
            } else {
                WeeklyRecapManager.markRecapAsViewed(context, data.weekId)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            WeeklyRecapManager.markRecapAsViewed(context, data.weekId)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap Left: Previous Slide
                                if (currentSlide > 0) currentSlide--
                            } else {
                                // Tap Right: Next Slide
                                if (currentSlide < totalSlides - 1) {
                                    currentSlide++
                                } else {
                                    WeeklyRecapManager.markRecapAsViewed(context, data.weekId)
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Background subtle gradient glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.12f),
                                Color.Transparent,
                                Color(0xFF0F172A).copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Top Segmented Progress Indicators (YouTube Stories style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until totalSlides) {
                        val progress by animateFloatAsState(
                            targetValue = when {
                                i < currentSlide -> 1f
                                i == currentSlide -> 1f
                                else -> 0f
                            },
                            animationSpec = if (i == currentSlide && !isPaused) tween(5000, easing = LinearEasing) else snap()
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(if (i < currentSlide) 1f else if (i == currentSlide) progress else 0f)
                                    .background(BrandLime)
                            )
                        }
                    }
                }

                // Header info bar
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
                                .background(BrandLime.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 14.sp)
                        }
                        Column {
                            Text(
                                text = "Weekly Recap",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = data.dateRangeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            WeeklyRecapManager.markRecapAsViewed(context, data.weekId)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Slide Content
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInHorizontally { width -> if (targetState > initialState) width else -width })
                            .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally { width -> if (targetState > initialState) -width else width })
                    },
                    modifier = Modifier.weight(1f)
                ) { slideIndex ->
                    when (slideIndex) {
                        0 -> SlideWeeklyOverview(data, currencySymbol)
                        1 -> SlideInvestments(data, currencySymbol)
                        2 -> SlideCreditCards(data, currencySymbol)
                        3 -> SlideTopCategory(data, currencySymbol)
                        4 -> SlideHabitsAndWins(data, currencySymbol)
                    }
                }

                // Bottom Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tap right for next • Hold to pause",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = DarkOnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideWeeklyOverview(data: WeeklyRecapData, currencySymbol: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📊", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Last Week at a Glance",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You spent a total of",
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$currencySymbol${"%,.0f".format(data.totalSpent)}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 42.sp
            ),
            color = BrandLime
        )
        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (data.isSpendReduced) "🎉" else "📈", fontSize = 16.sp)
                Text(
                    text = if (data.prevWeekSpent > 0) {
                        if (data.isSpendReduced) "${kotlin.math.abs(data.wowDeltaPct)}% less than previous week"
                        else "${data.wowDeltaPct}% higher than previous week"
                    } else "First full week tracked!",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (data.isSpendReduced) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Across ${data.totalTxnCount} transactions • ${currencySymbol}${"%,.0f".format(data.dailyAvg)}/day avg",
            style = MaterialTheme.typography.bodySmall,
            color = DarkOnSurfaceVariant
        )
    }
}

@Composable
private fun SlideInvestments(data: WeeklyRecapData, currencySymbol: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚀", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Money Put to Work",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Invested & Saved for Future",
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$currencySymbol${"%,.0f".format(data.totalInvested)}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 42.sp
            ),
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🌱 Wealth Habit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = BrandLime)
                Text(
                    text = if (data.totalInvested > 0) "Every rupee saved compounds into financial freedom." else "Plan a small recurring SIP this week to boost savings!",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SlideCreditCards(data: WeeklyRecapData, currencySymbol: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💳", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Credit Cards & Dues",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Card Spends Last Week",
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$currencySymbol${"%,.0f".format(data.totalCreditCardSpent)}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 42.sp
            ),
            color = Color(0xFFA855F7)
        )
        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CreditCard, null, tint = Color(0xFFA855F7), modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        text = "Credit Card Tracker",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "Auto-tracked & ready for settlement",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideTopCategory(data: WeeklyRecapData, currencySymbol: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(getCategoryEmoji(data.topCategoryName), fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Top Spending Category",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${data.topCategoryName} took the lead",
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$currencySymbol${"%,.0f".format(data.topCategoryAmount)}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 42.sp
            ),
            color = Color(0xFFF59E0B)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${data.topCategoryPct}% of your total weekly spend",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DarkOnSurface
        )

        if (!data.topMerchant.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏷️", fontSize = 14.sp)
                    Text(
                        text = "Biggest single buy: ${data.topMerchant} ($currencySymbol${"%,.0f".format(data.topMerchantAmount)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideHabitsAndWins(data: WeeklyRecapData, currencySymbol: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Habits & Smart Wins",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Zero Spend Days
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎯", fontSize = 24.sp)
                    Text(
                        text = "${data.zeroSpendDaysCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = BrandLime
                    )
                    Text("Zero-Spend Days", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            // Card 2: Weekend Ratio
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📅", fontSize = 24.sp)
                    Text(
                        text = "${data.weekendSpendPct}%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFFF59E0B)
                    )
                    Text("Weekend Spend", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BrandLime.copy(alpha = 0.15f)
        ) {
            Text(
                text = "✨ Great momentum! Stay mindful this week.",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandLime,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
