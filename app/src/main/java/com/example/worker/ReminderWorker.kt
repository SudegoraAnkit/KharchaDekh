package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.data.AppDatabase
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ReminderWorker", "Starting checks for daily reminder")
        val db = AppDatabase.getDatabase(applicationContext)

        val pendingCount = db.transactionDao().getPendingTransactionsCount()
        
        val last24Hours = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val entriesLast24h = db.transactionDao().getTransactionsCountSince(last24Hours)

        Log.d("ReminderWorker", "Pending transactions: $pendingCount, logged during 24h: $entriesLast24h")

        // Condition: Fire reminder if user has pending unresolved transactions OR zero entries in past 24 hours
        if (pendingCount > 0 || entriesLast24h == 0) {
            triggerReminderNotification()
        }

        return Result.success()
    }

    private fun triggerReminderNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "kharchadekh_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily evening reminder to clean and categorize your expenses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TO_PENDING", true)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(applicationContext, 999, intent, flags)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Review Today's Expenses 📝")
            .setContentText("Don't let your expenses pile up! Tap to categorize or log your spending.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }

    companion object {
        private const val REMINDER_WORK_NAME = "kharchadekh_daily_reminder"

        fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 30) {
            val workManager = WorkManager.getInstance(context)

            // Calculate initial delay until target time today, or tomorrow if target already passed today
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Run daily (every 24 hours)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("ReminderWorker", "Scheduled daily reminder for $hour:$minute. Initial delay in mins: ${initialDelay / 60000}")
        }
        
        fun cancelReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
            Log.d("ReminderWorker", "Cancelled daily reminder work")
        }
    }
}
