package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.CurrentUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.auth.anyone
import com.github.insertplaceholdername.tosca.auth.withGroup
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.json
import kotlinx.serialization.Serializable

@Serializable
class UserWithGroups(val userId: String, val firstName: String, val lastName: String, val groups: List<Group>)

fun Application.users(authKey: String, userRepository: UserRepository) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        authenticate(authKey) {
            anyone {
                get("/user") {
                    val currentUser = call.attributes[CurrentUser]
                    val dbUser = userRepository.getUser(currentUser.userId)
                    val user = UserWithGroups(currentUser.userId, dbUser.firstName, dbUser.lastName, currentUser.groups)
                    call.respond(user)
                }
            }

            withGroup(Group.SuperUser) {
                get("/users") {
                    val users = userRepository.allUsers()
                    call.respond(users)
                }
            }
        }
    }
}
