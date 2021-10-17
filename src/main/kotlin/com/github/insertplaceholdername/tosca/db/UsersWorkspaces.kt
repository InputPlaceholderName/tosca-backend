package com.github.insertplaceholdername.tosca.db

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

@Serializable(with = RoleSerializer::class)
enum class Role(role: String) {
    Admin("Admin"),
    Normal("Normal");

    companion object {
        fun fromString(str: String): Role =
            when (str.lowercase()) {
                "admin" -> Role.Admin
                else -> Role.Normal
            }
    }

    override fun toString(): String =
        when (this) {
            Admin -> "Admin"
            Normal -> "Normal"
        }
}

class RoleSerializer : KSerializer<Role> {
    override fun deserialize(decoder: Decoder): Role = Role.fromString(decoder.decodeString())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Role", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Role) = encoder.encodeString(value.toString())
}

object UsersWorkspaces : IntIdTable("users_workspaces") {
    val user = reference("user_id", Users)
    val workspace = reference("workspace", Workspaces)
    val role = text("role")
}

class UserWorkspaceDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserWorkspaceDAO>(UsersWorkspaces)

    var user by UserDAO referencedOn UsersWorkspaces.user
    var userId by UsersWorkspaces.user
    var workspace by WorkspaceDAO referencedOn UsersWorkspaces.workspace
    var workspaceId by UsersWorkspaces.workspace
    var role by UsersWorkspaces.role.transform(Role::toString, Role::fromString)

    fun toModel() = UserWorkspace(id.value, userId.value, workspaceId.value, role)
    fun toFullWorkspaceModel() = FullUserWorkspace(id.value, workspace.toModel(), role)
    fun toFullUserModel() = FullWorkspaceUser(id.value, user.toModel(), role)
}

@Serializable
data class UserWorkspace(val id: Int, val user: Int, val workspace: Int, val role: Role)

@Serializable
data class FullUserWorkspace(val id: Int, val workspace: Workspace, val role: Role)

@Serializable
data class FullWorkspaceUser(val id: Int, val user: User, val role: Role)

fun Iterable<UserWorkspaceDAO>.toWorkspaceModelList(): List<UserWorkspace> = map(UserWorkspaceDAO::toModel).toList()

fun Iterable<UserWorkspaceDAO>.toFullWorkspaceUserList(): List<FullWorkspaceUser> = map(UserWorkspaceDAO::toFullUserModel).toList()
fun Iterable<UserWorkspaceDAO>.toFullUserWorkspaceList(): List<FullUserWorkspace> = map(UserWorkspaceDAO::toFullWorkspaceModel).toList()
