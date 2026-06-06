package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbn = sbn ?: return

        // Skip our own app's notifications to avoid an endless alert loop
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("TxnNotification", "Received notification from ${sbn.packageName} - Title: $title, Text: $text")

        // Parse banking/transaction notifications
        val parsed = parseNotification(title, text)
        if (parsed != null) {
            Log.d("TxnNotification", "Successfully parsed notification: $parsed")
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val transaction = Transaction(
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    paymentMethod = if (parsed.type == "DEBIT") "UPI" else "NETBANKING",
                    isPending = true,
                    source = "NOTIFICATION",
                    smsSenderId = parsed.sender, // Using smsSenderId column for DB compatibility
                    timestamp = System.currentTimeMillis()
                )
                val id = db.transactionDao().insertTransaction(transaction)
                Log.d("TxnNotification", "Saved notification transaction with ID: $id")

                showEnrichmentNotification(applicationContext, id, parsed)
            }
        }
    }

    private fun parseNotification(title: String, text: String): ParsedNotification? {
        val textLower = text.lowercase()

        // Match banking debit/credit indicators
        val isDebit = textLower.contains("debited") || textLower.contains("withdrawn") ||
                textLower.contains("spent") || textLower.contains("paid") || 
                textLower.contains("sent") || textLower.contains("txnt o")
        val isCredit = textLower.contains("credited") || textLower.contains("received") || 
                textLower.contains("added")

        if (!isDebit && !isCredit) return null

        val type = if (isDebit) "DEBIT" else "CREDIT"

        // Regex for Amount matching patterns like Rs. X, Rs X, INR X, ₹ X, ₹. X, INR. X
        val amountRegex = Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(text) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Safe merchant extractor avoiding confidential/long text leakages
        val merchantRegex = Regex("(?:at|to|from|vpa|into|thru)\\s+([a-zA-Z0-9]{3,20})", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(text)
        var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: ""
        
        // Clean up merchant name
        if (merchant.isBlank() || merchant.all { it.isDigit() }) {
            merchant = title.ifBlank { "Unknown Merchant" }
        } else {
            merchant = merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val senderName = title.ifBlank { "Alert" }

        return ParsedNotification(
            amount = amount,
            type = type,
            merchant = merchant,
            sender = senderName
        )
    }

    private fun showEnrichmentNotification(context: Context, txnId: Long, parsed: ParsedNotification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "kharchadekh_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Category enrichment alerts for transaction parsing"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TRANSACTION_ID", txnId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, txnId.toInt(), intent, flags)

        val emoji = if (parsed.type == "DEBIT") "💸" else "🏦"
        val label = if (parsed.type == "DEBIT") "Spent" else "Received"
        
        val bodyContent = "$label ₹${parsed.amount} at ${parsed.merchant}? Tap to categorize it. $emoji"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("New Transaction Detected")
            .setContentText(bodyContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(txnId.toInt(), notification)
    }
}

data class ParsedNotification(
    val amount: Double,
    val type: String,
    val merchant: String,
    val sender: String
)
