package com.ankitsudegora.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.viewmodel.AnalyticsState
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyCashFlow(
    val monthLabel: String,
    val inflow: Double,
    val outflow: Double
)

@Composable
fun ChartsScreen(
    analytics: AnalyticsState,
    allTransactions: List<TransactionWithCategory>
) {
    // Animate drawing progress for the micro-animations
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val monthlyData = remember(allTransactions) {
        val result = mutableListOf<MonthlyCashFlow>()
        
        // Compute last 6 calendar months
        for (i in 5 downTo 0) {
            val calMonth = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val year = calMonth.get(Calendar.YEAR)
            val month = calMonth.get(Calendar.MONTH)
            val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(calMonth.time)
            
            val txnsInMonth = allTransactions.filter { item ->
                val tCal = Calendar.getInstance().apply { timeInMillis = item.transaction.timestamp }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month && !item.transaction.isPending
            }
            
            var inflow = 0.0
            var outflow = 0.0
            txnsInMonth.forEach { item ->
                val amt = item.transaction.amount
                if (item.transaction.type == "CREDIT") {
                    inflow += amt
                } else if (item.transaction.type == "DEBIT") {
                    if (item.category?.name != "CreditCard Payment") {
                        outflow += amt
                    }
                }
            }
            result.add(MonthlyCashFlow(monthLabel, inflow, outflow))
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

        // 1. Doughnut Chart Card
        item {
            DoughnutChartCard(
                analytics = analytics,
                animationProgress = animationProgress.value
            )
        }

        // 2. Inflow vs Outflow Trend Card
        item {
            CashFlowTrendCard(
                monthlyData = monthlyData,
                animationProgress = animationProgress.value
            )
        }
    }
}

@Composable
fun DoughnutChartCard(
    analytics: AnalyticsState,
    animationProgress: Float
) {
    val totalExpense = analytics.totalExpense
    
    // Theme-aware color palette
    val chartColors = listOf(
        Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
        Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFFFFCA28),
        Color(0xFF78909C), Color(0xFF26C6DA), Color(0xFF9CCC65), Color(0xFF5C6BC0)
    )

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

            if (totalExpense <= 0 || analytics.categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transaction data to display",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        analytics.categoryBreakdown.forEachIndexed { index, item ->
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
                            text = "₹${"%,.0f".format(totalExpense)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Breakdown list representation
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    analytics.categoryBreakdown.forEachIndexed { index, usage ->
                        val color = chartColors[index % chartColors.size]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${"%,.0f".format(usage.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(color.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${usage.percentage.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = color
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
fun CashFlowTrendCard(
    monthlyData: List<MonthlyCashFlow>,
    animationProgress: Float
) {
    val density = LocalDensity.current
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val textLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    
    val greenColor = Color(0xFF4CAF50)
    val redColor = Color(0xFFEF5350)

    val maxVal = remember(monthlyData) {
        val maxAmount = monthlyData.maxOfOrNull { maxOf(it.inflow, it.outflow) } ?: 0.0
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
            Text(
                text = "Inflow vs Outflow Trend",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenColor))
                    Text("Inflow (Credits)", style = MaterialTheme.typography.bodySmall, color = textLabelColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(redColor))
                    Text("Outflow (Debits)", style = MaterialTheme.typography.bodySmall, color = textLabelColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Reserve margins for axis labels
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
                    val columnsCount = monthlyData.size
                    val colWidth = plotWidth / columnsCount
                    val barWidth = colWidth * 0.3f // each bar uses 30% of column width

                    monthlyData.forEachIndexed { index, data ->
                        val centerX = yLabelWidth + (index * colWidth) + (colWidth / 2)
                        
                        // Scale factors
                        val inflowHeight = (data.inflow / maxVal) * plotHeight * animationProgress
                        val outflowHeight = (data.outflow / maxVal) * plotHeight * animationProgress

                        // Draw Inflow bar (Green)
                        val inflowTop = plotHeight - inflowHeight
                        drawRect(
                            brush = Brush.verticalGradient(listOf(greenColor, greenColor.copy(alpha = 0.7f))),
                            topLeft = Offset(centerX - barWidth - 4f, inflowTop.toFloat()),
                            size = Size(barWidth, inflowHeight.toFloat())
                        )

                        // Draw Outflow bar (Red)
                        val outflowTop = plotHeight - outflowHeight
                        drawRect(
                            brush = Brush.verticalGradient(listOf(redColor, redColor.copy(alpha = 0.7f))),
                            topLeft = Offset(centerX + 4f, outflowTop.toFloat()),
                            size = Size(barWidth, outflowHeight.toFloat())
                        )
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
                    monthlyData.forEach { data ->
                        Text(
                            text = data.monthLabel,
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
