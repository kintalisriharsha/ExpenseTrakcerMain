package com.example.expensetracker.frontend.services.homeService

import androidx.room.Dao
import androidx.room.Query
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.settingService.SettingsEntity
import com.example.expensetracker.services.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface HomeDao {

    // ── Settings (monthly/weekly budget + daily limit for the current month) ──
    @Query("SELECT * FROM settings WHERE year = :year AND month = :month LIMIT 1")
    fun observeSettings(year: Int, month: Int): Flow<SettingsEntity?>

    // ── Expenses ──
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM expenses
        WHERE tab = 1 AND date BETWEEN :weekStart AND :weekEnd
        """
    )
    fun observeWeeklySpent(weekStart: String, weekEnd: String): Flow<Double>

    // "date" is stored as a formatted string ("dd MMM yyyy"), so a SQL BETWEEN on it
    // is a lexicographic string comparison, not a chronological one — it silently
    // gives wrong sums whenever a range crosses a month boundary (e.g. a week that
    // spans "30 Sep 2026".."04 Oct 2026"). This query hands back every expense row
    // instead, so WidgetRepository can filter using real parsed LocalDate ranges.
    @Query("SELECT * FROM expenses WHERE tab = 1")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE tab = 1 AND date = :today")
    fun observeSpentToday(today: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM expenses WHERE date = :today")
    fun observeTotalToday(today: String): Flow<Int>

    @Query("SELECT * FROM expenses WHERE date = :today ORDER BY time DESC")
    fun observeRecentExpenses(today: String): Flow<List<ExpenseEntity>>

    // ── Todos — shown on Home in place of the old savings-goal / planner section ──
    @Query("SELECT * FROM todo WHERE checkBox = 0 ORDER BY checkBox ASC, id DESC LIMIT :limit")
    fun observeTodos(limit: Int): Flow<List<TodoEntity>>
}