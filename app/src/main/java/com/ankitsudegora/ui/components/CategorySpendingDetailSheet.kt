package com.ankitsudegora.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.theme.*
import java.util.*

data class CategorySpendItem(
    val categoryId: Long,
    val name: String,
    val iconResName: String,
    val amount: Double,
    val percentage: Float,
    val txnCount: Int,
    val trendData: List<Double>,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpendingDetailSheet(
    allTransactions: List<TransactionWithCategory>,
    categories: List<Category>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit
) {
    var selectedCycle by remember { mutableStateOf("MONTH") } // "MONTH", "YEAR", "ALL"
    var searchQuery by remember { mutableStateOf("") }

    val nonPending = remember(allTransactions) {
        allTransactions.filter { !it.transaction.isPending && it.transaction.type.equals("DEBIT", ignoreCase = true) }
    }

    val cal = remember { Calendar.getInstance() }
    val curMonth = remember { cal.get(Calendar.MONTH) }
    val curYear = remember { cal.get(Calendar.YEAR) }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }

    // Filter transactions based on cycle
    val cycleTxns = remember(nonPending, selectedCycle) {
        when (selectedCycle) {
            "MONTH" -> nonPending.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.transaction.timestamp }
                c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear
            }
            "YEAR" -> nonPending.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.transaction.timestamp }
                c.get(Calendar.YEAR) == curYear
            }
            else -> nonPending
        }
    }

    val totalCycleExpense = remember(cycleTxns) {
        cycleTxns.sumOf { it.transaction.amount }.coerceAtLeast(0.01)
    }

    val categoryColors = remember {
        listOf(
            Color(0xFF38BDF8), Color(0xFFEC4899), Color(0xFFF97316),
            Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFEF4444),
            Color(0xFF84CC16), Color(0xFF06B6D4), Color(0xFFA855F7)
        )
    }

    val categorySpendItems: List<CategorySpendItem> = remember(cycleTxns, categories, selectedCycle, searchQuery) {
        val catMap = mutableMapOf<Long, MutableList<TransactionWithCategory>>()
        cycleTxns.forEach { txn ->
            val catId = txn.category?.id ?: -1L
            catMap.getOrPut(catId) { mutableListOf() }.add(txn)
        }

        val itemsList = catMap.entries.toList().mapIndexed { idx, entry ->
            val catId = entry.key
            val txns = entry.value
            val cat = categories.find { it.id == catId }
            val catName = cat?.name ?: if (catId == -1L) "Other" else "General"
            val iconName = cat?.iconResName ?: "tag"
            val amount = txns.sumOf { it.transaction.amount }
            val pct = (amount / totalCycleExpense).toFloat().coerceIn(0f, 1f)
            val color = categoryColors[idx % categoryColors.size]

            // Compute trend data
            val trendData: List<Double> = if (selectedCycle == "YEAR") {
                val monthMap = (0..11).associateWith { 0.0 }.toMutableMap()
                txns.forEach { t ->
                    val c = Calendar.getInstance().apply { timeInMillis = t.transaction.timestamp }
                    val m = c.get(Calendar.MONTH)
                    monthMap[m] = (monthMap[m] ?: 0.0) + t.transaction.amount
                }
                (0..11).map { monthMap[it] ?: 0.0 }
            } else {
                val dayMap = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
                txns.forEach { t ->
                    val c = Calendar.getInstance().apply { timeInMillis = t.transaction.timestamp }
                    val d = c.get(Calendar.DAY_OF_MONTH)
                    dayMap[d] = (dayMap[d] ?: 0.0) + t.transaction.amount
                }
                (1..daysInMonth).map { dayMap[it] ?: 0.0 }
            }

            CategorySpendItem(
                categoryId = catId,
                name = catName,
                iconResName = iconName,
                amount = amount,
                percentage = pct,
                txnCount = txns.size,
                trendData = trendData,
                color = color
            )
        }.sortedByDescending { it.amount }

        if (searchQuery.isBlank()) itemsList
        else itemsList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = DarkBorder,
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "Total Outflow: $currencySymbol${"%,.0f".format(totalCycleExpense)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkOnSurface)
                }
            }

            // Cycle Segmented Toggle [ This Month | This Year | All Time ]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(
                        "MONTH" to "This Month",
                        "YEAR" to "This Year",
                        "ALL" to "All Time"
                    ).forEach { (cycleKey, label) ->
                        val isSelected = selectedCycle == cycleKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandLime else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCycle = cycleKey }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) DarkBackground else DarkOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Category Items List
            if (categorySpendItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending recorded in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkOnSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(categorySpendItems, key = { it.categoryId }) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
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
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(item.color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconVector(item.iconResName),
                                                contentDescription = item.name,
                                                tint = item.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = DarkOnSurface
                                            )
                                            Text(
                                                text = "${item.txnCount} transactions",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = DarkOnSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol${"%,.0f".format(item.amount)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                            color = DarkOnSurface
                                        )
                                        Text(
                                            text = "${(item.percentage * 100).toInt()}% of total",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = item.color
                                        )
                                    }
                                }

                                // Animated Progress Bar
                                val animatedProgress by animateFloatAsState(
                                    targetValue = item.percentage,
                                    animationSpec = tween(600),
                                    label = "category_progress"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                                            .background(item.color)
                                    )
                                }

                                // Trend Sparkline Curve
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .padding(top = 4.dp)
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val data: List<Double> = item.trendData
                                    val maxV: Double = (data.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                                    val path = Path()

                                    data.forEachIndexed { i: Int, v: Double ->
                                        val x = (i.toFloat() / (data.size.coerceAtLeast(2) - 1)) * w
                                        val y = h - ((v / maxV) * (h * 0.8f)).toFloat() - (h * 0.1f)
                                        if (i == 0) path.moveTo(x, y)
                                        else {
                                            val prevX = ((i - 1).toFloat() / (data.size.coerceAtLeast(2) - 1)) * w
                                            val prevY = h - ((data[i - 1] / maxV) * (h * 0.8f)).toFloat() - (h * 0.1f)
                                            val cx = (prevX + x) / 2f
                                            path.cubicTo(cx, prevY, cx, y, x, y)
                                        }
                                    }

                                    drawPath(
                                        path = path,
                                        color = item.color.copy(alpha = 0.85f),
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
}
