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

    @Query("SELECT * FROM settings ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getLatestSetting(): SettingsEntity?

    // Live version of the Auto Expense / notification switch. Reads the SAME row as
    // getLatestSetting() (which SmsReceiver uses), so UI and SMS handling can't disagree.
    // Emits null when there is no settings row yet.
    @Query("SELECT notification_enabled FROM settings ORDER BY year DESC, month DESC LIMIT 1")
    fun observeNotificationEnabled(): Flow<Boolean?>
}