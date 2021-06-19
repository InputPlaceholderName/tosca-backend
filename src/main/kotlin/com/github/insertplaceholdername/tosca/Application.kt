package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.oidc.accessTokenVerifier
import com.github.insertplaceholdername.tosca.oidc.idTokenVerifier
import com.github.insertplaceholdername.tosca.oidc.setupAuth
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun configureDB() {
    val config = if (!System.getenv("JDBC_DATABASE_URL").isNullOrEmpty()) {
        HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_DATABASE_URL")
            maximumPoolSize = 10
        }
    } else {
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${System.getenv("POSTGRES_HOST")}:5432/postgres"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("POSTGRES_USERNAME")
            password = System.getenv("POSTGRES_PASSWORD")
            maximumPoolSize = 10
        }
    }

    val dataSource = HikariDataSource(config)
    Flyway.configure().dataSource(dataSource).load().migrate()

    Database.connect(dataSource)
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureDB()
    setupAuth()
    val accessTokenVerifier = accessTokenVerifier()
    val idTokenVerifier = idTokenVerifier()

    routing {
        authenticate {
            get("/login") {
                call.respondRedirect("/")
            }
            get("/authorization-code/callback") {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>() ?: throw Exception("No principal was given")
                val accessToken = accessTokenVerifier.decode(principal.accessToken)
                val idTokenString = principal.extraParameters["id_token"] ?: throw Exception("id_token was not returned")
                val idToken = idTokenVerifier.decode(idTokenString, null)
                val firstName = idToken.claims["firstName"] ?: "Unknown"
                val lastName = idToken.claims["lastName"] ?: "Unknown"
                val groups = idToken.claims["groups"] ?: "none"
                val id = idToken.claims["id"] ?: "unknown"
                call.respond(mapOf("firstName" to firstName, "lastName" to lastName, "groups" to groups.toString(), "id" to id))

            }
        }
        get("/logout") {

        }
        users(ExposedUserRepository)
    }
}
