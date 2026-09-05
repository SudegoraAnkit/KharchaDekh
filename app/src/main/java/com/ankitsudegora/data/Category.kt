package com.ankitsudegora.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
