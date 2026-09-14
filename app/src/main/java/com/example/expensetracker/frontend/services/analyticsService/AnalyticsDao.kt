package com.example.expensetracker.frontend.services.analyticsService


import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * All dates in the `expenses` table are stored as "dd MMM yyyy" (e.g. "13 Sep 2026"),
 * NOT ISO-8601 — so SQLite's built-in strftime()/date() functions can't parse them
 * (they only understand "YYYY-MM-DD"-style strings). Every query below therefore
 * parses the format manually:
 *   - day   -> substr(date, 1, 2)   e.g. "13"
 *   - month -> substr(date, 4, 3)   e.g. "Sep"  -> mapped to 1-12 via CASE
 *   - year  -> substr(date, 8, 4)   e.g. "2026"
 *
 * This relies on the day always being zero-padded to 2 digits and the month
 * abbreviation always being the 3-letter English form (Jan, Feb, Mar, ...) —
 * i.e. the date must always be written with DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).
 */
private const val MONTH_NUM_EXPR = """
    (CASE substr(date, 4, 3)
        WHEN 'Jan' THEN 1  WHEN 'Feb' THEN 2  WHEN 'Mar' THEN 3  WHEN 'Apr' THEN 4
        WHEN 'May' THEN 5  WHEN 'Jun' THEN 6  WHEN 'Jul' THEN 7  WHEN 'Aug' THEN 8
        WHEN 'Sep' THEN 9  WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
    END)
"""

@Dao
interface AnalyticsDao {

    // ── 1. Total spent in a given month ────────────────────────────────────
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM expenses
        WHERE tab = 1
          AND CAST(substr(date, 8, 4) AS INTEGER) = :year
          AND (CASE substr(date, 4, 3)
                WHEN 'Jan' THEN 1  WHEN 'Feb' THEN 2  WHEN 'Mar' THEN 3  WHEN 'Apr' THEN 4
                WHEN 'May' THEN 5  WHEN 'Jun' THEN 6  WHEN 'Jul' THEN 7  WHEN 'Aug' THEN 8
                WHEN 'Sep' THEN 9  WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
              END) = :month
    """)
    fun getTotalSpent(month: Int, year: Int): Flow<Double>

    // ── 2. Category breakdown for a given month ────────────────────────────
    @Query("""
        SELECT category AS category, COALESCE(SUM(amount), 0.0) AS amount
        FROM expenses
        WHERE tab = 1
          AND CAST(substr(date, 8, 4) AS INTEGER) = :year
          AND (CASE substr(date, 4, 3)
                WHEN 'Jan' THEN 1  WHEN 'Feb' THEN 2  WHEN 'Mar' THEN 3  WHEN 'Apr' THEN 4
                WHEN 'May' THEN 5  WHEN 'Jun' THEN 6  WHEN 'Jul' THEN 7  WHEN 'Aug' THEN 8
                WHEN 'Sep' THEN 9  WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
              END) = :month
        GROUP BY category
        ORDER BY amount DESC
    """)
    fun getCategoryBreakdown(month: Int, year: Int): Flow<List<CategoryAmountRow>>

    // ── 3. Monthly trend — totals grouped by year/month within a range ─────
    // fromKey/toKey are (year * 100 + month) integers, e.g. April 2026 = 202604.
    // Plain string BETWEEN on "dd MMM yyyy" wouldn't sort chronologically, so we
    // parse year/month per row and filter on the computed numeric key instead.
    @Query("""
        SELECT
            CAST(substr(date, 8, 4) AS INTEGER) AS year,
            (CASE substr(date, 4, 3)
                WHEN 'Jan' THEN 1  WHEN 'Feb' THEN 2  WHEN 'Mar' THEN 3  WHEN 'Apr' THEN 4
                WHEN 'May' THEN 5  WHEN 'Jun' THEN 6  WHEN 'Jul' THEN 7  WHEN 'Aug' THEN 8
                WHEN 'Sep' THEN 9  WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
            END) AS month,
            COALESCE(SUM(amount), 0.0) AS amount
        FROM expenses
        WHERE tab = 1
        GROUP BY year, month
        HAVING (year * 100 + month) BETWEEN :fromKey AND :toKey
        ORDER BY year, month
    """)
    fun getMonthlyTrend(fromKey: Int, toKey: Int): Flow<List<MonthAmountRow>>

    // ── 4. Heatmap — daily totals within a given month ──────────────────────
    @Query("""
        SELECT CAST(substr(date, 1, 2) AS INTEGER) AS day,
               COALESCE(SUM(amount), 0.0) AS amount
        FROM expenses
        WHERE tab = 1
          AND CAST(substr(date, 8, 4) AS INTEGER) = :year
          AND (CASE substr(date, 4, 3)
                WHEN 'Jan' THEN 1  WHEN 'Feb' THEN 2  WHEN 'Mar' THEN 3  WHEN 'Apr' THEN 4
                WHEN 'May' THEN 5  WHEN 'Jun' THEN 6  WHEN 'Jul' THEN 7  WHEN 'Aug' THEN 8
                WHEN 'Sep' THEN 9  WHEN 'Oct' THEN 10 WHEN 'Nov' THEN 11 WHEN 'Dec' THEN 12
              END) = :month
        GROUP BY day
        ORDER BY day
    """)
    fun getHeatmap(month: Int, year: Int): Flow<List<DayAmountRow>>
}

data class CategoryAmountRow(
    val category: String,
    val amount: Double,
)

data class MonthAmountRow(
    val year: Int,
    val month: Int,
    val amount: Double,
)

data class DayAmountRow(
    val day: Int,
    val amount: Double,
)