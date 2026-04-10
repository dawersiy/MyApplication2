package com.example.myapplication.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.TodoRepository
import kotlinx.coroutines.runBlocking

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val database = AppDatabase.getDatabase(context)
        val todoRepository = TodoRepository(database)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            runBlocking {
                val todos = todoRepository.getAllTodos().first()
                val incompleteTodos = todos.filter { !it.isDone }

                // 更新 widget 视图
                if (incompleteTodos.isNotEmpty()) {
                    val firstTodo = incompleteTodos[0]
                    views.setTextViewText(R.id.widget_title, "Todo List")
                    views.setTextViewText(R.id.widget_todo_title, firstTodo.title)
                    // 这里可以添加更多待办事项信息
                } else {
                    views.setTextViewText(R.id.widget_title, "No Todo Items")
                    views.setTextViewText(R.id.widget_todo_title, "")
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}