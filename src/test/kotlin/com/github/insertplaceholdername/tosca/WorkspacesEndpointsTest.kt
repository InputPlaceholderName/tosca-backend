package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.api.UpdateWorkspace
import com.github.insertplaceholdername.tosca.api.WorkspaceUser
import com.github.insertplaceholdername.tosca.api.workspaces
import com.github.insertplaceholdername.tosca.auth.ApiUser
import com.github.insertplaceholdername.tosca.auth.Group
import com.github.insertplaceholdername.tosca.db.FullWorkspace
import com.github.insertplaceholdername.tosca.db.FullWorkspaceUser
import com.github.insertplaceholdername.tosca.db.Role
import com.github.insertplaceholdername.tosca.db.Signup
import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.WaitingList
import com.github.insertplaceholdername.tosca.db.Workspace
import com.github.insertplaceholdername.tosca.persistance.UserRepository
import com.github.insertplaceholdername.tosca.persistance.WorkspaceRepository
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Instant

class WorkspacesEndpointsTest {
    @Test
    fun `get workspaces as super user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))

        val workspaces = (0 until 5).map {
            Workspace(it, null, "n$it", "i$it", Instant.now())
        }.toList()

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun allWorkspaces(): List<Workspace> = workspaces
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/workspaces") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: List<Workspace> = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals(workspaces, response)
            }
        }
    }

    @Test
    fun `cant get workspaces as admin or everyone`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin, Group.Everyone))

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun allWorkspaces(): List<Workspace> = listOf()
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/workspaces") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `get workspace as superuser`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val signups = (0..5).map {
            Signup(it, 0, "t$it", "i$it", 2)
        }.toList()

        val waitingLists = (0..5).map {
            WaitingList(it, 0, "t$it", "i$it")
        }.toList()

        val users = (0..5).map {
            FullWorkspaceUser(it, User(it, "user$it", "f$it", "l$it"), listOf(Role.Normal, Role.Admin)[it % 2])
        }

        val workspace = FullWorkspace(0, "n", "i", null, Instant.now(), signups, waitingLists, users)

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/workspaces/0") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: FullWorkspace = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals(workspace, response)
            }
        }
    }

    @Test
    fun `get workspace as a member`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val signups = (0..5).map {
            Signup(it, 0, "t$it", "i$it", 2)
        }.toList()

        val waitingLists = (0..5).map {
            WaitingList(it, 0, "t$it", "i$it")
        }.toList()

        val user = FullWorkspaceUser(0, User(currentUser.id, "user", "f", "l"), Role.Normal)
        val workspace = FullWorkspace(0, "n", "i", null, Instant.now(), signups, waitingLists, listOf(user))

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/workspaces/0") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: FullWorkspace = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals(workspace, response)
            }
        }
    }

    @Test
    fun `can't get workspace as a non-member`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val signups = (0..5).map {
            Signup(it, 0, "t$it", "i$it", 2)
        }.toList()

        val waitingLists = (0..5).map {
            WaitingList(it, 0, "t$it", "i$it")
        }.toList()

        val user = FullWorkspaceUser(0, User(currentUser.id + 1, "user", "f", "l"), Role.Normal)
        val workspace = FullWorkspace(0, "n", "i", null, Instant.now(), signups, waitingLists, listOf(user))

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Get, "/workspaces/0") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `create workspace as super user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val user = User(0, currentUser.userId, "test", "test")
        val data = mutableListOf<Workspace>()
        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun storeWorkspace(name: String, information: String, creator: Int?): Workspace {
                        val wspace = Workspace(0, user, name, information)
                        data.add(wspace)
                        return wspace
                    }

                    override fun allWorkspaces(): List<Workspace> = data
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Post, "/workspaces") {
                    withValidLogin()
                    setJsonContentType()
                    setBody("{ \"name\": \"name\", \"information\": \"info\"}")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: Workspace = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals("name", response.name)
                assertEquals("info", response.information)
            }
        }
    }

    @Test
    fun `Non-admin should not be able to create workspace`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val user = User(0, currentUser.userId, "test", "test")
        val data = mutableListOf<Workspace>()
        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun storeWorkspace(name: String, information: String, creator: Int?): Workspace {
                        val wspace = Workspace(0, user, name, information)
                        data.add(wspace)
                        return wspace
                    }

                    override fun allWorkspaces(): List<Workspace> = data
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Post, "/workspaces") {
                    withValidLogin()
                    setJsonContentType()
                    setBody("{ \"name\": \"name\", \"information\": \"info\"}")
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertEquals(0, data.size)
            }
        }
    }

    @Test
    fun `update workspace as a SuperUser user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val user = User(0, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "original_name", "original_info", user, Instant.now(), listOf(), listOf(), listOf())
        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun updateWorkspace(
                        id: Int,
                        name: String?,
                        information: String?,
                        creator: Int?
                    ): FullWorkspace {
                        val newCreator = creator?.let {
                            User(it, "user2", "f2", "l2")
                        }
                        return workspace.copy(name = name ?: workspace.name, information = information ?: workspace.information, creator = newCreator)
                    }

                    override fun getWorkspace(id: Int): FullWorkspace? {
                        return workspace
                    }
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Patch, "/workspaces/0") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(UpdateWorkspace("name2", creator = 2))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: FullWorkspace = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals("name2", response.name)
                assertEquals(workspace.information, response.information)
                assertEquals(2, response.creator?.id)
            }
        }
    }

    @Test
    fun `update workspace as a creator user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val user = User(0, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "original_name", "original_info", user, Instant.now(), listOf(), listOf(), listOf())
        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun updateWorkspace(
                        id: Int,
                        name: String?,
                        information: String?,
                        creator: Int?
                    ): FullWorkspace {
                        val newCreator = creator?.let {
                            User(it, "user2", "f2", "l2")
                        }
                        return workspace.copy(name = name ?: workspace.name, information = information ?: workspace.information, creator = newCreator)
                    }

                    override fun getWorkspace(id: Int): FullWorkspace? {
                        return workspace
                    }
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Patch, "/workspaces/0") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(UpdateWorkspace("name2", creator = 2))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response: FullWorkspace = Json.decodeFromString(response.content ?: error("Failed to get content"))
                assertEquals("name2", response.name)
                assertEquals(workspace.information, response.information)
                assertEquals(2, response.creator?.id)
            }
        }
    }

    @Test
    fun `update workspace as a non-creator user`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin))
        val creator = User(1, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "original_name", "original_info", creator, Instant.now(), listOf(), listOf(), listOf())
        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace? {
                        return workspace
                    }
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Patch, "/workspaces/0") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(UpdateWorkspace("name2", creator = 2))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `add user to workspace as a SuperUser`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val creator = User(1, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "w1", "", creator, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun addUser(workspaceId: Int, userId: Int, role: Role): FullWorkspaceUser {
                        return FullWorkspaceUser(0, User(userId, "id", "f1", "l1"), role)
                    }

                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Post, "/workspaces/${workspace.id}/users") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(WorkspaceUser(1, Role.Normal))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val data: FullWorkspaceUser = Json.decodeFromString(response.content ?: error("Fail"))
                assertEquals(Role.Normal, data.role)
                assertEquals(1, data.user.id)
            }
        }
    }

    @Test
    fun `add user to workspace as a creator`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val creator = User(0, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "w1", "", creator, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun addUser(workspaceId: Int, userId: Int, role: Role): FullWorkspaceUser {
                        return FullWorkspaceUser(0, User(userId, "id", "f1", "l1"), role)
                    }

                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Post, "/workspaces/${workspace.id}/users") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(WorkspaceUser(1, Role.Normal))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                val data: FullWorkspaceUser = Json.decodeFromString(response.content ?: error("Fail"))
                assertEquals(Role.Normal, data.role)
                assertEquals(1, data.user.id)
            }
        }
    }

    @Test
    fun `add user to workspace as a non-creator should fail`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin))
        val creator = User(1, currentUser.userId, "test", "test")
        val workspace = FullWorkspace(0, "w1", "", creator, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Post, "/workspaces/${workspace.id}/users") {
                    withValidLogin()
                    setJsonContentType()
                    val body = Json.encodeToString(WorkspaceUser(1, Role.Normal))
                    setBody(body)
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `delete workspace as SuperUser`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.SuperUser))
        val workspace = FullWorkspace(0, "w1", "", null, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                    override fun deleteWorkspace(id: Int) {}
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Delete, "/workspaces/${workspace.id}") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun `delete workspace as creator`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Everyone))
        val creator = User(currentUser.id, currentUser.userId, "f1", "l1")
        val workspace = FullWorkspace(0, "w1", "", creator, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                    override fun deleteWorkspace(id: Int) {}
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Delete, "/workspaces/${workspace.id}") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun `delete workspace a non-creator or SuperUser should fail`() {
        val currentUser = ApiUser(0, "user1", listOf(Group.Admin))
        val creator = User(1, "user2", "f1", "l1")
        val workspace = FullWorkspace(0, "w1", "", creator, Instant.now(), listOf(), listOf(), listOf())

        runTest(currentUser, {
            workspaces(
                object : UserRepository {},
                object : WorkspaceRepository {
                    override fun getWorkspace(id: Int): FullWorkspace = workspace
                    override fun deleteWorkspace(id: Int) {
                        error("Should not be called")
                    }
                }
            )
        }) {
            with(
                handleRequest(HttpMethod.Delete, "/workspaces/${workspace.id}") {
                    withValidLogin()
                }
            ) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }
}
