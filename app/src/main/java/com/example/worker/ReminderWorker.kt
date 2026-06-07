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
        val isUnconditional = inputData.getBoolean("KEY_IS_UNCONDITIONAL", false)
        Log.d("ReminderWorker", "Starting checks for daily reminder. Unconditional = $isUnconditional")
        val db = AppDatabase.getDatabase(applicationContext)

        val pendingCount = db.transactionDao().getPendingTransactionsCount()
        
        val last24Hours = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val entriesLast24h = db.transactionDao().getTransactionsCountSince(last24Hours)

        Log.d("ReminderWorker", "Pending transactions: $pendingCount, logged during 24h: $entriesLast24h")

        // Condition: Fire reminder if unconditional OR user has pending unresolved transactions OR zero entries in past 24 hours
        if (isUnconditional || pendingCount > 0 || entriesLast24h == 0) {
            triggerReminderNotification()
        }

        // Night auto-backup if enabled (isUnconditional is true only for the evening/night reminder)
        val prefs = applicationContext.getSharedPreferences("kharchadekh_prefs", Context.MODE_PRIVATE)
        val autoBackupEnabled = prefs.getBoolean("auto_backup_night", false)
        if (isUnconditional && autoBackupEnabled) {
            Log.d("ReminderWorker", "Triggering night auto-backup...")
            val backupFile = com.example.util.BackupManager.backupDatabaseToDefaultFile(applicationContext)
            if (backupFile != null) {
                Log.d("ReminderWorker", "Night auto-backup completed: ${backupFile.absolutePath}")
            } else {
                Log.e("ReminderWorker", "Night auto-backup failed.")
            }
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
                description = "Daily reminders to clean and categorize your expenses"
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
            .setContentTitle("Review Expenses 📝")
            .setContentText("Don't let your expenses pile up! Tap to review notifications or log manual spends.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }

    companion object {

        private fun scheduleSingleReminder(
            context: Context,
            workName: String,
            hour: Int,
            minute: Int,
            isUnconditional: Boolean,
            forceRestart: Boolean
        ) {
            val workManager = WorkManager.getInstance(context)

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

            val inputData = workDataOf("KEY_IS_UNCONDITIONAL" to isUnconditional)

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            val policy = if (forceRestart) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP

            workManager.enqueueUniquePeriodicWork(
                workName,
                policy,
                request
            )
            Log.d("ReminderWorker", "Scheduled $workName for $hour:$minute. Unconditional: $isUnconditional. Policy: $policy. Delay: ${initialDelay / 60000}m")
        }

        fun scheduleAllReminders(
            context: Context, 
            customHour: Int = 20, 
            customMinute: Int = 30, 
            forceRestart: Boolean = false
        ) {
            // 1. Morning Reminder (9:00 AM) - Conditional
            scheduleSingleReminder(context, "kharchadekh_morning_reminder", 9, 0, false, forceRestart)

            // 2. Midday Reminder (1:30 PM) - Conditional
            scheduleSingleReminder(context, "kharchadekh_midday_reminder", 13, 30, false, forceRestart)

            // 3. Afternoon Reminder (5:30 PM) - Conditional
            scheduleSingleReminder(context, "kharchadekh_afternoon_reminder", 17, 30, false, forceRestart)

            // 4. Night/Evening Reminder (Custom user selected hour, Unconditional)
            scheduleSingleReminder(context, "kharchadekh_night_reminder", customHour, customMinute, true, forceRestart)
        }

        fun cancelAllReminders(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("kharchadekh_morning_reminder")
            workManager.cancelUniqueWork("kharchadekh_midday_reminder")
            workManager.cancelUniqueWork("kharchadekh_afternoon_reminder")
            workManager.cancelUniqueWork("kharchadekh_night_reminder")
            Log.d("ReminderWorker", "Cancelled all reminders work")
        }

        fun triggerBudgetNotification(context: Context, categoryName: String, spent: Double, limit: Double, isExceeded: Boolean) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "kharchadekh_budget_alerts"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when you approach or exceed your category spending budgets"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 888, intent, flags)

            val title = if (isExceeded) "⚠️ Budget Exceeded!" else "🚨 Approaching Budget Limit"
            val text = if (isExceeded) {
                "You have spent ₹%,.0f of your ₹%,.0f limit in '$categoryName'."
            } else {
                "You have spent 80% of your limit (₹%,.0f of ₹%,.0f) in '$categoryName'."
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text.format(spent, limit))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(categoryName.hashCode(), notification)
        }
    }
}
