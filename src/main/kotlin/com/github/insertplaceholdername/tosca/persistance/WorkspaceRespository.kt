package com.github.insertplaceholdername.tosca.persistance

import com.github.insertplaceholdername.tosca.db.FullWorkspace
import com.github.insertplaceholdername.tosca.db.FullWorkspaceUser
import com.github.insertplaceholdername.tosca.db.Role
import com.github.insertplaceholdername.tosca.db.UserDAO
import com.github.insertplaceholdername.tosca.db.UserWorkspaceDAO
import com.github.insertplaceholdername.tosca.db.UsersWorkspaces
import com.github.insertplaceholdername.tosca.db.Workspace
import com.github.insertplaceholdername.tosca.db.WorkspaceDAO
import com.github.insertplaceholdername.tosca.db.toFullWorkspaceUserList
import com.github.insertplaceholdername.tosca.db.toModelList
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

interface WorkspaceRepository {
    fun allWorkspaces(): List<Workspace> = listOf()
    fun storeWorkspace(name: String, information: String, creator: Int? = null): Workspace = throw NotImplementedError()
    fun deleteWorkspace(id: Int): Unit = throw NotImplementedError()
    fun getWorkspace(id: Int): FullWorkspace? = null
    fun updateWorkspace(id: Int, name: String? = null, information: String? = null, creator: Int? = null): FullWorkspace = throw NotImplementedError()
    fun addUser(workspaceId: Int, userId: Int, role: Role): FullWorkspaceUser = throw NotImplementedError()
    fun deleteUser(workspaceId: Int, userId: Int): Unit = throw NotImplementedError()
}

object ExposedWorkspaceRepository : WorkspaceRepository {
    override fun storeWorkspace(name: String, information: String, creator: Int?): Workspace = transaction {
        val userCreator = creator?.let { UserDAO.findById(creator) }

        WorkspaceDAO.new {
            this.name = name
            this.creator = userCreator
            this.information = information
        }.toModel()
    }

    override fun getWorkspace(id: Int): FullWorkspace? = transaction {
        WorkspaceDAO.findById(id)?.load(WorkspaceDAO::signups, WorkspaceDAO::waitingLists, WorkspaceDAO::users)?.toFullModel()
    }

    override fun updateWorkspace(id: Int, name: String?, information: String?, creator: Int?): FullWorkspace = transaction {
        val workspace = WorkspaceDAO.findById(id) ?: error("Workspace does not exist")

        name?.let { workspace.name = it }
        information?.let { workspace.information = it }
        workspace.creator = creator?.let { UserDAO.findById(creator) }

        workspace.toFullModel()
    }

    override fun allWorkspaces(): List<Workspace> = transaction {
        WorkspaceDAO.all().toModelList()
    }

    /**
     * Adds a user to the workspace with a given role. If the user already has a role, the role of the user is updated.
     */
    override fun addUser(workspaceId: Int, userId: Int, role: Role): FullWorkspaceUser = transaction {
        (
            UserWorkspaceDAO.find { UsersWorkspaces.workspace eq workspaceId and (UsersWorkspaces.user eq userId) }
                .firstOrNull()?.apply {
                    this.role = role
                } ?: UserWorkspaceDAO.new {
                this.user = UserDAO.findById(userId) ?: error("User does not exist")
                this.workspace = WorkspaceDAO.findById(workspaceId) ?: error("Workspace does not exist")
                this.role = role
            }
            ).let {
            FullWorkspaceUser(it.id.value, it.user.toModel(), it.role)
        }
    }

    override fun deleteUser(workspaceId: Int, userId: Int) = transaction {
        UserWorkspaceDAO.find { UsersWorkspaces.workspace eq workspaceId and (UsersWorkspaces.user eq userId) }
            .forEach(UserWorkspaceDAO::delete)
    }

    override fun deleteWorkspace(id: Int) {
        transaction {
            WorkspaceDAO.findById(id)?.delete()
        }
    }

    /**
     * Must be called within a transaction
     */
    private fun Iterable<WorkspaceDAO>.toFullModelList(): List<FullWorkspace> = map(WorkspaceDAO::toFullModel).toList()
}

private fun WorkspaceDAO.toFullModel(): FullWorkspace {
    return FullWorkspace(
        id.value,
        name,
        information,
        creator?.toModel(),
        createdAt,
        signups.toModelList(),
        waitingLists.toModelList(),
        UserWorkspaceDAO.find { UsersWorkspaces.workspace eq id.value }.toFullWorkspaceUserList()
    )
}
