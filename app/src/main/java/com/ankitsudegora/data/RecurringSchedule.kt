package com.ankitsudegora.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
