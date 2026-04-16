package com.tommy.todoapp.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubTasks(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubTask>
) {
    val completedByRule: Boolean
        get() = if (subtasks.isEmpty()) task.isCompleted else subtasks.all { it.isCompleted }

    val completedSubTaskCount: Int
        get() = subtasks.count { it.isCompleted }
}
