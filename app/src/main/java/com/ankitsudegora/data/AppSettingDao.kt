package com.ankitsudegora.data

import androidx.room.*

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettings(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}
