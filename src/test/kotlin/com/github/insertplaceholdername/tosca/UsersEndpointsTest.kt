package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class UsersEndpointsTest {
    @Test fun testRequest() = withTestApplication({
        users(object: UserRepository {
            override fun allUsers() = listOf(User(1, "bananmannen", "Sean", "Banan"))
        })
    }) {
        with(handleRequest(HttpMethod.Get, "/users")) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }
}