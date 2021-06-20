package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.Groups
import com.github.insertplaceholdername.tosca.auth.JwtAuthorization
import com.github.insertplaceholdername.tosca.auth.setupAuth
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
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
    setupAuth(ExposedUserRepository)

    routing {

        authenticate("jwt") {
            get("/secret") {
                val user = JwtAuthorization(call).user()
                call.respond("Welcome $user")
            }

            get("/everyone") {
                val auth = JwtAuthorization(call)
                if (!auth.isAtLeast(Groups.Everyone)) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be member of Everyone group")
                }
                call.respond("Welcome ${auth.user()}")
            }

            get("/superuser") {
                val auth = JwtAuthorization(call)
                if (!auth.isAtLeast(Groups.SuperUser)) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be member of SuperUser group")
                }
                call.respond("Welcome ${auth.user()}")
            }

            get("/admin") {
                val auth = JwtAuthorization(call)
                if (!auth.isAtLeast(Groups.Admin)) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be member of SuperUser or Admin group")
                }
                call.respond("Welcome ${auth.user()}")
            }
        }
        get("/logout") {
        }
        users(ExposedUserRepository)
    }
}
