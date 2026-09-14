package com.example.expensetracker.frontend.services.settingService

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun initializeSetting(
        settingsEntity: SettingsEntity
    ): Long

    @Query("SELECT * FROM settings WHERE year = :year AND month = :month LIMIT 1")
    fun getSetting(year: Int, month: Int): Flow<SettingsEntity?>   // nullable return type

    @Update
    suspend fun updateSetting(settingsEntity: SettingsEntity)
}