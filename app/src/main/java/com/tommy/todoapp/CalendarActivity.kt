package com.tommy.todoapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tommy.todoapp.data.AppDatabase
import com.tommy.todoapp.data.TaskWithSubTasks
import com.tommy.todoapp.ui.CalendarDay
import com.tommy.todoapp.ui.CalendarDayAdapter
import com.tommy.todoapp.ui.DayStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {
    private val taskDao by lazy { AppDatabase.getDatabase(this).taskDao() }
    private val calendarAdapter = CalendarDayAdapter()
    private lateinit var monthStats: TextView
    private lateinit var monthSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendarRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        monthStats = findViewById(R.id.monthStats)
        monthSubtitle = findViewById(R.id.monthSubtitle)
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }

        val month = Calendar.getInstance()
        findViewById<TextView>(R.id.monthText).text =
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)

        findViewById<RecyclerView>(R.id.calendarRecyclerView).apply {
            layoutManager = GridLayoutManager(this@CalendarActivity, 7)
            adapter = calendarAdapter
        }

        observeMonth(month)
    }

    private fun observeMonth(month: Calendar) {
        val monthStart = monthStart(month)
        val monthEnd = monthEnd(month)
        lifecycleScope.launch {
            taskDao.observeTasksForRange(monthStart, monthEnd).collectLatest { tasks ->
                val days = buildCalendarDays(month, tasks)
                calendarAdapter.submitList(days)
                updateStats(days)
            }
        }
    }

    private fun buildCalendarDays(month: Calendar, tasks: List<TaskWithSubTasks>): List<CalendarDay> {
        val taskMap = tasks.groupBy { it.task.date }
        val firstDay = month.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        val leadingBlanks = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val today = startOfDay(System.currentTimeMillis())
        val days = mutableListOf<CalendarDay>()

        repeat(leadingBlanks) {
            days.add(CalendarDay(dayOfMonth = 0, status = DayStatus.EMPTY))
        }

        val maxDay = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDay) {
            val current = firstDay.clone() as Calendar
            current.set(Calendar.DAY_OF_MONTH, day)
            val date = startOfDay(current.timeInMillis)
            val dayTasks = taskMap[date].orEmpty()
            val status = when {
                dayTasks.isEmpty() -> DayStatus.EMPTY
                dayTasks.all { it.completedByRule } -> DayStatus.COMPLETE
                else -> DayStatus.INCOMPLETE
            }
            days.add(CalendarDay(dayOfMonth = day, status = status, isToday = date == today))
        }
        return days
    }

    private fun updateStats(days: List<CalendarDay>) {
        val trackedDays = days.filter { it.dayOfMonth != 0 && it.status != DayStatus.EMPTY }
        val completeDays = trackedDays.count { it.status == DayStatus.COMPLETE }
        val percent = if (trackedDays.isEmpty()) 0 else (completeDays * 100) / trackedDays.size
        monthStats.text = "$completeDays complete days"
        monthSubtitle.text = "$percent% of tracked days completed"
    }

    private fun monthStart(month: Calendar): Long {
        return (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun monthEnd(month: Calendar): Long {
        return (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
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
