package com.github.insertplaceholdername.tosca.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Workspaces : IntIdTable() {
    val creator = reference("creator", Users).nullable()
    val name = text("name")
    val info = text("info")
}

class WorkspaceDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkspaceDAO>(Workspaces)

    var creator by UserDAO optionalReferencedOn  Workspaces.creator
    var name by Workspaces.name
    var info by Workspaces.info

    fun toModel() = Workspace(id.value, creator?.toModel() ?: User.unknown(), name, info)
}

@Serializable
data class Workspace(val id: Int, val creator: User, val name: String, val info: String)