package com.example.server

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class ContractDao {
    private val logger = LoggerFactory.getLogger(ContractDao::class.java)

    fun create(contract: Contract): Int? {
        return try {
            logger.info("Attempting to create contract: ${contract.name}")
            transaction {
                val id = Contracts.insert {
                    it[userId] = contract.userId
                    it[name] = contract.name
                    it[filePath] = contract.filePath
                    it[fileSize] = contract.fileSize
                    it[contentType] = contract.contentType
                } get Contracts.id
                logger.info("Contract created successfully with ID: $id")
                id
            }
        } catch (e: Exception) {
            logger.error("Error creating contract: ${e.message}", e)
            logger.error("Contract details: userId=${contract.userId}, name=${contract.name}, contentType=${contract.contentType}, fileSize=${contract.fileSize}")
            null
        }
    }
    fun findById(id: Int): Contract? {
        return try {
            transaction {
                Contracts.select { Contracts.id eq id }
                    .mapNotNull { toContract(it) }
                    .singleOrNull()
            }
        } catch (e: Exception) {
            logger.error("Error finding contract by ID: ${e.message}", e)
            null
        }
    }

    fun findByUserId(userId: Int): List<Contract> {
        return try {
            transaction {
                Contracts.select { Contracts.userId eq userId }
                    .mapNotNull { toContract(it) }
            }
        } catch (e: Exception) {
            logger.error("Error finding contracts by user ID: ${e.message}", e)
            emptyList()
        }
    }

    private fun toContract(row: ResultRow): Contract =
        Contract(
            id = row[Contracts.id],
            userId = row[Contracts.userId],
            name = row[Contracts.name],
            filePath = row[Contracts.filePath],
            fileSize = row[Contracts.fileSize],
            contentType = row[Contracts.contentType]
        )
}

data class Contract(
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val filePath: String,
    val fileSize: Long,
    val contentType: String
)