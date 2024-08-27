package com.example.app

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.example.server.CalendarEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.Calendar

class CalendarFragment : Fragment() {
    private lateinit var calendarView: CalendarView
    private lateinit var tokenManager: TokenManager
    private val events = mutableListOf<CalendarEvent>()

    private val client = HttpClient(Android)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        tokenManager = TokenManager(requireContext())

        loadCalendarEvents()

        return view
    }

    private fun loadCalendarEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val response = client.get("http://10.0.2.2:8080/calendar-events") {
                    header("Authorization", "Bearer $token")
                }

                if (response.status.isSuccess()) {
                    val responseBody = response.bodyAsText()
                    events.clear()
                    events.addAll(parseCalendarEvents(responseBody))
                    updateCalendarView()
                } else {
                    throw Exception("Failed to fetch events: ${response.status}")
                }
            } catch (e: Exception) {
                Log.e("CalendarFragment", "Error loading calendar events: ${e.message}")
                showToast("Failed to load calendar events: ${e.message}")
            }
        }
    }

    private fun parseCalendarEvents(responseBody: String): List<CalendarEvent> {
        val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
        return jsonArray.map { jsonElement ->
            val jsonObject = jsonElement.jsonObject
            CalendarEvent(
                id = jsonObject["id"]?.jsonPrimitive?.int ?: 0,
                userId = jsonObject["userId"]?.jsonPrimitive?.int ?: 0,
                contractId = jsonObject["contractId"]?.jsonPrimitive?.int ?: 0,
                title = jsonObject["title"]?.jsonPrimitive?.content ?: "",
                date = jsonObject["date"]?.jsonPrimitive?.long ?: 0L
            )
        }
    }

    private fun updateCalendarView() {
        events.forEach { event ->
            addEventMarker(event.date)
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val eventsForDay = events.filter {
                val eventCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
                eventCalendar.get(Calendar.YEAR) == year &&
                        eventCalendar.get(Calendar.MONTH) == month &&
                        eventCalendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth
            }
            showEventsForDay(eventsForDay)
        }
    }

    private fun addEventMarker(date: Long) {
        // This is a simplified version. You might need to implement a custom decorator
        // for more advanced visual representation of events
        calendarView.setDate(date, true, true)
    }

    private fun showEventsForDay(events: List<CalendarEvent>) {
        if (events.isEmpty()) {
            showToast("No events for this day")
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

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}