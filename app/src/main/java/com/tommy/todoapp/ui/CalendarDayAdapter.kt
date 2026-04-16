package com.tommy.todoapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tommy.todoapp.R

enum class DayStatus {
    EMPTY,
    COMPLETE,
    INCOMPLETE
}

data class CalendarDay(
    val dayOfMonth: Int,
    val status: DayStatus,
    val isToday: Boolean = false
)

class CalendarDayAdapter :
    ListAdapter<CalendarDay, CalendarDayAdapter.CalendarDayViewHolder>(CalendarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)

        fun bind(day: CalendarDay) {
            val context = dayNumber.context
            if (day.dayOfMonth == 0) {
                dayNumber.text = ""
                dayNumber.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                return
            }

            dayNumber.text = day.dayOfMonth.toString()
            val background = when {
                day.isToday -> R.drawable.bg_calendar_today
                day.status == DayStatus.COMPLETE -> R.drawable.bg_calendar_complete
                day.status == DayStatus.INCOMPLETE -> R.drawable.bg_calendar_incomplete
                else -> R.drawable.bg_calendar_neutral
            }
            val textColor = when {
                day.isToday -> R.color.on_primary
                day.status == DayStatus.COMPLETE -> R.color.on_secondary_container
                day.status == DayStatus.INCOMPLETE -> R.color.on_error_container
                else -> R.color.on_surface
            }
            dayNumber.setBackgroundResource(background)
            dayNumber.setTextColor(ContextCompat.getColor(context, textColor))
        }
    }

    private class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.dayOfMonth == newItem.dayOfMonth
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}
