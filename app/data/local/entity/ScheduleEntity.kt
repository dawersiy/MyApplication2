package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val startTime: Long,
    val endTime: Long,

    val location: String = "",
    val note: String = "",

    // 提前提醒分钟数：0/5/10/15...
    val reminderMinutes: Int = 10,

    // NONE, DAILY, WEEKLY, MONTHLY
    val repeatType: String = "NONE",

    // 日程标签：工作、娱乐、休息、摸鱼等
    val tag: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)