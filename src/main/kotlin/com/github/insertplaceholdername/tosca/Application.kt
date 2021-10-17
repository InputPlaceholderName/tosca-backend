package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.api.users
import com.github.insertplaceholdername.tosca.api.workspaces
import com.github.insertplaceholdername.tosca.auth.GroupAuthorization
import com.github.insertplaceholdername.tosca.auth.JwtConfig
import com.github.insertplaceholdername.tosca.auth.setupAuth
import com.github.insertplaceholdername.tosca.persistance.ExposedUserRepository
import com.github.insertplaceholdername.tosca.persistance.ExposedWorkspaceRepository
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.config.tryGetString
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
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
fun allRoutes(root: Route): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}
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
        install(ContentNegotiation) {
            json()
        }

        authenticate("jwt") {
            route("/v1/") {
                users(ExposedUserRepository)
                workspaces(ExposedUserRepository, ExposedWorkspaceRepository)
            }
        }
    }

    val root = feature(Routing)
    val allRoutes = allRoutes(root)
    val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }

    allRoutesWithMethod.forEach {
        println("route: $it")
    }
}
