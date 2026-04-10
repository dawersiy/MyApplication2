package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val database: AppDatabase) {
    suspend fun insertTodo(todo: TodoEntity): Long {
        return database.todoDao().insert(todo)
    }

    suspend fun updateTodo(todo: TodoEntity) {
        database.todoDao().update(todo)
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        database.todoDao().delete(todo)
    }

    fun getAllTodos(): Flow<List<TodoEntity>> {
        return database.todoDao().getAllTodos()
    }

    suspend fun updateDoneState(id: Long, done: Boolean) {
        database.todoDao().updateDoneState(id, done)
    }
}