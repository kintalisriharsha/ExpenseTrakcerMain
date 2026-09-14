package com.example.expensetracker.frontend.services.analyticsService


import com.example.expensetracker.data.local.entity.analytics.AnalyticsSummary
import com.example.expensetracker.data.local.entity.analytics.CategoryBreakdown
import com.example.expensetracker.data.local.entity.analytics.Heatmap
import com.example.expensetracker.data.local.entity.analytics.HeatmapDay
import com.example.expensetracker.data.local.entity.analytics.MonthTrendItem
import com.example.expensetracker.data.local.entity.analytics.MonthlyTrend
import com.example.expensetracker.data.local.entity.analytics.TotalSpentSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class AnalyticsRepository(
    private val analyticsDao: AnalyticsDao,
) {

    fun getSummary(
        month: Int? = null,
        year: Int? = null,
        trendMonths: Int = 6,
        monthlyBudget: Double = 0.0,
    ): Flow<AnalyticsSummary> {
        val cal = Calendar.getInstance()
        val useMonth = month ?: (cal.get(Calendar.MONTH) + 1)
        val useYear = year ?: cal.get(Calendar.YEAR)
        val (fromKey, toKey) = trendRange(useMonth, useYear, trendMonths)

        val totalFlow = analyticsDao.getTotalSpent(useMonth, useYear)
        val categoryFlow = analyticsDao.getCategoryBreakdown(useMonth, useYear)
        val trendFlow = analyticsDao.getMonthlyTrend(fromKey, toKey)
        val heatmapFlow = analyticsDao.getHeatmap(useMonth, useYear)

        return combine(totalFlow, categoryFlow, trendFlow, heatmapFlow) { total, categories, trendRows, heatmapRows ->
            AnalyticsSummary(
                totalSpent = buildTotalSummary(total, monthlyBudget),
                categoryBreakdown = CategoryBreakdown(total = total, categories = categories),
                monthlyTrend = buildTrend(trendRows),
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

    private fun buildTrend(rows: List<MonthAmountRow>): MonthlyTrend {
        val items = rows.map { row ->
            MonthTrendItem(
                month = monthName(row.month),
                year = row.year,
                amount = row.amount,
            )
        }
        val trendPct = if (items.size >= 2) {
            val prev = items[items.size - 2].amount
            val curr = items.last().amount
            if (prev > 0) ((curr - prev) / prev) * 100.0 else 0.0
        } else 0.0
        return MonthlyTrend(trendPct, items)
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