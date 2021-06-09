package com.github.insertplaceholdername.tosca.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val userId = text("user_id").uniqueIndex()
    val firstName = text("first_name")
    val lastName = text("last_name")
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(Users)

    var userId by Users.userId
    var firstName by Users.firstName
    var lastName by Users.lastName
    fun toModel() = User(id=id.value, userId=userId, firstName=firstName, lastName=lastName)
}

class User(id: Int, val userId: String, val firstName: String, val lastName: String)