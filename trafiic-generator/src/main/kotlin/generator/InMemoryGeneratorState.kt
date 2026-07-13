package ru.formatkoda.generator

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.formatkoda.model.SimulatedAccount
import ru.formatkoda.model.SimulatedUser
import java.math.BigDecimal
import kotlin.minus
import kotlin.plus

class InMemoryGeneratorState: GeneratorState {
    private val mutex = Mutex()
    private val users = mutableMapOf<String, SimulatedUser>()

    override suspend fun getUser(login: String) =
        mutex.withLock {
            users[login]?.deepCopy()
        }

    override suspend fun getRandomUser() =
        mutex.withLock {
            users.values
                .randomOrNull()
                ?.deepCopy()
        }

    override suspend fun getRandomActiveAccount(login: String) =
        mutex.withLock {
            users[login]
                ?.randomActiveAccount()
                ?.copy()
        }

    override suspend fun addUser(user: SimulatedUser) =
        mutex.withLock {
            users[user.login] = user.deepCopy()
        }

    override suspend fun addAccount(login: String, account: SimulatedAccount) =
        mutex.withLock {
            val user = users[login] ?: return@withLock

            if (user.accounts.any { it.number == account.number })
                return@withLock

            user.accounts += account.copy()
        }

    override suspend fun increaseBalance(number: String, amount: BigDecimal): Boolean =
        mutex.withLock {
            val account = findAccount(number) ?: return@withLock false

            account.balance += amount
            true
        }

    override suspend fun reserveDebit(accountNumber: String, amount: BigDecimal) =
        mutex.withLock {
            require(amount > BigDecimal.ZERO) {
                "amount must be positive"
            }

            val account = findAccount(accountNumber) ?: return@withLock false

            if (account.balance < amount)
                return@withLock false

            account.balance -= amount
            true
        }

    override suspend fun rollbackDebit(accountNumber: String, amount: BigDecimal) =
        mutex.withLock {
            val account = findAccount(accountNumber) ?: return@withLock false

            account.balance += amount
            true
        }

    override suspend fun completeTransfer(toAccountNumber: String, amount: BigDecimal) =
        mutex.withLock {
            val account = findAccount(toAccountNumber) ?: return@withLock false

            account.balance += amount
            true
        }

    override suspend fun replaceBalance(accountNumber: String, balance: BigDecimal) =
        mutex.withLock {
            val account = findAccount(accountNumber) ?: return@withLock false

            account.balance = balance
            true
        }

    override suspend fun updateToken(login: String, token: String) =
        mutex.withLock {
            users[login]?.token = token
        }

    private fun findAccount(accountNumber: String) =
        users.values
            .asSequence()
            .flatMap { it.accounts.asSequence() }
            .firstOrNull { it.number == accountNumber }
}