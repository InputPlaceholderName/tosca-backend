package com.github.insertplaceholdername.tosca.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*

object Users : IdTable<String>() {
    override val id: Column<EntityID<String>> = text("id").entityId()
    val firstName = text("first_name")
    val lastName = text("last_name")
    override val primaryKey = PrimaryKey(id)
}

class UserDAO(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserDAO>(Users)
    var firstName by Users.firstName
    var lastName by Users.lastName
    fun toModel() = User(id=id.value, firstName=firstName, lastName=lastName)
}

class User(val id: String, val firstName: String, val lastName: String)