package com.example.expensetracker.frontend.important

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Appstate {
    var isPickerOpen by mutableStateOf(false)
    var notificationEnabled by mutableStateOf(false)
    var isDark by mutableStateOf(false)
    var contactsGranted by mutableStateOf(false) // ← add this

    var smsGranted by mutableStateOf(false)

    var phoneGranted by mutableStateOf(false)
}