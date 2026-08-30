package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.components.getIconVector
import com.ankitsudegora.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen(
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    onRepayCard: (CreditCard, Double, List<Long>) -> Unit,
    onExportStatement: (CreditCard, List<TransactionWithCategory>) -> Unit,
    onAddCreditCard: (String) -> Unit = {},
    onEditCreditCard: (CreditCard) -> Unit = {},
    onDeleteCreditCard: (CreditCard) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var selectedCardId by remember { mutableStateOf<Long?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCard?>(null) }
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }

    LaunchedEffect(creditCards) {
        if (selectedCardId == null && creditCards.isNotEmpty()) {
            selectedCardId = creditCards.first().id
        } else if (creditCards.isNotEmpty() && creditCards.none { it.id == selectedCardId }) {
            selectedCardId = creditCards.first().id
        }
    }

    val selectedCard = remember(creditCards, selectedCardId) {
        creditCards.find { it.id == selectedCardId } ?: creditCards.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Credit Cards",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Text(
                            text = "Track dues, statements & RuPay UPI",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkOnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkOnSurface
                        )
                    }
                },
                actions = {
                    if (selectedCard != null) {
                        IconButton(
                            onClick = { cardToEdit = selectedCard },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Card",
                                tint = DarkOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { cardToDelete = selectedCard },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkError.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Card",
                                tint = DarkError,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = { showAddCardDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandLimeContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Card",
                            tint = BrandOnLimeContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (creditCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BrandLime.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "No credit cards registered yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Text(
                            text = "Add your ICICI RuPay, HDFC, SBI, or other cards to track unbilled dues and statement history seamlessly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Button(
                            onClick = { showAddCardDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandLime,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Your First Card", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Modern Card Selector Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    creditCards.forEach { card ->
                        val isSelected = selectedCardId == card.id
                        val unbilledCount = remember(allTransactions, card.id) {
                            allTransactions.count { it.transaction.paidViaCcId == card.id && it.transaction.ccRepaymentId == null && it.transaction.type == "DEBIT" }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandLimeContainer else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BrandLime else DarkBorder
                            ),
                            modifier = Modifier.clickable { selectedCardId = card.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = if (isSelected) BrandOnLimeContainer else DarkOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = card.cardName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) BrandOnLimeContainer else DarkOnSurface
                                )
                                if (unbilledCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) BrandOnLimeContainer.copy(alpha = 0.2f) else BrandLime.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$unbilledCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (isSelected) BrandOnLimeContainer else BrandLime
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedCard != null) {
                    ModernCardDetailsView(
                        card = selectedCard,
                        transactions = allTransactions,
                        onRepayCard = onRepayCard,
                        onExportStatement = onExportStatement,
                        onEditCard = { cardToEdit = selectedCard }
                    )
                }
            }
        }

        // Add Card Dialog
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
                            text = "Enter your card or bank name and the last 4 digits for identification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newCardName,
                            onValueChange = { newCardName = it },
                            placeholder = { Text("Card Name (e.g. ICICI Coral RuPay, HDFC Regalia)", color = DarkOnSurfaceVariant) },
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

        // Edit Card Dialog
        if (cardToEdit != null) {
            val card = cardToEdit!!
            val digitsInCard = card.cardName.filter { it.isDigit() }
            val initialLast4 = if (digitsInCard.length >= 4) digitsInCard.takeLast(4) else digitsInCard
            val initialNameOnly = card.cardName
                .replace("••••", "")
                .replace(initialLast4, "")
                .trim()
                .trimEnd('(', ')', '-')
                .ifBlank { card.cardName }

            var editName by remember(card) { mutableStateOf(initialNameOnly) }
            var editLast4 by remember(card) { mutableStateOf(initialLast4) }

            AlertDialog(
                onDismissRequest = { cardToEdit = null },
                title = {
                    Text(
                        text = "Edit Credit Card Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Update your card nickname and last 4 digits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Card / Bank Name", color = DarkOnSurfaceVariant) },
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
                            value = editLast4,
                            onValueChange = { input ->
                                editLast4 = input.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Last 4 Digits Only", color = DarkOnSurfaceVariant) },
                            placeholder = { Text("e.g. 8042", color = DarkOnSurfaceVariant) },
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
                            if (editName.isNotBlank()) {
                                val finalName = if (editLast4.isNotBlank()) {
                                    "${editName.trim()} •••• $editLast4"
                                } else {
                                    editName.trim()
                                }
                                onEditCreditCard(card.copy(cardName = finalName))
                                cardToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToEdit = null }) {
                        Text("Cancel", color = DarkOnSurfaceVariant)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Delete Card Confirmation Dialog
        if (cardToDelete != null) {
            val card = cardToDelete!!
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = {
                    Text(
                        text = "Delete Credit Card?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove \"${card.cardName}\"?\n\nAll existing transactions linked to this card will be safely retained in your general expense history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCreditCard(card)
                            cardToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkError, contentColor = Color.White)
                    ) {
                        Text("Delete Card", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text("Keep Card", color = DarkOnSurfaceVariant)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun ModernCardDetailsView(
    card: CreditCard,
    transactions: List<TransactionWithCategory>,
    onRepayCard: (CreditCard, Double, List<Long>) -> Unit,
    onExportStatement: (CreditCard, List<TransactionWithCategory>) -> Unit,
    onEditCard: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("UNBILLED") } // "UNBILLED" or "SETTLED"

    // Filter transactions linked to this card
    val cardTxns = remember(transactions, card.id) {
        transactions.filter { it.transaction.paidViaCcId == card.id && it.transaction.type == "DEBIT" }
    }

    val unbilledTxns = remember(cardTxns) {
        cardTxns.filter { it.transaction.ccRepaymentId == null }
    }

    val billedTxns = remember(cardTxns) {
        cardTxns.filter { it.transaction.ccRepaymentId != null }
    }

    val totalUnbilledBalance = remember(unbilledTxns) {
        unbilledTxns.sumOf { it.transaction.amount }
    }

    val totalSettledBalance = remember(billedTxns) {
        billedTxns.sumOf { it.transaction.amount }
    }

    var selectedTxnIdsForRepayment by remember { mutableStateOf(setOf<Long>()) }

    // Auto select all unbilled transactions for repayment
    LaunchedEffect(card.id, unbilledTxns) {
        selectedTxnIdsForRepayment = unbilledTxns.map { it.transaction.id }.toSet()
    }

    val totalRepaymentAmount = remember(unbilledTxns, selectedTxnIdsForRepayment) {
        unbilledTxns.filter { selectedTxnIdsForRepayment.contains(it.transaction.id) }
            .sumOf { it.transaction.amount }
    }

    // Dynamic Card Network Detection (RuPay vs Visa vs Mastercard vs Amex)
    val cardNameLower = card.cardName.lowercase()
    val isRuPay = cardNameLower.contains("rupay")
    val isVisa = cardNameLower.contains("visa")
    val isMastercard = cardNameLower.contains("mastercard") || cardNameLower.contains("master")
    val isAmex = cardNameLower.contains("amex") || cardNameLower.contains("american")

    // Dynamic Card Digits from card name or ID
    val cardDigits = remember(card.cardName, card.id) {
        val digits = card.cardName.filter { it.isDigit() }
        if (digits.length >= 4) digits.takeLast(4)
        else if (digits.isNotEmpty()) digits.padStart(4, '0')
        else card.id.toString().padStart(4, '0')
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Tier-1 Physical Card Mockup UI (Apple Card / CRED style)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandLime.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B), // Dark Slate
                                    Color(0xFF0F172A), // Midnight Navy
                                    Color(0xFF020617)  // Deep Obsidian
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Top Header: Chip + Waves + Bank Name + Quick Edit Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Golden EMV Chip Graphic
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFFEAB308),
                                    modifier = Modifier.size(width = 30.dp, height = 22.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp)
                                            .border(1.dp, Color(0xFFCA8A04), RoundedCornerShape(3.dp))
                                    )
                                }
                                // Contactless Wave Icon
                                Text("📶", fontSize = 14.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                    .clickable { onEditCard() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = card.cardName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = DarkOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Card",
                                    tint = BrandLime,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Card Middle: Masked Digits
                        Text(
                            text = "••••   ••••   ••••   $cardDigits",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 16.sp
                            ),
                            color = DarkOnSurface.copy(alpha = 0.9f)
                        )

                        // Card Bottom Row: Dynamic Unbilled Balance + Dynamic Network Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "UNBILLED DUES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 9.sp
                                    ),
                                    color = BrandLime
                                )
                                Text(
                                    text = "₹${"%,.2f".format(totalUnbilledBalance)}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                    color = DarkOnSurface
                                )
                            }

                            // Dynamic Card Network Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isRuPay -> Color(0xFF1E3A8A)
                                    isVisa -> Color(0xFF1E40AF)
                                    isMastercard -> Color(0xFF7C2D12)
                                    isAmex -> Color(0xFF047857)
                                    else -> DarkSurfaceVariant
                                },
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isRuPay) {
                                        Text("⚡ RuPay", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF38BDF8))
                                    } else if (isVisa) {
                                        Text("VISA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
                                    } else if (isMastercard) {
                                        Text("Mastercard", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFFF97316))
                                    } else if (isAmex) {
                                        Text("AMEX", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF6EE7B7))
                                    } else {
                                        Text("💳 CARD", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Real Dynamic Ledger Overview & Health Status
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = BrandLime, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Card Ledger & Summary",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }

                        // Dynamic Dues Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (unbilledTxns.isEmpty()) BrandLimeContainer else DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (unbilledTxns.isEmpty()) "✓ All Dues Clear 🎉" else "💳 ${unbilledTxns.size} Dues Pending",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (unbilledTxns.isEmpty()) BrandOnLimeContainer else BrandLime
                            )
                        }
                    }

                    // Dynamic 3-Column Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Unbilled Dues", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "₹${"%,.0f".format(totalUnbilledBalance)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (totalUnbilledBalance > 0) Color(0xFFF59E0B) else BrandLime
                            )
                        }
                        Column {
                            Text("Settled History", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "₹${"%,.0f".format(totalSettledBalance)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }
                        Column {
                            Text("Total Spends", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
                            Text(
                                text = "${cardTxns.size} Txns",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avgSpend = if (cardTxns.isNotEmpty()) cardTxns.sumOf { it.transaction.amount } / cardTxns.size else 0.0
                        Text(
                            text = "Avg: ₹${"%,.0f".format(avgSpend)}/spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkOnSurfaceVariant
                        )
                        Text(
                            text = "Export Statement (.csv)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandLime,
                            modifier = Modifier.clickable { onExportStatement(card, cardTxns) }
                        )
                    }
                }
            }
        }

        // 3. Segmented Switcher: [ Unbilled Spends ] | [ Settled Statements ]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "UNBILLED" to "Unbilled (${unbilledTxns.size})",
                    "SETTLED" to "Settled History (${billedTxns.size})"
                ).forEach { (tabKey, label) ->
                    val isSel = selectedTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) BrandLime else Color.Transparent)
                            .clickable { selectedTab = tabKey }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = if (isSel) DarkBackground else DarkOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Tab Content
        if (selectedTab == "UNBILLED") {
            if (unbilledTxns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎉", fontSize = 28.sp)
                            Text(
                                text = "All dues are settled!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = "No unbilled transactions for this card. Future spends will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Settle Dues Checklist & Action Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
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
                                Text(
                                    text = "Select Transactions to Settle",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = if (selectedTxnIdsForRepayment.size == unbilledTxns.size) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandLime,
                                    modifier = Modifier.clickable {
                                        selectedTxnIdsForRepayment = if (selectedTxnIdsForRepayment.size == unbilledTxns.size) {
                                            emptySet()
                                        } else {
                                            unbilledTxns.map { it.transaction.id }.toSet()
                                        }
                                    }
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                unbilledTxns.forEach { txnWithCat ->
                                    val t = txnWithCat.transaction
                                    val isChecked = selectedTxnIdsForRepayment.contains(t.id)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChecked) DarkSurfaceElevated else DarkSurfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isChecked) BrandLime.copy(alpha = 0.4f) else DarkBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedTxnIdsForRepayment = if (isChecked) {
                                                    selectedTxnIdsForRepayment - t.id
                                                } else {
                                                    selectedTxnIdsForRepayment + t.id
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedTxnIdsForRepayment = if (checked == true) {
                                                            selectedTxnIdsForRepayment + t.id
                                                        } else {
                                                            selectedTxnIdsForRepayment - t.id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = BrandLime,
                                                        checkmarkColor = DarkBackground
                                                    )
                                                )

                                                Column {
                                                    Text(
                                                        text = t.merchant,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = DarkOnSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(t.timestamp))
                                                    Text(
                                                        text = "$dateStr • ${txnWithCat.category?.name ?: "Expense"}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = DarkOnSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "₹${"%,.2f".format(t.amount)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = DarkOnSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // 1-Tap Settle Selected Button
                            Button(
                                onClick = {
                                    if (totalRepaymentAmount > 0) {
                                        onRepayCard(card, totalRepaymentAmount, selectedTxnIdsForRepayment.toList())
                                    }
                                },
                                enabled = totalRepaymentAmount > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandLime,
                                    contentColor = DarkBackground,
                                    disabledContainerColor = DarkSurfaceVariant,
                                    disabledContentColor = DarkOnSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (totalRepaymentAmount > 0) "Settle ₹${"%,.0f".format(totalRepaymentAmount)} (${selectedTxnIdsForRepayment.size} Selected)" else "Select Dues to Settle",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // SETTLED STATEMENTS TAB
            if (billedTxns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📄", fontSize = 28.sp)
                            Text(
                                text = "No settled statements yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                            Text(
                                text = "When you record card bill repayments, settled transactions will be archived here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(billedTxns, key = { it.transaction.id }) { txnWithCat ->
                    val t = txnWithCat.transaction
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = t.merchant,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "$dateStr • ${txnWithCat.category?.name ?: "Expense"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkOnSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandLime,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Settlement ID: ${t.ccRepaymentId ?: "Paid"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandLime
                                    )
                                }
                            }

                            Text(
                                text = "₹${"%,.2f".format(t.amount)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkOnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
