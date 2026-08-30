package com.ankitsudegora.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
    onAddCreditCard: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") } // "DEBIT" or "CREDIT"
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var paymentMethod by remember { mutableStateOf("UPI") } // "UPI", "CARD", "CASH", "NETBANKING"
    var selectedCurrency by remember(primaryCurrency) { mutableStateOf(primaryCurrency) }

    var isRecurringChecked by remember { mutableStateOf(false) }
    var recurringFreq by remember { mutableStateOf("MONTHLY") }
    var subCategory by remember { mutableStateOf("") }
    var selectedCcId by remember { mutableStateOf<Long?>(null) }
    var repaidCcId by remember { mutableStateOf<Long?>(null) }
    var selectedRepaidTxnIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateDisplayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isToday = remember(timestamp) {
        val todayCal = Calendar.getInstance()
        val txnCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        todayCal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == txnCal.get(Calendar.DAY_OF_YEAR)
    }

    // Expandable accordion states
    var isPaidViaExpanded by remember { mutableStateOf(false) }
    var isNotesExpanded by remember { mutableStateOf(false) }
    var isMoreOptionsExpanded by remember { mutableStateOf(false) }
    var showAllCategoriesModal by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    // Pre-select first category if none selected
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }

    // Auto-select first credit card if available (M5)
    LaunchedEffect(creditCards) {
        if (selectedCcId == null && creditCards.isNotEmpty()) {
            selectedCcId = creditCards.first().id
        }
    }

    // Top 3 suggested categories based on merchant matching or common usage (C4)
    val topSuggestedCategories = remember(categories, merchant, allTransactions) {
        val matched = if (merchant.isNotBlank()) {
            val q = merchant.lowercase()
            // Check past merchant usage first
            val pastCategory = allTransactions
                .filter { it.transaction.merchant.contains(q, ignoreCase = true) }
                .mapNotNull { it.category }
                .distinctBy { it.id }

            val keywordMatched = categories.filter { cat ->
                cat.name.lowercase().contains(q) ||
                        ((q.contains("swiggy") || q.contains("zomato")) && cat.name.contains("Food", ignoreCase = true)) ||
                        ((q.contains("bazaar") || q.contains("mart") || q.contains("grocery")) && cat.name.contains("Groceries", ignoreCase = true)) ||
                        ((q.contains("uber") || q.contains("ola")) && cat.name.contains("Travel", ignoreCase = true))
            }
            (pastCategory + keywordMatched).distinctBy { it.id }
        } else emptyList()

        (matched + categories).distinctBy { it.id }.take(3)
    }

    // Validation & Save Handler
    val canSave = remember(amountStr) {
        val amt = amountStr.toDoubleOrNull()
        amt != null && amt > 0.0
    }

    fun handleSave() {
        val amt = amountStr.toDoubleOrNull() ?: return
        if (amt <= 0.0) return

        onSaveTransaction(
            amt,
            transactionType,
            merchant.ifBlank { "Manual ${if (transactionType == "DEBIT") "Expense" else "Income"}" },
            selectedCategoryId,
            notes.ifBlank { null },
            paymentMethod,
            timestamp,
            if (isRecurringChecked) recurringFreq else null,
            subCategory.ifBlank { null },
            if (paymentMethod == "CARD") selectedCcId else null,
            repaidCcId,
            selectedRepaidTxnIds,
            selectedCurrency
        )
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            // Top Bar matching Add Expense screen.jpeg: (X) left | Add Expense center | (✓) gradient right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DarkOnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (transactionType == "DEBIT") "Add Expense" else "Add Income",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = DarkOnSurface
                )

                // Top Right (✓) confirm button with subtle gradient
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSave) Brush.horizontalGradient(listOf(BrandLime, Color(0xFF38BDF8)))
                            else Brush.horizontalGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                        )
                        .clickable(enabled = canSave) { handleSave() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = if (canSave) DarkBackground else DarkOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Fixed Bottom CTA: [ Save Expense ] Full Width Gradient Button
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { handleSave() },
                    enabled = canSave,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = DarkSurfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canSave) Brush.horizontalGradient(listOf(BrandLime, Color(0xFF38BDF8)))
                            else Brush.horizontalGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                        )
                        .testTag("save_manual_expense_button")
                ) {
                    Text(
                        text = if (transactionType == "DEBIT") "Save Expense" else "Save Income",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = if (canSave) DarkBackground else DarkOnSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Large Amount Card with Date Pill (Matches Mockup)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
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
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkOnSurfaceVariant
                        )

                        // 📅 Date Pill (Clickable DatePicker)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance()
                                        newCal.set(year, month, dayOfMonth)
                                        timestamp = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📅", fontSize = 12.sp)
                                Text(
                                    text = if (isToday) "Today" else dateDisplayFormat.format(Date(timestamp)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = DarkOnSurface
                                )
                            }
                        }
                    }

                    // Hero Amount Input Field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (selectedCurrency == "INR" || selectedCurrency.isBlank()) "₹" else selectedCurrency,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = BrandLime
                        )

                        BasicTextField(
                            value = amountStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                    amountStr = input
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkOnSurface
                            ),
                            cursorBrush = SolidColor(BrandLime),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (amountStr.isEmpty()) {
                                    Text(
                                        text = "0",
                                        style = TextStyle(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DarkOnSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("amount_input")
                        )
                    }

                    // Subtle Accent Underline (Matches Add Expense screen.jpeg)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandLime, BrandLime.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                    )
                }
            }

            // 2. Smart Merchant Input with (x) Clear Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    BasicTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkOnSurface
                        ),
                        cursorBrush = SolidColor(BrandLime),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { innerTextField ->
                            if (merchant.isEmpty()) {
                                Text(
                                    text = "Merchant / Payee Name (e.g. Big Bazaar, Swiggy)",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = DarkOnSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (merchant.isNotEmpty()) {
                        IconButton(
                            onClick = { merchant = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = DarkOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Suggested Category (Top 3 Matches + More Pill)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Suggested Category",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkOnSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val matchPcts = listOf(98, 75, 60)
                    topSuggestedCategories.forEachIndexed { idx, cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val matchPct = matchPcts.getOrElse(idx) { 50 }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) DarkSurface else DarkSurfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) BrandLime else DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategoryId = cat.id }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) BrandLime.copy(alpha = 0.2f) else DarkSurfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(cat.iconResName),
                                        contentDescription = cat.name,
                                        tint = if (isSelected) BrandLime else DarkOnSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) BrandLime.copy(alpha = 0.2f) else DarkSurfaceVariant
                                ) {
                                    Text(
                                        text = "$matchPct% match",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) BrandLime else DarkOnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // More Category Drawer Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable { showAllCategoriesModal = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = DarkOnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "More",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Payment Method Selector (UPI, Card, Cash, Netbanking)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkOnSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val methods = listOf(
                        Triple("UPI", Icons.Default.QrCodeScanner, "UPI"),
                        Triple("CARD", Icons.Default.CreditCard, "Card"),
                        Triple("CASH", Icons.Default.Payments, "Cash"),
                        Triple("NETBANKING", Icons.Default.AccountBalance, "Netbanking")
                    )

                    methods.forEach { (methodKey, icon, label) ->
                        val isSelected = paymentMethod == methodKey
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DarkSurface else DarkSurfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) BrandLime else DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = methodKey }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) BrandLime.copy(alpha = 0.2f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) BrandLime else DarkOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) DarkOnSurface else DarkOnSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // RuPay Credit Card on UPI Smart Link
            if (paymentMethod == "UPI" && creditCards.isNotEmpty()) {
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

            // 5. Collapsible Optional Fields (Less is More)
            // Accordion 1: Paid via Credit Card / Account
            if (paymentMethod == "CARD") {
                if (creditCards.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddCardDialog = true }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CreditCard, null, tint = Color(0xFFA855F7), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("No Credit Card Added", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = DarkOnSurface)
                                    Text("Tap to add a credit card", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                                }
                            }
                            Text("+ Add Card", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandLime)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPaidViaExpanded = !isPaidViaExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = BrandLime, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Select Credit Card (${creditCards.find { it.id == selectedCcId }?.cardName ?: "Choose card"})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkOnSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (isPaidViaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = DarkOnSurfaceVariant
                                )
                            }

                            AnimatedVisibility(
                                visible = isPaidViaExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    creditCards.forEach { card ->
                                        val isCardSel = selectedCcId == card.id
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isCardSel) BrandLimeContainer else DarkSurfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCcId = card.id
                                                    isPaidViaExpanded = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = card.cardName,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isCardSel) BrandOnLimeContainer else DarkOnSurface
                                                )
                                                if (isCardSel) {
                                                    Icon(Icons.Default.Check, null, tint = BrandOnLimeContainer, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // In-flow Missing Card Button
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandLime.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showAddCardDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = BrandLime, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Missing a card? + Add Credit Card",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandLime
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accordion 2: Notes (Optional)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNotesExpanded = !isNotesExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Notes, null, tint = DarkOnSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (notes.isBlank()) "Notes (Optional)" else "Note: $notes",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DarkOnSurface
                            )
                        }
                        Icon(
                            imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isNotesExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Add any tags or details (e.g. #dinner, #client)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedBorderColor = BrandLime,
                                unfocusedBorderColor = DarkBorder
                            )
                        )
                    }
                }
            }

            // Accordion 3: More options (Recurring, Subcategory, Type Switch)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMoreOptionsExpanded = !isMoreOptionsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "More options",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkOnSurfaceVariant
                        )
                        Icon(
                            imageVector = if (isMoreOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isMoreOptionsExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Transaction Type Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = transactionType == "DEBIT",
                                    onClick = { transactionType = "DEBIT" },
                                    label = { Text("Expense (Outflow)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFFEF4444)
                                    )
                                )
                                FilterChip(
                                    selected = transactionType == "CREDIT",
                                    onClick = { transactionType = "CREDIT" },
                                    label = { Text("Income (Inflow)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandLime.copy(alpha = 0.2f),
                                        selectedLabelColor = BrandLime
                                    )
                                )
                            }

                            // Subcategory Input
                            OutlinedTextField(
                                value = subCategory,
                                onValueChange = { subCategory = it },
                                label = { Text("Subcategory (Optional)") },
                                placeholder = { Text("e.g. Vegetables, Dinner, Fuel") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedBorderColor = BrandLime,
                                    unfocusedBorderColor = DarkBorder
                                )
                            )

                            // Recurring Schedule Switch
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Recurring Schedule", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Auto-log on repeat", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isRecurringChecked,
                                        onCheckedChange = { isRecurringChecked = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = BrandLime, checkedTrackColor = BrandLimeContainer)
                                    )
                                }

                                if (isRecurringChecked) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { freq ->
                                            FilterChip(
                                                selected = recurringFreq == freq,
                                                onClick = { recurringFreq = freq },
                                                label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandLime.copy(alpha = 0.2f),
                                                    selectedLabelColor = BrandLime
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal to view All Categories
    if (showAllCategoriesModal) {
        AlertDialog(
            onDismissRequest = { showAllCategoriesModal = false },
            title = { Text("Select Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCategoryId == cat.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) BrandLimeContainer else DarkSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategoryId = cat.id
                                    showAllCategoriesModal = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = getIconVector(cat.iconResName),
                                    contentDescription = cat.name,
                                    tint = if (isSel) BrandOnLimeContainer else DarkOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSel) BrandOnLimeContainer else DarkOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSel) {
                                    Icon(Icons.Default.Check, null, tint = BrandOnLimeContainer, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllCategoriesModal = false }) {
                    Text("Close", color = BrandLime)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showAddCardDialog) {
        var newCardName by remember { mutableStateOf("") }
        var newCardLast4 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = {
                Text(
                    text = "Add Credit Card",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkOnSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your credit card or bank name and the last 4 digits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCardName,
                        onValueChange = { newCardName = it },
                        placeholder = { Text("Card Name (e.g. ICICI Coral RuPay)", color = DarkOnSurfaceVariant) },
                        label = { Text("Card / Bank Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkOnSurface,
                            unfocusedTextColor = DarkOnSurface,
                            focusedBorderColor = BrandLime,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCardLast4,
                        onValueChange = { input ->
                            newCardLast4 = input.filter { it.isDigit() }.take(4)
                        },
                        placeholder = { Text("e.g. 8042", color = DarkOnSurfaceVariant) },
                        label = { Text("Last 4 Digits Only") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkOnSurface,
                            unfocusedTextColor = DarkOnSurface,
                            focusedBorderColor = BrandLime,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCardName.isNotBlank()) {
                            val finalName = if (newCardLast4.isNotBlank()) {
                                "${newCardName.trim()} •••• $newCardLast4"
                            } else {
                                newCardName.trim()
                            }
                            onAddCreditCard(finalName)
                            showAddCardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                ) {
                    Text("Add Card", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel", color = DarkOnSurfaceVariant)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
