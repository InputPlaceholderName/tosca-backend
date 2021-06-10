package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.UserDAO
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.users(userRepository: UserRepository) {
    routing {
        get("/users") {
            val users = userRepository.allUsers()
            call.respond(users)
        }

        post("/users") {
        }
    }
}