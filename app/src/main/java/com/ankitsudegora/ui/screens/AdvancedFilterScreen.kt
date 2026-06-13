package com.ankitsudegora.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getIconVector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterScreen(
    categories: List<Category>,
    allTransactions: List<TransactionWithCategory>,
    onNavigateBack: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var minAmountStr by remember { mutableStateOf("") }
    var maxAmountStr by remember { mutableStateOf("") }
    var selectedTimebox by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "WEEK", "MONTH", "3_MONTHS", "CUSTOM"
    var transactionType by remember { mutableStateOf("ALL") } // "ALL", "DEBIT", "CREDIT"

    // Custom Date Range State
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Filtered transactions computed reactively
    val filteredTransactions = remember(
        allTransactions, query, selectedCategoryId, minAmountStr, maxAmountStr, selectedTimebox, transactionType, customStartDate, customEndDate
    ) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        allTransactions.filter { item ->
            val txn = item.transaction
            if (txn.isPending) return@filter false

            // 1. Keyword search
            if (query.isNotBlank()) {
                val matchesMerchant = txn.merchant.contains(query, ignoreCase = true)
                val matchesNotes = txn.notes?.contains(query, ignoreCase = true) == true
                if (!matchesMerchant && !matchesNotes) return@filter false
            }

            // 2. Category selection
            if (selectedCategoryId != null && txn.categoryId != selectedCategoryId) {
                return@filter false
            }

            // 3. Min Amount
            val minAmount = minAmountStr.toDoubleOrNull()
            if (minAmount != null && txn.amount < minAmount) {
                return@filter false
            }

            // 4. Max Amount
            val maxAmount = maxAmountStr.toDoubleOrNull()
            if (maxAmount != null && txn.amount > maxAmount) {
                return@filter false
            }

            // 5. Transaction Type
            if (transactionType != "ALL" && txn.type != transactionType) {
                return@filter false
            }

            // 6. Timebox filters
            when (selectedTimebox) {
                "TODAY" -> {
                    calendar.timeInMillis = now
                    val currentYear = calendar.get(Calendar.YEAR)
                    val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

                    calendar.timeInMillis = txn.timestamp
                    val itemYear = calendar.get(Calendar.YEAR)
                    val itemDay = calendar.get(Calendar.DAY_OF_YEAR)

                    if (currentYear != itemYear || currentDay != itemDay) return@filter false
                }
                "WEEK" -> {
                    if (now - txn.timestamp > 7L * 24 * 60 * 60 * 1000) return@filter false
                }
                "MONTH" -> {
                    calendar.timeInMillis = now
                    val currentYear = calendar.get(Calendar.YEAR)
                    val currentMonth = calendar.get(Calendar.MONTH)

                    calendar.timeInMillis = txn.timestamp
                    val itemYear = calendar.get(Calendar.YEAR)
                    val itemMonth = calendar.get(Calendar.MONTH)

                    if (currentYear != itemYear || currentMonth != itemMonth) return@filter false
                }
                "3_MONTHS" -> {
                    if (now - txn.timestamp > 90L * 24 * 60 * 60 * 1000) return@filter false
                }
                "CUSTOM" -> {
                    val start = customStartDate
                    val end = customEndDate
                    if (start != null && txn.timestamp < start) return@filter false
                    if (end != null && txn.timestamp > end) return@filter false
                }
            }

            true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("advanced_filter_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("filter_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(
                    text = "Advanced Search",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Search text input
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search payee, merchant, or notes") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_search_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Debit / Credit / All type filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    listOf("ALL" to "All", "DEBIT" to "Expenses", "CREDIT" to "Inflows").forEach { (typeKey, label) ->
                        val isSel = transactionType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { transactionType = typeKey }
                                .padding(vertical = 10.dp)
                                .testTag("type_filter_$typeKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" option
                    val isAllSelected = selectedCategoryId == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Categories") },
                        modifier = Modifier.testTag("cat_chip_all"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    categories.forEach { cat ->
                        val isSel = selectedCategoryId == cat.id
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            modifier = Modifier.testTag("cat_chip_${cat.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Amount Limits Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Amount Range (INR)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                minAmountStr = input
                            }
                        },
                        label = { Text("Min Amount") },
                        prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("min_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = maxAmountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                maxAmountStr = input
                            }
                        },
                        label = { Text("Max Amount") },
                        prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("max_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Date Presets Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "Anytime",
                        "TODAY" to "Today",
                        "WEEK" to "Last 7 Days",
                        "MONTH" to "This Month",
                        "3_MONTHS" to "Last 3 Months",
                        "CUSTOM" to "Custom Range"
                    ).forEach { (timeboxKey, label) ->
                        val isSel = selectedTimebox == timeboxKey
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTimebox = timeboxKey },
                            label = { Text(label) },
                            modifier = Modifier.testTag("time_chip_$timeboxKey"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (selectedTimebox == "CUSTOM") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Date Picker Button
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                customStartDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val cal = Calendar.getInstance()
                                    cal.set(y, m, d, 0, 0, 0)
                                    customStartDate = cal.timeInMillis
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("custom_start_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = customStartDate?.let { dateFormatter.format(Date(it)) } ?: "Start Date"
                            )
                        }

                        // End Date Picker Button
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                customEndDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val cal = Calendar.getInstance()
                                    cal.set(y, m, d, 23, 59, 59)
                                    customEndDate = cal.timeInMillis
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("custom_end_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = customEndDate?.let { dateFormatter.format(Date(it)) } ?: "End Date"
                            )
                        }
                    }
                }
            }
        }

        // Search Results Section
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matching Ledger Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredTransactions.size} found",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = "No results",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions match the selected criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { "filter_item_${it.transaction.id}" }) { item ->
                TransactionListItem(
                    item = item,
                    onEditClicked = { onEditTransaction(item.transaction.id) },
                    onDeleteClicked = { onDeleteTransaction(item.transaction) }
                )
            }
        }
    }
}
