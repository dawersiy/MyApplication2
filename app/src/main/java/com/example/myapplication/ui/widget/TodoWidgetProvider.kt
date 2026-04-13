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

class TodoWidgetProvider : AppWidgetProvider() {

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
        if (intent.action == ACTION_REFRESH_TODO) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_test)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getTodos(context: Context): List<TodoItem> {
        // 从 SharedPreferences 获取待办事项数据
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val taskData = prefs.getString("tasks_$today", null)

        return if (taskData != null) {
            parseTasks(taskData)
        } else {
            emptyList()
        }
    }

    private fun parseTasks(data: String): List<TodoItem> {
        // 解析任务数据
        return try {
            data.split("|").mapNotNull { item ->
                val parts = item.split(":")
                if (parts.size >= 2) {
                    TodoItem(parts[1], false) // 简单处理，默认为未完成
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        const val ACTION_REFRESH_TODO = "com.example.myapplication.ACTION_REFRESH_TODO"
    }
}

data class TodoItem(val title: String, val isCompleted: Boolean)
