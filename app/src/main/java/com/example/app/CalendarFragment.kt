package com.example.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.example.server.CalendarEvent
import com.example.server.CalendarEventDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class CalendarFragment : Fragment() {
    private lateinit var calendarView: CalendarView
    private lateinit var calendarEventDao: CalendarEventDao
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        calendarEventDao = CalendarEventDao()
        tokenManager = TokenManager(requireContext())

        loadCalendarEvents()

        return view
    }

    private fun loadCalendarEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = getCurrentUserId()
                val events = withContext(Dispatchers.IO) {
                    calendarEventDao.findAllByUserId(userId)
                }

                // Add event markers to calendar
                events.forEach { event ->
                    addEventMarker(event.date)
                }

                // Set up date change listener
                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.timeInMillis

                    val eventsForDay = events.filter { it.date == selectedDate }
                    showEventsForDay(eventsForDay)
                }
            } catch (e: Exception) {
                // Handle error (e.g., show an error message to the user)
            }
        }
    }

    private fun getCurrentUserId(): Int {
        // Implement this method to get the current user's ID from your authentication system
        val token = tokenManager.getToken() ?: throw Exception("No token found")
        // Decode the token and extract the user ID
        // This is a placeholder implementation
        return 1 // Replace with actual user ID extraction
    }

    private fun addEventMarker(date: Long) {
        // This is a simplified version. You might need to implement a custom decorator
        // for more advanced visual representation of events
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendarView.setDate(date, true, true)
    }

    private fun showEventsForDay(events: List<CalendarEvent>) {
        if (events.isEmpty()) {
            return
        }

        val message = events.joinToString("\n") { it.title }
        AlertDialog.Builder(requireContext())
            .setTitle("Events for ${formatDate(events.first().date)}")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatDate(date: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
    }
}