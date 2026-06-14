package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.data.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    allTransactions: List<TransactionWithCategory>,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onExportCsv: (List<TransactionWithCategory>) -> Unit,
    onExportPdf: (List<TransactionWithCategory>) -> Unit
) {
    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Calendar?>(Calendar.getInstance()) }

    val currentYear = calendarState.get(Calendar.YEAR)
    val currentMonth = calendarState.get(Calendar.MONTH)

    val monthName = remember(calendarState) {
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        format.format(calendarState.time)
    }

    // Generate calendar grid dates
    val calendarDays = remember(currentYear, currentMonth) {
        val days = mutableListOf<Calendar?>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Pad days before start of month (assuming Sunday is first day of week)
        for (i in Calendar.SUNDAY until firstDayOfWeek) {
            days.add(null)
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            days.add(dayCal)
        }

        // Pad days after end of month to align grid
        val totalCells = ((days.size + 6) / 7) * 7
        while (days.size < totalCells) {
            days.add(null)
        }
        days
    }

    // Group transactions by date for fast lookup
    val dailyDebitTotals = remember(allTransactions, currentYear, currentMonth) {
        val totals = mutableMapOf<Int, Double>()
        val cal = Calendar.getInstance()
        allTransactions.filter { !it.transaction.isPending && it.transaction.type == "DEBIT" }.forEach { item ->
            cal.timeInMillis = item.transaction.timestamp
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                totals[day] = totals.getOrDefault(day, 0.0) + item.transaction.amount
            }
        }
        totals
    }

    // Transactions matching the selected date
    val selectedTransactions = remember(allTransactions, selectedDate) {
        selectedDate?.let { sel ->
            allTransactions.filter { item ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = item.transaction.timestamp
                cal.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == sel.get(Calendar.MONTH) &&
                cal.get(Calendar.DAY_OF_MONTH) == sel.get(Calendar.DAY_OF_MONTH) &&
                !item.transaction.isPending
            }
        } ?: emptyList()
    }

    val monthTransactions = remember(allTransactions, currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        allTransactions.filter { item ->
            cal.timeInMillis = item.transaction.timestamp
            cal.get(Calendar.YEAR) == currentYear &&
            cal.get(Calendar.MONTH) == currentMonth &&
            !item.transaction.isPending
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Title & Switcher Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar View",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            calendarState = (calendarState.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month")
                    }

                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            calendarState = (calendarState.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(Icons.Default.ChevronRight, "Next Month")
                    }
                }
            }
        }

        // Trust Nudge for Calendar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Calculated 100% offline. No data leaves your mobile device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("calendar_monthly_summary_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Total Spend",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val monthlyTotal = dailyDebitTotals.values.sum()
                            Text(
                                text = "₹${"%,.2f".format(monthlyTotal)}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportCsv(monthTransactions) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Export CSV", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { onExportPdf(monthTransactions) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Export PDF", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Weekday Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp)
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                // Placeholder for week total column header
                Text(
                    text = "Total",
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Grid Weeks Row
        val weeks = calendarDays.chunked(7)
        items(weeks) { weekDays ->
            // Calculate week total spent
            val weekTotal = weekDays.filterNotNull().sumOf { dayCal ->
                dailyDebitTotals[dayCal.get(Calendar.DAY_OF_MONTH)] ?: 0.0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Render days of the week
                weekDays.forEach { dayCal ->
                    if (dayCal == null) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                        val totalSpent = dailyDebitTotals[dayNum] ?: 0.0
                        val isSelected = selectedDate?.let {
                            it.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                            it.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                            it.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
                        } ?: false

                        val cellBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cellBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                .clickable { selectedDate = dayCal }
                                .testTag("cal_day_$dayNum")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )

                                if (totalSpent > 0.0) {
                                    Text(
                                        text = "₹${totalSpent.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(1.dp))
                                }
                            }
                        }
                    }
                }

                // Render week total column
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(start = 6.dp)
                        .height(44.dp)
                        .testTag("week_total_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹${weekTotal.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Selected Day Details Header
        item {
            val selectedDateStr = selectedDate?.let {
                val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                format.format(it.time)
            } ?: "No date selected"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity on $selectedDateStr",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    val dailySum = selectedTransactions.filter { it.transaction.type == "DEBIT" }.sumOf { it.transaction.amount }
                    if (dailySum > 0.0) {
                        Text(
                            text = "Spent: ₹${"%,.2f".format(dailySum)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Selected Day Transactions List
        if (selectedTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ledger activities recorded for this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            items(selectedTransactions, key = { "cal_txn_${it.transaction.id}" }) { item ->
                TransactionListItem(
                    item = item,
                    onEditClicked = { onEditTransaction(item.transaction.id) },
                    onDeleteClicked = { onDeleteTransaction(item.transaction) }
                )
            }
        }
    }
}
