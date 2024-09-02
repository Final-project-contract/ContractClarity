package com.example.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.application.call
import io.ktor.server.routing.delete
fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val contractDao = ContractDao() // Create an instance of ContractDao

    embeddedServer(Netty, port = port) {
        routing {
            get("/") {
                call.respondText("ContractClarity API is running!")
            }
//            delete("/contracts/{id}") {
//                val id = call.parameters["id"]?.toIntOrNull()
//                if (id != null) {
//                    val success = contractDao.delete(id)
//                    if (success) {
//                        call.respondText("Contract deleted successfully.", status = HttpStatusCode.NoContent)
//                    } else {
//                        call.respondText("Contract not found.", status = HttpStatusCode.NotFound)
//                    }
//                } else {
//                    call.respondText("Invalid contract ID.", status = HttpStatusCode.BadRequest)
//                }
//            }

        }
    }.start(wait = true)
}