package ru.formatkoda.trafficgenerator.repository

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import ru.formatkoda.trafficgenerator.domain.AccountStatus
import ru.formatkoda.trafficgenerator.domain.SimulatedAccount
import ru.formatkoda.trafficgenerator.domain.SimulatedUser
import java.math.BigDecimal

class PostgresGeneratorRepository(
    private val database: Database
) : GeneratorRepository {

    override suspend fun getUser(id: Long): SimulatedUser? =
        dbQuery {
            val userRow = UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?: return@dbQuery null

            userRow.toUser(accounts = findAccounts(id))
        }

    override suspend fun getUserByAccountId(id: Long): SimulatedUser? =
        dbQuery {
            val accountRow = AccountsTable
                .selectAll()
                .where { AccountsTable.id eq id }
                .singleOrNull()
                ?: return@dbQuery null

            val userId = accountRow[AccountsTable.userId]

            val userRow = UsersTable
                .selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?: return@dbQuery null

            userRow.toUser(accounts = findAccounts(userId))
        }

    override suspend fun getRandomUser(): SimulatedUser? =
        dbQuery {
            val userRow = UsersTable
                .selectAll()
                .orderBy(Random())
                .limit(1)
                .singleOrNull()
                ?: return@dbQuery null

            val id = userRow[UsersTable.id]

            userRow.toUser(accounts = findAccounts(id))
        }

    override suspend fun getAccountForTopUp(): SimulatedAccount? =
        getActiveAccount(AccountsTable.balance less BigDecimal.ONE) ?: getActiveAccount()

    override suspend fun getAccountForWithdraw(): SimulatedAccount? =
        getActiveAccount(AccountsTable.balance greaterEq BigDecimal.ONE) ?: getActiveAccount()

    override suspend fun getUsersCount(): Long =
        dbQuery {
            UsersTable
                .selectAll()
                .count()
        }

    override suspend fun addUser(user: SimulatedUser): SimulatedUser =
        dbQuery {
            val statement = UsersTable.insert {
                it[login] = user.login
                it[password] = user.password
                it[token] = user.token
                it[userSegment] = user.userSegment
            }

            user.copy(id = statement[UsersTable.id])
        }

    override suspend fun addAccount(userId: Long, account: SimulatedAccount): SimulatedAccount =
        dbQuery {
            val statement = AccountsTable.insert {
                it[number] = account.number
                it[AccountsTable.userId] = userId
                it[balance] = account.balance
                it[type] = account.type
                it[status] = account.status
            }

            account.copy(id = statement[AccountsTable.id])
        }

    override suspend fun increaseBalance(accountNumber: String, amount: BigDecimal): Boolean {
        checkAmount(amount)
        return dbQuery {
            AccountsTable.update(
                where = { AccountsTable.number eq accountNumber }
            ) {
                it.update(
                    AccountsTable.balance,
                    AccountsTable.balance + amount
                )
            } == 1
        }
    }

    override suspend fun reserveDebit(accountNumber: String, amount: BigDecimal): Boolean {
        checkAmount(amount)

        return dbQuery {
            AccountsTable.update(
                where = {
                    (AccountsTable.number eq accountNumber) and
                            (AccountsTable.balance greaterEq amount)
                }
            ) {
                it.update(
                    AccountsTable.balance,
                    AccountsTable.balance - amount
                )
            } == 1
        }
    }

    override suspend fun replaceBalance(accountNumber: String, balance: BigDecimal): Boolean {
        require(balance >= BigDecimal.ZERO) {
            "balance must not be negative"
        }

        return dbQuery {
            AccountsTable.update(
                where = { AccountsTable.number eq accountNumber }
            ) {
                it[AccountsTable.balance] = balance
            } == 1
        }
    }

    override suspend fun updateToken(id: Long, token: String) =
        dbQuery {
            UsersTable.update(
                where = { UsersTable.id eq id }
            ) { it[UsersTable.token] = token }
        } == 1

    override suspend fun clearAll() {
        dbQuery {
            AccountsTable.deleteAll()
            UsersTable.deleteAll()
        }
    }

    private suspend fun getActiveAccount(condition: Op<Boolean> = Op.TRUE): SimulatedAccount? =
        dbQuery {
            AccountsTable
                .selectAll()
                .where { condition and (AccountsTable.status eq AccountStatus.ACTIVE) }
                .orderBy(Random())
                .limit(1)
                .singleOrNull()
                ?.toAccount()
        }

    private fun findAccounts(userId: Long): MutableList<SimulatedAccount> =
        AccountsTable
            .selectAll()
            .where { AccountsTable.userId eq userId }
            .mapTo(mutableListOf()) { it.toAccount() }

    private fun ResultRow.toUser(accounts: MutableList<SimulatedAccount>): SimulatedUser =
        SimulatedUser(
            id = this[UsersTable.id],
            login = this[UsersTable.login],
            password = this[UsersTable.password],
            token = this[UsersTable.token],
            userSegment = this[UsersTable.userSegment],
            accounts = accounts
        )

    private fun ResultRow.toAccount(): SimulatedAccount =
        SimulatedAccount(
            id = this[AccountsTable.id],
            number = this[AccountsTable.number],
            balance = this[AccountsTable.balance],
            type = this[AccountsTable.type],
            status = this[AccountsTable.status]
        )

    private suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        suspendTransaction(db = database, statement = block)

    private fun checkAmount(amount: BigDecimal) =
        require(amount > BigDecimal.ZERO) {
            "amount must be positive"
        }
}