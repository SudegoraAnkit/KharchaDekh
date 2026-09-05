package com.ankitsudegora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankitsudegora.data.TransactionWithCategory
import com.ankitsudegora.ui.theme.*

@Composable
fun TransactionListItem(
    item: TransactionWithCategory,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    primaryCurrency: String = "INR",
    onConvertAmount: ((Double, String) -> Double)? = null
) {
    val isDebit = item.transaction.type == "DEBIT"
    val catColor = CategoryColors.getCategoryColor(item.category?.name)

    Card(
        onClick = onEditClicked,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${item.transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.5f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = item.category?.iconResName ?: "category"
                    Icon(
                        imageVector = getIconVector(icon),
                        contentDescription = item.category?.name ?: "Expense",
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val subCatLabel = if (!item.transaction.subCategory.isNullOrBlank()) " (${item.transaction.subCategory})" else ""
                    Text(
                        text = (item.category?.name ?: "Uncategorized") + subCatLabel + 
                                if (!item.transaction.notes.isNullOrBlank()) " • " + item.transaction.notes else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val date = java.util.Date(item.transaction.timestamp)
                    val format = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                    Text(
                        text = "${format.format(date)} • ${item.transaction.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                val sign = if (isDebit) "-" else "+"
                val amountColor = if (isDebit) DarkOnSurface else BrandLime
                
                val primarySymbol = getCurrencySymbol(primaryCurrency)
                val amountText = if (item.transaction.currency.uppercase() != primaryCurrency.uppercase() && onConvertAmount != null) {
                    val converted = onConvertAmount(item.transaction.amount, item.transaction.currency)
                    "$sign $primarySymbol${"%,.2f".format(converted)}"
                } else {
                    "$sign ${getCurrencySymbol(item.transaction.currency)}${"%,.2f".format(item.transaction.amount)}"
                }

                Text(
                    text = amountText,
                    style = MoneyTypography.ListItemAmount,
                    color = amountColor,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = DarkError.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
