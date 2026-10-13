package com.example.expensetracker.frontend.services.important

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,   // was 3: added expenses.paymentMode

    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun todoDao(): TodoDao
    abstract fun settingDao(): SettingDao
    abstract fun homeDao() : HomeDao
    abstract fun analyticsDao() : AnalyticsDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v3 -> v4: add payment mode ("Bank" / "Credit Card") to expenses.
        // Existing rows become "Bank", so no data is lost.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE expenses ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'Bank'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()  // still dev mode: only used if no migration path exists
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}