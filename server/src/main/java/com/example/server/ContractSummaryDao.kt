package com.example.server

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ContractSummaryDao {
    fun create(summary: ContractSummary): Int? {
        return transaction {
            ContractSummaries.insert {
                it[ContractSummaries.contractId] = summary.contractId
                it[ContractSummaries.summaryText] = summary.summaryText
            } get ContractSummaries.id
        }
    }

    fun findByContractId(contractId: Int): ContractSummary? {
        return transaction {
            ContractSummaries.select { ContractSummaries.contractId eq contractId }
                .mapNotNull { toContractSummary(it) }
                .singleOrNull()
        }
    }

    private fun toContractSummary(row: ResultRow): ContractSummary =
        ContractSummary(
            id = row[ContractSummaries.id],
            contractId = row[ContractSummaries.contractId],
            summaryText = row[ContractSummaries.summaryText]
        )
}

data class ContractSummary(
    val id: Int = 0,
    val contractId: Int,
    val summaryText: String
)