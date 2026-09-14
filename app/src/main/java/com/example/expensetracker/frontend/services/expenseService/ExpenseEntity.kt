package com.example.expensetracker.services.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    var tab: Boolean = true,
    val category: String,
    val amount: Double,
    val notes: String?,
    val date: String,
    val time: String,
    val contactName: String,
    val contactNumber: String,
    val icon: String,
    val title: String,
)