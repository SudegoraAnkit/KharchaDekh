package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            if (messages.isEmpty()) return

            val firstMessage = messages[0]
            val sender = firstMessage.originatingAddress ?: return
            
            // Reconstruct full SMS body if multi-part
            val body = messages.joinToString("") { it.messageBody ?: "" }

            Log.d("SmsReceiver", "Received SMS from: $sender body: $body")

            val parsed = parseSms(sender, body)
            if (parsed != null) {
                Log.d("SmsReceiver", "Parsed SMS: $parsed")
                // Save to database as pending
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val transaction = Transaction(
                        amount = parsed.amount,
                        type = parsed.type,
                        merchant = parsed.merchant,
                        paymentMethod = if (parsed.type == "DEBIT") "UPI" else "NETBANKING",
                        isPending = true,
                        source = "SMS",
                        smsSenderId = parsed.sender,
                        timestamp = System.currentTimeMillis()
                    )
                    val id = db.transactionDao().insertTransaction(transaction)
                    Log.d("SmsReceiver", "Saved transaction with ID: $id")

                    // Push system notification for quick enrichment
                    showNotification(context, id, parsed)
                }
            }
        }
    }

    private fun parseSms(sender: String, body: String): ParsedSms? {
        if (!isAlphanumericSender(sender)) return null

        val bodyLower = body.lowercase()

        // Match banking debit/credit indicators
        val isDebit = bodyLower.contains("debited") || bodyLower.contains("withdrawn") ||
                bodyLower.contains("spent") || bodyLower.contains("paid") || 
                bodyLower.contains("sent") || bodyLower.contains("txnt o")
        val isCredit = bodyLower.contains("credited") || bodyLower.contains("received") || 
                bodyLower.contains("added")

        if (!isDebit && !isCredit) return null

        val type = if (isDebit) "DEBIT" else "CREDIT"

        // Regex for Amount matching patterns like Rs. X, Rs X, INR X, ₹ X, ₹. X, INR. X
        val amountRegex = Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Safe merchant extractor avoiding confidential/long text leakages
        // e.g., "spent Rs. 250 at Swiggy", "paid to Amazon", "sent to 9876543210@upi"
        val merchantRegex = Regex("(?:at|to|from|vpa|into|thru)\\s+([a-zA-Z0-9]{3,20})", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(body)
        var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: ""
        
        // Clean up merchant name if empty or looks like a numeric string
        if (merchant.isBlank() || merchant.all { it.isDigit() }) {
            merchant = "Unknown Merchant"
        } else {
            // Capitalize merchant name nicely
            merchant = merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return ParsedSms(
            amount = amount,
            type = type,
            merchant = merchant,
            sender = sender
        )
    }

    private fun isAlphanumericSender(sender: String): Boolean {
        // Alphanumeric banking senders in India are usually 8 characters, or contain letters, with a hyphen.
        // Match standard format like AD-SBIINB, VM-HDFCBK or VM-PAYTM
        // Filter out personal phone numbers (10 digits, +91 digits, etc.)
        val cleanSender = sender.trim()
        val digitsOnly = cleanSender.replace("+91", "").replace(" ", "").trim()
        if (digitsOnly.length == 10 && digitsOnly.all { it.isDigit() }) {
            return false
        }
        return cleanSender.any { it.isLetter() }
    }

    private fun showNotification(context: Context, txnId: Long, parsed: ParsedSms) {
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

        // Action when tapped: open MainActivity and pass transaction id to auto-navigate
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TRANSACTION_ID", txnId)
        }

        // Custom PendingIntent flags
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
            .setSmallIcon(android.R.drawable.stat_sys_warning) // fallback small icon
            .setContentTitle("New Transaction Detected")
            .setContentText(bodyContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(txnId.toInt(), notification)
    }
}

data class ParsedSms(
    val amount: Double,
    val type: String,
    val merchant: String,
    val sender: String
)
