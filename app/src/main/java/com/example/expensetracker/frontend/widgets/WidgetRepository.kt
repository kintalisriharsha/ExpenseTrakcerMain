package com.example.expensetracker.frontend.widgets

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.important.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class ExpenseWidgetSnapshot(
    val spentToday        : Double,
    val dailyLimit        : Double,
    val weeklySpent        : Double,
    val weeklyBudget       : Double,
    val monthlySpent       : Double,
    val monthlyBudget      : Double,
    val latestExpenseLabel : String,
    val latestExpenseAmount: Double,
)

/**
 * One-shot data fetcher for home-screen widgets.
 *
 * Unlike HomeViewModel, this never stays subscribed to a Flow — widgets ask for
 * a snapshot each time Android calls provideGlance() (on placement, on the periodic
 * update tick, and whenever you manually trigger updateAll()/update() after the
 * user changes something in-app). Reuses HomeDao as-is, no schema changes needed.
 */
class WidgetRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).homeDao()

    suspend fun getTodoSnapshot(limit: Int): List<TodoEntity> =
        dao.observeTodos(limit).first()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getExpenseSnapshot(): ExpenseWidgetSnapshot {
        val today     = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        val weekFields = WeekFields.of(Locale.getDefault())
        val weekStart  = today.with(weekFields.dayOfWeek(), 1)
        val weekEnd    = weekStart.plusDays(6)

        val monthStart = today.withDayOfMonth(1)
        val monthEnd   = today.withDayOfMonth(today.lengthOfMonth())

        val settings = dao.observeSettings(today.year, today.monthValue).first()

        val weeklySpent = dao.observeWeeklySpent(
            weekStart.format(formatter),
            weekEnd.format(formatter)
        ).first()

        // Reusing observeWeeklySpent here since it's just a date-range sum under
        // the hood — swap for a dedicated observeMonthlySpent(...) if you have one.
        val monthlySpent = dao.observeWeeklySpent(
            monthStart.format(formatter),
            monthEnd.format(formatter)
        ).first()

        val spentToday = dao.observeSpentToday(today.format(formatter)).first()

        // TODO: confirm this against your actual DAO — assumed a method that
        // returns the single most recent expense row (or null if none exist),
        // with `category`/`title` and `amount` fields.

        val storedDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        val todayFormatted     = today.format(storedDateFormatter)
        val latest = dao.observeRecentExpenses(todayFormatted).first().firstOrNull()

        return ExpenseWidgetSnapshot(
            spentToday          = spentToday,
            dailyLimit          = settings?.dailyLimit ?: 0.0,
            weeklySpent         = weeklySpent,
            weeklyBudget        = settings?.weeklyBudget ?: 0.0,
            monthlySpent        = monthlySpent,
            monthlyBudget       = settings?.monthlyBudget ?: 0.0,
            latestExpenseLabel  = latest?.category ?: "No expenses yet",
            latestExpenseAmount = latest?.amount ?: 0.0,
        )
    }
}