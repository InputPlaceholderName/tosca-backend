package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.Groups
import com.github.insertplaceholdername.tosca.auth.JwtAuthorization
import com.github.insertplaceholdername.tosca.auth.setupAuth
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.config.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.sentry.Sentry
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun configureDB(applicationConfig: Config) {

    val config = if (applicationConfig.tryGetString("database.jdbcUrl")?.isNotEmpty() == true) {
        HikariConfig().apply {
            jdbcUrl = applicationConfig.getString("database.jdbcUrl")
            maximumPoolSize = 10
        }
    } else {
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${applicationConfig.getString("database.host")}:5432/postgres"
            driverClassName = "org.postgresql.Driver"
            username = applicationConfig.getString("database.user")
            password = applicationConfig.getString("database.password")
            maximumPoolSize = 10
        }
    }

    val dataSource = HikariDataSource(config)
    Flyway.configure().dataSource(dataSource).load().migrate()

    Database.connect(dataSource)
}

fun setupSentry(applicationConfig: Config) {
    if (applicationConfig.tryGetString("sentry.dsn")?.isNotEmpty() == true) {
        Sentry.init(applicationConfig.getString("sentry.dsn"))
        println("Initialized Sentry")
    } else {
        println("No Sentry DSN provided")
    }
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    val config = ConfigFactory.load() ?: throw Exception("Could not load config")

    configureDB(config)
    setupAuth(ExposedUserRepository)
    setupSentry(config)

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
