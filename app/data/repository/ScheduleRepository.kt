package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val database: AppDatabase) {
    suspend fun insertSchedule(schedule: ScheduleEntity): Long {
        return database.scheduleDao().insert(schedule)
    }

    suspend fun updateSchedule(schedule: ScheduleEntity) {
        database.scheduleDao().update(schedule)
    }

    suspend fun deleteSchedule(schedule: ScheduleEntity) {
        database.scheduleDao().delete(schedule)
    }

    fun getSchedulesByRange(start: Long, end: Long): Flow<List<ScheduleEntity>> {
        return database.scheduleDao().getSchedulesByRange(start, end)
    }

    fun getTodaySchedules(): Flow<List<ScheduleEntity>> {
        return database.scheduleDao().getTodaySchedules()
    }

    suspend fun getScheduleById(id: Long): ScheduleEntity? {
        return database.scheduleDao().getById(id)
    }

    fun getSchedulesByTagInRange(start: Long, end: Long): Flow<List<ScheduleEntity>> {
        return database.scheduleDao().getSchedulesByTagInRange(start, end)
    }
}