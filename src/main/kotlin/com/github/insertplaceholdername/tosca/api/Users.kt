package com.github.insertplaceholdername.tosca.api

import com.github.insertplaceholdername.tosca.auth.CurrentUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.auth.anyone
import com.github.insertplaceholdername.tosca.auth.withGroup
import com.github.insertplaceholdername.tosca.persistance.UserRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.serialization.Serializable

@Serializable
class UserWithGroups(
    val id: Int,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val groups: List<Group>
)

fun Route.users(userRepository: UserRepository) {
    anyone {
        get("user") {
            val currentUser = call.attributes[CurrentUser]
            val dbUser = userRepository.getUser(currentUser.userId)!!
            val user = UserWithGroups(dbUser.id, currentUser.userId, dbUser.firstName, dbUser.lastName, currentUser.groups)
            call.respond(user)
        }

        get("users/{id}") {
            call.parameters["id"]?.toInt()?.let { id ->
                val currentUser = call.attributes[CurrentUser]
                if (currentUser.id != id && currentUser.notHasGroup(Group.SuperUser)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    userRepository.getUser(id)?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound)
                }
            } ?: call.respond(HttpStatusCode.UnprocessableEntity)
        }

        get("users/{id}/workspaces") {
            call.parameters["id"]?.toInt()?.let { id ->
                val currentUser = call.attributes[CurrentUser]
                if (currentUser.id != id && currentUser.notHasGroup(Group.SuperUser)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    userRepository.getUser(id)?.let { user ->
                        call.respond(user.workspaces)
                    } ?: call.respond(HttpStatusCode.NotFound)
                }
            } ?: call.respond(HttpStatusCode.UnprocessableEntity)
        }
    }

    withGroup(Group.SuperUser) {
        get("users") {
            val users = userRepository.allUsers()
            call.respond(users)
        }
    }
}
