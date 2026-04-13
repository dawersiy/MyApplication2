package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.Task
import com.example.myapplication.model.ScheduleStats
import com.example.myapplication.model.LunarInfo
import com.example.myapplication.model.CalendarDay
import com.example.myapplication.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    // 状态流
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _scheduleStats = MutableStateFlow<List<ScheduleStats>>(emptyList())
    val scheduleStats: StateFlow<List<ScheduleStats>> = _scheduleStats.asStateFlow()

    // 加载指定日期的任务
    fun loadTasksByDate(date: String) {
        // 同步加载任务
        _tasks.value = taskRepository.getTasksByDate(date)
    }

    // 添加任务
    fun addTask(task: Task, date: String) {
        // 同步保存任务
        taskRepository.saveTask(task, date)
        // 同步加载任务
        _tasks.value = taskRepository.getTasksByDate(date)
    }

    // 删除任务
    fun deleteTask(taskId: String, date: String) {
        // 同步删除任务
        taskRepository.deleteTask(taskId, date)
        // 同步加载任务
        _tasks.value = taskRepository.getTasksByDate(date)
    }

    // 计算统计数据
    fun calculateStats(tasks: List<Task>) {
        val tagCounts = tasks.groupBy { it.tag }
        val stats = tagCounts.map {
            ScheduleStats(
                tag = it.key,
                count = it.value.size,
                totalMinutes = it.value.sumOf { task -> 30 } // 假设每个任务30分钟
            )
        }
        _scheduleStats.value = stats
    }

    // 获取农历信息
    fun getLunarInfo(calendar: Calendar): LunarInfo {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 简化的农历计算
        val lunarMonths = listOf(
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"
        )
        val lunarDays = listOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )

        val lunarMonth = if (month <= lunarMonths.size) lunarMonths[month - 1] else ""
        val lunarDay = if (day <= lunarDays.size) lunarDays[day - 1] else ""
        val lunarDate = "$lunarMonth$lunarDay"

        // 简化的节气计算
        val solarTerms = when (month) {
            1 -> if (day <= 5) "小寒" else "大寒"
            2 -> if (day <= 4) "立春" else "雨水"
            3 -> if (day <= 5) "惊蛰" else "春分"
            4 -> if (day <= 4) "清明" else "谷雨"
            5 -> if (day <= 5) "立夏" else "小满"
            6 -> if (day <= 5) "芒种" else "夏至"
            7 -> if (day <= 7) "小暑" else "大暑"
            8 -> if (day <= 7) "立秋" else "处暑"
            9 -> if (day <= 7) "白露" else "秋分"
            10 -> if (day <= 7) "寒露" else "霜降"
            11 -> if (day <= 7) "立冬" else "小雪"
            12 -> if (day <= 7) "大雪" else "冬至"
            else -> ""
        }

        return LunarInfo(lunarDate, solarTerms)
    }

    // 生成日历天
    fun generateCalendarDays(calendar: Calendar): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // 设置为当月第一天
        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        // 获取当月第一天是星期几
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

        // 添加上个月的天数
        val prevMonth = calendar.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysToAddFromPrevMonth = (firstDayOfWeek - 1).coerceAtLeast(0)

        for (i in daysToAddFromPrevMonth downTo 1) {
            val day = daysInPrevMonth - (daysToAddFromPrevMonth - i)
            days.add(CalendarDay(
                day = day,
                isCurrentMonth = false,
                isHoliday = false,
                holidayName = "",
                lunarDay = ""
            ))
        }

        // 添加当月的天数
        val daysInCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInCurrentMonth) {
            val isHoliday = isHoliday(year, month + 1, i)
            val holidayName = if (isHoliday) getHolidayName(month + 1, i) else ""
            val lunarDay = getLunarDay(i)

            days.add(CalendarDay(
                day = i,
                isCurrentMonth = true,
                isHoliday = isHoliday,
                holidayName = holidayName,
                lunarDay = lunarDay
            ))
        }

        // 添加下个月的天数
        val remainingDays = 42 - days.size // 6行7列
        for (i in 1..remainingDays) {
            days.add(CalendarDay(
                day = i,
                isCurrentMonth = false,
                isHoliday = false,
                holidayName = "",
                lunarDay = ""
            ))
        }

        return days
    }

    // 检查是否是节假日
    private fun isHoliday(year: Int, month: Int, day: Int): Boolean {
        // 简化的节假日判断
        return when {
            month == 1 && day == 1 -> true // 元旦
            month == 5 && day == 1 -> true // 劳动节
            month == 10 && day == 1 -> true // 国庆节
            else -> false
        }
    }

    // 获取节假日名称
    private fun getHolidayName(month: Int, day: Int): String {
        return when {
            month == 1 && day == 1 -> "元旦"
            month == 5 && day == 1 -> "劳动节"
            month == 10 && day == 1 -> "国庆节"
            else -> ""
        }
    }

    // 获取农历日
    private fun getLunarDay(day: Int): String {
        val lunarDays = listOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        return if (day <= lunarDays.size) lunarDays[day - 1] else ""
    }

    // 检查是否是今天
    fun isToday(day: CalendarDay, today: Calendar): Boolean {
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        return day.isCurrentMonth && day.day == currentDay
    }

    // 检查两个日期是否相等
    fun isDateEqual(day: CalendarDay, date: java.util.Date, calendar: Calendar): Boolean {
        val checkCalendar = calendar.clone() as Calendar
        if (day.isCurrentMonth) {
            checkCalendar.set(Calendar.DAY_OF_MONTH, day.day)
        } else if (day.day > 15) {
            // 上个月
            checkCalendar.add(Calendar.MONTH, -1)
            checkCalendar.set(Calendar.DAY_OF_MONTH, day.day)
        } else {
            // 下个月
            checkCalendar.add(Calendar.MONTH, 1)
            checkCalendar.set(Calendar.DAY_OF_MONTH, day.day)
        }

        val checkDate = checkCalendar.time
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(checkDate) == sdf.format(date)
    }
}
