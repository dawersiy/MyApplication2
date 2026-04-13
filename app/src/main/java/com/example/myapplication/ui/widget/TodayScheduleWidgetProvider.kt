package com.example.myapplication.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodayScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_simple)

        // 设置标题
        views.setTextViewText(R.id.widget_title, "今日日程")

        // 设置日期
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        val todayStr = dateFormat.format(today.time)
        views.setTextViewText(R.id.widget_date, todayStr)

        // 获取今日任务
        val tasks = getTodayTasks(context)

        // 设置内容
        if (tasks.isEmpty()) {
            views.setTextViewText(R.id.widget_content, "暂无日程")
        } else {
            val taskText = tasks.joinToString("\n") { task ->
                "• ${task.hour}:${task.minute.toString().padStart(2, '0')} ${task.title}"
            }
            views.setTextViewText(R.id.widget_content, taskText)
        }

        // 设置点击事件 - 打开主应用
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getTodayTasks(context: Context): List<Task> {
        try {
            val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val taskData = prefs.getString("tasks_$today", "")
            return parseTasks(taskData)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun parseTasks(data: String?): List<Task> {
        try {
            if (data.isNullOrEmpty()) return emptyList()
            
            return data.split("|").mapNotNull { item ->
                try {
                    val parts = item.split(":")
                    if (parts.size >= 4) {
                        Task(
                            id = parts[0],
                            title = parts[1],
                            hour = parts[2].toInt(),
                            minute = parts[3].toInt(),
                            tag = if (parts.size > 4) parts[4] else "",
                            deadline = if (parts.size > 5) parts[5].toBoolean() else false,
                            importance = if (parts.size > 6) parts[6].toBoolean() else false,
                            reminderEnabled = if (parts.size > 7) parts[7].toBoolean() else false,
                            reminderMinutes = if (parts.size > 8) parts[8].toInt() else 0,
                            repeatType = if (parts.size > 9) parts[9] else "",
                            priority = if (parts.size > 10) parsePriorityLevel(parts[10]) else PriorityLevel.LOW
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun parsePriorityLevel(value: String): PriorityLevel {
        return when (value) {
            "HIGH" -> PriorityLevel.HIGH
            "MEDIUM" -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
    }
}

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

enum class PriorityLevel {
    HIGH,
    MEDIUM,
    LOW
}

