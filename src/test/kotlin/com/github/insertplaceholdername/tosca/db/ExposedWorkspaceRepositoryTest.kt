package com.github.insertplaceholdername.tosca.db

import com.github.insertplaceholdername.tosca.persistance.ExposedUserRepository
import com.github.insertplaceholdername.tosca.persistance.ExposedWorkspaceRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

internal class ExposedWorkspaceRepositoryTest {
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
    fun storeWorkspace() {
        val workspace = ExposedWorkspaceRepository.storeWorkspace("test", "info", null)
        assertEquals("test", workspace.name)
        assertEquals("info", workspace.information)
    }

    @Test
    fun allWorkspaces() {
        ExposedWorkspaceRepository.storeWorkspace("w1", "i1")
        ExposedWorkspaceRepository.storeWorkspace("w2", "i2")

        val storedWorkspaces = ExposedWorkspaceRepository.allWorkspaces()
        assertEquals(2, storedWorkspaces.size)
    }

    @Test
    fun updateWorkspace() {
        val originalWspace = ExposedWorkspaceRepository.storeWorkspace("w1", "i1", null)
        ExposedWorkspaceRepository.updateWorkspace(originalWspace.id, "w2", "i2")

        val updated = ExposedWorkspaceRepository.getWorkspace(originalWspace.id) ?: error("Fail")
        assertEquals("w2", updated.name)
        assertEquals("i2", updated.information)
        assertEquals(originalWspace.createdAt, updated.createdAt)
    }

    @Test
    fun getWorkspace() {
        val id = ExposedWorkspaceRepository.storeWorkspace("id1", "i1").id
        val workspace = ExposedWorkspaceRepository.getWorkspace(id) ?: error("Failed to get workspace")
        assertEquals("id1", workspace.name)
        assertEquals("i1", workspace.information)
        assertNull(workspace.creator)
    }

    @Test
    fun setCreator() {
        val user = ExposedUserRepository.storeUser("u1", "f1", "l1")
        val id = ExposedWorkspaceRepository.storeWorkspace("id1", "i1", user.id).id
        val workspace = ExposedWorkspaceRepository.getWorkspace(id) ?: error("Failed to get workspace")
        val creator = workspace.creator ?: error("Failed to get creator")

        assertEquals("u1", creator.userId)
        assertEquals("f1", creator.firstName)
        assertEquals("l1", creator.lastName)
    }

    @Test
    fun getUsers() {
        val user = ExposedUserRepository.storeUser("u1", "f1", "l1")
        val id = ExposedWorkspaceRepository.storeWorkspace("id1", "i1", user.id).id
        ExposedWorkspaceRepository.addUser(id, user.id, Role.Normal)
        val workspace = ExposedWorkspaceRepository.getWorkspace(id) ?: error("Failed to get workspace")

        assertEquals(1, workspace.users.size)
        val testUser = workspace.users[0]
        assertEquals("u1", testUser.user.userId)
        assertEquals("f1", testUser.user.firstName)
        assertEquals("l1", testUser.user.lastName)
        assertEquals(Role.Normal, testUser.role)
    }

    @Test
    fun `update workspace user`() {
        val user = ExposedUserRepository.storeUser("u1", "f1", "l1")
        val id = ExposedWorkspaceRepository.storeWorkspace("id1", "i1", user.id).id
        val workspaceUserId = ExposedWorkspaceRepository.addUser(id, user.id, Role.Normal).id
        assertEquals(listOf(FullWorkspaceUser(workspaceUserId, user, Role.Normal)), ExposedWorkspaceRepository.getWorkspace(id)?.users)
        val workspaceUserId2 = ExposedWorkspaceRepository.addUser(id, user.id, Role.Admin).id
        assertEquals(listOf(FullWorkspaceUser(workspaceUserId2, user, Role.Admin)), ExposedWorkspaceRepository.getWorkspace(id)?.users)
    }

    @Test
    fun `delete workspace user`() {
        val user = ExposedUserRepository.storeUser("u1", "f1", "l1")
        val user2 = ExposedUserRepository.storeUser("u2", "f2", "l2")
        val id = ExposedWorkspaceRepository.storeWorkspace("id1", "i1", user.id).id
        ExposedWorkspaceRepository.addUser(id, user.id, Role.Normal)
        ExposedWorkspaceRepository.addUser(id, user2.id, Role.Normal)

        ExposedWorkspaceRepository.deleteUser(id, user.id)

        val users = ExposedWorkspaceRepository.getWorkspace(id)?.users

        assertEquals(1, users?.size)
        assertEquals(true, users?.none { it.id == user.id })
    }

    @Test
    fun `delete workspace as SuperUser`() {
        val workspace = ExposedWorkspaceRepository.storeWorkspace("w1", "info")
        ExposedWorkspaceRepository.deleteWorkspace(workspace.id)
        assertEquals(0, ExposedWorkspaceRepository.allWorkspaces().size)
    }
}
