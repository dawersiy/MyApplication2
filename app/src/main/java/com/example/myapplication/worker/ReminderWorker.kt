package com.example.myapplication.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.notification.NotificationHelper

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "任务提醒"
        val content = inputData.getString("content") ?: "您有一个任务即将开始"
        val priority = inputData.getString("priority") ?: "LOW"
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notificationPriority = when (priority) {
            "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "MEDIUM" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
        
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(notificationPriority)
            .setAutoCancel(false)
            .setOngoing(priority == "HIGH") // 高优任务设置为持续通知
            .build()
        
        // 对于高优任务，发送多次提醒
        if (priority == "HIGH") {
            // 立即发送一次
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            
            // 3分钟后再次提醒
            Thread.sleep(3 * 60 * 1000)
            notificationManager.notify((System.currentTimeMillis() + 1).toInt(), notification)
            
            // 6分钟后第三次提醒
            Thread.sleep(3 * 60 * 1000)
            notificationManager.notify((System.currentTimeMillis() + 2).toInt(), notification)
        } else {
            // 普通任务只发送一次提醒
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
        
        return Result.success()
    }
}