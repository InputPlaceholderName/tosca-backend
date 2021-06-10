package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import io.ktor.application.*
import io.ktor.client.features.json.serializer.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class UsersEndpointsTest {

    @Test fun testRequest() = withTestApplication({

        install(ContentNegotiation) {
            KotlinxSerializer(kotlinx.serialization.json.Json {})
        }

        users(object: UserRepository {
            override fun allUsers() = listOf(User(1, "bananmannen", "Sean", "Banan"))
        })
    }) {
        with(handleRequest(HttpMethod.Get, "/users")) {
            assertEquals(HttpStatusCode.OK, response.status())
            response.content?.to
        }
    }

}