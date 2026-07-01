package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreditCardScreen(
    creditCards: List<CreditCard>,
    allTransactions: List<TransactionWithCategory>,
    onRepayCard: (CreditCard, Double, List<Long>) -> Unit,
    onExportStatement: (CreditCard, List<TransactionWithCategory>) -> Unit
) {
    var selectedCardId by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(creditCards) {
        if (selectedCardId == null && creditCards.isNotEmpty()) {
            selectedCardId = creditCards.first().id
        }
    }

    val selectedCard = remember(creditCards, selectedCardId) {
        creditCards.find { it.id == selectedCardId } ?: creditCards.firstOrNull()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Credit Card Tracker",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (creditCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No credit cards registered yet.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Go to Settings -> Credit Card Management to add one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Card Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    creditCards.forEach { card ->
                        val isSelected = selectedCardId == card.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else Color.Transparent
                                )
                                .clickable { selectedCardId = card.id }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.cardName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedCard != null) {
                    CardDetailsView(
                        card = selectedCard,
                        transactions = allTransactions,
                        onRepayCard = onRepayCard,
                        onExportStatement = onExportStatement
                    )
                }
            }
        }
    }
}

@Composable
fun CardDetailsView(
    card: CreditCard,
    transactions: List<TransactionWithCategory>,
    onRepayCard: (CreditCard, Double, List<Long>) -> Unit,
    onExportStatement: (CreditCard, List<TransactionWithCategory>) -> Unit
) {
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

    var selectedTxnIdsForRepayment by remember { mutableStateOf(setOf<Long>()) }

    // Auto select all unbilled transactions for repayment
    LaunchedEffect(card.id, unbilledTxns) {
        selectedTxnIdsForRepayment = unbilledTxns.map { it.transaction.id }.toSet()
    }

    val totalRepaymentAmount = remember(unbilledTxns, selectedTxnIdsForRepayment) {
        unbilledTxns.filter { selectedTxnIdsForRepayment.contains(it.transaction.id) }
            .sumOf { it.transaction.amount }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Balance Overview Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.cardName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "UNBILLED BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "₹${"%,.2f".format(totalUnbilledBalance)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${unbilledTxns.size} Unsettled Ledger Entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            
                            IconButton(
                                onClick = { onExportStatement(card, cardTxns) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export CSV Statement",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Repayment Action Card
        if (unbilledTxns.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            text = "Record Bill Repayment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Check the unbilled transactions to settle. Saving this will write a repayment ledger entry of ₹${"%,.0f".format(totalRepaymentAmount)} under the CreditCard Payment category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Checklist
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            unbilledTxns.forEach { txnWithCat ->
                                val t = txnWithCat.transaction
                                val isChecked = selectedTxnIdsForRepayment.contains(t.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTxnIdsForRepayment = if (isChecked) {
                                                selectedTxnIdsForRepayment - t.id
                                            } else {
                                                selectedTxnIdsForRepayment + t.id
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedTxnIdsForRepayment = if (checked == true) {
                                                selectedTxnIdsForRepayment + t.id
                                            } else {
                                                selectedTxnIdsForRepayment - t.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val dateStr = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(t.timestamp))
                                        Text(
                                            text = t.merchant,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "$dateStr • ${txnWithCat.category?.name ?: "Uncategorized"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "₹${"%,.0f".format(t.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (totalRepaymentAmount > 0) {
                                    onRepayCard(card, totalRepaymentAmount, selectedTxnIdsForRepayment.toList())
                                }
                            },
                            enabled = totalRepaymentAmount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Settle Selected Transactions")
                        }
                    }
                }
            }
        }

        // Billed / Settled Statements Log
        item {
            Text(
                text = "Settled Statements & Historical Log",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (billedTxns.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No settled transactions in statement history yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(billedTxns, key = { it.transaction.id }) { txnWithCat ->
                val t = txnWithCat.transaction
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t.timestamp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = t.merchant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$dateStr • ${txnWithCat.category?.name ?: "Uncategorized"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Settlement ID: ${t.ccRepaymentId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "₹${"%,.0f".format(t.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
