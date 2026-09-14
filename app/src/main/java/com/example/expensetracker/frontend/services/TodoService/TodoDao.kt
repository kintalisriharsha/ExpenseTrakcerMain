package com.example.expensetracker.frontend.services.TodoService

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface TodoDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todoEntity: TodoEntity) : Long

    @Update
    suspend fun updateTodo(todoEntity: TodoEntity)

    @Delete
    suspend fun deleteTodo(todoEntity: TodoEntity)

    @Query("Select * from todo")
    fun getAll() : Flow<List<TodoEntity>>


    @Query("SELECT * FROM todo WHERE Name LIKE '%' || :query || '%'")
    fun searchTodos(query: String) : Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistoryPage(limit: Int, offset: Int): List<TodoEntity>
}