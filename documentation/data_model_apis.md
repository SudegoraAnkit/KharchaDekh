# Data Model & Interfaces: KharchaDekh

Since KharchaDekh runs entirely on-device, my data architecture is built around local storage relationships and system integration interfaces rather than remote REST endpoints.

---

## 1. Database Schema (Room SQLite)

I designed three primary relational entities in Room.

  ┌────────────────────────────────────────────────────────┐
  │                        Category                        │
  ├────────────────────────────────────────────────────────┤
  │ PK id           : Long                                 │
  │    name         : String                               │
  │    iconResName  : String                               │
  │    budgetLimit  : Double?                              │
  │    isCustom     : Boolean                              │
  └───────────┬───────────────────────────┬────────────────┘
              │ 1                         │ 1
              │                           │
              │ 0..* (FK categoryId)      │ 0..* (FK categoryId)
              ▼                           ▼
  ┌────────────────────────┐  ┌────────────────────────────┐
  │      Transaction       │  │     RecurringSchedule      │
  ├────────────────────────┤  ├────────────────────────────┤
  │ PK id       : Long     │  │ PK id           : Long     │
  │    amount   : Double   │  │    amount       : Double   │
  │    type     : String   │  │    type         : String   │
  │    merchant : String   │  │    merchant     : String   │
  │ FK catId    : Long?    │  │ FK catId        : Long?    │
  │    notes    : String?  │  │    notes        : String?  │
  │    time     : Long     │  │    payMethod    : String   │
  │    method   : String   │  │    frequency    : String   │
  │    isPending: Boolean  │  │    lastTriggered: Long     │
  │    source   : String   │  │    nextTrigger  : Long     │
  │    senderId : String?  │  │    isActive     : Boolean  │
  │    subCat   : String?  │  │    subCat       : String?  │
  └────────────────────────┘  └────────────────────────────┘

  ┌────────────────────────┐  1     0..* ┌─────────────────┐
  │      GroceryList       ├─────────────►│   GroceryItem   │
  ├────────────────────────┤             ├─────────────────┤
  │ PK id       : Long     │             │ PK id  : Long   │
  │    name     : String   │             │ FK list: Long   │
  │    budgetCap: Double?  │             │    name: String │
  │    created  : Long     │             │    qty : Int    │
  │    status   : String   │             │    prc : Double │
  └────────────────────────┘             │    isChk: Boolean│
                                         └─────────────────┘
```

### A. Category Entity (`categories` table)
Represents budget groups.
*   `id` (Long, Primary Key, AutoGenerate): Unique category ID.
*   `name` (String): The category label (e.g. "Food", "Entertainment").
*   `iconResName` (String): Resource name for mapping vector icons (e.g. "shopping_bag", "restaurant").
*   `budgetLimit` (Double, Nullable): Monthly threshold limit (INR).
*   `isCustom` (Boolean): Identifies user-created custom categories.

### B. Transaction Entity (`transactions` table)
Represents a finalized or pending financial entry.
*   `id` (Long, Primary Key, AutoGenerate)
*   `amount` (Double): Monetary value.
*   `type` (String): Spends classification (`DEBIT` or `CREDIT`).
*   `merchant` (String): Parsed business entity name (e.g., "Swiggy", "Zomato", "Zara").
*   `categoryId` (Long, Foreign Key, Nullable): Maps to `Category.id` (set to `ON DELETE SET NULL`).
*   `notes` (String, Nullable): User-entered notes.
*   `timestamp` (Long): Transaction time (epoch milliseconds).
*   `paymentMethod` (String): Payment method (`UPI`, `CASH`, `CARD`, `NETBANKING`).
*   `isPending` (Boolean): Flag representing unfinalized SMS notifications.
*   `source` (String): Entry origin (`NOTIFICATION`, `MANUAL`, `RECURRING`).
*   `smsSenderId` (String, Nullable): The sender ID of the notification (e.g. "AD-HDFCBK").
*   `subCategory` (String, Nullable): Specific detail type label (e.g., "Mutual Funds", "Home Rent").

### C. RecurringSchedule Entity (`recurring_schedules` table)
Represents scheduled payments.
*   `id` (Long, Primary Key, AutoGenerate)
*   `amount` (Double)
*   `type` (String)
*   `merchant` (String)
*   `categoryId` (Long, Foreign Key, Nullable)
*   `notes` (String, Nullable)
*   `paymentMethod` (String)
*   `frequency` (String): Recurrence interval (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`).
*   `lastTriggered` (Long): Timestamp of last execution.
*   `nextTriggerTime` (Long): Timestamp of next scheduled execution.
*   `isActive` (Boolean): Toggle switch status.
*   `subCategory` (String, Nullable): Specific detail type label.

### D. GroceryList Entity (`grocery_lists` table)
Represents a user's offline shopping draft.
*   `id` (Long, Primary Key, AutoGenerate): Unique checklist draft ID.
*   `name` (String): The list label (e.g., "Weekly Groceries").
*   `budgetCap` (Double, Nullable): Optional budget threshold.
*   `createdTimestamp` (Long): Creation timestamp (epoch milliseconds).
*   `status` (String): Status code (`DRAFT` or `COMPLETED`).

### E. GroceryItem Entity (`grocery_items` table)
Represents an item within a shopping list.
*   `id` (Long, Primary Key, AutoGenerate): Unique item row ID.
*   `listId` (Long, Foreign Key): Maps to `GroceryList.id` (set to `ON DELETE CASCADE`).
*   `name` (String): Item name (e.g., "Organic Milk").
*   `quantity` (Int): Item count.
*   `price` (Double): Price estimate or actual checkout unit price.
*   `isChecked` (Boolean): Checked checklist state.

---

## 2. On-Device Notification Parsing Logic (Regex Engine)

The local parsing engine uses regular expression matching rules to process notifications.

### Match Rule: Debit Transactions
Matches values and merchants for outgoing payments:
```kotlin
val debitRegexes = listOf(
    // e.g. "debited by Rs 150 at Swiggy" or "spent Rs. 350 at Zomato"
    Regex("(?:debited|spent|withdrawn|paid|sent)\\s+(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
    Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:debited|spent|paid|sent|withdrawn)")
)
```
*   **Capture Group 1**: Amount (commas are stripped out, e.g. `"14,500.00"` -> `14500.0`).
*   **Merchant Extractor**:
    ```kotlin
    Regex("(?:at|to|vpa|into|thru)\\s+([a-zA-Z0-9\\s]{3,20})", RegexOption.IGNORE_CASE)
    ```

### Match Rule: Credit Transactions
Matches incoming values:
```kotlin
val creditRegexes = listOf(
    // e.g. "credited by Rs 1,200" or "Rs 500 received"
    Regex("(?:credited|received|added|deposited)\\s+(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
    Regex("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:credited|received|added|deposited)")
)
```

---

## 3. Storage Access Framework (SAF) & File Interfacing

I implemented Android system contract pickers to handle database file reads/writes and share reports.

### A. Backup Writer (`ACTION_CREATE_DOCUMENT`)
*   **MIME Type**: `application/octet-stream`
*   **Default Filename**: `kharchadekh_backup.db`
*   **Write Protocol**: MainActivity registers an Activity Result Contract launcher. When users tap Backup, it launches the file picker. Once the file path `Uri` is returned, the app streams the checkpointed database file directly to it:
    ```kotlin
    val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? -> ... }
    ```

### B. Restore Reader (`ACTION_OPEN_DOCUMENT`)
*   **MIME Types**: `*/*` or `application/octet-stream`
*   **Read Protocol**: Opens the file explorer. Once the backup file is selected, the returned `Uri` is processed, copying the file over the existing database sandbox file path.

### C. File Sharing (`FileProvider`)
To share generated reports (CSV/PDF) without requiring storage permissions on modern Android versions, I declared a secure `FileProvider` path inside `AndroidManifest.xml`:
*   **Authority**: `${applicationId}.fileprovider`
*   **Path Mapping**: maps the cache folder `exports/` (`context.cacheDir/exports`) to a secure sharing URI.
