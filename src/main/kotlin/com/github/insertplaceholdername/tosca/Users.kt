package com.github.insertplaceholdername.tosca

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*

fun Application.users(userRepository: UserRepository) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        get("/users") {
            val users = userRepository.allUsers()
            call.respond(users)
        }

        post("/users") {
        }
    }
}