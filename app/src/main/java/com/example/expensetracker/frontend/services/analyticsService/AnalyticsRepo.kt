package com.example.expensetracker.frontend.services.analyticsService


import com.example.expensetracker.data.local.entity.analytics.AnalyticsSummary
import com.example.expensetracker.data.local.entity.analytics.CategoryBreakdown
import com.example.expensetracker.data.local.entity.analytics.Heatmap
import com.example.expensetracker.data.local.entity.analytics.HeatmapDay
import com.example.expensetracker.data.local.entity.analytics.MonthTrendItem
import com.example.expensetracker.data.local.entity.analytics.MonthlyTrend
import com.example.expensetracker.data.local.entity.analytics.TotalSpentSummary
import com.example.expensetracker.frontend.services.settingService.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class AnalyticsRepository(
    private val analyticsDao: AnalyticsDao,
    private val settingRepository: SettingRepository,
) {

    fun getSummary(
        month: Int? = null,
        year: Int? = null,
        trendMonths: Int = 6,
    ): Flow<AnalyticsSummary> {
        val cal = Calendar.getInstance()
        val useMonth = month ?: (cal.get(Calendar.MONTH) + 1)
        val useYear = year ?: cal.get(Calendar.YEAR)
        val (fromKey, toKey) = trendRange(useMonth, useYear, trendMonths)

        val totalFlow = analyticsDao.getTotalSpent(useMonth, useYear)
        val categoryFlow = analyticsDao.getCategoryBreakdown(useMonth, useYear)
        val trendFlow = analyticsDao.getMonthlyTrend(fromKey, toKey)
        val heatmapFlow = analyticsDao.getHeatmap(useMonth, useYear)
        // Pulled straight from Settings for the month being viewed, instead of relying on a
        // caller to remember to pass a budget in. This is also what used to silently default
        // to 0.0 and make the budget disappear on the Analytics screen.
        val budgetFlow = settingRepository.getSettings(useYear, useMonth)
            .map { it?.monthlyBudget ?: 0.0 }

        return combine(
            totalFlow, categoryFlow, trendFlow, heatmapFlow, budgetFlow
        ) { total, categories, trendRows, heatmapRows, budget ->
            AnalyticsSummary(
                totalSpent = buildTotalSummary(total, budget),
                categoryBreakdown = CategoryBreakdown(total = total, categories = categories),
                monthlyTrend = buildTrend(trendRows, budget, useMonth, useYear, trendMonths),
                heatmap = buildHeatmap(useYear, useMonth, heatmapRows),
            )
        }
    }

    private fun buildTotalSummary(total: Double, budget: Double): TotalSpentSummary {
        val usedPct = if (budget > 0) (total / budget) * 100.0 else 0.0
        val remaining = budget - total
        val savingsRate = if (budget > 0) ((budget - total) / budget) * 100.0 else 0.0
        return TotalSpentSummary(total, budget, usedPct, remaining, savingsRate)
    }

    /**
     * Builds one MonthTrendItem per month in the [monthsBack]-month window ending at
     * [month]/[year] — including months with zero expenses. `getMonthlyTrend`'s query only
     * returns rows for months that actually have expenses, which used to make a brand-new
     * account's trend collapse down to a single data point; backfilling with 0 keeps the
     * chart spanning the full window every time.
     */
    private fun buildTrend(rows: List<MonthAmountRow>, budget: Double, month: Int, year: Int, monthsBack: Int): MonthlyTrend {
        val byKey = rows.associateBy { it.year * 100 + it.month }

        val cursor = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -(monthsBack - 1))
        }

        val items = (0 until monthsBack).map {
            val y = cursor.get(Calendar.YEAR)
            val m = cursor.get(Calendar.MONTH) + 1
            val amount = byKey[y * 100 + m]?.amount ?: 0.0
            cursor.add(Calendar.MONTH, 1)
            MonthTrendItem(month = monthName(m), year = y, amount = amount)
        }

        val trendPct = if (items.size >= 2) {
            val prev = items[items.size - 2].amount
            val curr = items.last().amount
            if (prev > 0) ((curr - prev) / prev) * 100.0 else 0.0
        } else 0.0

        return MonthlyTrend(trendPct = trendPct, months = items, budget = budget)
    }

    private fun buildHeatmap(year: Int, month: Int, rows: List<DayAmountRow>): Heatmap {
        val avg = if (rows.isNotEmpty()) rows.sumOf { it.amount } / rows.size else 0.0
        return Heatmap(
            year = year,
            month = month,
            avgSpend = avg,
            daily = rows.map { HeatmapDay(it.day, it.amount) },
        )
    }

    /**
     * (year*100 + month) [from, to] window covering [monthsBack] months, ending at [month]/[year].
     * e.g. April 2026 -> 202604. Matches AnalyticsDao.getMonthlyTrend's HAVING clause, since the
     * `expenses.date` column is stored as "dd MMM yyyy" and can't be range-compared as a string.
     */
    private fun trendRange(month: Int, year: Int, monthsBack: Int): Pair<Int, Int> {
        val toKey = year * 100 + month

        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -(monthsBack - 1))
        }
        val fromKey = start.get(Calendar.YEAR) * 100 + (start.get(Calendar.MONTH) + 1)

        return fromKey to toKey
    }

    private fun monthName(month: Int): String = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ).getOrElse(month - 1) { "?" }
}