package com.example.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.final_project.R
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class CustomCalendarView @JvmOverloads constructor(

    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val importantDates = mutableSetOf<Calendar>()

    private val calendar: Calendar = Calendar.getInstance()
    private lateinit var monthYearTextView: TextView
    private lateinit var prevButton: TextView
    private lateinit var nextButton: TextView
    private lateinit var daysGrid: GridLayout

    private var onDateChangeListener: ((year: Int, month: Int, dayOfMonth: Int) -> Unit)? = null
    private var onMonthChangeListener: ((year: Int, month: Int) -> Unit)? = null
    private var onHeaderClickListener: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.custom_calendar_view, this, true)

        monthYearTextView = findViewById(R.id.monthYearTextView)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        daysGrid = findViewById(R.id.daysGrid)

        setupListeners()
        updateCalendarView()
    }
    fun setImportantDates(dates: Set<Long>) {
        importantDates.clear()
        dates.forEach { dateInMillis ->
            val calendar = Calendar.getInstance().apply { timeInMillis = dateInMillis }
            importantDates.add(calendar)
        }
        updateDaysGrid() // Refresh the grid to reflect changes
    }

    private fun setupListeners() {
        prevButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarView()
        }

        nextButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarView()
        }

        monthYearTextView.setOnClickListener {
            onHeaderClickListener?.invoke()
        }
    }

    private fun updateCalendarView() {
        updateHeader()
        updateDaysGrid()
        onMonthChangeListener?.invoke(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    private fun updateHeader() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearTextView.text = dateFormat.format(calendar.time)
    }

    private fun updateDaysGrid() {
        daysGrid.removeAllViews()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

        // Add day names
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (dayName in dayNames) {
            addDayToGrid(dayName, isHeader = true)
        }

        // Add empty cells before the first day of the month
        for (i in 1 until firstDayOfWeek) {
            addDayToGrid("")
        }

        // Add days of the month
        for (day in 1..daysInMonth) {
            addDayToGrid(day.toString(), day == calendar.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun addDayToGrid(text: String, isSelected: Boolean = false, isHeader: Boolean = false) {
        val dayView = LayoutInflater.from(context).inflate(R.layout.calendar_day_view, daysGrid, false) as TextView
        dayView.text = text

        // Check if the current day is an important date
        if (!isHeader && text.isNotEmpty()) {
            val day = text.toInt()
            val dayCalendar = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            if (importantDates.any { it.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                        it.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
                        it.get(Calendar.DAY_OF_MONTH) == dayCalendar.get(Calendar.DAY_OF_MONTH) }) {
                dayView.setBackgroundResource(R.drawable.important_date_background) // Set background for important dates
                dayView.setTypeface(null, android.graphics.Typeface.BOLD) // Make the text bold
            }
        }

        if (isSelected) {
            dayView.setBackgroundResource(R.drawable.selected_day_background)
        }
        if (isHeader) {
            dayView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        if (!isHeader && text.isNotEmpty()) {
            dayView.setOnClickListener {
                calendar.set(Calendar.DAY_OF_MONTH, text.toInt())
                updateDaysGrid()
                onDateChangeListener?.invoke(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
        daysGrid.addView(dayView)
    }


    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        calendar.set(year, month, dayOfMonth)
        updateCalendarView()
        onDateChangeListener?.invoke(year, month, dayOfMonth)
    }

    fun setOnDateChangeListener(listener: (year: Int, month: Int, dayOfMonth: Int) -> Unit) {
        onDateChangeListener = listener
    }

    fun setOnMonthChangeListener(listener: (year: Int, month: Int) -> Unit) {
        onMonthChangeListener = listener
    }

    fun setOnHeaderClickListener(listener: () -> Unit) {
        onHeaderClickListener = listener
    }
}