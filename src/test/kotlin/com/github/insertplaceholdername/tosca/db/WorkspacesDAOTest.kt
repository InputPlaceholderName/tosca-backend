package com.github.insertplaceholdername.tosca.db

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class WorkspacesDAOTest {
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
    fun getEmptyWorkspacesList() {
        val users = transaction { WorkspaceDAO.all().toList() }
        assertEquals(0, users.size)
    }

    @Test
    fun addWorkspace() {
        transaction {
            val user = UserDAO.new {
                userId = "Test"
                firstName = "Adam"
                lastName = "Svensson"
            }

            WorkspaceDAO.new {
                creator = user
                name = "Workspace1"
                info = "Info"
            }
        }

        val workspaces = transaction { WorkspaceDAO.all().toList() }
        assertEquals(1, workspaces.size)
    }
}
