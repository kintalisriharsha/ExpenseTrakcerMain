package com.example.expensetracker

import android.app.Application
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.example.expensetracker.frontend.services.important.AppDatabase
import com.example.expensetracker.frontend.widgets.ExpenseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application so the expense-widget refresh isn't wired into every
 * insert/update/delete call site individually (TransactionViewModel.addExpense,
 * updateExpense, deleteExpense, SmsProcessingService, and any future write path).
 *
 * Room's getAllFlow() already re-emits automatically whenever the "expenses"
 * table changes (Room tracks table invalidation under the hood), so collecting
 * it once here and calling updateAll() on every emission gives every write path
 * an instant widget refresh for free, with nothing to remember at the call site.
 */
class ExpenseTrackerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val expenseDao = AppDatabase.getInstance(this).expenseDao()

            appScope.launch {
                expenseDao.getAllFlow().collect {
                    // Fired on every insert/update/delete to "expenses" — including
                    // SMS-parsed inserts and manual add/edit/delete from the UI.
                    ExpenseWidget().updateAll(this@ExpenseTrackerApp)
                }
            }
        }
    }
}
