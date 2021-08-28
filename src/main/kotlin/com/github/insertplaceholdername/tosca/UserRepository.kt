package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.UserDAO
import com.github.insertplaceholdername.tosca.db.Users
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun allUsers(): List<User> = listOf()
    fun storeUser(userId: String, firstName: String, lastName: String): User
}

object ExposedUserRepository : UserRepository {
    override fun allUsers() = transaction { UserDAO.all().map { user -> user.toModel() }.toList() }

    override fun storeUser(newUserId: String, firstName: String, lastName: String): User {
        return transaction {
            val existingUser = UserDAO.find { Users.userId eq newUserId }.firstOrNull()

            val user = if (existingUser != null) {
                existingUser.firstName = firstName
                existingUser.lastName = lastName

                existingUser
            } else {
                UserDAO.new {
                    this.userId = newUserId
                    this.firstName = firstName
                    this.lastName = lastName
                }
            }

            user.toModel()
        }
    }
}
