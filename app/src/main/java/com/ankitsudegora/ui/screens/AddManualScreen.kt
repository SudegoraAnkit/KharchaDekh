package com.ankitsudegora.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import com.ankitsudegora.ui.components.getIconVector
import java.text.SimpleDateFormat
import java.util.*

import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory

@Composable
fun AddManualScreen(
    categories: List<Category>,
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    onSaveTransaction: (amount: Double, type: String, merchant: String, categoryId: Long?, notes: String?, paymentMethod: String, timestamp: Long, recurringFrequency: String?, subCategory: String?, paidViaCcId: Long?, repaidCcId: Long?, selectedRepaidTxnIds: List<Long>) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") } // "DEBIT" or "CREDIT"
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var paymentMethod by remember { mutableStateOf("CASH") } // "CASH", "UPI", "CARD", "NETBANKING"

    var isRecurringChecked by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    var subCategory by remember { mutableStateOf<String?>(null) }

    // DateTime configuration
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val calendar = remember { Calendar.getInstance() }
    
    val filteredCategories = remember(categories, transactionType) {
        val inflowNames = setOf("salary", "refund", "interest", "other inflow")
        if (transactionType == "CREDIT") {
            categories.filter { it.name.lowercase() in inflowNames }
        } else {
            categories.filter { it.name.lowercase() !in inflowNames }
        }
    }

    LaunchedEffect(filteredCategories) {
        if (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull { it.name.lowercase() != "others" }?.id ?: filteredCategories.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedCategoryId) {
        subCategory = null
    }

    val subCategoryOptions = remember(selectedCategoryId, categories) {
        val selectedCat = categories.find { it.id == selectedCategoryId }
        when (selectedCat?.name?.lowercase()) {
            "interest" -> listOf("Savings Interest", "FD Interest", "PPF Interest", "Other Interest")
            "rent & maintenance" -> listOf("Home Rent", "Office Rent", "Vehicle Rent", "Equipment Rent")
            "sip/invest" -> listOf("Mutual Funds", "Stocks / Equity", "Provident Fund", "Gold / Real Estate", "Other Investment")
            "creditcard payment" -> listOf("HDFC Card", "SBI Card", "ICICI Card", "Other Credit Card")
            "courses" -> listOf("Professional Skills", "Academic", "Language / Hobby", "Certifications")
            "home maintenance" -> listOf("Repairs & Plumb", "Painting / Decor", "Cleaning Services", "Society Maintenance")
            "subscriptions" -> listOf("OTT / Streaming", "Software / Apps", "Gym / Health", "Newspaper / News")
            "domestic help" -> listOf("Maid Salary", "Cook Salary", "Driver Salary", "Security Guard")
            "insurance" -> listOf("Health Insurance", "Life Insurance", "Car / Bike Insurance", "Home/Home Contents")
            "taxes" -> listOf("Income Tax", "Property Tax", "Professional Tax", "Road Tax")
            "pets" -> listOf("Pet Food", "Vet / Medicines", "Toys & Grooming", "Pet Boarding")
            "gifts & charity" -> listOf("Festival Gifts", "Wedding Gifts", "NGO / Donations", "Birthday Gifts")
            "cashback & rewards" -> listOf("UPI Scratch Card", "Credit Card Cashback", "Refund Reward", "Referral Bonus")
            "freelance/side hustle" -> listOf("Consulting Work", "Writing/Content", "Software Gig", "Asset Sale")
            else -> emptyList()
        }
    }

    val dateFormater = remember { SimpleDateFormat("dd MMM YYYY", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("manual_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                text = "Quick Log Transaction",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            
            // Debit / Credit Segment Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
            ) {
                listOf("DEBIT" to "Spent", "CREDIT" to "Received").forEach { (typeKey, label) ->
                    val isSel = transactionType == typeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { transactionType = typeKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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

        // Amount Display & Input
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (transactionType == "DEBIT") "Amount Spent" else "Amount Received",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    TextField(
                        value = amountStr,
                        onValueChange = { input ->
                            // Permit positive numbers with up to 2 decimal places
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountStr = input
                            }
                        },
                        placeholder = {
                            Text(
                                "0.00",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .testTag("amount_input_field")
                    )
                }
            }
        }

        // Merchant Input
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Merchant / Payee Name (e.g., Zomato, Swiggy, Grofers)") },
            placeholder = { Text("Where did you spend/get this?") },
            leadingIcon = { Icon(Icons.Default.Storefront, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("merchant_input_field"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp)
        )

        // Date Picker Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable {
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(context, { _, y, m, d ->
                        calendar.set(Calendar.YEAR, y)
                        calendar.set(Calendar.MONTH, m)
                        calendar.set(Calendar.DAY_OF_MONTH, d)
                        selectedTimestamp = calendar.timeInMillis
                    }, year, month, day).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Date of Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = dateFormater.format(Date(selectedTimestamp)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.secondary)
        }

        // Fast Category Selector Grid Title
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Adaptive visual Category quick-select matrix
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = 4

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCategories.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            val isSel = selectedCategoryId == cat.id
                            val containerCol = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val contentCol = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            val borderThickness = if (isSel) 2.dp else 1.dp
                            val borderCol = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                            Card(
                                onClick = { selectedCategoryId = cat.id },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerCol),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("cat_tile_${cat.name.lowercase()}"),
                                border = androidx.compose.foundation.BorderStroke(borderThickness, borderCol)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(cat.iconResName),
                                        contentDescription = cat.name,
                                        tint = contentCol,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = contentCol,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        // Fill empty items to balance spacing if final row is not complete
                        if (row.size < columns) {
                            repeat(columns - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Detail sub-category selection row
        if (subCategoryOptions.isNotEmpty()) {
            Text(
                text = "Select Detail Type",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subCategoryOptions.forEach { option ->
                    val isSel = subCategory == option
                    FilterChip(
                        selected = isSel,
                        onClick = { subCategory = if (isSel) null else option },
                        label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Payment Mode Radio selection block
        Text(
            text = "Payment Mode",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "CASH" to Icons.Default.Money,
                "UPI" to Icons.Default.QrCodeScanner,
                "CARD" to Icons.Default.CreditCard,
                "NETBANKING" to Icons.Default.AccountBalance
            ).forEach { (method, iconVec) ->
                val isSel = paymentMethod == method
                val tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { paymentMethod = method }
                        .padding(vertical = 8.dp)
                        .testTag("pay_mode_$method")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = method,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = method,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Paid via Credit Card Selector
        var paidViaCcChecked by remember { mutableStateOf(false) }
        var selectedCcId by remember { mutableStateOf<Long?>(null) }
        var ccDropdownExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(creditCards) {
            if (selectedCcId == null && creditCards.isNotEmpty()) {
                selectedCcId = creditCards.first().id
            }
        }

        // Repayment fields
        var selectedRepaidCcId by remember { mutableStateOf<Long?>(null) }
        var repaymentCcDropdownExpanded by remember { mutableStateOf(false) }
        var selectedRepaidTxnIds by remember { mutableStateOf(setOf<Long>()) }

        LaunchedEffect(creditCards) {
            if (selectedRepaidCcId == null && creditCards.isNotEmpty()) {
                selectedRepaidCcId = creditCards.first().id
            }
        }

        val creditCardPaymentCategory = remember(categories) {
            categories.find { it.name == "CreditCard Payment" }
        }

        val isCcRepaymentSelected = selectedCategoryId == creditCardPaymentCategory?.id && transactionType == "DEBIT"

        // Unbilled transactions checklist
        val unbilledTxnsForCc = remember(allTransactions, selectedRepaidCcId) {
            allTransactions.filter {
                it.transaction.paidViaCcId == selectedRepaidCcId &&
                it.transaction.ccRepaymentId == null &&
                it.transaction.type == "DEBIT"
            }
        }

        // Auto check all unbilled items when card changes
        LaunchedEffect(selectedRepaidCcId, unbilledTxnsForCc) {
            selectedRepaidTxnIds = unbilledTxnsForCc.map { it.transaction.id }.toSet()
        }

        // UI for Paid via Credit Card (Shown for DEBIT, unless it is a CC repayment itself)
        if (transactionType == "DEBIT" && !isCcRepaymentSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Paid via Credit Card", fontWeight = FontWeight.SemiBold)
                        }
                        Switch(
                            checked = paidViaCcChecked,
                            onCheckedChange = {
                                paidViaCcChecked = it
                                if (it && selectedCcId == null && creditCards.isNotEmpty()) {
                                    selectedCcId = creditCards.first().id
                                }
                            }
                        )
                    }

                    if (paidViaCcChecked) {
                        if (creditCards.isEmpty()) {
                            Text(
                                "No saved credit cards. Please add one in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            val selectedCc = creditCards.find { it.id == selectedCcId } ?: creditCards.firstOrNull()
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { ccDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedCc?.cardName ?: "Select Credit Card")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = ccDropdownExpanded,
                                    onDismissRequest = { ccDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    creditCards.forEach { card ->
                                        DropdownMenuItem(
                                            text = { Text(card.cardName) },
                                            onClick = {
                                                selectedCcId = card.id
                                                ccDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // UI for CreditCard Payment Category selection (Repayment checklist)
        if (isCcRepaymentSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Credit Card Repayment Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Select the credit card you are paying off:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (creditCards.isEmpty()) {
                        Text(
                            "No saved credit cards. Please add one in Settings first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        val selectedRepaidCc = creditCards.find { it.id == selectedRepaidCcId } ?: creditCards.firstOrNull()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { repaymentCcDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(selectedRepaidCc?.cardName ?: "Select Credit Card")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = repaymentCcDropdownExpanded,
                                onDismissRequest = { repaymentCcDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                creditCards.forEach { card ->
                                    DropdownMenuItem(
                                        text = { Text(card.cardName) },
                                        onClick = {
                                            selectedRepaidCcId = card.id
                                            repaymentCcDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (unbilledTxnsForCc.isEmpty()) {
                            Text(
                                "No unbilled transactions found for this card.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "Select transactions to settle (will automatically update total amount):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Transaction list checklist
                            unbilledTxnsForCc.forEach { item ->
                                val isChecked = selectedRepaidTxnIds.contains(item.transaction.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds - item.transaction.id
                                            } else {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds + item.transaction.id
                                            }
                                            // Recalculate amount if user settles matching items
                                            val newTotal = unbilledTxnsForCc.filter { selectedRepaidTxnIds.contains(it.transaction.id) }.sumOf { it.transaction.amount }
                                            if (newTotal > 0.0) {
                                                amountStr = newTotal.toString()
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked == true) {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds + item.transaction.id
                                            } else {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds - item.transaction.id
                                            }
                                            val newTotal = unbilledTxnsForCc.filter { selectedRepaidTxnIds.contains(it.transaction.id) }.sumOf { it.transaction.amount }
                                            if (newTotal > 0.0) {
                                                amountStr = newTotal.toString()
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val dateFormatted = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(item.transaction.timestamp))
                                        Text(item.transaction.merchant, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("$dateFormatted • ${item.category?.name ?: "Uncategorized"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("₹${"%,.0f".format(item.transaction.amount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Optional Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            placeholder = { Text("Add transaction notes e.g., dinner with friends, travel fuel") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp)
        )

        // Recurring designation card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("add_recurring_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (isRecurringChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Designate as Recurring",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-trigger transactions on schedule",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = isRecurringChecked,
                        onCheckedChange = { isRecurringChecked = it },
                        modifier = Modifier.testTag("add_recurring_switch")
                    )
                }

                if (isRecurringChecked) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text(
                        text = "Select Schedule Frequency",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly").forEach { (freqKey, label) ->
                            val isSel = selectedFrequency == freqKey
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFrequency = freqKey },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f).testTag("add_freq_chip_$freqKey"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                val freq = if (isRecurringChecked) selectedFrequency else null
                val ccId = if (paidViaCcChecked && transactionType == "DEBIT" && !isCcRepaymentSelected) selectedCcId else null
                val repCcId = if (isCcRepaymentSelected) selectedRepaidCcId else null
                val repTxnIds = if (isCcRepaymentSelected) selectedRepaidTxnIds.toList() else emptyList()

                onSaveTransaction(
                    amt,
                    transactionType,
                    merchant.trim(),
                    selectedCategoryId,
                    notes.trim().ifBlank { null },
                    paymentMethod,
                    selectedTimestamp,
                    freq,
                    subCategory,
                    ccId,
                    repCcId,
                    repTxnIds
                )
            },
            enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_transaction_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Text(
                    text = "Save Ledger Entry",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
