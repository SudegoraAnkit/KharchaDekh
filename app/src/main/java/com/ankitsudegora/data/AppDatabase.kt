package com.ankitsudegora.data

import android.content.Context
import androidx.room.*
import androidx.room.Transaction as RoomTransactionAnnot
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconResName: String, // String representation for the icon mapping
    val isCustom: Boolean = false,
    val budgetLimit: Double? = null
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val merchant: String,
    val categoryId: Long? = null,
    val notes: String? = null,
    val timestamp: Long,
    val paymentMethod: String, // "UPI", "CARD", "CASH", "NETBANKING"
    val isPending: Boolean = false, // True for notifications requiring category/merchant verification
    val source: String = "MANUAL", // "MANUAL", "SMS", "NOTIFICATION", "RECURRING"
    val smsSenderId: String? = null, // For trace tracking
    val subCategory: String? = null,
    val refNumber: String? = null,
    val linkedListId: Long? = null,
    val paidViaCcId: Long? = null,
    val ccRepaymentId: Long? = null,
    val refundedTxnId: Long? = null,
    val currency: String = "INR"
)

@Entity(
    tableName = "recurring_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class RecurringSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val merchant: String,
    val categoryId: Long?,
    val notes: String?,
    val paymentMethod: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val lastTriggered: Long,
    val nextTriggerTime: Long,
    val isActive: Boolean = true,
    val subCategory: String? = null,
    val currency: String = "INR"
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String,
    val type: String // "STRING", "INT", "BOOLEAN", "FLOAT", "LONG"
)

data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String
)

@Entity(tableName = "exchange_rates", primaryKeys = ["baseCurrency", "targetCurrency"])
data class ExchangeRate(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val timestamp: Long
)

@Entity(tableName = "planned_lists")
data class PlannedList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetCap: Double? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // "DRAFT" or "COMPLETED"
    val categoryId: Long? = null
)

@Entity(
    tableName = "planned_items",
    foreignKeys = [
        ForeignKey(
            entity = PlannedList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class PlannedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
    val isChecked: Boolean = false
)

data class PlannedListWithItems(
    @Embedded val plannedList: PlannedList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<PlannedItem>
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettings(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}

@Dao
interface TransactionDao {
    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionWithCategory>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM transactions WHERE isPending = 1 ORDER BY timestamp DESC")
    fun getPendingTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE isPending = 1")
    suspend fun getPendingTransactionsCount(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :since")
    suspend fun getTransactionsCountSince(since: Long): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND type = 'DEBIT' AND isPending = 0 AND timestamp >= :since")
    suspend fun getCategorySpentSince(categoryId: Long, since: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = :type AND LOWER(merchant) = LOWER(:merchant) AND timestamp >= :minTimestamp")
    suspend fun getMatchingTransactionCount(amount: Double, type: String, merchant: String, minTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE refNumber = :refNumber")
    suspend fun getTransactionCountByRefNumber(refNumber: String): Int

    @Query("SELECT * FROM transactions WHERE amount = :amount AND type = :type AND refNumber IS NULL AND timestamp >= :since LIMIT 1")
    suspend fun getMatchingPendingTransaction(amount: Double, type: String, since: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET ccRepaymentId = :repaymentId WHERE id IN (:txnIds)")
    suspend fun updateCcRepaymentIdForTransactions(repaymentId: Long, txnIds: List<Long>)

    @Query("SELECT * FROM transactions WHERE linkedListId = :listId LIMIT 1")
    suspend fun getTransactionByLinkedListId(listId: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE linkedListId = :listId ORDER BY timestamp DESC")
    suspend fun getTransactionsByLinkedListId(listId: Long): List<Transaction>
}

@Dao
interface RecurringScheduleDao {
    @Query("SELECT * FROM recurring_schedules ORDER BY id DESC")
    fun getAllSchedulesFlow(): Flow<List<RecurringSchedule>>

    @Query("SELECT * FROM recurring_schedules WHERE isActive = 1")
    suspend fun getActiveSchedules(): List<RecurringSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: RecurringSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: RecurringSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: RecurringSchedule)
}

@Dao
interface PlannedDao {
    @RoomTransactionAnnot
    @Query("SELECT * FROM planned_lists ORDER BY createdTimestamp DESC")
    fun getAllPlannedListsFlow(): Flow<List<PlannedListWithItems>>

    @RoomTransactionAnnot
    @Query("SELECT * FROM planned_lists WHERE id = :id")
    suspend fun getPlannedListWithItemsById(id: Long): PlannedListWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedList(plannedList: PlannedList): Long

    @Update
    suspend fun updatePlannedList(plannedList: PlannedList)

    @Delete
    suspend fun deletePlannedList(plannedList: PlannedList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedItem(item: PlannedItem): Long

    @Update
    suspend fun updatePlannedItem(item: PlannedItem)

    @Delete
    suspend fun deletePlannedItem(item: PlannedItem)

    @Query("SELECT price FROM planned_items WHERE LOWER(name) = LOWER(:name) AND price > 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLastPriceForItem(name: String): Double?
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun getAllCardsFlow(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    suspend fun getAllCards(): List<CreditCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCard): Long

    @Delete
    suspend fun deleteCard(card: CreditCard)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRatesFlow(): Flow<List<ExchangeRate>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAllRates(): List<ExchangeRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRate>)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllRates()
}

@Database(entities = [Category::class, Transaction::class, RecurringSchedule::class, AppSetting::class, PlannedList::class, PlannedItem::class, CreditCard::class, ExchangeRate::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringScheduleDao(): RecurringScheduleDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun plannedDao(): PlannedDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Salary', 'payments', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Refund', 'restore', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Interest', 'trending_up', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Other Inflow', 'savings', 0, NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`key`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Rename old table
                db.execSQL("ALTER TABLE `recurring_schedules` RENAME TO `recurring_schedules_old`")
                // 2. Create new table with foreign key
                db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `merchant` TEXT NOT NULL, `categoryId` INTEGER, `notes` TEXT, `paymentMethod` TEXT NOT NULL, `frequency` TEXT NOT NULL, `lastTriggered` INTEGER NOT NULL, `nextTriggerTime` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                // 3. Copy data
                db.execSQL("INSERT INTO `recurring_schedules` (`id`, `amount`, `type`, `merchant`, `categoryId`, `notes`, `paymentMethod`, `frequency`, `lastTriggered`, `nextTriggerTime`, `isActive`) SELECT `id`, `amount`, `type`, `merchant`, `categoryId`, `notes`, `paymentMethod`, `frequency`, `lastTriggered`, `nextTriggerTime`, `isActive` FROM `recurring_schedules_old`")
                // 4. Drop old table
                db.execSQL("DROP TABLE `recurring_schedules_old`")
                // 5. Create index
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_schedules_categoryId` ON `recurring_schedules` (`categoryId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add subCategory column to transactions and recurring_schedules
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `subCategory` TEXT")
                db.execSQL("ALTER TABLE `recurring_schedules` ADD COLUMN `subCategory` TEXT")

                // 2. Insert new default categories
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('SIP/Invest', 'trending_up', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('CreditCard Payment', 'credit_card', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Courses', 'school', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Home Maintenance', 'build', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Rent', 'real_estate_agent', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Subscriptions', 'subscriptions', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Domestic Help', 'groups', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Insurance', 'shield', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Taxes', 'description', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Pets', 'pets', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Gifts & Charity', 'card_giftcard', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Cashback & Rewards', 'local_offer', 0, NULL)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES ('Freelance/Side Hustle', 'laptop', 0, NULL)")

                // 3. Create grocery_lists and grocery_items tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `grocery_lists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `budgetCap` REAL, `createdTimestamp` INTEGER NOT NULL, `status` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `grocery_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listId` INTEGER NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `price` REAL NOT NULL, `isChecked` INTEGER NOT NULL, FOREIGN KEY(`listId`) REFERENCES `grocery_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_items_listId` ON `grocery_items` (`listId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `refNumber` TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create credit_cards table
                db.execSQL("CREATE TABLE IF NOT EXISTS `credit_cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cardName` TEXT NOT NULL)")
                
                // 2. Add columns to transactions table
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `linkedListId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `paidViaCcId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `ccRepaymentId` INTEGER")
                
                // 3. Rename grocery_lists to planned_lists
                db.execSQL("ALTER TABLE `grocery_lists` RENAME TO `planned_lists`")
                
                // 4. Recreate grocery_items as planned_items with the correct foreign key referencing planned_lists
                db.execSQL("CREATE TABLE IF NOT EXISTS `planned_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listId` INTEGER NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `price` REAL NOT NULL, `isChecked` INTEGER NOT NULL, FOREIGN KEY(`listId`) REFERENCES `planned_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `planned_items` (`id`, `listId`, `name`, `quantity`, `price`, `isChecked`) SELECT `id`, `listId`, `name`, `quantity`, `price`, `isChecked` FROM `grocery_items`")
                db.execSQL("DROP TABLE `grocery_items`")
                
                // 5. Create index with correct Room schema name
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_items_listId` ON `planned_items` (`listId`)")
                
                // 6. Add categoryId column to planned_lists
                db.execSQL("ALTER TABLE `planned_lists` ADD COLUMN `categoryId` INTEGER")

                // 7. Merge duplicate Rent categories to Rent & Maintenance
                db.execSQL("UPDATE transactions SET categoryId = (SELECT id FROM categories WHERE name = 'Rent & Maintenance' LIMIT 1) WHERE categoryId = (SELECT id FROM categories WHERE name = 'Rent' LIMIT 1)")
                db.execSQL("DELETE FROM categories WHERE name = 'Rent'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate planned_items to point to planned_lists (correcting the foreign key constraint from MIGRATION_7_8)
                db.execSQL("CREATE TABLE IF NOT EXISTS `planned_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listId` INTEGER NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `price` REAL NOT NULL, `isChecked` INTEGER NOT NULL, FOREIGN KEY(`listId`) REFERENCES `planned_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `planned_items_new` (`id`, `listId`, `name`, `quantity`, `price`, `isChecked`) SELECT `id`, `listId`, `name`, `quantity`, `price`, `isChecked` FROM `planned_items`")
                db.execSQL("DROP TABLE `planned_items`")
                db.execSQL("ALTER TABLE `planned_items_new` RENAME TO `planned_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_items_listId` ON `planned_items` (`listId`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `refundedTxnId` INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'INR'")
                db.execSQL("ALTER TABLE `recurring_schedules` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'INR'")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exchange_rates` (`baseCurrency` TEXT NOT NULL, `targetCurrency` TEXT NOT NULL, `rate` REAL NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`baseCurrency`, `targetCurrency`))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kharcha_dekh_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration(false)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories
                        val defaults = listOf(
                            "('Food & Dining', 'restaurant', 0, NULL)",
                            "('Groceries', 'shopping_cart', 0, NULL)",
                            "('Rent & Maintenance', 'home', 0, NULL)",
                            "('Fuel & Travel', 'directions_car', 0, NULL)",
                            "('Shopping', 'shopping_bag', 0, NULL)",
                            "('Bills & Utilities', 'receipt_long', 0, NULL)",
                            "('Entertainment', 'movie', 0, NULL)",
                            "('Health & Medical', 'medical_services', 0, NULL)",
                            "('EMI & Loans', 'account_balance', 0, NULL)",
                            "('Others', 'category', 0, NULL)",
                            "('Salary', 'payments', 0, NULL)",
                            "('Refund', 'restore', 0, NULL)",
                            "('Interest', 'trending_up', 0, NULL)",
                            "('Other Inflow', 'savings', 0, NULL)",
                            "('SIP/Invest', 'trending_up', 0, NULL)",
                            "('CreditCard Payment', 'credit_card', 0, NULL)",
                            "('Courses', 'school', 0, NULL)",
                            "('Home Maintenance', 'build', 0, NULL)",
                            "('Subscriptions', 'subscriptions', 0, NULL)",
                            "('Domestic Help', 'groups', 0, NULL)",
                            "('Insurance', 'shield', 0, NULL)",
                            "('Taxes', 'description', 0, NULL)",
                            "('Pets', 'pets', 0, NULL)",
                            "('Gifts & Charity', 'card_giftcard', 0, NULL)",
                            "('Cashback & Rewards', 'local_offer', 0, NULL)",
                            "('Freelance/Side Hustle', 'laptop', 0, NULL)"
                        )
                        defaults.forEach { values ->
                            db.execSQL("INSERT OR IGNORE INTO categories (name, iconResName, isCustom, budgetLimit) VALUES $values")
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndResetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}