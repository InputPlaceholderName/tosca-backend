package com.github.insertplaceholdername.tosca.db

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class UserDAOTest {
    companion object {
        @ClassRule
        @JvmField
        val db = PostgresContainer
    }

    @Before
    fun clearDB() {
        db.clear()
    }

    @Test
    fun getEmptyUserList() {
        val users = transaction { UserDAO.all().toList() }
        assertEquals(0, users.size)
    }

    @Test
    fun addUser() {
        transaction {
            UserDAO.new {
                userId = "Test"
                firstName = "Adam"
                lastName = "Svensson"
            }
        }
        val users = transaction { UserDAO.all().toList() }
        assertEquals(1, users.size)
    }
}
