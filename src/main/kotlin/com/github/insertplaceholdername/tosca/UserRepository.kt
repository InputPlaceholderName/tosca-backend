package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.UserDAO
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun allUsers(): List<User> = listOf()
    fun storeUser(userId: String, firstName: String, lastName: String)
}

object ExposedUserRepository : UserRepository {
    override fun allUsers() = transaction { UserDAO.all().map { user -> user.toModel() }.toList() }
    override fun storeUser(userId: String, firstName: String, lastName: String) {
        transaction {
            UserDAO.new {
                this.userId = userId
                this.firstName = firstName
                this.lastName = lastName
            }
        }
    }
}
