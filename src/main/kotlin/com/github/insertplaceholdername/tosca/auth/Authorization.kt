package com.github.insertplaceholdername.tosca.auth

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = GroupsSerializer::class)
enum class Groups(private val level: Int) {
    Everyone(1),
    Admin(2),
    SuperUser(3);

    companion object {
        fun fromString(str: String): Groups =
            when (str.lowercase()) {
                "admin" -> Admin
                "superuser" -> SuperUser
                else -> Everyone
            }
    }

    override fun toString(): String =
        when (this) {
            Everyone -> "Everyone"
            Admin -> "Admin"
            SuperUser -> "SuperUser"
        }
}

class GroupsSerializer : KSerializer<Groups> {
    override fun deserialize(decoder: Decoder): Groups =
        Groups.fromString(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Group", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Groups) {
        encoder.encodeString(value.toString())
    }
}

@Serializable
data class ApiUser(val userId: String, val groups: List<Groups>)

interface Authorization {
    fun user(): ApiUser
    fun hasGroup(vararg groups: Groups) = user().groups.any { groups.contains(it) }
    fun isAtLeast(group: Groups): Boolean = user().groups.any { it >= group }
}

class JwtAuthorization(private val call: ApplicationCall) : Authorization {
    override fun user(): ApiUser {
        val principal = call.authentication.principal<JWTPrincipal>()
        return Json.decodeFromString(principal?.get(JwtConfig.claimKey) ?: throw Exception("Can only be called within authorization block"))
    }
}
