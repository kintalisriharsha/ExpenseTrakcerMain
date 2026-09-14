package com.example.expensetracker.frontend.services.homeService

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.services.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class HomeDashboard(
    val monthlyBudget   : Double,
    val weeklyBudget    : Double,
    val dailyLimit      : Double,
    val weeklySpent     : Double,
    val weeklyRemaining : Double,
    val weeklyExceeded  : Boolean,
    val spentToday      : Double,
    val totalToday      : Int,
    val recentExpenses  : List<ExpenseEntity>,
    val todos           : List<TodoEntity>,
)

class HomeRepository(
    private val homeDao: HomeDao
) {
    // Must match the exact pattern used everywhere expenses are stored (SmsParser, AddExpense screen, etc.)
    @RequiresApi(Build.VERSION_CODES.O)
    private val storedDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    @RequiresApi(Build.VERSION_CODES.O)
    fun observeDashboard(recentExpenseLimit: Int = 5, todoLimit: Int = 5): Flow<HomeDashboard> {
        val today      = LocalDate.now()
        val year       = today.year
        val monthName  = today.monthValue

        val weekFields = WeekFields.of(Locale.getDefault())
        val weekStart  = today.with(weekFields.dayOfWeek(), 1)
        val weekEnd    = weekStart.plusDays(6)

        val todayFormatted     = today.format(storedDateFormatter)
        val weekStartFormatted = weekStart.format(storedDateFormatter)
        val weekEndFormatted   = weekEnd.format(storedDateFormatter)

        return combine(
            homeDao.observeSettings(year, monthName),
            homeDao.observeWeeklySpent(weekStartFormatted, weekEndFormatted),
            homeDao.observeSpentToday(todayFormatted),
            homeDao.observeTotalToday(todayFormatted),
            homeDao.observeRecentExpenses(todayFormatted),
            homeDao.observeTodos(todoLimit)
        ) { values ->
            val settings    = values[0] as com.example.expensetracker.frontend.services.settingService.SettingsEntity?
            val weeklySpent = values[1] as Double
            val spentToday  = values[2] as Double
            val totalToday  = values[3] as Int
            @Suppress("UNCHECKED_CAST")
            val recent      = values[4] as List<ExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val todos       = values[5] as List<TodoEntity>

            val weeklyBudget = settings?.weeklyBudget ?: 0.0
            val remaining    = weeklyBudget - weeklySpent

            HomeDashboard(
                monthlyBudget   = settings?.monthlyBudget ?: 0.0,
                weeklyBudget    = weeklyBudget,
                dailyLimit      = settings?.dailyLimit ?: 0.0,
                weeklySpent     = weeklySpent,
                weeklyRemaining = remaining,
                weeklyExceeded  = weeklyBudget > 0.0 && remaining < 0.0,
                spentToday      = spentToday,
                totalToday      = totalToday,
                recentExpenses  = recent,
                todos           = todos,
            )
        }
    }
}