package com.ankitsudegora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Category::class,
        Transaction::class,
        RecurringSchedule::class,
        AppSetting::class,
        PlannedList::class,
        PlannedItem::class,
        CreditCard::class,
        ExchangeRate::class
    ],
    version = 11,
    exportSchema = false
)
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