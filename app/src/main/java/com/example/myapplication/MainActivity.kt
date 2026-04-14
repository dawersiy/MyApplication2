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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.myapplication.notification.NotificationHelper
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.CalendarViewModel
import com.example.myapplication.viewmodel.CalendarViewModelFactory
import com.example.myapplication.model.Task
import com.example.myapplication.model.PriorityLevel
import com.example.myapplication.model.CalendarDay
import com.example.myapplication.utils.BackgroundManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.appwidget.AppWidgetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CalendarViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this, CalendarViewModelFactory(this))[CalendarViewModel::class.java]

        // 初始化通知渠道
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        setContent {
            MyApplicationTheme {
                CalendarApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    val calendar = remember { Calendar.getInstance() }
    val currentDate = remember { mutableStateOf(calendar.time) }
    val selectedDate = remember { mutableStateOf(calendar.time) }
    val today = Calendar.getInstance()
    val showScheduleEditor = remember { mutableStateOf(false) }
    val showSettings = remember { mutableStateOf(false) }
    val showStatistics = remember { mutableStateOf(false) }
    val isDarkMode = remember { mutableStateOf(false) }
    val isWidgetEnabled = remember { mutableStateOf(true) }
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
        
        // 初始化小组件启用状态,注意这里可能会有问题desuwa
        val packageManager = context.packageManager
        val componentName = ComponentName(context, com.example.myapplication.ui.widget.TodayScheduleWidgetProvider::class.java)
        val currentState = packageManager.getComponentEnabledSetting(componentName)
        isWidgetEnabled.value = (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MM月", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy年", Locale.getDefault())

    // 计算选中日期的农历信息
    val selectedCalendar = Calendar.getInstance()
    selectedCalendar.time = selectedDate.value
    val lunarInfo = viewModel.getLunarInfo(selectedCalendar)

    // 颜色设置
    val backgroundColor = if (isDarkMode.value) Color.Black else MaterialTheme.colorScheme.background
    val textColor = if (isDarkMode.value) Color.White else MaterialTheme.colorScheme.onBackground
    val surfaceColor = if (isDarkMode.value) Color.DarkGray else Color(0xFFF5F5DC) // 淡米色
    val primaryColor = if (isDarkMode.value) Color.Blue else MaterialTheme.colorScheme.primary

    // 观察任务数据
    val tasks by viewModel.tasks.collectAsState(emptyList())



    // 加载任务
    LaunchedEffect(selectedDate.value) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = dateFormat.format(selectedDate.value)
        viewModel.loadTasksByDate(selectedDateStr)
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
                    bitmap?.let { bmp ->
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
                title = { Text("日历", color = Color.Black) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Button(onClick = { showSettings.value = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceColor,
                                contentColor = textColor
                            ),
                            modifier = Modifier.graphicsLayer(alpha = 0.7f)) {
                            Text("设置", color = textColor)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = 0.3f), // 淡化背景图片
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
                val days = viewModel.generateCalendarDays(calendar)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(days) {
                        CalendarDay(
                            day = it,
                            isToday = viewModel.isToday(it, today),
                            isCurrentMonth = it.isCurrentMonth,
                            isSelected = viewModel.isDateEqual(it, selectedDate.value, calendar),
                            onClick = {
                                selectedDate.value = viewModel.getSelectedDate(it, calendar)
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
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = lunarInfo.lunarDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    if (lunarInfo.solarTerms.isNotEmpty()) {
                        Text(
                            text = " (${lunarInfo.solarTerms})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                // 编辑日程按钮
                Button(
                    onClick = { showScheduleEditor.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode.value) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
                    Text("编辑${dateFormat.format(selectedDate.value)}日程", color = textColor)
                }

                // 任务列表（按优先级排序）
                val sortedTasks = tasks.sortedBy {
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
                                                else -> "低"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when (task.priority) {
                                                PriorityLevel.HIGH -> Color.Red
                                                PriorityLevel.MEDIUM -> Color.Yellow
                                                PriorityLevel.LOW -> Color.Green
                                                else -> Color.Green
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Button(
                                            onClick = {
                                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                val selectedDateStr = dateFormat.format(selectedDate.value)
                                                viewModel.deleteTask(task.id, selectedDateStr)
                                                // 手动刷新小组件
                                                updateScheduleWidget(context)
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
                                            if (task.reminderEnabled) {
                                                Text(
                                                    text = "提前 ${task.reminderMinutes} 分钟提醒",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textColor,
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
    }

    // 日程编辑器
    if (showScheduleEditor.value) {
        ScheduleEditor(
            onDismiss = { showScheduleEditor.value = false },
            onSave = { task ->
                // 保存任务
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDateStr = dateFormat.format(selectedDate.value)
                viewModel.addTask(task, selectedDateStr)

                // 设置提醒
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

                        // 实际设置提醒
                        val notificationHelper = NotificationHelper(context)
                        val notificationId = System.currentTimeMillis().toInt()
                        notificationHelper.showScheduleReminderNotification(
                            "日程提醒",
                            "${task.title} - ${task.hour}:${task.minute.toString().padStart(2, '0')}",
                            notificationId
                        )

                        // 显示 Toast 消息提示用户
                        Toast.makeText(context, "提醒已设置，将在任务开始前 ${task.reminderMinutes} 分钟通知您", Toast.LENGTH_SHORT).show()
                    } else {
                        println("提醒时间已过，无法设置提醒")
                    }
                }

                // 更新小组件
                updateScheduleWidget(context)

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

                        // 计算本周任务统计
                        val weekTasks = tasks
                        viewModel.calculateStats(weekTasks)
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

                        // 计算本月任务统计
                        val monthTasks = tasks
                        viewModel.calculateStats(monthTasks)
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
                    if ((viewModel.scheduleStats.value ?: emptyList()).isEmpty()) {
                        Text(
                            text = "暂无统计数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    } else {
                        (viewModel.scheduleStats.value ?: emptyList()).forEach { stats ->
                            Text(
                                text = "${stats.tag}: ${stats.count} 次",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // 根据次数最多的活动类型给出评价
                        val maxStats = (viewModel.scheduleStats.value ?: emptyList()).maxByOrNull { it.count }
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

    // 设置界面
    if (showSettings.value) {
        ModalBottomSheet(
            onDismissRequest = { showSettings.value = false },
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
                    text = "设置",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 小组件开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "启用小组件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Switch(
                        checked = isWidgetEnabled.value,
                        onCheckedChange = {
                            isWidgetEnabled.value = it
                            updateWidgetEnabledState(context, it)
                        },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // 背景设置
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "背景设置",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 自定义背景开关
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
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
                                    .padding(bottom = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode.value) Color.DarkGray else Color.LightGray,
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
                                        .size(150.dp)
                                        .padding(bottom = 12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    }
                }

                // 统计功能
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "时间分布统计",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 统计选项
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                val weekTasks = tasks
                                viewModel.calculateStats(weekTasks)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode.value) Color.DarkGray else Color.LightGray,
                                contentColor = textColor
                            )) {
                                Text("本周", color = textColor)
                            }

                            Button(onClick = {
                                val monthTasks = tasks
                                viewModel.calculateStats(monthTasks)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode.value) Color.DarkGray else Color.LightGray,
                                contentColor = textColor
                            )) {
                                Text("本月", color = textColor)
                            }
                        }

                        // 统计结果
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            if ((viewModel.scheduleStats.value ?: emptyList()).isEmpty()) {
                                Text(
                                    text = "暂无统计数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            } else {
                                (viewModel.scheduleStats.value ?: emptyList()).forEach { stats ->
                                    Text(
                                        text = "${stats.tag}: ${stats.count} 次",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // 根据次数最多的活动类型给出评价
                                val maxStats = (viewModel.scheduleStats.value ?: emptyList()).maxByOrNull { it.count }
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
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 关闭按钮
                Button(
                    onClick = { showSettings.value = false },
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
}

@Composable
fun CalendarDay(
    day: CalendarDay,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = when {
        isSelected -> if (isDarkMode) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary
        isToday -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFFE3F2FD) // 今天蓝色背景
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isCurrentMonth -> if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground
        else -> if (isDarkMode) Color.Gray else Color.LightGray
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = day.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
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
        if (day.isHoliday && day.holidayName.isNotEmpty()) {
            Text(
                text = day.holidayName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        }
    }
}

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
                    // 小时滚轮
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text("小时", style = MaterialTheme.typography.bodySmall, color = textColor)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Button(
                                onClick = { hour.value = (hour.value - 1 + 24) % 24 },
                                modifier = Modifier.size(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceColor,
                                    contentColor = textColor
                                )
                            ) {
                                Text("↑", fontSize = 12.sp)
                            }
                            Text(
                                text = hour.value.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(40.dp)
                            )
                            Button(
                                onClick = { hour.value = (hour.value + 1) % 24 },
                                modifier = Modifier.size(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceColor,
                                    contentColor = textColor
                                )
                            ) {
                                Text("↓", fontSize = 12.sp)
                            }
                        }
                    }
                    Text(text = ":", style = MaterialTheme.typography.headlineSmall, color = textColor)
                    // 分钟滚轮
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("分钟", style = MaterialTheme.typography.bodySmall, color = textColor)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Button(
                                onClick = { minute.value = (minute.value - 1 + 60) % 60 },
                                modifier = Modifier.size(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceColor,
                                    contentColor = textColor
                                )
                            ) {
                                Text("↑", fontSize = 12.sp)
                            }
                            Text(
                                text = minute.value.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(40.dp)
                            )
                            Button(
                                onClick = { minute.value = (minute.value + 1) % 60 },
                                modifier = Modifier.size(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceColor,
                                    contentColor = textColor
                                )
                            ) {
                                Text("↓", fontSize = 12.sp)
                            }
                        }
                    }
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
                    text = "标签",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    tags.forEach { tag ->
                        Button(
                            onClick = { selectedTag.value = tag },
                            modifier = Modifier.padding(2.dp), // 缩小间距
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
                    val repeatOptions = listOf(
                        Pair("NONE", "无"),
                        Pair("DAILY", "每日"),
                        Pair("WEEKLY", "每周")
                    )
                    repeatOptions.forEach { (value, label) ->
                        Button(
                            onClick = { repeatType.value = value },
                            modifier = Modifier
                                .padding(2.dp) // 缩小间距
                                .width(60.dp), // 统一按钮宽度
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (repeatType.value == value) (if (isDarkMode) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary) else surfaceColor,
                                contentColor = if (repeatType.value == value) Color.White else textColor
                            )
                        ) {
                            Text(label, 
                                color = if (repeatType.value == value) Color.White else textColor,
                                fontSize = 12.sp // 缩小字体
                            )
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
                                reminderEnabled = reminderEnabled.value,
                                reminderMinutes = reminderMinutes.value,
                                repeatType = repeatType.value,
                                priority = priority
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
                    Text("保存", color = Color.White)
                }
            }
        }
    }
}

// 更新日程小组件
fun updateScheduleWidget(context: android.content.Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, com.example.myapplication.ui.widget.TodayScheduleWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    // 发送更新广播
    val intent = android.content.Intent(context, com.example.myapplication.ui.widget.TodayScheduleWidgetProvider::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
    context.sendBroadcast(intent)
}

// 更新小组件启用状态
fun updateWidgetEnabledState(context: android.content.Context, enabled: Boolean) {
    val packageManager = context.packageManager
    val componentName = ComponentName(context, com.example.myapplication.ui.widget.TodayScheduleWidgetProvider::class.java)
    
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    
    packageManager.setComponentEnabledSetting(
        componentName,
        newState,
        PackageManager.DONT_KILL_APP
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarAppPreview() {
    val context = LocalContext.current
    val viewModel = ViewModelProvider(
        context as androidx.lifecycle.ViewModelStoreOwner,
        CalendarViewModelFactory(context)
    )[CalendarViewModel::class.java]
    MyApplicationTheme {
        CalendarApp(viewModel)
    }
}
