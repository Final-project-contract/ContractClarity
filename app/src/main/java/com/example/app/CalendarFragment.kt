package com.example.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {
    private lateinit var customCalendarView: CustomCalendarView
    private lateinit var eventsTextView: TextView
    private lateinit var tokenManager: TokenManager
    private val events = mutableListOf<CalendarEvent>()
    private val calendar = Calendar.getInstance()

    private val client = HttpClient(Android)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        customCalendarView = view.findViewById(R.id.customCalendarView)
        eventsTextView = view.findViewById(R.id.eventsTextView)
        tokenManager = TokenManager(requireContext())

        setupCalendarView()
        loadCalendarEvents()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadCalendarEvents()
    }

    private fun setupCalendarView() {
        customCalendarView.setOnDateChangeListener { year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            showEventsForDay(getEventsForSelectedDate())
        }

        customCalendarView.setOnMonthChangeListener { year, month ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            loadCalendarEvents()
        }

        customCalendarView.setOnHeaderClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                customCalendarView.setDate(year, month, dayOfMonth)
                calendar.set(year, month, dayOfMonth)
                showEventsForDay(getEventsForSelectedDate())
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

//    private fun loadCalendarEvents() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val token = tokenManager.getToken() ?: throw Exception("No token found")
//                val response = client.get("http://10.0.2.2:8080/calendar-events") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (response.status.isSuccess()) {
//                    val responseBody = response.bodyAsText()
//                    events.clear()
//                    events.addAll(parseCalendarEvents(responseBody))
//                    Log.d("CalendarFragment", "Loaded ${events.size} events")
//                    showEventsForDay(getEventsForSelectedDate())
//                } else {
//                    throw Exception("Failed to fetch events: ${response.status}")
//                }
//            } catch (e: Exception) {
//                Log.e("CalendarFragment", "Error loading calendar events: ${e.message}")
//                showToast("Failed to load calendar events: ${e.message}")
//            }
//        }
//    }
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
                Log.d("CalendarFragment", "Loaded ${events.size} events")

                // Extract important dates and update the calendar view
                val importantDates = events.map { it.date }.toSet()
                customCalendarView.setImportantDates(importantDates)

                showEventsForDay(getEventsForSelectedDate())
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

    private fun getEventsForSelectedDate(): List<CalendarEvent> {
        val startOfDay = calendar.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = startOfDay.clone() as Calendar
        endOfDay.add(Calendar.DAY_OF_MONTH, 1)

        val filteredEvents = events.filter {
            it.date >= startOfDay.timeInMillis && it.date < endOfDay.timeInMillis
        }
        Log.d("CalendarFragment", "Filtered ${filteredEvents.size} events for selected date")
        return filteredEvents
    }

    private fun showEventsForDay(events: List<CalendarEvent>) {
        Log.d("CalendarFragment", "Showing ${events.size} events for selected date")
        if (events.isEmpty()) {
            eventsTextView.text = "No events for this day"
            return
        }

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val eventDate = dateFormat.format(calendar.time)
        val eventList = events.joinToString("\n") { "• ${it.title}" }
        val message = "Events for $eventDate:\n$eventList"
        eventsTextView.text = message
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}