package com.example.server

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant

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
                    it[uploadTime] = Instant.now()
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

    fun delete(id: Int): Boolean {
        return try {
            logger.info("Attempting to delete contract with ID: $id")
            transaction {
                val deletedCount = Contracts.deleteWhere { Contracts.id eq id }
                val success = deletedCount > 0
                if (success) {
                    logger.info("Contract deleted successfully with ID: $id")
                } else {
                    logger.warn("No contract found with ID: $id")
                }
                success
            }
        } catch (e: Exception) {
           // logger.error("Error deleting contract with ID: $id, ${e.message}", e)
            false
        }
    }

    private fun toContract(row: ResultRow): Contract =
        Contract(
            id = row[Contracts.id],
            userId = row[Contracts.userId],
            name = row[Contracts.name],
            filePath = row[Contracts.filePath],
            fileSize = row[Contracts.fileSize],
            contentType = row[Contracts.contentType],
            uploadTime = row[Contracts.uploadTime].toEpochMilli()
        )
}

data class Contract(
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val filePath: String,
    val fileSize: Long,
    val contentType: String,
    val uploadTime: Long? = null
)

//package com.example.server
//
//import org.jetbrains.exposed.sql.ResultRow
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.select
//import org.jetbrains.exposed.sql.transactions.transaction
//import org.slf4j.LoggerFactory
//import java.time.Instant
//
//class ContractDao {
//    private val logger = LoggerFactory.getLogger(ContractDao::class.java)
//
//    fun create(contract: Contract): Int? {
//        return try {
//            logger.info("Attempting to create contract: ${contract.name}")
//            transaction {
//                val id = Contracts.insert {
//                    it[userId] = contract.userId
//                    it[name] = contract.name
//                    it[filePath] = contract.filePath
//                    it[fileSize] = contract.fileSize
//                    it[contentType] = contract.contentType
//                    it[uploadTime] = Instant.now()
//                } get Contracts.id
//                logger.info("Contract created successfully with ID: $id")
//                id
//            }
//        } catch (e: Exception) {
//            logger.error("Error creating contract: ${e.message}", e)
//            logger.error("Contract details: userId=${contract.userId}, name=${contract.name}, contentType=${contract.contentType}, fileSize=${contract.fileSize}")
//            null
//        }
//    }
//
//    fun findById(id: Int): Contract? {
//        return try {
//            transaction {
//                Contracts.select { Contracts.id eq id }
//                    .mapNotNull { toContract(it) }
//                    .singleOrNull()
//            }
//        } catch (e: Exception) {
//            logger.error("Error finding contract by ID: ${e.message}", e)
//            null
//        }
//    }
//
//    fun findByUserId(userId: Int): List<Contract> {
//        return try {
//            transaction {
//                Contracts.select { Contracts.userId eq userId }
//                    .mapNotNull { toContract(it) }
//            }
//        } catch (e: Exception) {
//            logger.error("Error finding contracts by user ID: ${e.message}", e)
//            emptyList()
//        }
//    }
//
//
//    private fun toContract(row: ResultRow): Contract =
//        Contract(
//            id = row[Contracts.id],
//            userId = row[Contracts.userId],
//            name = row[Contracts.name],
//            filePath = row[Contracts.filePath],
//            fileSize = row[Contracts.fileSize],
//            contentType = row[Contracts.contentType],
//            uploadTime = row[Contracts.uploadTime].toEpochMilli()
//        )
//}
//
//data class Contract(
//    val id: Int = 0,
//    val userId: Int,
//    val name: String,
//    val filePath: String,
//    val fileSize: Long,
//    val contentType: String,
//    val uploadTime: Long? = null
//)