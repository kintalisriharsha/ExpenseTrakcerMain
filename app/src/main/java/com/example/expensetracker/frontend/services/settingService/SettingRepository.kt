package com.example.expensetracker.frontend.services.settingService

import kotlinx.coroutines.flow.Flow

class SettingRepository(
    private val settingDao: SettingDao
) {

    suspend fun insertSettings(settingsEntity: SettingsEntity){
        settingDao.initializeSetting(settingsEntity)
    }

    fun getSettings(year: Int, month: Int): Flow<SettingsEntity?> {
        return settingDao.getSetting(year, month)
    }

    suspend fun updateSettings(settingsEntity: SettingsEntity) {
        settingDao.updateSetting(settingsEntity)
    }

}