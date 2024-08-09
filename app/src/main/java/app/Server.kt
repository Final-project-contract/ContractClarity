package app

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.LoggerFactory

object Server {
    private val logger = LoggerFactory.getLogger(Server::class.java)
    private val secret = System.getenv("JWT_SECRET") ?: "89VZJuRkKB0sglml"
    private const val ISSUER = "com.example.final_project"
    private const val AUDIENCE = "contract-app-users"
    private const val REALM = "Data Management"
    private val userDao = UserDao()
    private val contractDao = ContractDao()
    private val contractSummaryDao = ContractSummaryDao()

    fun start() {
        val port = System.getenv("PORT")?.toInt() ?: 8080
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            install(Authentication)
            configureSecurity()
            configureRouting()
            configureDatabase()
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
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@post
                        }
                        val contract = call.receive<Contract>()
                        val contractId = contractDao.create(contract.copy(userId = userId))
                        if (contractId != null) {
                            call.respond(HttpStatusCode.Created, mapOf("contractId" to contractId))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Failed to create contract")
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating contract: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while creating the contract")
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
                        call.respond(contracts)
                    } catch (e: Exception) {
                        logger.error("Error fetching contracts: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching contracts")
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
            }
        }
    }

    private fun Application.configureSecurity() {
        authentication {
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
    }

    private fun Application.configureDatabase() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/contract_management",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "235689"
        )

        transaction {
            SchemaUtils.create(Users, Contracts, ContractSummaries)
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
    Server.start()
}