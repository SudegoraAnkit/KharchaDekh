package com.ankitsudegora.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ankitsudegora.MainActivity
import com.ankitsudegora.R
import com.ankitsudegora.data.AppDatabase
import com.ankitsudegora.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val context = context ?: return
        val intent = intent ?: return
        val action = intent.action ?: return

        Log.d("TxnActionReceiver", "Received action: $action")

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val txnId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_DISMISS -> {
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                    Log.d("TxnActionReceiver", "Dismissed notification ID: $notificationId")
                }
            }
            ACTION_DISCARD -> {
                if (txnId != -1L) {
                    val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                    val type = intent.getStringExtra(EXTRA_TYPE) ?: "DEBIT"
                    val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: "Unknown"
                    val refNumber = intent.getStringExtra(EXTRA_REF_NUMBER)
                    val smsSenderId = intent.getStringExtra(EXTRA_SMS_SENDER_ID)
                    val notes = intent.getStringExtra(EXTRA_NOTES)
                    val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                    val paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD) ?: "UPI"

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context.applicationContext)
                            val transaction = Transaction(
                                id = txnId,
                                amount = amount,
                                type = type,
                                merchant = merchant,
                                paymentMethod = paymentMethod,
                                isPending = true,
                                source = "NOTIFICATION",
                                smsSenderId = smsSenderId,
                                timestamp = timestamp,
                                refNumber = refNumber,
                                notes = notes
                            )
                            db.transactionDao().deleteTransaction(transaction)
                            Log.d("TxnActionReceiver", "Successfully deleted transaction ID: $txnId")

                            // Post the "Undo" notification
                            showUndoNotification(
                                context,
                                notificationId,
                                txnId,
                                amount,
                                type,
                                merchant,
                                refNumber,
                                paymentMethod,
                                smsSenderId,
                                notes,
                                timestamp
                            )
                        } catch (e: Exception) {
                            Log.e("TxnActionReceiver", "Failed to delete transaction", e)
                        }
                    }
                }
            }
            ACTION_UNDO -> {
                if (txnId != -1L) {
                    val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                    val type = intent.getStringExtra(EXTRA_TYPE) ?: "DEBIT"
                    val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: "Unknown"
                    val refNumber = intent.getStringExtra(EXTRA_REF_NUMBER)
                    val smsSenderId = intent.getStringExtra(EXTRA_SMS_SENDER_ID)
                    val notes = intent.getStringExtra(EXTRA_NOTES)
                    val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                    val paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD) ?: "UPI"

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context.applicationContext)
                            val transaction = Transaction(
                                id = txnId,
                                amount = amount,
                                type = type,
                                merchant = merchant,
                                paymentMethod = paymentMethod,
                                isPending = true,
                                source = "NOTIFICATION",
                                smsSenderId = smsSenderId,
                                timestamp = timestamp,
                                refNumber = refNumber,
                                notes = notes
                            )
                            db.transactionDao().insertTransaction(transaction)
                            Log.d("TxnActionReceiver", "Successfully restored/inserted transaction ID: $txnId")

                            // Re-post original notification
                            showOriginalNotification(
                                context,
                                notificationId,
                                txnId,
                                amount,
                                type,
                                merchant,
                                refNumber,
                                paymentMethod,
                                smsSenderId,
                                notes,
                                timestamp
                            )
                        } catch (e: Exception) {
                            Log.e("TxnActionReceiver", "Failed to restore transaction", e)
                        }
                    }
                }
            }
        }
    }

    private fun showUndoNotification(
        context: Context,
        notificationId: Int,
        txnId: Long,
        amount: Double,
        type: String,
        merchant: String,
        refNumber: String?,
        paymentMethod: String,
        smsSenderId: String?,
        notes: String?,
        timestamp: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "kharchadekh_notifications"

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Action Intent to Undo Discard
        val undoIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = ACTION_UNDO
            putExtra(EXTRA_TRANSACTION_ID, txnId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_MERCHANT, merchant)
            putExtra(EXTRA_REF_NUMBER, refNumber)
            putExtra(EXTRA_PAYMENT_METHOD, paymentMethod)
            putExtra(EXTRA_SMS_SENDER_ID, smsSenderId)
            putExtra(EXTRA_NOTES, notes)
            putExtra(EXTRA_TIMESTAMP, timestamp)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            undoIntent,
            flags
        )

        // Action Intent to Dismiss notification permanently
        val dismissIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            dismissIntent,
            flags
        )

        val bodyContent = "₹$amount at $merchant alert discarded."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Transaction Alert Discarded")
            .setContentText(bodyContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Undo", undoPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showOriginalNotification(
        context: Context,
        notificationId: Int,
        txnId: Long,
        amount: Double,
        type: String,
        merchant: String,
        refNumber: String?,
        paymentMethod: String,
        smsSenderId: String?,
        notes: String?,
        timestamp: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "kharchadekh_notifications"

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TRANSACTION_ID", txnId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, txnId.toInt(), mainIntent, flags)

        // Review Later Action (Dismisses the notification)
        val reviewLaterIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = ACTION_REVIEW_LATER
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val reviewLaterPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            reviewLaterIntent,
            flags
        )

        // Discard Action (Deletes from DB and shows Undo notification)
        val discardIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = ACTION_DISCARD
            putExtra(EXTRA_TRANSACTION_ID, txnId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_MERCHANT, merchant)
            putExtra(EXTRA_REF_NUMBER, refNumber)
            putExtra(EXTRA_PAYMENT_METHOD, paymentMethod)
            putExtra(EXTRA_SMS_SENDER_ID, smsSenderId)
            putExtra(EXTRA_NOTES, notes)
            putExtra(EXTRA_TIMESTAMP, timestamp)
        }
        val discardPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            discardIntent,
            flags
        )

        val emoji = if (type == "DEBIT") "💸" else "🏦"
        val label = if (type == "DEBIT") "Spent" else "Received"
        val bodyContent = "$label ₹$amount at $merchant? Tap to categorize it. $emoji"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Transaction Detected")
            .setContentText(bodyContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Review Later", reviewLaterPendingIntent)
            .addAction(0, "Discard Alert", discardPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val ACTION_DISMISS = "com.ankitsudegora.action.DISMISS"
        const val ACTION_DISCARD = "com.ankitsudegora.action.DISCARD"
        const val ACTION_UNDO = "com.ankitsudegora.action.UNDO"
        const val ACTION_REVIEW_LATER = "com.ankitsudegora.action.DISMISS"

        const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_AMOUNT = "EXTRA_AMOUNT"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_MERCHANT = "EXTRA_MERCHANT"
        const val EXTRA_REF_NUMBER = "EXTRA_REF_NUMBER"
        const val EXTRA_SMS_SENDER_ID = "EXTRA_SMS_SENDER_ID"
        const val EXTRA_NOTES = "EXTRA_NOTES"
        const val EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP"
        const val EXTRA_PAYMENT_METHOD = "EXTRA_PAYMENT_METHOD"
    }
}
