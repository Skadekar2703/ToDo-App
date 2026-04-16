package com.tommy.todoapp.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.tommy.todoapp.R
import com.tommy.todoapp.data.SubTask
import com.tommy.todoapp.data.TaskWithSubTasks

class TaskAdapter(
    private val onTaskChecked: (TaskWithSubTasks, Boolean) -> Unit,
    private val onSubTaskChecked: (TaskWithSubTasks, SubTask, Boolean) -> Unit,
    private val onAddSubTask: (TaskWithSubTasks) -> Unit
) : ListAdapter<TaskWithSubTasks, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckBox: MaterialCheckBox = itemView.findViewById(R.id.taskCheckBox)
        private val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        private val subTaskProgressText: TextView = itemView.findViewById(R.id.subTaskProgressText)
        private val subTaskProgress: LinearProgressIndicator = itemView.findViewById(R.id.subTaskProgress)
        private val subTaskContainer: LinearLayout = itemView.findViewById(R.id.subTaskContainer)
        private val addSubTaskButton: TextView = itemView.findViewById(R.id.addSubTaskButton)

        fun bind(taskWithSubTasks: TaskWithSubTasks) {
            val isComplete = taskWithSubTasks.completedByRule
            itemView.setBackgroundResource(
                if (isComplete) R.drawable.bg_completed_card else R.drawable.bg_card
            )

            taskTitle.text = taskWithSubTasks.task.title
            taskTitle.paintFlags = if (isComplete) {
                taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            taskCheckBox.setOnCheckedChangeListener(null)
            taskCheckBox.isChecked = isComplete
            taskCheckBox.setOnCheckedChangeListener { _, checked ->
                onTaskChecked(taskWithSubTasks, checked)
            }

            val subTasks = taskWithSubTasks.subtasks
            if (subTasks.isEmpty()) {
                subTaskProgress.visibility = View.GONE
                subTaskProgressText.text = if (isComplete) "Completed" else "No subtasks yet"
            } else {
                val progress = (taskWithSubTasks.completedSubTaskCount * 100) / subTasks.size
                subTaskProgress.visibility = View.VISIBLE
                subTaskProgress.setProgressCompat(progress, true)
                subTaskProgressText.text =
                    "${taskWithSubTasks.completedSubTaskCount}/${subTasks.size} items"
            }

            renderSubTasks(taskWithSubTasks)
            addSubTaskButton.setOnClickListener { onAddSubTask(taskWithSubTasks) }
        }

        private fun renderSubTasks(taskWithSubTasks: TaskWithSubTasks) {
            subTaskContainer.removeAllViews()
            val inflater = LayoutInflater.from(subTaskContainer.context)
            taskWithSubTasks.subtasks.forEach { subTask ->
                val row = inflater.inflate(R.layout.item_subtask, subTaskContainer, false)
                val checkBox = row.findViewById<MaterialCheckBox>(R.id.subTaskCheckBox)
                val title = row.findViewById<TextView>(R.id.subTaskTitle)
                title.text = subTask.title
                title.paintFlags = if (subTask.isCompleted) {
                    title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = subTask.isCompleted
                checkBox.setOnCheckedChangeListener { _, checked ->
                    onSubTaskChecked(taskWithSubTasks, subTask, checked)
                }
                subTaskContainer.addView(row)
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskWithSubTasks>() {
        override fun areItemsTheSame(oldItem: TaskWithSubTasks, newItem: TaskWithSubTasks): Boolean {
            return oldItem.task.id == newItem.task.id
        }

        override fun areContentsTheSame(oldItem: TaskWithSubTasks, newItem: TaskWithSubTasks): Boolean {
            return oldItem == newItem
        }
    }
}
