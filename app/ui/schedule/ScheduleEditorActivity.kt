package com.example.myapplication.ui.schedule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.ScheduleRepository
import com.example.myapplication.data.local.entity.ScheduleEntity
import com.example.myapplication.data.model.RepeatType
import com.example.myapplication.data.model.ReminderType

class ScheduleEditorActivity : AppCompatActivity() {
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_editor)

        // 初始化 Repository
        val database = AppDatabase.getInstance(this)
        scheduleRepository = ScheduleRepository(database)

        // 处理编辑逻辑
        val scheduleId = intent.getIntExtra("schedule_id", -1)
        if (scheduleId != -1) {
            // 编辑现有日程
            loadSchedule(scheduleId)
        } else {
            // 创建新日程
        }
    }

    private fun loadSchedule(id: Int) {
        // 这里可以加载现有日程数据并显示在界面上
    }

    private fun saveSchedule() {
        // 这里可以获取界面上的数据并保存日程
        val schedule = ScheduleEntity(
            title = "Test Schedule",
            description = "Test Description",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000, // 1 hour later
            repeatType = RepeatType.NONE,
            reminderType = ReminderType.NONE,
            reminderMinutes = 0
        )

        // 保存到数据库
        // lifecycleScope.launch { scheduleRepository.insertSchedule(schedule) }
    }
}