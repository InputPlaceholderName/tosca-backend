package com.github.insertplaceholdername.tosca

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
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
    routing {
        users(ExposedUserRepository)
    }
}
