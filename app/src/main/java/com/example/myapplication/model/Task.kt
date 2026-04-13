package com.example.myapplication.model

// 任务优先级枚举
enum class PriorityLevel {
    HIGH,
    MEDIUM,
    LOW
}

// 任务数据类
data class Task(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val tag: String,
    val deadline: Boolean,
    val importance: Boolean,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val repeatType: String,
    val priority: PriorityLevel
)
