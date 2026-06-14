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
    val smsSenderId: String? = null // For trace tracking
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
    val isActive: Boolean = true
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
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

@Database(entities = [Category::class, Transaction::class, RecurringSchedule::class, AppSetting::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringScheduleDao(): RecurringScheduleDao
    abstract fun appSettingDao(): AppSettingDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kharcha_dekh_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
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
                            "('Other Inflow', 'savings', 0, NULL)"
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
