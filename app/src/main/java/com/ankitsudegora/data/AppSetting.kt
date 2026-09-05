package com.ankitsudegora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String,
    val type: String // "STRING", "INT", "BOOLEAN", "FLOAT", "LONG"
)
