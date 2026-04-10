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

class ScheduleListFragment : Fragment() {
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 Repository
        val database = AppDatabase.getDatabase(requireContext())
        scheduleRepository = ScheduleRepository(database)

        // 加载今日日程数据
        lifecycleScope.launch {
            scheduleRepository.getTodaySchedules().collect {
                // 这里可以更新 UI 显示今日日程列表
            }
        }

        return inflater.inflate(R.layout.fragment_schedule_list, container, false)
    }
}