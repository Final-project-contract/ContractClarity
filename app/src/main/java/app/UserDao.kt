package app

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

class UserDao {
    fun create(user: User): Int? {
        return transaction {
            Users.insert {
                it[email] = user.email
                it[password] = BCrypt.hashpw(user.password, BCrypt.gensalt())
                it[fullName] = user.fullName
                it[industry] = user.industry
            } get Users.id
        }
    }

    fun findByEmail(email: String): User? {
        return transaction {
            Users.select { Users.email eq email }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun authenticate(email: String, password: String): Int? {
        return transaction {
            val user = Users.select { Users.email eq email }
                .mapNotNull { toUser(it) }
                .singleOrNull()

            if (user != null && BCrypt.checkpw(password, user.password)) {
                user.id
            } else {
                null
            }
        }
    }

    private fun toUser(row: ResultRow): User =
        User(
            id = row[Users.id],
            email = row[Users.email],
            password = row[Users.password],
            fullName = row[Users.fullName],
            industry = row[Users.industry]
        )
}

data class User(
    val id: Int = 0,
    val email: String,
    val password: String,
    val fullName: String,
    val industry: String
)