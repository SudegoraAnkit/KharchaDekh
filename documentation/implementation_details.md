# Implementation Details: KharchaDekh

This document outlines specific code details, UI components, and backend routines I developed for the application.

---

## 1. Declarative Compose UI Components

### A. Dashboard Segmented Breakdown Bar
Instead of importing a heavy chart library, I built a custom, lightweight segmented progress bar in Compose. It calculates the percentage usage for each category and maps them to a horizontal layout:

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
) {
    val barColors = listOf(
        Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
        Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFFFFCA28)
    )

    analytics.categoryBreakdown.forEachIndexed { index, item ->
        if (item.percentage > 0) {
            val col = barColors[index % barColors.size]
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(item.percentage.coerceAtLeast(1f))
                    .background(col)
            )
        }
    }
}
```
*   **Aesthetic Effect**: The layout dynamically weights boxes according to percentage spent, creating a smooth, responsive distribution bar.

### B. High-Contrast Credit Card Overrides
To ensure readability in both light and dark themes, I implemented dynamic color styling for transaction list entries:

```kotlin
val isDebit = item.transaction.type == "DEBIT"
val isDark = isSystemInDarkTheme()

val cardColor = if (isDebit) {
    MaterialTheme.colorScheme.surface
} else {
    if (isDark) Color(0xFF0F2D24) else Color(0xFFE6F9F6)
}

val amountColor = if (isDebit) {
    MaterialTheme.colorScheme.onSurface
} else {
    if (isDark) Color(0xFF4ADE80) else Color(0xFF0F766E)
}
```

---

## 2. Notification listener Parsing Pipeline

The `TransactionNotificationListener` processes incoming notification strings locally. It extracts transaction details using a structured pipeline:

```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val extras = sbn.notification.extras
    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

    // 1. Package name validation
    if (!isFinancialApp(packageName)) return

    // 2. Local regex extraction
    val transaction = parseNotificationText(title, text) ?: return

    // 3. Save entry as isPending = true
    CoroutineScope(Dispatchers.IO).launch {
        database.transactionDao().insert(transaction)
        triggerSystemNotification(transaction)
    }
}
```

---

## 3. Native Canvas PDF Exporter (`Exporter.kt`)

I wrote the PDF statement exporter using Android's native `PdfDocument` to avoid adding large external dependencies. It formats transaction logs and draws them directly to a virtual canvas:

```kotlin
fun exportToPdf(context: Context, transactions: List<TransactionWithCategory>): Uri? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var yPosition = 50f

    // 1. Draw Title
    val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("KharchaDekh Expense Report", 50f, yPosition, headerPaint)
    yPosition += 40f

    // 2. Draw Table Rows with Page Break logic
    val textPaint = Paint().apply { color = Color.BLACK; textSize = 12f }
    for (item in transactions) {
        if (yPosition > 780f) { // Page height boundary
            pdfDocument.finishPage(page)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
            yPosition = 50f
        }
        
        val t = item.transaction
        canvas.drawText(t.merchant, 150f, yPosition, textPaint)
        canvas.drawText("₹%,.2f".format(t.amount), 480f, yPosition, textPaint)
        yPosition += 22f
    }
    pdfDocument.finishPage(page)
    ...
}
```

---

## 4. Multi-Alarm WorkManager Reminders

To schedule 4 daily reminder nudges reliably, I set up a calendar scheduler inside `ReminderWorker.kt` that calculates the initial startup delay for each worker relative to the target time:

```kotlin
private fun scheduleSingleReminder(
    context: Context,
    workName: String,
    hour: Int,
    minute: Int,
    isUnconditional: Boolean,
    forceRestart: Boolean
) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    if (calendar.before(Calendar.getInstance())) {
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Push to next day if time has passed
    }

    val initialDelay = calendar.timeInMillis - System.currentTimeMillis()
    val inputData = workDataOf("KEY_IS_UNCONDITIONAL" to isUnconditional)

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .build()

    val policy = if (forceRestart) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(workName, policy, request)
}
```
