package com.tommy.todoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tommy.todoapp.data.AppDatabase
import com.tommy.todoapp.data.SubTask
import com.tommy.todoapp.data.Task
import com.tommy.todoapp.data.TaskWithSubTasks
import com.tommy.todoapp.ui.TaskAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val taskDao by lazy { AppDatabase.getDatabase(this).taskDao() }
    private val todayDate by lazy { startOfDay(System.currentTimeMillis()) }
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var progressSummary: TextView
    private lateinit var progressBadge: TextView
    private lateinit var progressPercent: TextView
    private lateinit var dailyProgress: CircularProgressIndicator
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressSummary = findViewById(R.id.progressSummary)
        progressBadge = findViewById(R.id.progressBadge)
        progressPercent = findViewById(R.id.progressPercent)
        dailyProgress = findViewById(R.id.dailyProgress)
        emptyText = findViewById(R.id.emptyText)

        findViewById<TextView>(R.id.dateText).text =
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(todayDate)
        findViewById<TextView>(R.id.openCalendarButton).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        setupTaskList()
        findViewById<FloatingActionButton>(R.id.addTaskFab).setOnClickListener { showAddTaskDialog() }
        observeToday()
    }

    private fun setupTaskList() {
        taskAdapter = TaskAdapter(
            onTaskChecked = { taskWithSubTasks, checked ->
                lifecycleScope.launch { setTaskCompletion(taskWithSubTasks, checked) }
            },
            onSubTaskChecked = { taskWithSubTasks, subTask, checked ->
                lifecycleScope.launch { setSubTaskCompletion(taskWithSubTasks, subTask, checked) }
            },
            onAddSubTask = { showAddSubTaskDialog(it) }
        )
        findViewById<RecyclerView>(R.id.taskRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun observeToday() {
        lifecycleScope.launch {
            taskDao.observeTasksForDate(todayDate).collectLatest { tasks ->
                taskAdapter.submitList(tasks)
                emptyText.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                updateProgress(tasks)
            }
        }
    }

    private fun updateProgress(tasks: List<TaskWithSubTasks>) {
        val total = tasks.size
        val completed = tasks.count { it.completedByRule }
        val percent = if (total == 0) 0 else (completed * 100) / total
        progressSummary.text = "$completed of $total tasks completed"
        progressPercent.text = "$percent%"
        dailyProgress.setProgressCompat(percent, true)
        progressBadge.text = when {
            total == 0 -> "Ready"
            percent == 100 -> "Day Complete"
            percent >= 50 -> "Great Pace"
            else -> "Keep Going"
        }
    }

    private fun showAddTaskDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 10, 32, 0)
        }
        val titleInput = EditText(this).apply {
            hint = "Task title"
            setSingleLine(false)
            background = getDrawable(R.drawable.bg_input)
        }
        val subTasksInput = EditText(this).apply {
            hint = "Subtasks, separated by commas"
            setSingleLine(false)
            background = getDrawable(R.drawable.bg_input)
        }
        container.addView(titleInput)
        container.addView(subTasksInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 18 })

        MaterialAlertDialogBuilder(this)
            .setTitle("Add task")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = titleInput.text.toString().trim()
                        if (title.isBlank()) {
                            Toast.makeText(this@MainActivity, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val subTasks = subTasksInput.text.toString()
                            .split(",", "\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        lifecycleScope.launch {
                            val taskId = taskDao.insertTask(Task(title = title, date = todayDate)).toInt()
                            subTasks.forEach { subTitle ->
                                taskDao.insertSubTask(SubTask(taskId = taskId, title = subTitle))
                            }
                        }
                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun showAddSubTaskDialog(taskWithSubTasks: TaskWithSubTasks) {
        val input = EditText(this).apply {
            hint = "Subtask title"
            background = getDrawable(R.drawable.bg_input)
            setPadding(32, 18, 32, 18)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Add subtask")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = input.text.toString().trim()
                        if (title.isBlank()) {
                            Toast.makeText(this@MainActivity, "Subtask title cannot be empty", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            taskDao.insertSubTask(SubTask(taskId = taskWithSubTasks.task.id, title = title))
                            taskDao.updateTaskCompletion(taskWithSubTasks.task.id, false)
                        }
                        dismiss()
                    }
                }
            }
            .show()
    }

    private suspend fun setTaskCompletion(taskWithSubTasks: TaskWithSubTasks, checked: Boolean) {
        val taskId = taskWithSubTasks.task.id
        if (taskWithSubTasks.subtasks.isEmpty()) {
            taskDao.updateTaskCompletion(taskId, checked)
        } else {
            taskDao.updateAllSubTasksForTask(taskId, checked)
            taskDao.updateTaskCompletion(taskId, checked)
        }
    }

    private suspend fun setSubTaskCompletion(
        taskWithSubTasks: TaskWithSubTasks,
        subTask: SubTask,
        checked: Boolean
    ) {
        taskDao.updateSubTaskCompletion(subTask.id, checked)
        val hasSubTasks = taskDao.getSubTaskCount(taskWithSubTasks.task.id) > 0
        val hasPending = taskDao.getPendingSubTaskCount(taskWithSubTasks.task.id) > 0
        taskDao.updateTaskCompletion(taskWithSubTasks.task.id, hasSubTasks && !hasPending)
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
