package com.ankitsudegora.service

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
import com.ankitsudegora.MainActivity
import com.ankitsudegora.R
import com.ankitsudegora.data.AppDatabase
import com.ankitsudegora.data.Transaction
import com.ankitsudegora.util.SemanticTransactionParser
import com.ankitsudegora.util.ParsedTransaction
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

        // Parse banking/transaction notifications via hybrid Semantic parser
        val parsed = SemanticTransactionParser.parse(title, text)
        if (parsed != null) {
            Log.d("TxnNotification", "Successfully parsed transaction: $parsed")
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(applicationContext)
                
                // Deduplication Strategy
                if (parsed.refNumber != null) {
                    // Case A: Alert has a UTR ref number (e.g. SMS)
                    val duplicateCount = db.transactionDao().getTransactionCountByRefNumber(parsed.refNumber)
                    if (duplicateCount > 0) {
                        Log.w("TxnNotification", "Duplicate UTR reference detected: ${parsed.refNumber}. Dropping.")
                        return@launch
                    }
                    
                    // Check if we can enrich a pending transaction of the same amount/type from the last 2 hours
                    val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
                    val pendingTxn = db.transactionDao().getMatchingPendingTransaction(
                        amount = parsed.amount,
                        type = parsed.type,
                        since = twoHoursAgo
                    )
                    
                    if (pendingTxn != null) {
                        // Enrich existing pending transaction instead of inserting a duplicate!
                        val updatedTxn = pendingTxn.copy(
                            refNumber = parsed.refNumber,
                            merchant = parsed.merchant.ifBlank { pendingTxn.merchant }
                        )
                        db.transactionDao().updateTransaction(updatedTxn)
                        Log.d("TxnNotification", "Successfully enriched pending transaction ID ${pendingTxn.id} with UTR ${parsed.refNumber}")
                        return@launch
                    }
                } else {
                    // Case B: Alert does NOT have a UTR (e.g. Push Notification)
                    // Check within a tight 1-minute window for matching amount, type, and merchant to drop duplicates
                    val oneMinuteAgo = System.currentTimeMillis() - 60000L
                    val duplicateCount = db.transactionDao().getMatchingTransactionCount(
                        amount = parsed.amount,
                        type = parsed.type,
                        merchant = parsed.merchant,
                        minTimestamp = oneMinuteAgo
                    )
                    if (duplicateCount > 0) {
                        Log.w("TxnNotification", "Duplicate non-UTR transaction detected within 1 minute. Dropping.")
                        return@launch
                    }
                }

                val transaction = Transaction(
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    paymentMethod = if (parsed.type == "DEBIT") "UPI" else "NETBANKING",
                    isPending = true,
                    source = "NOTIFICATION",
                    smsSenderId = parsed.sender,
                    timestamp = System.currentTimeMillis(),
                    refNumber = parsed.refNumber
                )
                val id = db.transactionDao().insertTransaction(transaction)
                Log.d("TxnNotification", "Saved notification transaction with ID: $id")

                showEnrichmentNotification(applicationContext, id, parsed)
            }
        }
    }

    private fun showEnrichmentNotification(context: Context, txnId: Long, parsed: ParsedTransaction) {
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Transaction Detected")
            .setContentText(bodyContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(txnId.toInt(), notification)
    }
}
