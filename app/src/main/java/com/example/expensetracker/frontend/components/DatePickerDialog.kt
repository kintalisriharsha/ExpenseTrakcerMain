package com.example.expensetracker.frontend.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.expensetracker.ui.theme.Primary
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

// How far into the future dates can be picked (was 1 month)
private const val MAX_MONTHS_AHEAD = 2

// Converts a local calendar date to UTC midnight (the format DatePicker uses)
private fun Calendar.toUtcMidnight(): Long {
    val y = get(Calendar.YEAR)
    val m = get(Calendar.MONTH)
    val d = get(Calendar.DAY_OF_MONTH)
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(y, m, d)
    }.timeInMillis
}

// ── Single Date Picker (now allows up to 2 months ahead) ──────────────────────
// Still used by the Edit screen, where one expense has exactly one date.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDateMillis: Long? = null   // must be UTC midnight if provided
) {
    val todayUtcMidnight = remember { Calendar.getInstance().toUtcMidnight() }

    val maxCalendar = remember {
        Calendar.getInstance().apply { add(Calendar.MONTH, MAX_MONTHS_AHEAD) }
    }
    val maxUtcMidnight = remember { maxCalendar.toUtcMidnight() }
    val maxYear = remember { maxCalendar.get(Calendar.YEAR) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: todayUtcMidnight,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= maxUtcMidnight

            override fun isSelectableYear(year: Int): Boolean =
                year <= maxYear
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val formatted = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .format(Date(millis))
                    onDateSelected(formatted)
                } ?: onDismiss()
            }) {
                Text("OK", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ── Helpers for multi-date selection ──────────────────────────────────────────

private val storedDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** Same "dd MMM yyyy" text your Room `date` column already uses (e.g. "13 Sep 2026"). */
@RequiresApi(Build.VERSION_CODES.O)
fun LocalDate.toStoredDate(): String = format(storedDateFormatter)

/**
 * Splits [total] equally into [parts] amounts that add back up to EXACTLY [total]
 * (to the paisa). If it doesn't divide evenly, the leftover paise go one each to the
 * first shares, e.g. 100.00 over 3 dates -> 33.34, 33.33, 33.33.
 */
fun splitAmountEqually(total: Double, parts: Int): List<Double> {
    if (parts <= 1) return listOf(total)
    val totalPaise = (total * 100).roundToLong()
    val base  = totalPaise / parts
    val extra = (totalPaise % parts).toInt()
    return List(parts) { i -> (base + if (i < extra) 1 else 0) / 100.0 }
}

/** Parses the stored "dd MMM yyyy" text back into a LocalDate (null if it can't be read). */
@RequiresApi(Build.VERSION_CODES.O)
fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this, storedDateFormatter) }
        .recoverCatching {
            LocalDate.parse(this, DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
        }
        .getOrNull()

/** Short human-readable summary shown in the Date row of the Add screen. */
@RequiresApi(Build.VERSION_CODES.O)
fun summarizeDates(dates: Collection<LocalDate>): String {
    if (dates.isEmpty()) return "Select dates"
    val sorted = dates.sorted()
    if (sorted.size == 1) return sorted.first().toStoredDate()

    val contiguous = sorted.zipWithNext().all { (a, b) -> a.plusDays(1) == b }
    return if (contiguous) {
        "${sorted.first().toStoredDate()} – ${sorted.last().toStoredDate()} (${sorted.size} days)"
    } else {
        val short = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
        val preview = sorted.take(3).joinToString(", ") { it.format(short) }
        val more = if (sorted.size > 3) "…" else ""
        "${sorted.size} dates: $preview$more"
    }
}

/**
 * Maps a touch position inside the day grid to the date under it
 * (null for blank cells before/after the month or outside the grid).
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun gridDayAt(
    pos: Offset,
    gridWidthPx: Int,
    cellHeightPx: Float,
    month: YearMonth,
    firstDayOffset: Int
): LocalDate? {
    if (gridWidthPx <= 0 || pos.x < 0f || pos.y < 0f) return null
    val col = (pos.x / (gridWidthPx / 7f)).toInt().coerceIn(0, 6)
    val row = (pos.y / cellHeightPx).toInt()
    val dayNumber = row * 7 + col - firstDayOffset + 1
    return if (dayNumber in 1..month.lengthOfMonth()) month.atDay(dayNumber) else null
}

// ── Multi-date picker (new) ───────────────────────────────────────────────────
// Two ways to pick, and you can mix them freely:
//   • Days  – tap a day to select it, tap again to unselect it
//   • Drag  – press a day and drag across others to select everything in between
//             (start the drag on an already-selected day to UNselect the swept days)
//   • Range – tap a start day, then an end day: everything between gets selected
// Selected dates accumulate, so you can add a range AND some single days.

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MultiDatePickerModal(
    initialDates: Set<LocalDate> = emptySet(),
    onDatesSelected: (Set<LocalDate>) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val maxDate = remember { today.plusMonths(MAX_MONTHS_AHEAD.toLong()) }
    val maxMonth = remember { YearMonth.from(maxDate) }

    var selected by remember { mutableStateOf(initialDates) }
    var visibleMonth by remember {
        mutableStateOf(YearMonth.from(initialDates.minOrNull() ?: today))
    }
    var rangeMode by remember { mutableStateOf(false) }
    var rangeAnchor by remember { mutableStateOf<LocalDate?>(null) }

    val surface     = if (isDark) Color(0xFF1E2536) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted   = if (isDark) Color(0xFF6B7589) else Color(0xFF9AA5B4)
    val chipBg      = if (isDark) Color(0xFF2A3347) else Color(0xFFF1F5F9)

    // Drag-selection state: what was selected when the drag began, where it began,
    // and whether this drag is adding (true) or removing (false) days.
    var dragStart by remember { mutableStateOf<LocalDate?>(null) }
    var dragBase by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var dragAdding by remember { mutableStateOf(true) }

    fun applyDrag(current: LocalDate) {
        val start = dragStart ?: return
        val from = if (current.isBefore(start)) current else start
        val to   = if (current.isBefore(start)) start else current
        val swept = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) && !it.isAfter(maxDate) }
            .toSet()
        // Always recomputed from the snapshot, so dragging back shrinks the sweep.
        selected = if (dragAdding) dragBase + swept else dragBase - swept
    }

    fun onDayClick(day: LocalDate) {
        if (day.isAfter(maxDate)) return
        if (!rangeMode) {
            selected = if (day in selected) selected - day else selected + day
        } else {
            val anchor = rangeAnchor
            if (anchor == null) {
                rangeAnchor = day
                selected = selected + day
            } else {
                val from = if (day.isBefore(anchor)) day else anchor
                val to   = if (day.isBefore(anchor)) anchor else day
                val days = generateSequence(from) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(to) && !it.isAfter(maxDate) }
                    .toList()
                selected = selected + days
                rangeAnchor = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    "Select dates",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        selected.isEmpty() -> "Tap days or drag across them"
                        selected.size == 1 -> "1 date selected"
                        else               -> "${selected.size} dates selected"
                    },
                    fontSize = 13.sp,
                    color = textMuted
                )

                Spacer(Modifier.height(14.dp))

                // Mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Pick days" to false, "Pick range" to true).forEach { (label, isRange) ->
                        val active = rangeMode == isRange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) Primary else Color.Transparent)
                                .clickable {
                                    rangeMode = isRange
                                    rangeAnchor = null
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else textPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous month", tint = textPrimary)
                    }
                    Text(
                        text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${visibleMonth.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    IconButton(
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        enabled = visibleMonth.isBefore(maxMonth)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, "Next month",
                            tint = if (visibleMonth.isBefore(maxMonth)) textPrimary else textMuted.copy(alpha = 0.4f)
                        )
                    }
                }

                // Weekday header
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Day grid (weeks start on Sunday)
                val firstOfMonth = visibleMonth.atDay(1)
                val offset = firstOfMonth.dayOfWeek.value % 7   // Sun=0 … Sat=6
                val daysInMonth = visibleMonth.lengthOfMonth()
                val rows = (offset + daysInMonth + 6) / 7

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Tap = toggle a day (or range-mode anchor/end)
                        .pointerInput(visibleMonth) {
                            detectTapGestures { pos ->
                                gridDayAt(pos, size.width, 40.dp.toPx(), visibleMonth, offset)
                                    ?.let { onDayClick(it) }
                            }
                        }
                        // Drag = sweep across days
                        .pointerInput(visibleMonth) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    val day = gridDayAt(pos, size.width, 40.dp.toPx(), visibleMonth, offset)
                                    if (day == null || day.isAfter(maxDate)) {
                                        dragStart = null
                                    } else {
                                        rangeAnchor = null
                                        dragStart = day
                                        dragBase = selected
                                        dragAdding = day !in selected
                                        applyDrag(day)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    gridDayAt(change.position, size.width, 40.dp.toPx(), visibleMonth, offset)
                                        ?.let { applyDrag(it) }
                                },
                                onDragEnd = { dragStart = null },
                                onDragCancel = { dragStart = null }
                            )
                        }
                ) {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val dayNumber = row * 7 + col - offset + 1
                                if (dayNumber !in 1..daysInMonth) {
                                    Spacer(Modifier.weight(1f).height(40.dp))
                                } else {
                                    val day = visibleMonth.atDay(dayNumber)
                                    val isSelected = day in selected
                                    val isToday = day == today
                                    val disabled = day.isAfter(maxDate)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Primary else Color.Transparent)
                                            .then(
                                                if (isToday && !isSelected)
                                                    Modifier.border(1.dp, Primary, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dayNumber.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                disabled   -> textMuted.copy(alpha = 0.4f)
                                                else       -> textPrimary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }   // end grid Column (tap + drag)

                Spacer(Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selected = emptySet(); rangeAnchor = null },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text("Clear", color = if (selected.isNotEmpty()) Color(0xFFE74C3C) else Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    TextButton(
                        onClick = { onDatesSelected(selected) },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text(
                            "OK",
                            color = if (selected.isNotEmpty()) Primary else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}