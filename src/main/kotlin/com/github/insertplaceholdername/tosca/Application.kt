package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.UserDAO
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDB () {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${System.getenv("DB_HOST") ?: "localhost"}:5432/postgres"
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "test123test"
        maximumPoolSize = 10
    }

    val dataSource = HikariDataSource(config)
    Flyway.configure().dataSource(dataSource).load().migrate()

    Database.connect(dataSource)
}

fun main(args: Array<String>) {
    configureDB()
    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            get("/users") {
                val users = transaction {
                    UserDAO.all().map { user -> user.toModel() }.toList()
                }
                call.respond(users)
            }

            post("/users") {
            }
        }
    }.start(wait = true)
}


@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(testing: Boolean = false) {
}
