package com.example.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.Date
import java.util.UUID

object Server {
    private val logger = LoggerFactory.getLogger(Server::class.java)
    private val secret = System.getenv("JWT_SECRET") ?: "89VZJuRkKB0sglml"
    private const val ISSUER = "com.example.final_project"
    private const val AUDIENCE = "contract-app-users"
    private const val REALM = "Data Management"
    private val userDao = UserDao()
    private val contractDao = ContractDao()
    private val contractSummaryDao = ContractSummaryDao()
    private val calendarEventDao = CalendarEventDao()

    fun start() {
        org.apache.log4j.BasicConfigurator.configure()
        val port = System.getenv("PORT")?.toInt() ?: 8080
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Patch)
                allowMethod(HttpMethod.Delete)
                allowHeader(HttpHeaders.Authorization)
                allowHeader(HttpHeaders.ContentType)
            }
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            install(Authentication) {
                jwt {
                    realm = REALM
                    verifier(
                        JWT
                            .require(Algorithm.HMAC256(secret))
                            .withAudience(AUDIENCE)
                            .withIssuer(ISSUER)
                            .build()
                    )
                    validate { credential ->
                        if (credential.payload.audience.contains(AUDIENCE)) JWTPrincipal(credential.payload) else null
                    }
                }
            }
            configureRouting()
            configureDatabase()
            updateDatabaseSchema()
        }.start(wait = true)
    }

    private fun Application.configureRouting() {
        routing {
            post("/register") {
                try {
                    val user = call.receive<User>()
                    val userId = userDao.create(user)
                    if (userId != null) {
                        call.respond(HttpStatusCode.Created, mapOf("userId" to userId))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Registration failed")
                    }
                } catch (e: Exception) {
                    logger.error("Error in registration: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An error occurred during registration")
                }
            }

            post("/login") {
                try {
                    val user = call.receive<User>()
                    val userId = userDao.authenticate(user.email, user.password)
                    if (userId != null) {
                        val token = createJwtToken(userId)
                        call.respond(mapOf("token" to token))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                    }
                } catch (e: Exception) {
                    logger.error("Error in login: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An error occurred during login")
                }
            }

            authenticate {
                post("/contracts") {
                    try {
                        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        if (userId == null) {
                            logger.warn("Invalid token: userId is null")
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@post
                        }

                        logger.info("Receiving contract upload for user $userId")

                        val multipart = call.receiveMultipart()
                        var fileName = ""
                        var fileBytes: ByteArray? = null
                        var contentType = ""

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    fileName = part.originalFileName ?: "unnamed"
                                    contentType = part.contentType?.toString() ?: "application/octet-stream"
                                    fileBytes = part.streamProvider().readBytes()
                                    logger.info("Received file: $fileName, type: $contentType, size: ${fileBytes?.size ?: 0} bytes")
                                }
                                else -> logger.warn("Unexpected part in multipart data: ${part::class.simpleName}")
                            }
                            part.dispose()
                        }

                        if (fileBytes != null) {
                            val uploadDir = File("uploads/contracts")
                            uploadDir.mkdirs()
                            val file = File(uploadDir, "${UUID.randomUUID()}_$fileName")
                            file.writeBytes(fileBytes!!)

                            val filePath = file.absolutePath
                            val uploadTime = Instant.now().toEpochMilli()
                            val contract = Contract(
                                userId = userId,
                                name = fileName,
                                filePath = filePath,
                                fileSize = fileBytes!!.size.toLong(),
                                contentType = contentType,
                                uploadTime = uploadTime
                            )
                            val contractId = contractDao.create(contract)
                            if (contractId != null) {
                                logger.info("Contract created successfully with ID: $contractId")
                                call.respond(HttpStatusCode.Created, mapOf("contractId" to contractId))
                            } else {
                                logger.error("Failed to create contract in database")
                                call.respond(HttpStatusCode.InternalServerError, "Failed to create contract in database")
                            }
                        } else {
                            logger.warn("No file received in the request")
                            call.respond(HttpStatusCode.BadRequest, "No file received")
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating contract: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while creating the contract: ${e.message}")
                    }
                }

                get("/contracts") {
                    try {
                        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@get
                        }
                        val contracts = contractDao.findByUserId(userId)
                        logger.info("Fetched ${contracts.size} contracts for user $userId")
                        call.respond(contracts)
                    } catch (e: Exception) {
                        logger.error("Error fetching contracts: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching contracts: ${e.message}")
                    }
                }

                get("/contracts/{id}/file") {
                    try {
                        val contractId = call.parameters["id"]?.toIntOrNull()
                        if (contractId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid contract ID")
                            return@get
                        }

                        val contract = contractDao.findById(contractId)
                        if (contract == null) {
                            call.respond(HttpStatusCode.NotFound, "Contract not found")
                            return@get
                        }

                        val file = File(contract.filePath)
                        if (!file.exists()) {
                            call.respond(HttpStatusCode.NotFound, "Contract file not found")
                            return@get
                        }

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, contract.name).toString()
                        )
                        call.respondFile(file)
                    } catch (e: Exception) {
                        logger.error("Error serving contract file: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while serving the contract file")
                    }
                }

                post("/contracts/{id}/summary") {
                    try {
                        val contractId = call.parameters["id"]?.toIntOrNull()
                        if (contractId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid contract ID")
                            return@post
                        }
                        val summary = call.receive<ContractSummary>()
                        val summaryId = contractSummaryDao.create(summary.copy(contractId = contractId))
                        if (summaryId != null) {
                            call.respond(HttpStatusCode.Created, mapOf("summaryId" to summaryId))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Failed to create summary")
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating contract summary: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while creating the contract summary")
                    }
                }

                get("/contracts/{id}/summary") {
                    try {
                        val contractId = call.parameters["id"]?.toIntOrNull()
                        if (contractId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid contract ID")
                            return@get
                        }
                        val summary = contractSummaryDao.findByContractId(contractId)
                        if (summary != null) {
                            call.respond(summary)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Summary not found")
                        }
                    } catch (e: Exception) {
                        logger.error("Error fetching contract summary: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching the contract summary")
                    }
                }

                get("/profile") {
                    try {
                        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@get
                        }
                        val user = userDao.findById(userId)
                        if (user != null) {
                            call.respond(mapOf("fullName" to user.fullName))
                        } else {
                            call.respond(HttpStatusCode.NotFound, "User not found")
                        }
                    } catch (e: Exception) {
                        logger.error("Error fetching user profile: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching the user profile")
                    }
                }


                post("/calendar-events") {
                    try {
                        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        if (userId == null) {
                            logger.warn("Invalid token: userId is null")
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@post
                        }

                        val event = call.receive<CalendarEvent>()
                        logger.info("Attempting to create calendar event for user $userId")
                        val eventId = calendarEventDao.create(event.copy(userId = userId))
                        if (eventId != null) {
                            logger.info("Calendar event created successfully with ID: $eventId")
                            call.respond(HttpStatusCode.Created, mapOf("eventId" to eventId))
                        } else {
                            logger.error("Failed to create calendar event in database")
                            call.respond(HttpStatusCode.BadRequest, "Failed to create calendar event")
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating calendar event: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while creating the calendar event")
                    }
                }
                get("/calendar-events") {
                    try {
                        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@get
                        }
                        val events = calendarEventDao.findAllByUserId(userId)
                        call.respond(events)
                    } catch (e: Exception) {
                        logger.error("Error fetching calendar events: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching calendar events")
                    }
                }
            }
        }
    }

    private fun Application.configureDatabase() {
        Database.connect(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/contract_management",
            driver = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver",
            user = System.getenv("DB_USER") ?: "postgres",
            password = System.getenv("DB_PASSWORD") ?: "235689"
        )
    }

    private fun updateDatabaseSchema() {
        transaction {
            SchemaUtils.create(Users, Contracts, ContractSummaries, CalendarEvents)

            // Check if the content_type column exists
            val contentTypeExists = try {
                Contracts.columns.any { it.name == "content_type" }
            } catch (e: Exception) {
                false
            }

            if (!contentTypeExists) {
                // Add the content_type column
                SchemaUtils.createMissingTablesAndColumns(Contracts)

                // If the above doesn't work, try this raw SQL approach:
                // exec("ALTER TABLE contracts ADD COLUMN content_type VARCHAR(100)")
            }

            // Check if the upload_time column exists
            val uploadTimeExists = try {
                Contracts.columns.any { it.name == "upload_time" }
            } catch (e: Exception) {
                false
            }

            if (!uploadTimeExists) {
                // Add the upload_time column
                SchemaUtils.createMissingTablesAndColumns(Contracts)

                // If the above doesn't work, try this raw SQL approach:
                // exec("ALTER TABLE contracts ADD COLUMN upload_time TIMESTAMP")
            }

            SchemaUtils.createMissingTablesAndColumns(CalendarEvents)

            commit()
        }
    }

    private fun createJwtToken(userId: Int): String {
        return JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hour expiration
            .sign(Algorithm.HMAC256(secret))
    }
}

fun main() {
    println("Starting server on port 8080...")
    Server.start()
}