package com.github.insertplaceholdername.tosca

import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.UserDAO
import com.github.insertplaceholdername.tosca.db.Users
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun allUsers(): List<User> = listOf()
    fun storeUser(userId: String, firstName: String, lastName: String): User
    fun getUser(userId: String): User
    fun getUser(userId: Int): User
}

object ExposedUserRepository : UserRepository {
    override fun allUsers() = transaction { UserDAO.all().map { user -> user.toModel() }.toList() }

    override fun storeUser(userId: String, firstName: String, lastName: String): User {
        return transaction {
            UserDAO.find { Users.userId eq userId }.firstOrNull()?.let {
                it.firstName = firstName
                it.lastName = lastName

                return@transaction it.toModel()
            }

            UserDAO.new {
                this.userId = userId
                this.firstName = firstName
                this.lastName = lastName
            }.toModel()
        }
    }

    override fun getUser(userId: String): User {
        return transaction {
            UserDAO.find { Users.userId eq userId }.first().toModel()
        }
    }

    override fun getUser(userId: Int): User {
        return transaction {
            UserDAO.findById(userId)!!.toModel()
        }
    }
}
