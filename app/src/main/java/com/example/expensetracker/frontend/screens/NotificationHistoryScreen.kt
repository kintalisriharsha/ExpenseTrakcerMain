package com.example.expensetracker.frontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.services.entity.ExpenseEntity
import java.util.Locale

/**
 * In-app view of the same events that trigger a system notification, satisfying
 * "notifications should also be visible in the app". Deliberately reuses the
 * existing expenses table (filtering rows whose `notes` starts with the
 * "Auto-parsed from SMS" marker SmsParser writes) instead of introducing a new
 * Room entity — adding a new @Entity would require bumping the DB version, and
 * this project's AppDatabase uses fallbackToDestructiveMigration(), which would
 * wipe all existing expense data on the next app update. Reusing existing data
 * avoids that risk entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    navController: NavController,
    isDark: Boolean,
    expenseViewModel: ExpenseViewModel
) {
    val uiState by expenseViewModel.uiState.collectAsState()

    val smsNotifications = uiState.expenses
        .filter { it.notes?.startsWith("Auto-parsed from SMS") == true }
        .sortedWith(compareByDescending<ExpenseEntity> { it.date }.thenByDescending { it.time })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (smsNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No notifications yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(smsNotifications, key = { it.id ?: it.hashCode() }) { expense ->
                NotificationCard(expense)
            }
        }
    }
}

@Composable
private fun NotificationCard(expense: ExpenseEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(expense.icon)
            }

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                // Mirrors the system notification: app name as the header,
                // description of what happened as the body.
                Text(
                    "Expenzo",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Added ${expense.title} \u2014 ${expense.category} \u00B7 \u20B9${formatAmount(expense.amount)} to your records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${expense.date} \u00B7 ${expense.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toInt().toString()
    else String.format(Locale.getDefault(), "%.2f", amount)
}
