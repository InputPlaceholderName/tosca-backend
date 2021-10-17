package com.github.insertplaceholdername.tosca.api

import com.github.insertplaceholdername.tosca.auth.CurrentUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.auth.anyone
import com.github.insertplaceholdername.tosca.auth.withAnyGroup
import com.github.insertplaceholdername.tosca.auth.withGroup
import com.github.insertplaceholdername.tosca.db.Role
import com.github.insertplaceholdername.tosca.persistance.UserRepository
import com.github.insertplaceholdername.tosca.persistance.WorkspaceRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class NewWorkspace(val name: String, val information: String?)

@Serializable
data class UpdateWorkspace(val name: String? = null, val information: String? = null, val creator: Int? = null)

@Serializable
data class WorkspaceUser(val user: Int, val role: Role)

@Serializable
data class WorkspaceUserRole(val role: Role)

fun Route.workspaces(userRepository: UserRepository, workspaceRepository: WorkspaceRepository) {

    route("/workspaces") {
        withAnyGroup(Group.SuperUser, Group.Admin) {
            post {
                val user = call.attributes[CurrentUser]
                val newWorkspace = call.receive<NewWorkspace>()
                val workspace = workspaceRepository.storeWorkspace(
                    newWorkspace.name,
                    newWorkspace.information ?: "", user.id
                )

                call.respond(workspace)
            }
        }

        withGroup(Group.SuperUser) {
            get {
                call.respond(workspaceRepository.allWorkspaces())
            }
        }

        route("/{id}/users") {
            anyone {
                post {
                    val currentUser = call.attributes[CurrentUser]
                    val workspaceId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
                    val workspace = workspaceRepository.getWorkspace(workspaceId) ?: return@post call.respond(HttpStatusCode.NotFound)

                    if (workspace.creator?.id != currentUser.id && currentUser.notHasGroup(Group.SuperUser)) {
                        return@post call.respond(HttpStatusCode.Forbidden)
                    }
                    val user = call.receive<WorkspaceUser>()
                    call.respond(workspaceRepository.addUser(workspaceId, user.user, user.role))
                }

                put("/{userId}") {
                    val currentUser = call.attributes[CurrentUser]
                    val workspaceId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.UnprocessableEntity)
                    val userId = call.parameters["userId"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.UnprocessableEntity)
                    val workspace = workspaceRepository.getWorkspace(workspaceId) ?: return@put call.respond(HttpStatusCode.NotFound)

                    if (workspace.creator?.id != currentUser.id && currentUser.notHasGroup(Group.SuperUser)) {
                        return@put call.respond(HttpStatusCode.Forbidden)
                    }

                    val role = call.receive<WorkspaceUserRole>().role
                    call.respond(workspaceRepository.addUser(workspaceId, userId, role))
                }

                delete("/{userId}") {
                    val currentUser = call.attributes[CurrentUser]
                    val workspaceId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.UnprocessableEntity)
                    val userId = call.parameters["userId"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.UnprocessableEntity)
                    val workspace = workspaceRepository.getWorkspace(workspaceId) ?: return@delete call.respond(HttpStatusCode.NotFound)

                    if (workspace.creator?.id != currentUser.id && currentUser.notHasGroup(Group.SuperUser)) {
                        return@delete call.respond(HttpStatusCode.Forbidden)
                    }

                    workspaceRepository.deleteUser(workspaceId, userId)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
        anyone {
            get("/{id}") {
                val currentUser = call.attributes[CurrentUser]
                workspaceRepository.getWorkspace(call.parameters["id"]!!.toInt())?.let {
                    if (it.userIsNotMember(currentUser.id) && currentUser.notHasGroup(Group.SuperUser)) {
                        call.respond(HttpStatusCode.Forbidden)
                    } else {
                        call.respond(it)
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }

            patch("/{id}") {
                val currentUser = call.attributes[CurrentUser]
                val updateWorkspace = call.receive<UpdateWorkspace>()
                workspaceRepository.getWorkspace(call.parameters["id"]!!.toInt())?.let {
                    if (it.creator?.id != currentUser.id && currentUser.notHasGroup(Group.SuperUser)) {
                        call.respond(HttpStatusCode.Forbidden)
                    } else {
                        val data = workspaceRepository.updateWorkspace(it.id, updateWorkspace.name ?: it.name, updateWorkspace.information ?: it.information, updateWorkspace.creator)
                        call.respond(data)
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }

            delete("/{id}") {
                val currentUser = call.attributes[CurrentUser]
                workspaceRepository.getWorkspace(call.parameters["id"]!!.toInt())?.let {
                    if (it.creator?.id != currentUser.id && currentUser.notHasGroup(Group.SuperUser)) {
                        call.respond(HttpStatusCode.Forbidden)
                    } else {
                        workspaceRepository.deleteWorkspace(it.id)
                        call.respond(HttpStatusCode.NoContent)
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
