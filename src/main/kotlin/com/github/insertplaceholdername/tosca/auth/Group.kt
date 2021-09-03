package com.github.insertplaceholdername.tosca.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = GroupSerializer::class)
enum class Group(private val level: Int) {
    Everyone(1),
    Admin(2),
    SuperUser(3);

    companion object {
        fun fromString(str: String): Group =
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

class GroupSerializer : KSerializer<Group> {
    override fun deserialize(decoder: Decoder): Group =
        Group.fromString(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Group", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Group) {
        encoder.encodeString(value.toString())
    }
}
