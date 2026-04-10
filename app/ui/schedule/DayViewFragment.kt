package com.example.myapplication.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class DayViewFragment : Fragment() {
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 Repository
        val database = AppDatabase.getDatabase(requireContext())
        scheduleRepository = ScheduleRepository(database)

        // 加载当天的日程数据
        lifecycleScope.launch {
            // 计算当天的开始和结束时间
            val todayStart = System.currentTimeMillis() / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000)
            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
            
            scheduleRepository.getSchedulesByRange(todayStart, todayEnd).collect {
                // 这里可以更新 UI 显示当天的日程
            }
        }

        return inflater.inflate(R.layout.fragment_day_view, container, false)
    }
}