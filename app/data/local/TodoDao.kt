package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.example.myapplication.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("SELECT * FROM todo ORDER BY isDone ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("UPDATE todo SET isDone = :done WHERE id = :id")
    suspend fun updateDoneState(id: Long, done: Boolean)
}