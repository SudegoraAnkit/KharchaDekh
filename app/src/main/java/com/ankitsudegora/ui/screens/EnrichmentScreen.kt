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
import androidx.compose.ui.unit.sp
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
    onDeleteTransaction: ((Transaction) -> Unit)? = null,
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
    var paymentMethod by remember { mutableStateOf("UPI") }
    var selectedCcId by remember { mutableStateOf<Long?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
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
            paymentMethod = txn.paymentMethod ?: "UPI"
            selectedCcId = txn.paidViaCcId
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
    var showAllCategories by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(notes.isNotBlank()) }
    var paymentDetailsExpanded by remember { mutableStateOf(false) }

    val dateFormatted = remember(existingTransaction?.timestamp) {
        val date = Date(existingTransaction?.timestamp ?: System.currentTimeMillis())
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val txnDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
        if (today == txnDay) "Today" else SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }

    // Top 3 suggested categories + more
    val suggestedCategories = remember(filteredCategories, selectedCategoryId) {
        val selCat = filteredCategories.find { it.id == selectedCategoryId }
        val others = filteredCategories.filter { it.id != selectedCategoryId }.take(2)
        val list = mutableListOf<Category>()
        if (selCat != null) list.add(selCat)
        list.addAll(others)
        if (list.size < 3) {
            val remaining = filteredCategories.filter { it !in list }.take(3 - list.size)
            list.addAll(remaining)
        }
        list
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Top Bar with (X), Title, and (✓)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("enrich_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DarkOnSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isPending) "Review Expense" else "Edit Expense",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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

                // Top Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPending) {
                        IconButton(
                            onClick = { showDiscardDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkError.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Discard",
                                tint = DarkError
                            )
                        }
                    }

                    // Top (✓) confirm button
                    IconButton(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: existingTransaction!!.amount
                            val catId = selectedCategoryId ?: filteredCategories.firstOrNull()?.id ?: 0L
                            val freq = if (isRecurringChecked) selectedFrequency else null

                            onFinalizeTransaction(
                                transactionId,
                                catId,
                                notes.trim().ifBlank { null },
                                amt,
                                merchant.trim().ifBlank { "Unknown Merchant" },
                                transactionType,
                                freq,
                                subCategory,
                                selectedCcId,
                                null,
                                emptyList(),
                                selectedLinkedListId,
                                selectedRefundedTxnId
                            )
                            if (onSkipToNext != null && pendingCount > 1) {
                                onSkipToNext()
                            } else {
                                onNavigateBack()
                            }
                        },
                        enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank())
                                    BrandLime else DarkSurfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank())
                                DarkBackground else DarkOnSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Big & Clear Hero Amount Card (Matches Mockup)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DarkOnSurfaceVariant
                            )

                            // Date Pill on Top-Right
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = DarkOnSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkOnSurface
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "₹",
                                style = MoneyTypography.HeroAmount.copy(color = BrandLime),
                                modifier = Modifier.padding(end = 4.dp)
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
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = DarkOnSurface,
                                    unfocusedTextColor = DarkOnSurface
                                ),
                                textStyle = MoneyTypography.HeroAmount.copy(color = DarkOnSurface)
                            )
                        }
                    }
                }

                // 2. Smart Merchant Input with (x) clear button
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Payee", color = DarkOnSurfaceVariant) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = BrandLime
                        )
                    },
                    trailingIcon = if (merchant.isNotEmpty()) {
                        {
                            IconButton(onClick = { merchant = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkOnSurfaceVariant)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandLime,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = DarkOnSurface,
                        unfocusedTextColor = DarkOnSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enrich_merchant_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // 3. Suggested Category (Top 3 AI Matches + Confidence % + More)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Suggested Category",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedCategories.forEach { cat ->
                            val isSel = selectedCategoryId == cat.id
                            val catColor = CategoryColors.getCategoryColor(cat.name)
                            val matchPct = if (isSel) confidenceScore else (confidenceScore - 15).coerceAtLeast(60)

                            Card(
                                onClick = { selectedCategoryId = cat.id },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) DarkSurfaceElevated else DarkSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSel) 1.5.dp else 1.dp,
                                    if (isSel) BrandLime else DarkBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = if (isSel) 0.3f else 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(cat.iconResName),
                                            contentDescription = cat.name,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isSel) DarkOnSurface else DarkOnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$matchPct% match",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSel) BrandLime else DarkOnSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // More Categories Card
                        Card(
                            onClick = { showAllCategories = !showAllCategories },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(96.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More Categories",
                                    tint = DarkOnSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (showAllCategories) "Less" else "More",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Full Categories Grid if "More" is toggled
                    AnimatedVisibility(visible = showAllCategories) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            filteredCategories.chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { cat ->
                                        val isSel = selectedCategoryId == cat.id
                                        val catColor = CategoryColors.getCategoryColor(cat.name)
                                        Card(
                                            onClick = {
                                                selectedCategoryId = cat.id
                                                showAllCategories = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSel) DarkSurfaceElevated else DarkSurface
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (isSel) 1.5.dp else 1.dp,
                                                if (isSel) BrandLime else DarkBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
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
                                                    tint = catColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = cat.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = DarkOnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (row.size < 4) {
                                        repeat(4 - row.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Quick Payment Method Row (UPI, Card, Cash, Netbanking)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("UPI", Icons.Default.QrCodeScanner, "UPI"),
                            Triple("CARD", Icons.Default.CreditCard, "Card"),
                            Triple("CASH", Icons.Default.Payments, "Cash"),
                            Triple("NETBANKING", Icons.Default.AccountBalance, "Netbanking")
                        ).forEach { (modeKey, icon, label) ->
                            val isSel = paymentMethod.equals(modeKey, ignoreCase = true)
                            Card(
                                onClick = { paymentMethod = modeKey },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) DarkSurfaceElevated else DarkSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSel) 1.5.dp else 1.dp,
                                    if (isSel) BrandLime else DarkBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSel) BrandLime else DarkOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSel) DarkOnSurface else DarkOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // RuPay Credit Card on UPI Smart Link
                    if (paymentMethod.equals("UPI", ignoreCase = true) && creditCards.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCcId != null) BrandLimeContainer.copy(alpha = 0.35f) else DarkSurfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCcId != null) BrandLime else DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedCcId != null) selectedCcId = null
                                    else selectedCcId = creditCards.first().id
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚡", fontSize = 14.sp)
                                    Column {
                                        Text(
                                            text = if (selectedCcId != null) "Paid via RuPay CC: ${creditCards.find { it.id == selectedCcId }?.cardName}"
                                            else "Paid using RuPay Credit Card on UPI?",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (selectedCcId != null) BrandOnLimeContainer else DarkOnSurface
                                        )
                                        Text(
                                            text = if (selectedCcId != null) "Linked to Credit Card statement dues"
                                            else "Tap to link expense to your Credit Card",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = DarkOnSurfaceVariant
                                        )
                                    }
                                }
                                if (selectedCcId != null) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Linked", tint = BrandLime, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Link Card", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandLime))
                                }
                            }
                        }
                    }
                }

                // 5. Collapsible Optional Notes & Details (Less is More)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notesExpanded = !notesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EditNote, null, tint = DarkOnSurfaceVariant, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (notes.isBlank()) "Notes (Optional)" else "Notes: $notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (notes.isBlank()) DarkOnSurfaceVariant else DarkOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = DarkOnSurfaceVariant
                            )
                        }

                        if (notesExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("Add transaction notes or tags", color = DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                                singleLine = false,
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandLime,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f),
                                    focusedTextColor = DarkOnSurface,
                                    unfocusedTextColor = DarkOnSurface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 6. More Options (Recurring, Checklist, Subcategories)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedDetails = !showAdvancedDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More options",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkOnSurfaceVariant
                            )
                            Icon(
                                imageVector = if (showAdvancedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = DarkOnSurfaceVariant
                            )
                        }
                        if (showAdvancedDetails) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Subcategories
                                if (subCategoryOptions.isNotEmpty()) {
                                    Text("Specific Sub-Category", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
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
                                                    containerColor = DarkSurfaceVariant,
                                                    labelColor = DarkOnSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }

                                // Recurring switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Designate as Recurring", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                                        Text("Auto-trigger on schedule", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isRecurringChecked,
                                        onCheckedChange = { isRecurringChecked = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = BrandLime)
                                    )
                                }

                                if (isRecurringChecked) {
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
                                                modifier = Modifier.weight(1f),
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

                                // Link to Planned Checklist
                                if (transactionType == "DEBIT" && allPlannedLists.isNotEmpty()) {
                                    val chosenList = allPlannedLists.find { it.plannedList.id == selectedLinkedListId }
                                    Text("Link to Shopping List", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
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

                Spacer(modifier = Modifier.height(10.dp))
            // Fixed Bottom 1-Tap Save Button (No Duplicate Checkmark Icons)
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: existingTransaction!!.amount
                    val catId = selectedCategoryId ?: filteredCategories.firstOrNull()?.id ?: 0L
                    val freq = if (isRecurringChecked) selectedFrequency else null

                    onFinalizeTransaction(
                        transactionId,
                        catId,
                        notes.trim().ifBlank { null },
                        amt,
                        merchant.trim().ifBlank { "Unknown Merchant" },
                        transactionType,
                        freq,
                        subCategory,
                        selectedCcId,
                        null,
                        emptyList(),
                        selectedLinkedListId,
                        selectedRefundedTxnId
                    )
                    if (onSkipToNext != null && pendingCount > 1) {
                        onSkipToNext()
                    } else {
                        onNavigateBack()
                    }
                },
                enabled = amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandLime,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = DarkOnSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_enrichment_button")
            ) {
                Text(
                    text = if (isPending && pendingCount > 1) "Confirm & Next →" else if (isPending) "Confirm" else "Save Expense",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && merchant.isNotBlank()) DarkBackground else DarkOnSurfaceVariant
                )
            }
        }

        if (showDiscardDialog && existingTransaction != null) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = DarkError,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Discard this spending?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "This will exclude this transaction from your budget, analytics, and expense reports. You can always add it back manually if needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            if (onDeleteTransaction != null) {
                                onDeleteTransaction(existingTransaction!!)
                            }
                            if (onSkipToNext != null && pendingCount > 1) {
                                onSkipToNext()
                            } else {
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkError, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard Expense", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep Spending", color = DarkOnSurfaceVariant)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
}
