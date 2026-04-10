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

class MonthViewFragment : Fragment() {
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 Repository
        val database = AppDatabase.getDatabase(requireContext())
        scheduleRepository = ScheduleRepository(database)

        // 加载本月的日程数据
        lifecycleScope.launch {
            // 计算本月的开始和结束时间
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.add(java.util.Calendar.MONTH, 1)
            calendar.add(java.util.Calendar.MILLISECOND, -1)
            val monthEnd = calendar.timeInMillis
            
            scheduleRepository.getSchedulesByRange(monthStart, monthEnd).collect {
                // 这里可以更新 UI 显示本月的日程
            }
        }

        return inflater.inflate(R.layout.fragment_month_view, container, false)
    }
}