package com.example.server

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ContractDao {
    fun create(contract: Contract): Int? {
        return transaction {
            Contracts.insert {
                it[userId] = contract.userId
                it[name] = contract.name
                it[url] = contract.url
            } get Contracts.id
        }
    }

    fun findById(id: Int): Contract? {
        return transaction {
            Contracts.select { Contracts.id eq id }
                .mapNotNull { toContract(it) }
                .singleOrNull()
        }
    }

    fun findByUserId(userId: Int): List<Contract> {
        return transaction {
            Contracts.select { Contracts.userId eq userId }
                .mapNotNull { toContract(it) }
        }
    }

    private fun toContract(row: ResultRow): Contract =
        Contract(
            id = row[Contracts.id],
            userId = row[Contracts.userId],
            name = row[Contracts.name],
            url = row[Contracts.url]
        )
}

data class Contract(
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val url: String
)