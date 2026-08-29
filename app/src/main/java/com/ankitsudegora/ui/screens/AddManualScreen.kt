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
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddManualScreen(
    categories: List<Category>,
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    isMultiCurrencyEnabled: Boolean,
    primaryCurrency: String,
    onSaveTransaction: (amount: Double, type: String, merchant: String, categoryId: Long?, notes: String?, paymentMethod: String, timestamp: Long, recurringFrequency: String?, subCategory: String?, paidViaCcId: Long?, repaidCcId: Long?, selectedRepaidTxnIds: List<Long>, currency: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") } // "DEBIT" or "CREDIT"
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var paymentMethod by remember { mutableStateOf("UPI") } // "CASH", "UPI", "CARD", "NETBANKING"
    var selectedCurrency by remember(primaryCurrency) { mutableStateOf(primaryCurrency) }

    var isRecurringChecked by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    var subCategory by remember { mutableStateOf<String?>(null) }

    // Paid via Credit Card Selector
    var paidViaCcChecked by remember { mutableStateOf(false) }
    var selectedCcId by remember { mutableStateOf<Long?>(null) }
    var ccDropdownExpanded by remember { mutableStateOf(false) }

    // Repayment fields
    var selectedRepaidCcId by remember { mutableStateOf<Long?>(null) }
    var repaymentCcDropdownExpanded by remember { mutableStateOf(false) }
    var selectedRepaidTxnIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(creditCards) {
        if (selectedCcId == null && creditCards.isNotEmpty()) {
            selectedCcId = creditCards.first().id
        }
        if (selectedRepaidCcId == null && creditCards.isNotEmpty()) {
            selectedRepaidCcId = creditCards.first().id
        }
    }

    val creditCardPaymentCategory = remember(categories) {
        categories.find { it.name == "CreditCard Payment" }
    }

    val isCcRepaymentSelected = selectedCategoryId == creditCardPaymentCategory?.id && transactionType == "DEBIT"

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

    val dateFormater = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("manual_back_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = DarkOnSurface
                )
            }

            Text(
                text = if (transactionType == "DEBIT") "Add Expense" else "Add Income",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = DarkOnSurface
            )

            IconButton(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && merchant.isNotBlank()) {
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
                            repTxnIds,
                            selectedCurrency
                        )
                    }
                },
                enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank()) BrandLime else DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank()) DarkBackground else DarkOnSurfaceVariant
                )
            }
        }

        // Expense / Income Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .padding(4.dp)
        ) {
            listOf("DEBIT" to "Expense", "CREDIT" to "Income").forEach { (typeKey, label) ->
                val isSel = transactionType == typeKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) BrandLime else Color.Transparent)
                        .clickable { transactionType = typeKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSel) DarkBackground else DarkOnSurfaceVariant
                    )
                }
            }
        }


        // Amount Display & Input Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (transactionType == "DEBIT") "Amount Spent" else "Amount Received",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMultiCurrencyEnabled) {
                        var currencyDropdownExpanded by remember { mutableStateOf(false) }
                        val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AED", "AUD", "CAD", "SGD")

                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                            Surface(
                                onClick = { currencyDropdownExpanded = true },
                                color = BrandLimeContainer,
                                contentColor = BrandOnLimeContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = getCurrencySymbol(selectedCurrency),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Currency",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = currencyDropdownExpanded,
                                onDismissRequest = { currencyDropdownExpanded = false }
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("$curr (${getCurrencySymbol(curr)})", fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            selectedCurrency = curr
                                            currencyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = getCurrencySymbol(primaryCurrency),
                            style = MoneyTypography.HeroAmount.copy(color = BrandLime),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    TextField(
                        value = amountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountStr = input
                            }
                        },
                        placeholder = {
                            Text(
                                "0",
                                style = MoneyTypography.HeroAmount.copy(color = DarkOnSurfaceVariant.copy(alpha = 0.3f))
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
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = DarkOnSurface,
                            unfocusedTextColor = DarkOnSurface
                        ),
                        textStyle = MoneyTypography.HeroAmount.copy(
                            color = DarkOnSurface,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .testTag("amount_input_field")
                    )
                }
            }
        }

        // Merchant Input
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Merchant / Payee Name", color = DarkOnSurfaceVariant) },
            placeholder = { Text("e.g. Swiggy, Uber, Amazon, D-Mart", color = DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Storefront, null, tint = BrandLime) },
            trailingIcon = if (merchant.isNotEmpty()) {
                {
                    IconButton(onClick = { merchant = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkOnSurfaceVariant)
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("merchant_input_field"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = BrandLime,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = DarkOnSurface,
                unfocusedTextColor = DarkOnSurface
            )
        )

        // Date Picker Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
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
                    tint = BrandLime
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Date of Expense", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                    Text(
                        text = dateFormater.format(Date(selectedTimestamp)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                }
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = DarkOnSurfaceVariant)
        }

        // Fast Category Selector Grid Title
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface
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
                            val catColor = CategoryColors.getCategoryColor(cat.name)
                            val containerCol = if (isSel) DarkSurfaceElevated else DarkSurface
                            val borderCol = if (isSel) BrandLime else DarkBorder

                            Card(
                                onClick = { selectedCategoryId = cat.id },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = containerCol),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("cat_tile_${cat.name.lowercase()}"),
                                border = androidx.compose.foundation.BorderStroke(if (isSel) 1.5.dp else 1.dp, borderCol)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = if (isSel) 0.25f else 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(cat.iconResName),
                                            contentDescription = cat.name,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSel) DarkOnSurface else DarkOnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
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
                text = "Detail Subcategory",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkOnSurface
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
                            selectedContainerColor = BrandLimeContainer,
                            selectedLabelColor = BrandOnLimeContainer,
                            containerColor = DarkSurface,
                            labelColor = DarkOnSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSel) BrandLime else DarkBorder,
                            selectedBorderColor = BrandLime,
                            enabled = true,
                            selected = isSel
                        )
                    )
                }
            }
        }

        // Payment Mode Radio selection block
        Text(
            text = "Payment Mode",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DarkOnSurface
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "UPI" to Icons.Default.QrCodeScanner,
                "CARD" to Icons.Default.CreditCard,
                "CASH" to Icons.Default.Money,
                "NETBANKING" to Icons.Default.AccountBalance
            ).forEach { (method, iconVec) ->
                val isSel = paymentMethod == method
                val tint = if (isSel) BrandLime else DarkOnSurfaceVariant
                
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
                            .background(if (isSel) BrandLimeContainer else DarkSurfaceVariant),
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
                        color = if (isSel) BrandLime else DarkOnSurfaceVariant
                    )
                }
            }
        }


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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
                                tint = BrandLime
                            )
                            Text("Paid via Credit Card", fontWeight = FontWeight.SemiBold, color = DarkOnSurface)
                        }
                        Switch(
                            checked = paidViaCcChecked,
                            onCheckedChange = {
                                paidViaCcChecked = it
                                if (it && selectedCcId == null && creditCards.isNotEmpty()) {
                                    selectedCcId = creditCards.first().id
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = BrandLime
                            )
                        )
                    }

                    if (paidViaCcChecked) {
                        if (creditCards.isEmpty()) {
                            Text(
                                "No saved credit cards. Please add one in Cards tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkError
                            )
                        } else {
                            val selectedCc = creditCards.find { it.id == selectedCcId } ?: creditCards.firstOrNull()
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { ccDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkOnSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text(selectedCc?.cardName ?: "Select Credit Card")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = ccDropdownExpanded,
                                    onDismissRequest = { ccDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(DarkSurface)
                                ) {
                                    creditCards.forEach { card ->
                                        DropdownMenuItem(
                                            text = { Text(card.cardName, color = DarkOnSurface) },
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Credit Card Repayment Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOnSurface
                    )

                    Text(
                        text = "Select the credit card you are paying off:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )

                    if (creditCards.isEmpty()) {
                        Text(
                            "No saved credit cards. Please add one in Cards tab first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkError
                        )
                    } else {
                        val selectedRepaidCc = creditCards.find { it.id == selectedRepaidCcId } ?: creditCards.firstOrNull()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { repaymentCcDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkOnSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                            ) {
                                Text(selectedRepaidCc?.cardName ?: "Select Credit Card")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = repaymentCcDropdownExpanded,
                                onDismissRequest = { repaymentCcDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(DarkSurface)
                            ) {
                                creditCards.forEach { card ->
                                    DropdownMenuItem(
                                        text = { Text(card.cardName, color = DarkOnSurface) },
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
                                color = DarkOnSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Select transactions to settle:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurfaceVariant
                            )

                            unbilledTxnsForCc.forEach { item ->
                                val isChecked = selectedRepaidTxnIds.contains(item.transaction.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChecked) DarkSurfaceVariant else Color.Transparent)
                                        .clickable {
                                            if (isChecked) {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds - item.transaction.id
                                            } else {
                                                selectedRepaidTxnIds = selectedRepaidTxnIds + item.transaction.id
                                            }
                                            val newTotal = unbilledTxnsForCc.filter { selectedRepaidTxnIds.contains(it.transaction.id) }.sumOf { it.transaction.amount }
                                            if (newTotal > 0.0) {
                                                amountStr = newTotal.toString()
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
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
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = BrandLime,
                                            checkmarkColor = DarkBackground
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val dateFormatted = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(item.transaction.timestamp))
                                        Text(item.transaction.merchant, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = DarkOnSurface)
                                        Text("$dateFormatted • ${item.category?.name ?: "Uncategorized"}", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceVariant)
                                    }
                                    Text("₹${"%,.0f".format(item.transaction.amount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = DarkOnSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)", color = DarkOnSurfaceVariant) },
            placeholder = { Text("e.g., lunch with friends, fuel", color = DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = BrandLime) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = BrandLime,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = DarkOnSurface,
                unfocusedTextColor = DarkOnSurface
            )
        )

        // Recurring designation card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_recurring_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            tint = if (isRecurringChecked) BrandLime else DarkOnSurfaceVariant
                        )
                        Column {
                            Text(
                                text = "Designate as Recurring",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = "Auto-trigger transactions on schedule",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isRecurringChecked,
                        onCheckedChange = { isRecurringChecked = it },
                        modifier = Modifier.testTag("add_recurring_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = BrandLime
                        )
                    )
                }

                if (isRecurringChecked) {
                    HorizontalDivider(color = DarkBorder)
                    
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurfaceVariant
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
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_freq_chip_$freqKey"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandLimeContainer,
                                    selectedLabelColor = BrandOnLimeContainer,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = DarkOnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                    repTxnIds,
                    selectedCurrency
                )
            },
            enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandLime,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceVariant,
                disabledContentColor = DarkOnSurfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_transaction_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Text(
                    text = "Save Expense",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
        
    }
}

fun getCurrencySymbol(code: String): String {
    return when (code.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "AED" -> "د.إ"
        "AUD" -> "A$"
        "CAD" -> "C$"
        "SGD" -> "S$"
        else -> code
    }
}

