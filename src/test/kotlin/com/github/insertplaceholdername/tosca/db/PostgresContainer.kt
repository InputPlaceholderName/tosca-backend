package com.github.insertplaceholdername.tosca.db

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.rules.ExternalResource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

object PostgresContainer : ExternalResource() {
    class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)

    @Container
    val postgresContainer: MyPostgreSQLContainer = MyPostgreSQLContainer("postgres:13.3")
        .withDatabaseName("db")
        .withUsername("postgres")
        .withPassword("test")

    override fun before() {
        super.before()
        postgresContainer.start()
        val jdbcUrl = "jdbc:postgresql://${postgresContainer.host}:${postgresContainer.firstMappedPort}/${postgresContainer.databaseName}"

        Flyway.configure()
            .dataSource(jdbcUrl, postgresContainer.username, postgresContainer.password)
            .load()
            .migrate()

        Database.connect(jdbcUrl, user = postgresContainer.username, password = postgresContainer.password)
    }

    fun clear() {
        transaction {
            UserDAO.table.deleteAll()
            WorkspaceDAO.table.deleteAll()
            UserWorkspaceDAO.table.deleteAll()
        }
    }
}
