package com.example.expensetracker.data.local.entity.analytics

import com.example.expensetracker.frontend.services.analyticsService.CategoryAmountRow

data class TotalSpentSummary(
    val totalSpent: Double,
    val monthlyBudget: Double,
    val usedPct: Double,
    val remaining: Double,
    val savingsRate: Double,
)

data class CategoryBreakdown(
    val total: Double,
    val categories: List<CategoryAmountRow>,
)

data class MonthTrendItem(
    val month: String, // "Jan", "Feb", …
    val year: Int,
    val amount: Double,
)

data class MonthlyTrend(
    val trendPct: Double,
    val months: List<MonthTrendItem>,
)

data class HeatmapDay(
    val day: Int,
    val amount: Double,
)

data class Heatmap(
    val year: Int,
    val month: Int,
    val avgSpend: Double,
    val daily: List<HeatmapDay>,
)

data class AnalyticsSummary(
    val totalSpent: TotalSpentSummary,
    val categoryBreakdown: CategoryBreakdown,
    val monthlyTrend: MonthlyTrend,
    val heatmap: Heatmap,
)