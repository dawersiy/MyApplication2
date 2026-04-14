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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_SCHEDULE) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodayScheduleWidgetProvider::class.java)
            onUpdate(context, manager, manager.getAppWidgetIds(component))
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_today_schedule)

        views.setTextViewText(R.id.widget_title, "今日日程")
        views.setTextColor(R.id.widget_title, 0xFFFFFFFF.toInt()) // 白色

        val todayTasks = getTodayScheduleTasks(context)
        if (todayTasks.isEmpty()) {
            views.setTextViewText(R.id.widget_content, "今日暂无日程喵")
        } else {
            val scheduleText = todayTasks.joinToString("\n") { task ->
                buildString {
                    append("${task.hour.toString().padStart(2, '0')}:${task.minute.toString().padStart(2, '0')} ")
                    append("任务${task.title}喵:")
                    if (task.deadline) append(" 🔴紧急喵")
                    if (task.importance) append(" ⭐重要喵")
                    if (task.tag.isNotEmpty()) append(" 【${task.tag}】")
                }
            }
            views.setTextViewText(R.id.widget_content, scheduleText)
        }
        views.setTextColor(R.id.widget_content, 0xFFFFFFFF.toInt()) // 白色desuwa

        views.setInt(R.id.widget_container, "setBackgroundColor", 0xFF000000.toInt()) //黑色喵

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getTodayScheduleTasks(context: Context): List<Task> {
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = todayFormat.format(Calendar.getInstance().time)
        val taskData = prefs.getString("tasks_$today", "")
        return parseTaskData(taskData)
    }

    private fun parseTaskData(data: String?): List<Task> {
        if (data.isNullOrEmpty()) return emptyList()
        return try {
            data.split("|").mapNotNull { item ->
                val parts = item.split(":")
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
                        priority = parsePriority(parts[10])
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePriority(value: String): PriorityLevel {
        return when (value) {
            "HIGH" -> PriorityLevel.HIGH
            "MEDIUM" -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
    }

    companion object {
        const val ACTION_REFRESH_SCHEDULE = "com.example.myapplication.REFRESH_SCHEDULE_WIDGET"
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

enum class PriorityLevel { HIGH, MEDIUM, LOW }  //现在应该没问题了.....喵