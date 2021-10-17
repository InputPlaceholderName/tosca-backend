package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.api.users
import com.github.insertplaceholdername.tosca.auth.ApiUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.db.FullUser
import com.github.insertplaceholdername.tosca.db.FullUserWorkspace
import com.github.insertplaceholdername.tosca.db.Role
import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.Workspace
import com.github.insertplaceholdername.tosca.persistance.UserRepository
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant

internal class UsersEndpointsTest {
    @Test fun getOwnUser() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin))
        val user = FullUser(0, "user1", "F", "L", listOf())

        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
                override fun getUser(userId: String) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/user") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val resp: com.github.insertplaceholdername.tosca.api.UserWithGroups = Json.decodeFromString(response.content ?: "[]")
                assertEquals(0, resp.id)
                assertEquals(user.userId, resp.userId)
                assertEquals(user.firstName, resp.firstName)
                assertEquals(user.lastName, resp.lastName)
                assertEquals(currentUser.groups, resp.groups)
            }
        }
    }

    @Test fun getUsers() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val user = User(0, "user1", "F", "L")

        runTest(currentUser, {
            users(object : UserRepository {
                override fun allUsers() = listOf(user)
                override fun storeUser(userId: String, firstName: String, lastName: String) = User(0, userId, firstName, lastName)
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val resp: List<User> = Json.decodeFromString(response.content ?: "[]")
                assertEquals(listOf(user), resp)
            }
        }
    }

    @Test fun getUsersMissingNonSuperUser() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        runTest(currentUser, {
            users(object : UserRepository {})
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test fun getUserWorkspaces() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        val createdAt = Instant.now()
        val workspaces = listOf(FullUserWorkspace(0, Workspace(0, null, "w1", "i1", createdAt), Role.Admin))
        val user = FullUser(0, "u1", "f1", "l1", workspaces)
        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users/${currentUser.id}/workspaces") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val userWorkspaces: List<FullUserWorkspace> = Json.decodeFromString(response.content ?: error("No content"))
                assertEquals(workspaces, userWorkspaces)
            }
        }
    }

    @Test fun `Non super user can't get other users workspaces`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        val createdAt = Instant.now()
        val workspaces = listOf(FullUserWorkspace(0, Workspace(0, null, "w1", "i1", createdAt), Role.Admin))
        val user = FullUser(0, "u1", "f1", "l1", workspaces)
        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users/1/workspaces") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test fun `Non super user can't get other user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        val createdAt = Instant.now()
        val workspaces = listOf(FullUserWorkspace(0, Workspace(0, null, "w1", "i1", createdAt), Role.Admin))
        val user = FullUser(0, "u1", "f1", "l1", workspaces)
        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users/1") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test fun `Non super user can't get users`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        val createdAt = Instant.now()
        val workspaces = listOf(FullUserWorkspace(0, Workspace(0, null, "w1", "i1", createdAt), Role.Admin))
        val user = FullUser(0, "u1", "f1", "l1", workspaces)
        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test fun `Non super user can get own users`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))
        val createdAt = Instant.now()
        val workspaces = listOf(FullUserWorkspace(0, Workspace(0, null, "w1", "i1", createdAt), Role.Admin))
        val user = FullUser(0, "u1", "f1", "l1", workspaces)
        runTest(currentUser, {
            users(object : UserRepository {
                override fun getUser(userId: Int) = user
            }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users/${currentUser.id}") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: FullUser = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals(user, response)
            }
        }
    }
}
