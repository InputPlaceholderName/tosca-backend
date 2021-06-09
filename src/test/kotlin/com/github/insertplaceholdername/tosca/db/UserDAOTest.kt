package com.github.insertplaceholdername.tosca.db


import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import org.junit.ClassRule
import org.junit.rules.ExternalResource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)

object PostgresContainer: ExternalResource() {
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
}

@Testcontainers
internal class UserDAOTest {
    companion object {
        @ClassRule
        @JvmField
        val db = PostgresContainer
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