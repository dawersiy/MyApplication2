package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.example.myapplication.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedule WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("""
        SELECT * FROM schedule
        WHERE startTime BETWEEN :start AND :end
        ORDER BY startTime ASC
    """)
    fun getSchedulesByRange(start: Long, end: Long): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT * FROM schedule
        WHERE date(startTime / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
        ORDER BY startTime ASC
    """)
    fun getTodaySchedules(): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT * FROM schedule
        WHERE startTime BETWEEN :start AND :end
        GROUP BY tag
        ORDER BY tag ASC
    """)
    fun getSchedulesByTagInRange(start: Long, end: Long): Flow<List<ScheduleEntity>>
}