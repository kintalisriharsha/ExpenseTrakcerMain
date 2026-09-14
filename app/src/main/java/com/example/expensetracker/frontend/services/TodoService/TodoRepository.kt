package com.example.expensetracker.frontend.services.TodoService

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao){

    suspend fun addTodo(todoEntity: TodoEntity): TodoEntity{
        val generatedId = dao.insertTodo(todoEntity)
        return todoEntity.copy(id = generatedId)
    }

    suspend fun updateTodo(todoEntity: TodoEntity){
        dao.updateTodo( todoEntity )
    }

    suspend fun deleteTodo(todoEntity: TodoEntity){
        dao.deleteTodo(todoEntity)
    }

    fun getAllTodos() : Flow<List<TodoEntity>> {
        val todos = dao.getAll()
        return todos
    }

    fun searchTodos(query: String): Flow<List<TodoEntity>> {
        return dao.searchTodos(query)
    }

    // Paginated, newest-first — feeds the History screen's infinite scroll.
    suspend fun getHistory(limit: Int, offset: Int): List<TodoEntity> {
        return dao.getHistoryPage(limit, offset)
    }

}