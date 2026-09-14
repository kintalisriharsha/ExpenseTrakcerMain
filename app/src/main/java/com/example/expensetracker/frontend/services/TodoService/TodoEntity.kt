package com.example.expensetracker.frontend.services.TodoService

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color

enum class TodoCategory(val label: String, val emoji: String, val color: Color, val bg: Color) {
    GROCERY("Grocery", "🛒", Color(0xFF16A34A), Color(0xFFDCFCE7)),
    EGGS("Eggs", "🥚", Color(0xFFD97706), Color(0xFFFEF3C7)),
    VEGGIES("Veggies", "🥦", Color(0xFF059669), Color(0xFFD1FAE5)),
    FRUITS("Fruits", "🍎", Color(0xFFDC2626), Color(0xFFFFE4E6)),
    DAIRY("Dairy", "🥛", Color(0xFF2563EB), Color(0xFFDBEAFE)),
    MEAT("Meat", "🥩", Color(0xFFB45309), Color(0xFFFEF9C3)),
    SNACKS("Snacks", "🍿", Color(0xFF7C3AED), Color(0xFFF3E8FF)),
    OTHER("Other", "📦", Color(0xFF64748B), Color(0xFFF1F5F9))
}

@Entity("todo")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val Name: String,
    val Amount: Double,
    val checkBox: Boolean = false,
    val quantity: Int = 1,
    val tab: Boolean = false,
    val category: String? = null,          // TodoCategory.name, nullable = "no category"
    val createdAt: Long = System.currentTimeMillis(),   // epoch millis, used to bucket todos by day/week
) {
    val categoryEnum: TodoCategory?
        get() = category?.let { name -> TodoCategory.values().firstOrNull { it.name == name } }
}
