package app

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ContractSummaryDao {
    fun create(summary: ContractSummary): Int? {
        return transaction {
            ContractSummaries.insert {
                it[contractId] = summary.contractId
                it[summaryText] = summary.summaryText
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