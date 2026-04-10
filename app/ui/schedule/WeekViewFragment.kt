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

class WeekViewFragment : Fragment() {
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 Repository
        val database = AppDatabase.getDatabase(requireContext())
        scheduleRepository = ScheduleRepository(database)

        // 加载本周的日程数据
        lifecycleScope.launch {
            // 计算本周的开始和结束时间
            val today = System.currentTimeMillis()
            val dayOfWeek = (today / (24 * 60 * 60 * 1000)) % 7
            val weekStart = today - dayOfWeek * 24 * 60 * 60 * 1000
            val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000 - 1
            
            scheduleRepository.getSchedulesByRange(weekStart, weekEnd).collect {
                // 这里可以更新 UI 显示本周的日程
            }
        }

        return inflater.inflate(R.layout.fragment_week_view, container, false)
    }
}