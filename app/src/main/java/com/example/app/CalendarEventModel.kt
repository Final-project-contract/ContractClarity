package com.example.app

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEventModel(
    val id: Int = 0,
    val userId: Int,
    val contractId: Int,
    val title: String,
    val date: Long
)