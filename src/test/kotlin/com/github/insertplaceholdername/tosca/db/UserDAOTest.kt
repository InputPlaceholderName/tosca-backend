package com.github.insertplaceholdername.tosca.db


import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import org.junit.BeforeClass
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)


@Testcontainers
internal class UserDAOTest {
    companion object {
        @Container
        val postgresContainer = MyPostgreSQLContainer("postgres:13.3")
            .withDatabaseName("db")
            .withUsername("postgres")
            .withPassword("test")

        @BeforeClass
        @JvmStatic
        fun migrateDatabase() {
            postgresContainer.start()
            val jdbcUrl = "jdbc:postgresql://${postgresContainer.host}:${postgresContainer.firstMappedPort}/${postgresContainer.databaseName}"

            Flyway.configure()
                .dataSource(jdbcUrl, postgresContainer.username, postgresContainer.password)
                .load()
                .migrate()

            Database.connect(jdbcUrl, user = postgresContainer.username, password = postgresContainer.password)
        }
    }

    @Before
    fun setUp() {
        // TODO Clear tables
    }

    @Test
    fun getEmptyUserList() {
        val users = transaction { UserDAO.all().toList() }
        assertEquals(0, users.size)
    }
}