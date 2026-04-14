package com.example.myapplication.repository

import android.content.Context
import com.example.myapplication.model.Task
import com.example.myapplication.model.PriorityLevel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskRepository(private val context: Context) {
    private val PREFS_NAME = "tasks"
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 保存任务
    fun saveTask(task: Task, date: String) {
        try {
            val editor = prefs.edit()
            val existingData = prefs.getString("tasks_$date", "")
            
            val taskData = "${task.id}:${task.title}:${task.hour}:${task.minute}:${task.tag}:${task.deadline}:${task.importance}:${task.reminderEnabled}:${task.reminderMinutes}:${task.repeatType}:${task.priority}"
            
            val newData = if (existingData.isNullOrEmpty()) {
                taskData
            } else {
                "$existingData|$taskData"
            }
            
            editor.putString("tasks_$date", newData)
            editor.commit()
            
            // 发送广播更新小组件
            val intent = android.content.Intent("com.example.myapplication.REFRESH_SCHEDULE_WIDGET")
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 获取今日任务
    fun getTodayTasks(): List<Task> {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val taskData = prefs.getString("tasks_$today", "")
            return parseTasks(taskData)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // 获取指定日期的任务
    fun getTasksByDate(date: String): List<Task> {
        try {
            val taskData = prefs.getString("tasks_$date", "")
            val tasks = parseTasks(taskData)
            return tasks
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // 删除任务
    fun deleteTask(taskId: String, date: String) {
        try {
            val existingData = prefs.getString("tasks_$date", "")
            
            if (!existingData.isNullOrEmpty()) {
                val tasks = existingData.split("|")
                val filteredTasks = tasks.filter { !it.startsWith("$taskId:") }
                val newData = filteredTasks.joinToString("|")
                
                val editor = prefs.edit()
                editor.putString("tasks_$date", newData)
                editor.commit()
                
                // 发送广播更新小组件
                val intent = android.content.Intent("com.example.myapplication.REFRESH_SCHEDULE_WIDGET")
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 解析任务数据
    private fun parseTasks(data: String?): List<Task> {
        try {
            if (data.isNullOrEmpty()) return emptyList()
            
            return data.split("|").mapNotNull {
                try {
                    val parts = it.split(":")
                    if (parts.size >= 11) {
                        Task(
                            id = parts[0],
                            title = parts[1],
                            hour = parts[2].toInt(),
                            minute = parts[3].toInt(),
                            tag = parts[4],
                            deadline = parts[5].toBoolean(),
                            importance = parts[6].toBoolean(),
                            reminderEnabled = parts[7].toBoolean(),
                            reminderMinutes = parts[8].toInt(),
                            repeatType = parts[9],
                            priority = parsePriorityLevel(parts[10])
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // 解析优先级
    private fun parsePriorityLevel(value: String): PriorityLevel {
        return when (value) {
            "HIGH" -> PriorityLevel.HIGH
            "MEDIUM" -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
    }
}
