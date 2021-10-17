package com.github.insertplaceholdername.tosca.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Signups : IntIdTable("signups") {
    val workspace = reference("workspace", Workspaces)
    val title = text("title")
    val information = text("information")
    val maxUserSignups = integer("max_user_signups")
}

class SignupDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SignupDAO>(Signups)

    var workspace by WorkspaceDAO referencedOn Workspaces.id
    var workspaceId by Signups.workspace
    var information by Signups.information
    var maxUserSignups by Signups.maxUserSignups
    var title by Signups.title

    fun toModel() = Signup(id.value, workspaceId.value, title, information, maxUserSignups)
}

@Serializable
data class Signup(val id: Int, val workspace: Int, val title: String, val information: String, val maxUserSignups: Int)

fun Iterable<SignupDAO>.toModelList(): List<Signup> = map(SignupDAO::toModel).toList()
