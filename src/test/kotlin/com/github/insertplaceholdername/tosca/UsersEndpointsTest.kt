package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class UsersEndpointsTest {
    @Test fun testRequest() {
        val user = User(1, "bananmannen", "Sean", "Banan")

        withTestApplication({
            users(object : UserRepository {
                override fun allUsers() = listOf(user)
            })
        }) {
            with(handleRequest(HttpMethod.Get, "/users")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val resp: List<User> = Json.decodeFromString(response.content ?: "[]")
                assertEquals(listOf(user), resp)
            }
        }
    }
}
