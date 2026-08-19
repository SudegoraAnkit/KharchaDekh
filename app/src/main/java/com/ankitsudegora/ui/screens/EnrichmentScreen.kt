package com.ankitsudegora.ui.screens

import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.data.PlannedListWithItems

@Composable
fun EnrichmentScreen(
    transactionId: Long,
    categories: List<Category>,
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    allPlannedLists: List<PlannedListWithItems>,
    onGetTransaction: suspend (Long) -> Transaction?,
    onFinalizeTransaction: (id: Long, categoryId: Long, notes: String?, amount: Double, merchant: String, type: String, recurringFrequency: String?, subCategory: String?, paidViaCcId: Long?, repaidCcId: Long?, selectedRepaidTxnIds: List<Long>, linkedListId: Long?, refundedTxnId: Long?) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val scrollState = rememberScrollState()
    
    var existingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") } // DEBIT / CREDIT
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subCategory by remember { mutableStateOf<String?>(null) }

    var selectedRefundedTxnId by remember { mutableStateOf<Long?>(null) }
    var selectedLinkedListId by remember { mutableStateOf<Long?>(null) }
    var refundDropdownExpanded by remember { mutableStateOf(false) }
    var listDropdownExpanded by remember { mutableStateOf(false) }

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

    var isRecurringChecked by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }

    // Fetch matching transaction once on launch
    LaunchedEffect(transactionId) {
        val txn = onGetTransaction(transactionId)
        if (txn != null) {
            existingTransaction = txn
            amountStr = txn.amount.toString()
            merchant = txn.merchant
            notes = txn.notes ?: ""
            transactionType = txn.type
            val recommendedCat = if (txn.categoryId != null) null else {
                com.ankitsudegora.util.CategoryClassifier.recommendCategory(txn.merchant, allTransactions, categories)
            }
            selectedCategoryId = txn.categoryId ?: recommendedCat?.id ?: categories.firstOrNull { it.name.lowercase() != "others" }?.id ?: categories.firstOrNull()?.id
            isRecurringChecked = txn.source == "RECURRING"
            subCategory = txn.subCategory
            selectedRefundedTxnId = txn.refundedTxnId
            selectedLinkedListId = txn.linkedListId
        }
    }

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId != existingTransaction?.categoryId) {
            subCategory = null
        }
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

    if (existingTransaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isPending = existingTransaction?.isPending == true
    val titleText = if (isPending) "Categorization & Finalization" else "Edit Ledger Entry"
    val subText = if (isPending) {
        "Review, correct, and categorize this transaction to reconcile your ledger. Uncategorized items remain highlighted."
    } else {
        "Modify details of this authenticated ledger item. Changes will be updated immediately in your database."
    }
    val buttonLabel = if (isPending) "Verify & Reconcile Spends" else "Save Ledger Changes"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Back toolbar navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("enrich_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            
            // Sub badge representing SMS origins
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (existingTransaction?.source == "SMS" || existingTransaction?.source == "NOTIFICATION") {
                        "Automated Alert"
                    } else if (existingTransaction?.source == "RECURRING") {
                        "Scheduled Item"
                    } else {
                        "Manual Ledger"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            text = subText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        // Amount adjustments
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Transaction Amount",
                    style = MaterialTheme.typography.labelSmall,
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TextField(
                        value = amountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountStr = input
                            }
                        },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                // Debit Credit selection buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(2.dp)
                ) {
                    listOf("DEBIT" to "Debit / Dr Spends", "CREDIT" to "Credit / Cr Inflows").forEach { (typeKey, label) ->
                        val isSel = transactionType == typeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { transactionType = typeKey }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Editable Merchant parameter
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Inferred Merchant Name") },
            leadingIcon = { Icon(Icons.Default.Storefront, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("enrich_merchant_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Select Category matrix selector
        Text(
            text = "Select Reconciled Tag Category",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = 4
            val spacing = 6.dp
            val w = (maxWidth - (spacing * (columns - 1))) / columns

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
                                    .aspectRatio(1f),
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
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

        val debitTransactions = remember(allTransactions) {
            allTransactions.filter { it.transaction.type == "DEBIT" && !it.transaction.isPending }
        }

        var showAllDebitAmountsForRefund by remember { mutableStateOf(false) }
        val targetAmount = remember(amountStr, existingTransaction) {
            amountStr.toDoubleOrNull() ?: existingTransaction?.amount ?: 0.0
        }
        val filteredDebitTransactions = remember(debitTransactions, targetAmount, showAllDebitAmountsForRefund) {
            if (showAllDebitAmountsForRefund) {
                debitTransactions
            } else {
                debitTransactions.filter { Math.abs(it.transaction.amount - targetAmount) < 0.01 }
            }
        }

        // If CREDIT, show option to map to debit transaction (Refund mapping)
        if (transactionType == "CREDIT") {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Link to Original Debit (Refund Mapping)", fontWeight = FontWeight.SemiBold)
                    }

                    val chosenDebitTxn = remember(selectedRefundedTxnId, debitTransactions) {
                        debitTransactions.find { it.transaction.id == selectedRefundedTxnId }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { refundDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (chosenDebitTxn != null) {
                                    "₹${chosenDebitTxn.transaction.amount} - ${chosenDebitTxn.transaction.merchant}"
                                } else {
                                    "Select Debit Transaction (Optional)"
                                },
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedRefundedTxnId != null) {
                                    IconButton(
                                        onClick = { selectedRefundedTxnId = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear selection",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = refundDropdownExpanded,
                            onDismissRequest = { refundDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            filteredDebitTransactions.forEach { pt ->
                                val dateFormatted = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(pt.transaction.timestamp))
                                DropdownMenuItem(
                                    text = { Text("₹${pt.transaction.amount} - ${pt.transaction.merchant} ($dateFormatted)") },
                                    onClick = {
                                        selectedRefundedTxnId = pt.transaction.id
                                        refundDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllDebitAmountsForRefund = !showAllDebitAmountsForRefund }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = showAllDebitAmountsForRefund,
                            onCheckedChange = { showAllDebitAmountsForRefund = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Show all debit amounts (not only matches)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // If DEBIT, show option to map to planned checklists
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAddCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Link to Planned Checklist", fontWeight = FontWeight.SemiBold)
                    }

                    val chosenList = remember(selectedLinkedListId, allPlannedLists) {
                        allPlannedLists.find { it.plannedList.id == selectedLinkedListId }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { listDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (chosenList != null) {
                                    chosenList.plannedList.name
                                } else {
                                    "Select Checklist to Map (Optional)"
                                },
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedLinkedListId != null) {
                                    IconButton(
                                        onClick = { selectedLinkedListId = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear selection",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = listDropdownExpanded,
                            onDismissRequest = { listDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            allPlannedLists.filter { it.plannedList.status != "COMPLETED" }.forEach { pt ->
                                DropdownMenuItem(
                                    text = { Text("${pt.plannedList.name} (${pt.plannedList.status})") },
                                    onClick = {
                                        selectedLinkedListId = pt.plannedList.id
                                        listDropdownExpanded = false
                                        if (pt.plannedList.categoryId != null) {
                                            selectedCategoryId = pt.plannedList.categoryId
                                        }
                                        merchant = pt.plannedList.name
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SMS / original notification alert card details
        val context = LocalContext.current
        var showSmsDetailDialog by remember { mutableStateOf(false) }
        
        val isSmsSource = remember(existingTransaction) {
            existingTransaction?.source == "NOTIFICATION" || existingTransaction?.smsSenderId != null
        }
        val rawSmsText = remember(existingTransaction) {
            existingTransaction?.notes
        }

        if (isSmsSource && !rawSmsText.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                onClick = { showSmsDetailDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = "SMS",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Original Bank Notification / SMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rawSmsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (showSmsDetailDialog) {
                AlertDialog(
                    onDismissRequest = { showSmsDetailDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Transaction SMS Details")
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "From: ${existingTransaction?.smsSenderId ?: "System notification"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = rawSmsText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                        type = "vnd.android-dir/mms-sms"
                                    }
                                    try {
                                        context.startActivity(fallbackIntent)
                                    } catch (ex: Exception) {
                                        Log.e("EnrichmentScreen", "Failed to launch SMS app", ex)
                                    }
                                }
                                showSmsDetailDialog = false
                            }
                        ) {
                            Text("Open SMS App")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSmsDetailDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Optional Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Reconciled Transaction Notes (Optional)") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Recurring designation card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("recurring_settings_card")
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
                        modifier = Modifier.testTag("enrich_recurring_switch")
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
                                modifier = Modifier.weight(1f).testTag("freq_chip_$freqKey"),
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

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm & Finalize item
        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: existingTransaction!!.amount
                val catId = selectedCategoryId ?: filteredCategories.firstOrNull()?.id ?: 0L
                val freq = if (isRecurringChecked) selectedFrequency else null
                val ccId = if (paidViaCcChecked && transactionType == "DEBIT" && !isCcRepaymentSelected) selectedCcId else null
                val repCcId = if (isCcRepaymentSelected) selectedRepaidCcId else null
                val repTxnIds = if (isCcRepaymentSelected) selectedRepaidTxnIds.toList() else emptyList()

                onFinalizeTransaction(
                    transactionId,
                    catId,
                    notes.trim().ifBlank { null },
                    amt,
                    merchant.trim().ifBlank { "Unknown Merchant" },
                    transactionType,
                    freq,
                    subCategory,
                    ccId,
                    repCcId,
                    repTxnIds,
                    selectedLinkedListId,
                    selectedRefundedTxnId
                )
                onNavigateBack()
            },
            enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_enrichment_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
