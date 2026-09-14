package com.example.expensetracker.frontend.services.settingService

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(tableName = "settings", primaryKeys = ["year", "month"])
data class SettingsEntity(
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "month") val month: Int,
    @ColumnInfo(name = "monthly_budget") val monthlyBudget: Double,
    @ColumnInfo(name = "weekly_budget") val weeklyBudget: Double,
    @ColumnInfo(name = "daily_limit") val dailyLimit: Double,
    @ColumnInfo(name = "notification_enabled") val notificationEnabled: Boolean,
    @ColumnInfo(name = "is_dark_mode") val isDarkMode: Boolean,
)