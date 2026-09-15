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
        // Dates are always written by SmsParser / manual entry using this exact
        // fixed-locale format ("dd MMM yyyy", Locale.ENGLISH) — matching that here
        // (rather than Locale.getDefault()) is what keeps parsing reliable.
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        // Bounds recomputed fresh from LocalDate.now() on every snapshot — this is
        // what makes each bucket clear itself automatically the moment its period
        // actually rolls over (new day → daily resets, new week → weekly resets,
        // new month → monthly resets), with no persisted counter to reset by hand.
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekStart  = today.with(weekFields.dayOfWeek(), 1)
        val weekEnd    = weekStart.plusDays(6)

        val monthStart = today.withDayOfMonth(1)
        val monthEnd   = today.withDayOfMonth(today.lengthOfMonth())

        val settings = dao.observeSettings(today.year, today.monthValue).first()

        // Sum weekly/monthly in-memory over *real* parsed LocalDates rather than
        // via SQL BETWEEN on the raw "dd MMM yyyy" strings. A string BETWEEN is a
        // lexicographic comparison, so it silently mis-sums (or drops to 0) any
        // time the week/month range crosses a month boundary — e.g. a week
        // spanning "30 Sep 2026".."04 Oct 2026" would compare "30 Sep 2026" as
        // *greater* than "04 Oct 2026" alphabetically and get excluded. Parsing
        // each row's date for a real chronological comparison fixes that, so the
        // weekly/monthly totals stay accurate (and non-zero) for the whole period.
        val allExpenses = dao.observeAllExpenses().first()

        fun parseRowDate(dateStr: String): LocalDate? =
            try { LocalDate.parse(dateStr, formatter) } catch (e: Exception) { null }

        val weeklySpent = allExpenses.sumOf { row ->
            val d = parseRowDate(row.date)
            if (d != null && !d.isBefore(weekStart) && !d.isAfter(weekEnd)) row.amount else 0.0
        }

        val monthlySpent = allExpenses.sumOf { row ->
            val d = parseRowDate(row.date)
            if (d != null && !d.isBefore(monthStart) && !d.isAfter(monthEnd)) row.amount else 0.0
        }

        val spentToday = dao.observeSpentToday(today.format(formatter)).first()

        // TODO: confirm this against your actual DAO — assumed a method that
        // returns the single most recent expense row (or null if none exist),
        // with `category`/`title` and `amount` fields.

        val todayFormatted = today.format(formatter)
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