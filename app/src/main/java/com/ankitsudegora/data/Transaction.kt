package com.ankitsudegora.data

import androidx.room.*

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

data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)
