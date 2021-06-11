package com.github.insertplaceholdername.tosca

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json

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
