package com.example.server

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table() {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 128).uniqueIndex()
    val password = varchar("password", 60)
    val fullName = varchar("full_name", 128)
    val industry = varchar("industry", 128)

    override val primaryKey = PrimaryKey(id)
}

object Contracts : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 255)
    val filePath = varchar("file_path", 255)
    val fileSize = long("file_size")
    val contentType = varchar("content_type", 100)
    val uploadTime = timestamp("upload_time")

    override val primaryKey = PrimaryKey(id)
}

object ContractSummaries : Table() {
    val id = integer("id").autoIncrement()
    val contractId = integer("contract_id").references(Contracts.id)
    val summaryText = text("summary_text")

    override val primaryKey = PrimaryKey(id)
}