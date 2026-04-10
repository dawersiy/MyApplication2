package com.example.myapplication.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.myapplication.R

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

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_todo)
        
        // 这里可以添加实际的待办数据
        views.setTextViewText(R.id.widget_title, "待办事项")
        views.setTextViewText(R.id.widget_content, "暂无待办")
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}