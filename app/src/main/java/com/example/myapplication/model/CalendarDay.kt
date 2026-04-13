package com.example.myapplication.model

// 日历天数据类
data class CalendarDay(
    val day: Int,
    val isCurrentMonth: Boolean,
    val isHoliday: Boolean = false,
    val holidayName: String = "",
    val lunarDay: String = ""
)
