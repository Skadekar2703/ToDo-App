package com.tommy.todoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id DESC")
    fun observeTasksForDate(date: Long): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, id ASC")
    fun observeTasksForRange(startDate: Long, endDate: Long): Flow<List<TaskWithSubTasks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask): Long

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subTaskId")
    suspend fun updateSubTaskCompletion(subTaskId: Int, isCompleted: Boolean)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE taskId = :taskId")
    suspend fun updateAllSubTasksForTask(taskId: Int, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubTaskCount(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isCompleted = 0")
    suspend fun getPendingSubTaskCount(taskId: Int): Int
}
