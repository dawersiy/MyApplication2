package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val isDone: Boolean = false,
    val dueTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)