package com.example.server

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CalendarEventDao {
    private val logger = LoggerFactory.getLogger(CalendarEventDao::class.java)

    fun create(event: CalendarEvent): Int? {
        return try {
            transaction {
                logger.info("Attempting to create calendar event: $event")
                val id = CalendarEvents.insert {
                    it[userId] = event.userId
                    it[contractId] = event.contractId
                    it[title] = event.title
                    it[date] = event.date
                } get CalendarEvents.id
                logger.info("Calendar event created successfully with ID: $id")
                id
            }
        } catch (e: Exception) {
            logger.error("Error creating calendar event: ${e.message}", e)
            null
        }
    }

    fun findAllByUserId(userId: Int): List<CalendarEvent> {
        return try {
            transaction {
                logger.info("Fetching calendar events for user ID: $userId")
                val events = CalendarEvents.select { CalendarEvents.userId eq userId }
                    .mapNotNull { toCalendarEvent(it) }
                logger.info("Found ${events.size} calendar events for user ID: $userId")
                events
            }
        } catch (e: Exception) {
            logger.error("Error finding calendar events by user ID: ${e.message}", e)
            emptyList()
        }
    }

    fun findByContractId(contractId: Int): List<CalendarEvent> {
        return try {
            transaction {
                logger.info("Fetching calendar events for contract ID: $contractId")
                val events = CalendarEvents.select { CalendarEvents.contractId eq contractId }
                    .mapNotNull { toCalendarEvent(it) }
                logger.info("Found ${events.size} calendar events for contract ID: $contractId")
                events
            }
        } catch (e: Exception) {
            logger.error("Error finding calendar events by contract ID: ${e.message}", e)
            emptyList()
        }
    }

    fun deleteByContractId(contractId: Int): Boolean {
        return try {
            transaction {
                logger.info("Attempting to delete calendar events for contract ID: $contractId")
                val deletedCount = CalendarEvents.deleteWhere { CalendarEvents.contractId eq contractId }
                logger.info("Deleted $deletedCount calendar events for contract ID: $contractId")
                deletedCount > 0
            }
        } catch (e: Exception) {
            logger.error("Error deleting calendar events by contract ID: ${e.message}", e)
            false
        }
    }

    private fun toCalendarEvent(row: ResultRow): CalendarEvent =
        CalendarEvent(
            id = row[CalendarEvents.id],
            userId = row[CalendarEvents.userId],
            contractId = row[CalendarEvents.contractId],
            title = row[CalendarEvents.title],
            date = row[CalendarEvents.date]
        )
}

data class CalendarEvent(
    val id: Int = 0,
    val userId: Int,
    val contractId: Int,
    val title: String,
    val date: Long
)
