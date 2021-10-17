package com.github.insertplaceholdername.tosca.persistance

import com.github.insertplaceholdername.tosca.db.FullUser
import com.github.insertplaceholdername.tosca.db.User
import com.github.insertplaceholdername.tosca.db.UserDAO
import com.github.insertplaceholdername.tosca.db.UserWorkspaceDAO
import com.github.insertplaceholdername.tosca.db.Users
import com.github.insertplaceholdername.tosca.db.UsersWorkspaces
import com.github.insertplaceholdername.tosca.db.toFullUserWorkspaceList
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun allUsers(): List<User> = listOf()
    fun storeUser(userId: String, firstName: String, lastName: String): User = throw NotImplementedError()
    fun getUser(userId: String): FullUser? = throw NotImplementedError()
    fun getUser(userId: Int): FullUser? = throw NotImplementedError()
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

    override fun getUser(userId: String): FullUser? {
        return transaction {
            UserDAO.find { Users.userId eq userId }.firstOrNull().toFullUser()
        }
    }

    override fun getUser(userId: Int): FullUser? {
        return transaction {
            UserDAO.findById(userId).toFullUser()
        }
    }

    private fun UserDAO?.toFullUser(): FullUser? {
        return this?.let {
            val userWorkspaces = UserWorkspaceDAO.find { UsersWorkspaces.user eq it.id }
            FullUser(it.id.value, it.userId, it.firstName, it.lastName, userWorkspaces.toFullUserWorkspaceList())
        }
    }
}
