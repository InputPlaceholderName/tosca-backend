package com.github.insertplaceholdername.tosca.db


import org.junit.jupiter.api.BeforeEach
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)

@Testcontainers
internal class UserDAOTest {
    @Container
    val postgresContainer = MyPostgreSQLContainer("postgres:13.3")
        .withDatabaseName("db")
        .withUsername("user")
        .withPassword("password")

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun containerTest() {
        postgresContainer.start()
    }
}