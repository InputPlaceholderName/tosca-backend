package com.github.insertplaceholdername.tosca.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val userId = text("user_id").uniqueIndex()
    val firstName = text("first_name")
    val lastName = text("last_name")
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(Users)

    var userId by Users.userId
    var firstName by Users.firstName
    var lastName by Users.lastName
    val workspaces by WorkspaceDAO.via(UsersWorkspaces.user, UsersWorkspaces.workspace)

    fun toModel() = User(id = id.value, userId = userId, firstName = firstName, lastName = lastName)
}

@Serializable
data class User(val id: Int, val userId: String, val firstName: String, val lastName: String) {
    companion object {
        fun unknown() = User(-1, "Unknown", "Unknown", "Unknown")
    }
}

@Serializable
data class FullUser(val id: Int, val userId: String, val firstName: String, val lastName: String, val workspaces: List<FullUserWorkspace>) {
    companion object {
        fun unknown() = User(-1, "Unknown", "Unknown", "Unknown")
    }
}

fun Iterable<UserDAO>.toModel(): List<User> = map(UserDAO::toModel).toList()
