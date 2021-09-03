package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.ApiUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.db.User
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class UsersEndpointsTest {
    @Test fun getOwnUser() {
        val currentUser = ApiUser("user1", listOf(Group.Admin))
        val user = User(0, "user1", "F", "L")

        runTest(currentUser, {
            users(
                "basic",
                object : UserRepository {
                    override fun allUsers() = TODO()
                    override fun storeUser(userId: String, firstName: String, lastName: String) = TODO()
                    override fun getUser(userId: Int) = TODO()
                    override fun getUser(userId: String) = user
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/user") {
                    addHeader("Authorization", "Basic amV0YnJhaW5zOmZvb2Jhcg")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val resp: UserWithGroups = Json.decodeFromString(response.content ?: "[]")
                assertEquals(user.userId, resp.userId)
                assertEquals(user.firstName, resp.firstName)
                assertEquals(user.lastName, resp.lastName)
                assertEquals(currentUser.groups, resp.groups)
            }
        }
    }

    @Test fun getUsers() {
        val currentUser = ApiUser("user1", listOf(Group.SuperUser))
        val user = User(0, "user1", "F", "L")

        runTest(currentUser, {
            users(
                "basic",
                object : UserRepository {
                    override fun allUsers() = listOf(user)
                    override fun storeUser(userId: String, firstName: String, lastName: String) = User(0, userId, firstName, lastName)
                    override fun getUser(userId: Int) = TODO("Not yet implemented")
                    override fun getUser(userId: String) = TODO("Not yet implemented")
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users") {
                    addHeader("Authorization", "Basic amV0YnJhaW5zOmZvb2Jhcg")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val resp: List<User> = Json.decodeFromString(response.content ?: "[]")
                assertEquals(listOf(user), resp)
            }
        }
    }

    @Test fun getUsersMissingNonSuperUser() {
        val currentUser = ApiUser("user1", listOf(Group.Admin, Group.Everyone))
        runTest(currentUser, {
            users(
                "basic",
                object : UserRepository {
                    override fun allUsers() = TODO()
                    override fun storeUser(userId: String, firstName: String, lastName: String) = TODO()
                    override fun getUser(userId: Int) = TODO("Not yet implemented")
                    override fun getUser(userId: String) = TODO("Not yet implemented")
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/users") {
                    addHeader("Authorization", "Basic amV0YnJhaW5zOmZvb2Jhcg")
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }
}
