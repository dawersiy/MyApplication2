package com.example.myapplication.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.ScheduleRepository
import com.example.myapplication.notification.NotificationHelper
import kotlinx.coroutines.runBlocking

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val scheduleRepository = ScheduleRepository(database)
        val notificationHelper = NotificationHelper(applicationContext)

        runBlocking {
            val currentTime = System.currentTimeMillis()
            // 获取当天需要提醒的日程
            val schedules = scheduleRepository.getTodaySchedules().first()
            
            schedules.forEach {schedule ->
                // 计算提醒时间
                val reminderTime = schedule.startTime - (schedule.reminderMinutes * 60 * 1000)
                
                // 检查是否需要提醒
                if (reminderTime <= currentTime && reminderTime > currentTime - 60000) { // 1分钟内
                    notificationHelper.showScheduleReminderNotification(
                        schedule.title,
                        "Your schedule is starting soon!",
                        schedule.id.toInt()
                    )
                }
            }
        }

        return Result.success()
    }
}