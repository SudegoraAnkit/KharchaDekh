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
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String, // "UPI", "CASH", "CARD", "NETBANKING"
    val isPending: Boolean,
    val source: String, // "SMS" or "MANUAL"
    val smsSenderId: String? = null
)

// Data class to easily load Transaction alongside its Category
data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
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
    val type: String,
    val merchant: String,
    val categoryId: Long? = null,
    val notes: String? = null,
    val paymentMethod: String,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val lastTriggered: Long,
    val nextTriggerTime: Long,
    val isActive: Boolean = true
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface TransactionDao {
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

@Database(entities = [Category::class, Transaction::class, RecurringSchedule::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringScheduleDao(): RecurringScheduleDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kharcha_dekh_db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
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
    }
}
