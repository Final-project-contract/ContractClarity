package app

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Date

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
        }.start(wait = true)
    }

    private fun Application.configureRouting() {
        routing {
            post("/register") {
                try {
                    val user = call.receive<User>()
                    logger.info("Received registration request for user: ${user.email}")
                    val userId = userDao.create(user)
                    if (userId != null) {
                        logger.info("User registered successfully: $userId")
                        call.respond(HttpStatusCode.Created, mapOf("userId" to userId))
                    } else {
                        logger.warn("Registration failed for user: ${user.email}")
                        call.respond(HttpStatusCode.BadRequest, "Registration failed")
                    }
                } catch (e: Exception) {
                    logger.error("Error in registration: ${e.message}")
                    e.printStackTrace()
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

    private fun Application.configureDatabase() {
        val dbUrl = "postgres://u3muoju0j6oajo:pfcfb2100486e690377ab4266f1c5a4af296db4180e09961058a77a34745c000c@cat670aihdrkt1.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d5amu549gim58e"
        val dbUri = URI(dbUrl)
        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=require"

        Database.connect(
            url = jdbcUrl,
            driver = "org.postgresql.Driver",
            user = username,
            password = password
        )

        transaction {
            SchemaUtils.create(Users, Contracts, ContractSummaries)
        }
        logger.info("Database configured successfully")
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
