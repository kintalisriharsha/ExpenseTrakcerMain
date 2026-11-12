package com.example.expensetracker.frontend.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetracker.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

// ── Date Picker Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDateMillis: Long? = null   // must be UTC midnight if provided
) {
    val todayUtcMidnight = remember { Calendar.getInstance().toUtcMidnight() }

    // Latest selectable date = today + 1 month (local calendar)
    val maxCalendar = remember {
        Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
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