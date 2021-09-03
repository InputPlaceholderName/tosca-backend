package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.auth.CurrentUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.auth.GroupAuthorization
import com.github.insertplaceholdername.tosca.auth.JwtConfig
import com.github.insertplaceholdername.tosca.auth.setupAuth
import com.github.insertplaceholdername.tosca.auth.withAnyGroup
import com.github.insertplaceholdername.tosca.auth.withGroup
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.config.tryGetString
import io.ktor.features.CORS
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.sentry.Sentry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

    if (config.tryGetString("cors.allowedHosts")?.isNotEmpty() == true) {
        val hosts = config.getString("cors.allowedHosts").split(';')
        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)
            method(HttpMethod.Get)

            header(HttpHeaders.Authorization)
            header(HttpHeaders.AccessControlAllowOrigin)

            allowNonSimpleContentTypes = true

            for (host in hosts) {
                host(host, listOf("http", "https"))
            }
        }
    } else {
        error("WARNING! No CORS-allowed hosts configured!")
    }

    install(GroupAuthorization) {
        extractCurrentUser {
            Json.decodeFromString((it as JWTPrincipal)[JwtConfig.claimKey] ?: throw Exception("Can only be called within authorization block"))
        }
    }

    routing {
        authenticate("jwt") {
            withAnyGroup(Group.Everyone, Group.Admin, Group.SuperUser) {
                get("/secret") {
                    val user = call.attributes[CurrentUser]
                    call.respond("Welcome $user")
                }
            }

            withGroup(Group.Everyone) {
                get("/everyone") {
                    val user = call.attributes[CurrentUser]
                    call.respond("Welcome $user")
                }
            }

            withGroup(Group.SuperUser) {
                get("/superuser") {
                    val user = call.attributes[CurrentUser]
                    call.respond("Welcome $user")
                }
            }

            withGroup(Group.Admin) {
                get("/admin") {
                    val user = call.attributes[CurrentUser]
                    call.respond("Welcome $user")
                }
            }

            users("jwt", ExposedUserRepository)
        }
    }
}
