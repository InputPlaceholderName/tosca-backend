package com.github.insertplaceholdername.tosca.db

import com.github.insertplaceholdername.tosca.ExposedUserRepository
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

internal class ExposedUserRepositoryTest {
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
    fun storeUser() {
        val user = ExposedUserRepository.storeUser("test", "firstname", "lastname")
        assertEquals("test", user.userId)
        assertEquals("firstname", user.firstName)
        assertEquals("lastname", user.lastName)
    }

    @Test
    fun allUsers() {
        val users = listOf(
            ExposedUserRepository.storeUser("id1", "f1", "l1"),
            ExposedUserRepository.storeUser("id2", "f2", "l2")
        )

        val storedUsers = ExposedUserRepository.allUsers()
        assertEquals(users, storedUsers)
    }

    @Test
    fun updateUser() {
        ExposedUserRepository.storeUser("id1", "f1", "l1")
        val user = ExposedUserRepository.storeUser("id1", "f2", "l2")

        assertEquals("id1", user.userId)
        assertEquals("f2", user.firstName)
        assertEquals("l2", user.lastName)
    }
}
