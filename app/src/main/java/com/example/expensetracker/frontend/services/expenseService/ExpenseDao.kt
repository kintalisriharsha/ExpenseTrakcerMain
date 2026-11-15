package com.example.expensetracker.frontend.services.expenseService

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetracker.services.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // ── Reactive read — emits a new list whenever the table changes ───────────
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllFlow(): Flow<List<ExpenseEntity>>

    @Query("Select * From expenses Where id = :id")
    fun getExpenseById(id: Long?): Flow<ExpenseEntity?>

    @Query("""
    SELECT * FROM expenses
    WHERE (:category = '' OR category = :category)
    AND (:query = '' OR title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
    ORDER BY date DESC
""")
    fun unifiedSearch(query: String, category: String): Flow<List<ExpenseEntity>>


    // ── Writes ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity): Long   // returns the generated id

    // Inserts all rows in ONE transaction: either every date is saved or none are.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun update(entity: ExpenseEntity)

    // Update one expense and add copies for extra dates in ONE transaction.
    @Transaction
    suspend fun updateAndInsertAll(updated: ExpenseEntity, extras: List<ExpenseEntity>) {
        update(updated)
        if (extras.isNotEmpty()) insertAll(extras)
    }

    @Delete
    suspend fun delete(entity: ExpenseEntity)

}