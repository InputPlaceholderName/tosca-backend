package com.github.insertplaceholdername.tosca.db

import com.github.insertplaceholdername.tosca.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

object Workspaces : IntIdTable("workspaces") {
    val creator = reference("creator", Users).nullable()
    val name = text("name")
    val information = text("information")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

class WorkspaceDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkspaceDAO>(Workspaces)

    var creator by UserDAO optionalReferencedOn Workspaces.creator
    var name by Workspaces.name
    var information by Workspaces.information
    var createdAt by Workspaces.createdAt
    val signups by SignupDAO referrersOn Signups.workspace
    val waitingLists by WaitingListDAO referrersOn WaitingLists.workspace
    val users by UserDAO.via(UsersWorkspaces.workspace, UsersWorkspaces.user)

    fun toModel() = Workspace(id.value, creator?.toModel(), name, information, createdAt)
}

@Serializable
data class Workspace(
    val id: Int,
    val creator: User?,
    val name: String,
    val information: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null
)

@Serializable
data class FullWorkspace(
    val id: Int,
    val name: String,
    val information: String,
    val creator: User?,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val signups: List<Signup>,
    val waitingLists: List<WaitingList>,
    val users: List<FullWorkspaceUser>
) {

    fun userIsMember(userId: Int): Boolean = users.any { it.user.id == userId }
    fun userIsNotMember(userId: Int): Boolean = !userIsMember(userId)
}

fun Iterable<WorkspaceDAO>.toModelList(): List<Workspace> = map(WorkspaceDAO::toModel).toList()
