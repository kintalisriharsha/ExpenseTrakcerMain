package com.example.expensetracker.frontend.services.important

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.frontend.services.TodoService.TodoDao
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsDao
import com.example.expensetracker.frontend.services.settingService.SettingDao
import com.example.expensetracker.frontend.services.settingService.SettingsEntity
import com.example.expensetracker.frontend.services.expenseService.ExpenseDao
import com.example.expensetracker.frontend.services.homeService.HomeDao
import com.example.expensetracker.services.entity.ExpenseEntity

@Database(
    entities = [
        TodoEntity::class,
        SettingsEntity::class,
        ExpenseEntity::class
    ],
    version = 3,

    exportSchema = false
)
@TypeConverters(Converters::class)   // NEW annotation
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun todoDao(): TodoDao  // NEW
    abstract fun settingDao(): SettingDao
    abstract fun homeDao() : HomeDao
    abstract fun analyticsDao() : AnalyticsDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                )
                    .fallbackToDestructiveMigration()  // unchanged — still dev mode, wipes on version bump
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}