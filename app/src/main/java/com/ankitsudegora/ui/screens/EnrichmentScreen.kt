package com.ankitsudegora.ui.screens

import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.theme.*
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.data.PlannedListWithItems
import com.ankitsudegora.util.CategoryClassifier

@Composable
fun EnrichmentScreen(
    transactionId: Long,
    categories: List<Category>,
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    allPlannedLists: List<PlannedListWithItems>,
    onGetTransaction: suspend (Long) -> Transaction?,
    onFinalizeTransaction: (id: Long, categoryId: Long, notes: String?, amount: Double, merchant: String, type: String, recurringFrequency: String?, subCategory: String?, paidViaCcId: Long?, repaidCcId: Long?, selectedRepaidTxnIds: List<Long>, linkedListId: Long?, refundedTxnId: Long?) -> Unit,
    onNavigateBack: () -> Unit,
    pendingCount: Int = 1,
    currentIndex: Int = 1,
    onSkipToNext: (() -> Unit)? = null
) {
    BackHandler {
        onNavigateBack()
    }

    val scrollState = rememberScrollState()
    
    var existingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subCategory by remember { mutableStateOf<String?>(null) }
    var showAdvancedDetails by remember { mutableStateOf(false) }

    var selectedRefundedTxnId by remember { mutableStateOf<Long?>(null) }
    var selectedLinkedListId by remember { mutableStateOf<Long?>(null) }
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
            amountStr = if (txn.amount % 1.0 == 0.0) txn.amount.toInt().toString() else "%.2f".format(txn.amount)
            merchant = txn.merchant
            notes = txn.notes ?: ""
            transactionType = txn.type
            val recommendedCat = if (txn.categoryId != null) null else {
                CategoryClassifier.recommendCategory(txn.merchant, allTransactions, categories)
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

    val confidenceScore = remember(merchant, allTransactions) {
        CategoryClassifier.calculateConfidenceScore(merchant, allTransactions)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BrandLime)
        }
        return
    }

    val isPending = existingTransaction?.isPending == true

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with queue counter & skip option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("enrich_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkOnSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isPending) "Review Expense" else "Edit Expense",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        if (isPending && pendingCount > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$currentIndex of $pendingCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandLime
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isPending) "Verify and confirm in one tap" else "Update ledger details",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPending && onSkipToNext != null) {
                        TextButton(
                            onClick = onSkipToNext,
                            colors = ButtonDefaults.textButtonColors(contentColor = DarkOnSurfaceVariant)
                        ) {
                            Text(
                                text = "Skip →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (isPending) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandLimeContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "$confidenceScore% match",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandOnLimeContainer
                            )
                        }
                    }
                }
            }

            // Hero Review Card (Emotional Center)
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Transaction Type Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("DEBIT" to "Spent", "CREDIT" to "Received").forEach { (typeKey, label) ->
                            val isSel = transactionType == typeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) BrandLime else Color.Transparent)
                                    .clickable { transactionType = typeKey }
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSel) DarkBackground else DarkOnSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Large Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            style = MoneyTypography.HeroAmount.copy(color = BrandLime)
                        )
                        TextField(
                            value = amountStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amountStr = input
                                }
                            },
                            placeholder = { Text("0.00", style = MoneyTypography.HeroAmount.copy(color = DarkOnSurfaceVariant.copy(alpha = 0.3f))) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MoneyTypography.HeroAmount.copy(
                                color = DarkOnSurface,
                                textAlign = TextAlign.Start
                            ),
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Merchant Field
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant / Payee") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = BrandLime
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandLime,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            focusedLabelColor = BrandLime,
                            unfocusedLabelColor = DarkOnSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enrich_merchant_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Context Metadata (Date, Time, Bank Account source)
                    val dateFormatted = SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()).format(Date(existingTransaction?.timestamp ?: System.currentTimeMillis()))
                    val sourceText = when (existingTransaction?.source) {
                        "SMS", "NOTIFICATION" -> "Alert: ${existingTransaction?.smsSenderId ?: "Bank Notification"}"
                        "RECURRING" -> "Recurring Scheduled Trigger"
                        else -> "Manual Ledger Entry"
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$dateFormatted • $sourceText",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Category Selection Matrix
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkOnSurface
            )

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
                                val containerCol = if (isSel) catColor.copy(alpha = 0.22f) else DarkSurface
                                val borderCol = if (isSel) catColor else DarkBorder
                                val contentCol = if (isSel) catColor else DarkOnSurfaceVariant

                                Card(
                                    onClick = { selectedCategoryId = cat.id },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = containerCol),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    border = androidx.compose.foundation.BorderStroke(if (isSel) 2.dp else 1.dp, borderCol)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSel) catColor.copy(alpha = 0.3f) else DarkSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconVector(cat.iconResName),
                                                contentDescription = cat.name,
                                                tint = contentCol,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium),
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

            // Note (Optional) Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note (Optional)") },
                placeholder = { Text("e.g. Lunch with team, monthly groceries") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = DarkOnSurfaceVariant
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandLime,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedLabelColor = BrandLime,
                    unfocusedLabelColor = DarkOnSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Primary 1-Tap Confirmation & Secondary Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showAdvancedDetails = !showAdvancedDetails },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkOnSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = if (showAdvancedDetails) "Less ▲" else "More ▼",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: existingTransaction!!.amount
                        val catId = selectedCategoryId ?: filteredCategories.firstOrNull()?.id ?: 0L
                        val freq = if (isRecurringChecked) selectedFrequency else null
                        val ccId = null
                        val repCcId = null
                        val repTxnIds = emptyList<Long>()

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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = DarkBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_enrichment_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isPending) "Confirm ✓" else "Save Changes",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }

            // Expandable Advanced Details Section (Subcategories, Recurring, Planned list mapping)
            AnimatedVisibility(
                visible = showAdvancedDetails,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SubCategory chips
                    if (subCategoryOptions.isNotEmpty()) {
                        Text(
                            text = "Specific Sub-Category",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
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
                                    )
                                )
                            }
                        }
                    }

                    // Recurring Schedule Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recurring_settings_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
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
                                            text = "Auto-trigger on schedule",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkOnSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isRecurringChecked,
                                    onCheckedChange = { isRecurringChecked = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = BrandLime
                                    ),
                                    modifier = Modifier.testTag("enrich_recurring_switch")
                                )
                            }

                            if (isRecurringChecked) {
                                HorizontalDivider(color = DarkBorder)
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
                                                .testTag("freq_chip_$freqKey"),
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

                    // Link to Planned Checklist
                    if (transactionType == "DEBIT" && allPlannedLists.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Link to Shopping / Planned List",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                val chosenList = allPlannedLists.find { it.plannedList.id == selectedLinkedListId }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { listDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkOnSurface)
                                    ) {
                                        Text(chosenList?.plannedList?.name ?: "Select List (Optional)", modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(
                                        expanded = listDropdownExpanded,
                                        onDismissRequest = { listDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None") },
                                            onClick = {
                                                selectedLinkedListId = null
                                                listDropdownExpanded = false
                                            }
                                        )
                                        allPlannedLists.forEach { pl ->
                                            DropdownMenuItem(
                                                text = { Text(pl.plannedList.name) },
                                                onClick = {
                                                    selectedLinkedListId = pl.plannedList.id
                                                    listDropdownExpanded = false
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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
