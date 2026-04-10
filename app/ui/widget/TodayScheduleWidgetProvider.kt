package com.example.myapplication.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.ScheduleRepository
import kotlinx.coroutines.runBlocking

class TodayScheduleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val database = AppDatabase.getDatabase(context)
        val scheduleRepository = ScheduleRepository(database)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_today_schedule)

            runBlocking {
                // 获取当天的日程数据
                val schedules = scheduleRepository.getTodaySchedules().first()

                // 更新 widget 视图
                if (schedules.isNotEmpty()) {
                    val firstSchedule = schedules[0]
                    views.setTextViewText(R.id.widget_title, "Today's Schedule")
                    views.setTextViewText(R.id.widget_schedule_title, firstSchedule.title)
                    // 这里可以添加更多日程信息
                } else {
                    views.setTextViewText(R.id.widget_title, "No Schedule Today")
                    views.setTextViewText(R.id.widget_schedule_title, "")
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}