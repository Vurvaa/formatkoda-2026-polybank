package ru.formatkoda.trafficgenerator.repository

import org.jetbrains.exposed.v1.core.Table
import ru.formatkoda.trafficgenerator.domain.AccountStatus
import ru.formatkoda.trafficgenerator.domain.AccountType
import ru.formatkoda.trafficgenerator.domain.UserSegment

object UsersTable : Table("generator_users") {
    val id = long("id").autoIncrement()
    val login = varchar("login", 255).uniqueIndex()
    val password = varchar("password", 255)
    val token = text("token")
    val userSegment = enumerationByName<UserSegment>("user_segment", 20)

    override val primaryKey = PrimaryKey(id)
}

object AccountsTable : Table("generator_accounts") {
    val id = long("id").autoIncrement()
    val number = varchar("number", 20).uniqueIndex()
    val userId = long("user_id").references(UsersTable.id)
    val balance = decimal("balance", 19, 2)
    val type = enumerationByName<AccountType>("type", 20)
    val status = enumerationByName<AccountStatus>("status", 20)

    override val primaryKey = PrimaryKey(id)
}