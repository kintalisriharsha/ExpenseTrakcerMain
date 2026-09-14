package com.example.expensetracker.frontend.services.expenseService

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.expensetracker.services.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAll() : Flow<List<ExpenseEntity>>{
        return dao.getAllFlow()
    }

    fun getExpenseById(id: Long): Flow<ExpenseEntity> {
        return dao.getExpenseById(id)
    }

    // Used by the Search dialog (bounded, non-scrolling result list).
    fun unifiedSearchSimple(q: String, category: String): Flow<List<ExpenseEntity>> {
        return dao.unifiedSearch(q, category)
    }

    suspend fun add(entity: ExpenseEntity): Long {
        val newId = dao.insert(entity)
        return newId
    }

    suspend fun update(entity: ExpenseEntity) {
        dao.update(entity)
    }

    suspend fun delete(id: Long?) {
        val entity = dao.getExpenseById(id).firstOrNull() ?: return
        dao.delete(entity)
    }
}