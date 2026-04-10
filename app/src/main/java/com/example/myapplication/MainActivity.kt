package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.myapplication.notification.NotificationHelper
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// ScheduleStats 数据类
data class ScheduleStats(
    val tag: String,
    val count: Int,
    val totalMinutes: Int
)

// 任务优先级枚举
enum class PriorityLevel {
    HIGH,
    MEDIUM,
    LOW
}

// 任务数据类
data class Task(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val tag: String,
    val deadline: Boolean,
    val importance: Boolean,
    val priority: PriorityLevel,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val repeatType: String
)

// 农历信息数据类
data class LunarInfo(
    val lunarDate: String,
    val solarTerms: String
)

// 日历天数据类
data class CalendarDay(
    val day: Int,
    val isCurrentMonth: Boolean,
    val isHoliday: Boolean = false,
    val holidayName: String = "",
    val lunarDay: String = ""
)

// 背景图片管理工具
object BackgroundManager {
    private const val BACKGROUND_FILE_NAME = "background_image.jpg"
    
    fun saveBackground(context: android.content.Context, bitmap: Bitmap) {
        try {
            val file = File(context.filesDir, BACKGROUND_FILE_NAME)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun loadBackground(context: android.content.Context): Bitmap? {
        try {
            val file = File(context.filesDir, BACKGROUND_FILE_NAME)
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            } else {
                // 使用默认背景图片
                return BitmapFactory.decodeResource(context.resources, R.drawable.background_image)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    fun clearBackground(context: android.content.Context) {
        val file = File(context.filesDir, BACKGROUND_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}

// 获取农历信息
fun getLunarInfo(calendar: Calendar): LunarInfo {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    // 这里使用简化的农历计算，实际应用中可以使用更复杂的农历库
    val lunarMonths = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )
    val lunarDays = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    // 简化的节气计算
    val solarTerm = when (month) {
        1 -> if (day < 5) "小寒" else "大寒"
        2 -> if (day < 4) "立春" else "雨水"
        3 -> if (day < 6) "惊蛰" else "春分"
        4 -> if (day < 5) "清明" else "谷雨"
        5 -> if (day < 6) "立夏" else "小满"
        6 -> if (day < 7) "芒种" else "夏至"
        7 -> if (day < 8) "小暑" else "大暑"
        8 -> if (day < 8) "立秋" else "处暑"
        9 -> if (day < 8) "白露" else "秋分"
        10 -> if (day < 8) "寒露" else "霜降"
        11 -> if (day < 7) "立冬" else "小雪"
        12 -> if (day < 7) "大雪" else "冬至"
        else -> ""
    }

    return LunarInfo(
        lunarDate = "农历${lunarMonths[month - 1]}${lunarDays[day - 1]}",
        solarTerms = if (solarTerm.isNotEmpty()) "${solarTerm}" else "${year}年${month}月${day}日"
    )
}

// 生成日历天列表
fun generateCalendarDays(calendar: Calendar): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    // 保存当前日期
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // 计算当月第一天是星期几
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=周日, 2=周一, ..., 7=周六

    // 计算当月天数
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 添加上个月的天数
    val prevMonth = calendar.clone() as Calendar
    prevMonth.add(Calendar.MONTH, -1)
    val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfPrevMonth = daysInPrevMonth - (firstDayOfWeek - 2) // 调整为周一开始

    for (i in 0 until firstDayOfWeek - 2) {
        days.add(CalendarDay(
            day = startDayOfPrevMonth + i,
            isCurrentMonth = false
        ))
    }

    // 添加当月的天数
    for (i in 1..daysInMonth) {
        // 模拟节假日
        val isHoliday = false // 去除所有节假日
        val holidayName = ""

        // 模拟农历
        val lunarDay = when (i) {
            1 -> "初一"
            2 -> "初二"
            3 -> "初三"
            4 -> "初四"
            5 -> "初五"
            6 -> "初六"
            7 -> "初七"
            8 -> "初八"
            9 -> "初九"
            10 -> "初十"
            11 -> "十一"
            12 -> "十二"
            13 -> "十三"
            14 -> "十四"
            15 -> "十五"
            16 -> "十六"
            17 -> "十七"
            18 -> "十八"
            19 -> "十九"
            20 -> "二十"
            21 -> "廿一"
            22 -> "廿二"
            23 -> "廿三"
            24 -> "廿四"
            25 -> "廿五"
            26 -> "廿六"
            27 -> "廿七"
            28 -> "廿八"
            29 -> "廿九"
            30 -> "三十"
            31 -> "初一"
            else -> ""
        }

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
            isCurrentMonth = false
        ))
    }

    // 恢复当前日期
    calendar.set(Calendar.DAY_OF_MONTH, currentDay)

    return days
}

// 检查是否是今天
fun isToday(day: CalendarDay, today: Calendar): Boolean {
    val currentYear = today.get(Calendar.YEAR)
    val currentMonth = today.get(Calendar.MONTH)
    val currentDay = today.get(Calendar.DAY_OF_MONTH)

    val calendar = Calendar.getInstance()
    calendar.set(currentYear, currentMonth, day.day)

    return day.isCurrentMonth && calendar.get(Calendar.DAY_OF_MONTH) == currentDay
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
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(checkDate) == sdf.format(date)
}

// 计算统计数据
fun calculateStats(tasks: List<Task>): List<ScheduleStats> {
    // 按标签分组并统计数量
    val tagCounts = tasks.groupBy { it.tag }.mapValues { it.value.size }
    
    // 确保所有标签都有统计数据
    val allTags = listOf("工作", "娱乐", "休息", "摸鱼")
    val stats = allTags.map { tag ->
        ScheduleStats(tag, tagCounts.getOrDefault(tag, 0), 0)
    }
    
    return stats
}

// 日历天组件
@Composable
fun CalendarDay(day: CalendarDay, isToday: Boolean, isCurrentMonth: Boolean, isSelected: Boolean, onClick: () -> Unit, isDarkMode: Boolean) {
    val cardColor = when {
        isSelected -> if (isDarkMode) Color(0xFF1976D2) else Color(0xFFBBDEFB) // 选中日期蓝色背景
        day.isHoliday -> if (isDarkMode) Color(0xFFC62828) else Color(0xFFF8D7DA) // 节假日红色背景
        isToday -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFFE3F2FD) // 今天蓝色背景
        else -> if (isDarkMode) Color.DarkGray else Color.White
    }

    val textColor = if (isCurrentMonth) (if (isDarkMode) Color.White else Color.Black) else (if (isDarkMode) Color.Gray else Color.Gray)

    Card(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            if (day.lunarDay.isNotEmpty()) {
                Text(
                    text = day.lunarDay,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (day.isHoliday) {
                Text(
                    text = day.holidayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Color(0xFFEF5350) else Color.Red,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// 日程编辑器
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditor(
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    isDarkMode: Boolean
) {
    val schedule = remember { mutableStateOf("") }
    val reminderEnabled = remember { mutableStateOf(false) }
    val reminderMinutes = remember { mutableStateOf(10) }
    val repeatType = remember { mutableStateOf("NONE") }
    val hour = remember { mutableIntStateOf(12) }
    val minute = remember { mutableIntStateOf(0) }
    val selectedTag = remember { mutableStateOf("") }
    val tags = listOf("工作", "娱乐", "休息", "摸鱼")
    val deadline = remember { mutableStateOf(false) }
    val importance = remember { mutableStateOf(false) }
    
    // 自动计算优先级
    val priority = when {
        deadline.value && importance.value -> PriorityLevel.HIGH
        deadline.value || importance.value -> PriorityLevel.MEDIUM
        else -> PriorityLevel.LOW
    }

    val backgroundColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.background
    val textColor = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground
    val surfaceColor = if (isDarkMode) Color.DarkGray else MaterialTheme.colorScheme.surface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑日程",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor
                )
            }

            // 日程输入框
            TextField(
                value = schedule.value,
                onValueChange = { schedule.value = it },
                label = { Text("日程", color = textColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor
                )
            )

            // 时间设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "时间设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    TextField(
                        value = hour.value.toString(),
                        onValueChange = { 
                            if (it.isNotEmpty()) {
                                val input = it.toIntOrNull() ?: 0
                                hour.value = input.coerceIn(0, 23)
                            }
                        },
                        label = { Text("小时", color = textColor) },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(end = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor
                        )
                    )
                    Text(text = ":", style = MaterialTheme.typography.headlineSmall, color = textColor)
                    TextField(
                        value = minute.value.toString(),
                        onValueChange = { 
                            if (it.isNotEmpty()) {
                                val input = it.toIntOrNull() ?: 0
                                minute.value = input.coerceIn(0, 59)
                            }
                        },
                        label = { Text("分钟", color = textColor) },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor
                        )
                    )
                }
            }

            // 标签选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "标签选择",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    tags.forEach { tag ->
                        Button(
                            onClick = { selectedTag.value = tag },
                            modifier = Modifier.padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTag.value == tag) (if (isDarkMode) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary) else surfaceColor,
                                contentColor = if (selectedTag.value == tag) Color.White else textColor
                            )
                        ) {
                            Text(tag, color = if (selectedTag.value == tag) Color.White else textColor)
                        }
                    }
                }
            }

            // 紧急程度和重要性
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "紧急",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Switch(
                        checked = deadline.value,
                        onCheckedChange = { deadline.value = it },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "重要",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Switch(
                        checked = importance.value,
                        onCheckedChange = { importance.value = it },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // 优先级显示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "优先级",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Text(
                    text = when (priority) {
                        PriorityLevel.HIGH -> "高"
                        PriorityLevel.MEDIUM -> "中"
                        PriorityLevel.LOW -> "低"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (priority) {
                        PriorityLevel.HIGH -> Color.Red
                        PriorityLevel.MEDIUM -> Color.Yellow
                        PriorityLevel.LOW -> Color.Green
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // 提醒设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "提醒设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Switch(
                    checked = reminderEnabled.value,
                    onCheckedChange = { reminderEnabled.value = it },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // 提前提醒时间
            if (reminderEnabled.value) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提前提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    TextField(
                        value = reminderMinutes.value.toString(),
                        onValueChange = { 
                            if (it.isNotEmpty()) {
                                val input = it.toIntOrNull() ?: 0
                                reminderMinutes.value = input.coerceIn(0, 120)
                            }
                        },
                        label = { Text("分钟", color = textColor) },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor
                        )
                    )
                }
            }

            // 重复提醒
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "重复提醒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { type ->
                        Button(
                            onClick = { repeatType.value = type },
                            modifier = Modifier.padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (repeatType.value == type) (if (isDarkMode) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary) else surfaceColor,
                                contentColor = if (repeatType.value == type) Color.White else textColor
                            )
                        ) {
                            Text(type, color = if (repeatType.value == type) Color.White else textColor)
                        }
                    }
                }
            }

            // 按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    )
                ) {
                    Text("取消", color = textColor)
                }
                Button(
                    onClick = {
                        if (schedule.value.isNotEmpty()) {
                            val task = Task(
                                id = System.currentTimeMillis().toString(),
                                title = schedule.value,
                                hour = hour.value,
                                minute = minute.value,
                                tag = selectedTag.value,
                                deadline = deadline.value,
                                importance = importance.value,
                                priority = priority,
                                reminderEnabled = reminderEnabled.value,
                                reminderMinutes = reminderMinutes.value,
                                repeatType = repeatType.value
                            )
                            onSave(task)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("确定", color = Color.White)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化通知渠道
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        
        setContent {
            MyApplicationTheme {
                CalendarApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarApp() {
    val calendar = remember { Calendar.getInstance() }
    val currentDate = remember { mutableStateOf(calendar.time) }
    val selectedDate = remember { mutableStateOf(calendar.time) }
    val today = Calendar.getInstance()
    val showScheduleEditor = remember { mutableStateOf(false) }
    val showStatistics = remember { mutableStateOf(false) }
    val showBackgroundSettings = remember { mutableStateOf(false) }
    val isDarkMode = remember { mutableStateOf(false) }
    val scheduleStats = remember { mutableStateOf(listOf<ScheduleStats>()) }
    val backgroundImage = remember { mutableStateOf<ImageBitmap?>(null) }
    val useCustomBackground = remember { mutableStateOf(false) }

    val context = LocalContext.current

    // 加载保存的背景图片
    LaunchedEffect(Unit) {
        val bitmap = BackgroundManager.loadBackground(context)
        if (bitmap != null) {
            backgroundImage.value = bitmap.asImageBitmap()
            useCustomBackground.value = true
        }
    }

    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MM月", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy年", Locale.getDefault())

    // 计算选中日期的农历信息
    val selectedCalendar = Calendar.getInstance()
    selectedCalendar.time = selectedDate.value
    val lunarInfo = getLunarInfo(selectedCalendar)

    // 颜色设置
    val backgroundColor = if (isDarkMode.value) Color.Black else MaterialTheme.colorScheme.background
    val textColor = if (isDarkMode.value) Color.White else MaterialTheme.colorScheme.onBackground
    val surfaceColor = if (isDarkMode.value) Color.DarkGray else Color(0xFFF5F5DC) // 淡米色,试试,比白色好看desuwa
    val primaryColor = if (isDarkMode.value) Color.Blue else MaterialTheme.colorScheme.primary

    // 保存的任务数据
    val savedTasks = remember { mutableStateOf(listOf<Task>()) }

    // 加载保存的任务
    LaunchedEffect(selectedDate.value) {
        // 这里可以从存储中加载任务，现在使用空列表
        savedTasks.value = listOf()
    }

    // 相册选择
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bitmap?.let {bmp ->
                        backgroundImage.value = bmp.asImageBitmap()
                        useCustomBackground.value = true
                        BackgroundManager.saveBackground(context, bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 打开相册
    val openGallery = {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    // 权限请求
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，打开相册
            openGallery()
        }
    }

    // 检查权限并打开相册
    val checkPermissionAndOpenGallery = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历", color = textColor) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Button(onClick = { showBackgroundSettings.value = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceColor,
                                contentColor = textColor
                            )) {
                            Text("背景", color = textColor)
                        }
                        Button(onClick = { showStatistics.value = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceColor,
                                contentColor = textColor
                            )) {
                            Text("统计", color = textColor)
                        }
                        Text(
                            text = "夜间模式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Switch(
                            checked = isDarkMode.value,
                            onCheckedChange = { isDarkMode.value = it },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) {
        innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 显示背景图片
            if (useCustomBackground.value && backgroundImage.value != null) {
                Image(
                    bitmap = backgroundImage.value!!,
                    contentDescription = "背景图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // 年份和月份选择栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 年份选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { 
                        calendar.add(Calendar.YEAR, -1)
                        currentDate.value = calendar.time
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    ),
                    modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                        Text("<", color = textColor)
                    }
                    Text(
                        text = yearFormat.format(currentDate.value),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor
                    )
                    Button(onClick = { 
                        calendar.add(Calendar.YEAR, 1)
                        currentDate.value = calendar.time
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    ),
                    modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                        Text(">", color = textColor)
                    }
                }

                // 月份选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { 
                        calendar.add(Calendar.MONTH, -1)
                        currentDate.value = calendar.time
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    ),
                    modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                        Text("<", color = textColor)
                    }
                    Text(
                        text = monthFormat.format(currentDate.value),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor
                    )
                    Button(onClick = { 
                        calendar.add(Calendar.MONTH, 1)
                        currentDate.value = calendar.time
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    ),
                    modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                        Text(">", color = textColor)
                    }
                }

                // 放假安排
                Button(onClick = { /* 放假安排逻辑 */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceColor,
                    contentColor = textColor
                ),
                modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                    Text("放假安排", color = textColor)
                }

                // 起始日
                Button(onClick = { /* 起始日逻辑 */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceColor,
                    contentColor = textColor
                ),
                modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                    Text("起始日", color = textColor)
                }

                // 返回今天
                Button(onClick = { 
                    calendar.time = today.time
                    currentDate.value = calendar.time
                    selectedDate.value = today.time
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceColor,
                    contentColor = textColor
                ),
                modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                    Text("返回今天", color = textColor)
                }
            }

            // 星期标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp, 16.dp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = textColor
                    )
                }
            }

            // 日历网格
            val calendarDays = generateCalendarDays(calendar)
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calendarDays) {
                    day ->
                    val isSelected = isDateEqual(day, selectedDate.value, calendar)
                    CalendarDay(
                        day = day,
                        isToday = isToday(day, today),
                        isCurrentMonth = day.isCurrentMonth,
                        isSelected = isSelected,
                        onClick = {
                            // 计算选中日期的具体时间
                            val clickCalendar = calendar.clone() as Calendar
                            if (day.isCurrentMonth) {
                                clickCalendar.set(Calendar.DAY_OF_MONTH, day.day)
                            } else if (day.day > 15) {
                                // 上个月
                                clickCalendar.add(Calendar.MONTH, -1)
                                // 检查该月份是否有这么多天
                                val maxDays = clickCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val actualDay = if (day.day > maxDays) maxDays else day.day
                                clickCalendar.set(Calendar.DAY_OF_MONTH, actualDay)
                            } else {
                                // 下个月
                                clickCalendar.add(Calendar.MONTH, 1)
                                // 检查该月份是否有这么多天
                                val maxDays = clickCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val actualDay = if (day.day > maxDays) maxDays else day.day
                                clickCalendar.set(Calendar.DAY_OF_MONTH, actualDay)
                            }
                            selectedDate.value = clickCalendar.time
                        },
                        isDarkMode = isDarkMode.value
                    )
                }
            }

            // 农历信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lunarInfo.lunarDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                Text(
                    text = lunarInfo.solarTerms,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = textColor
                )
            }

            // 编辑日程按钮
            Button(
                onClick = { showScheduleEditor.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceColor,
                    contentColor = textColor
                )
            ) {
                Text("编辑今日日程", color = textColor)
            }

            // 任务列表（按优先级排序）
            val sortedTasks = savedTasks.value.sortedBy { 
                when (it.priority) {
                    PriorityLevel.HIGH -> 0
                    PriorityLevel.MEDIUM -> 1
                    PriorityLevel.LOW -> 2
                    else -> 2
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "今日任务",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor
                )
                if (sortedTasks.isEmpty()) {
                    Text(
                        text = "暂无任务",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                        color = textColor
                    )
                } else {
                    sortedTasks.forEachIndexed { index, task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColor
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${task.hour}:${task.minute.toString().padStart(2, '0')} ${task.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = when (task.priority) {
                                            PriorityLevel.HIGH -> "高"
                                            PriorityLevel.MEDIUM -> "中"
                                            PriorityLevel.LOW -> "低"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when (task.priority) {
                                            PriorityLevel.HIGH -> Color.Red
                                            PriorityLevel.MEDIUM -> Color.Yellow
                                            PriorityLevel.LOW -> Color.Green
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val newTasks = savedTasks.value.toMutableList()
                                            newTasks.removeAt(newTasks.indexOf(task))
                                            savedTasks.value = newTasks
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode.value) Color(0xFFC62828) else Color(0xFFF8D7DA),
                                            contentColor = if (isDarkMode.value) Color.White else Color(0xFFC62828)
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("×", fontSize = MaterialTheme.typography.headlineSmall.fontSize)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "标签: ${task.tag}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (task.deadline) {
                                            Text(
                                                text = "紧急",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Red,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                        if (task.importance) {
                                            Text(
                                                text = "重要",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Blue,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 背景设置
    if (showBackgroundSettings.value) {
        ModalBottomSheet(
            onDismissRequest = { showBackgroundSettings.value = false },
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "背景设置",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 自定义背景开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "使用自定义背景",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Switch(
                        checked = useCustomBackground.value,
                        onCheckedChange = { 
                            useCustomBackground.value = it
                            if (!it) {
                                backgroundImage.value = null
                                BackgroundManager.clearBackground(context)
                            } else {
                                // 重新加载保存的背景图片
                                val bitmap = BackgroundManager.loadBackground(context)
                                if (bitmap != null) {
                                    backgroundImage.value = bitmap.asImageBitmap()
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // 从相册选择图片
                if (useCustomBackground.value) {
                    Button(
                        onClick = { checkPermissionAndOpenGallery() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = textColor
                        )
                    ) {
                        Text("从相册选择图片", color = textColor)
                    }

                    // 预览当前背景
                    if (backgroundImage.value != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(200.dp)
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = backgroundImage.value!!,
                                contentDescription = "当前背景",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // 关闭按钮
                Button(
                    onClick = { showBackgroundSettings.value = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode.value) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("关闭", color = Color.White)
                }
            }
        }
    }

    // 日程编辑器
    if (showScheduleEditor.value) {
        ScheduleEditor(
            onDismiss = { showScheduleEditor.value = false },
            onSave = { task ->
                // 保存任务
                savedTasks.value = savedTasks.value + task
                
                // 这里可以添加设置提醒的逻辑
                if (task.reminderEnabled) {
                    // 计算提醒时间
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, task.hour)
                    calendar.set(Calendar.MINUTE, task.minute)
                    calendar.set(Calendar.SECOND, 0)
                    
                    // 提前提醒的时间
                    val reminderTime = calendar.timeInMillis - (task.reminderMinutes * 60 * 1000)
                    
                    // 确保提醒时间在当前时间之后
                    if (reminderTime > System.currentTimeMillis()) {
                        println("设置提醒: 任务 ${task.title}, 时间 ${task.hour}:${task.minute.toString().padStart(2, '0')}, 提前 ${task.reminderMinutes} 分钟, 优先级: ${task.priority}, 重复类型: ${task.repeatType}")
                    } else {
                        println("提醒时间已过，无法设置提醒")
                    }
                }
                showScheduleEditor.value = false
            },
            isDarkMode = isDarkMode.value
        )
    }

    // 统计页面
    if (showStatistics.value) {
        ModalBottomSheet(
            onDismissRequest = { showStatistics.value = false },
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "时间分布统计",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 统计选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { 
                        // 统计本周数据
                        val now = Calendar.getInstance()
                        val startOfWeek = now.clone() as Calendar
                        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
                        startOfWeek.set(Calendar.MINUTE, 0)
                        startOfWeek.set(Calendar.SECOND, 0)
                        
                        val endOfWeek = now.clone() as Calendar
                        endOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        endOfWeek.set(Calendar.HOUR_OF_DAY, 23)
                        endOfWeek.set(Calendar.MINUTE, 59)
                        endOfWeek.set(Calendar.SECOND, 59)
                        
                        // 这里应该从数据库或存储中获取任务数据，现在使用空列表
                        // 实际应用中，应该根据日期范围筛选任务
                        val weekTasks = savedTasks.value
                        val weekStats = calculateStats(weekTasks)
                        scheduleStats.value = weekStats
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    )) {
                        Text("本周", color = textColor)
                    }
                    
                    Button(onClick = { 
                        // 统计本月数据
                        val now = Calendar.getInstance()
                        val startOfMonth = now.clone() as Calendar
                        startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
                        startOfMonth.set(Calendar.HOUR_OF_DAY, 0)
                        startOfMonth.set(Calendar.MINUTE, 0)
                        startOfMonth.set(Calendar.SECOND, 0)
                        
                        val endOfMonth = now.clone() as Calendar
                        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
                        endOfMonth.set(Calendar.HOUR_OF_DAY, 23)
                        endOfMonth.set(Calendar.MINUTE, 59)
                        endOfMonth.set(Calendar.SECOND, 59)
                        
                        // 这里应该从数据库或存储中获取任务数据，现在使用空列表
                        // 实际应用中，应该根据日期范围筛选任务
                        val monthTasks = savedTasks.value
                        val monthStats = calculateStats(monthTasks)
                        scheduleStats.value = monthStats
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaceColor,
                        contentColor = textColor
                    )) {
                        Text("本月", color = textColor)
                    }
                }

                // 统计结果
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (scheduleStats.value.isEmpty()) {
                        Text(
                            text = "暂无统计数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    } else {
                        scheduleStats.value.forEach { stats ->
                            Text(
                                text = "${stats.tag}: ${stats.count} 次",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        // 根据次数最多的活动类型给出评价
                        val maxStats = scheduleStats.value.maxByOrNull { it.count }
                        maxStats?.let {
                            val evaluation = when (it.tag) {
                                "摸鱼" -> "乐队贝斯手"
                                "休息" -> "最尊重自身生理需求的人类(?)"
                                "娱乐" -> "和甲基苯氨比促进多巴胺之人"
                                "工作" -> "别工作了喵"
                                else -> "平凡的一天"
                            }
                            Text(
                                text = "评价: ${evaluation}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                // 关闭按钮
                Button(
                    onClick = { showStatistics.value = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode.value) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("关闭", color = Color.White)
                }
            }
        }
    }
}}

@Preview(showBackground = true)
@Composable
fun CalendarAppPreview() {
    MyApplicationTheme {
        CalendarApp()
    }
}
