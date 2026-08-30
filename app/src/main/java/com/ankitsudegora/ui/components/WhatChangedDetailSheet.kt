package com.ankitsudegora.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.ui.screens.CategoryDelta
import com.ankitsudegora.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatChangedDetailSheet(
    categoryChanges: List<CategoryDelta>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit
) {
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
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "What Changed This Month",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Text(
                        text = "Month-over-month category spending deltas",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkOnSurface)
                }
            }

            if (categoryChanges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No month-over-month category changes yet",
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
                    items(categoryChanges, key = { it.name }) { item ->
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
                                                .background(if (item.isIncrease) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconVector(item.iconName),
                                                contentDescription = item.name,
                                                tint = if (item.isIncrease) Color(0xFFEF4444) else Color(0xFF10B981),
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
                                                text = if (item.isIncrease) "↑ ${item.pctDelta}% vs last month" else "↓ ${kotlin.math.abs(item.pctDelta)}% vs last month",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (item.isIncrease) Color(0xFFEF4444) else Color(0xFF10B981)
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
                                            text = if (item.isIncrease) "$currencySymbol${"%,.0f".format(item.diff)} more" else "$currencySymbol${"%,.0f".format(kotlin.math.abs(item.diff))} less",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = DarkOnSurfaceVariant
                                        )
                                    }
                                }

                                // Sparkline Graph
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .padding(top = 4.dp)
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
}
