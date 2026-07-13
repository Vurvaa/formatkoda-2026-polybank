package ru.formatkoda.generator

import ru.formatkoda.model.SimulatedAccount
import ru.formatkoda.model.SimulatedUser
import java.math.BigDecimal

interface GeneratorState {
    suspend fun getUser(login: String): SimulatedUser?
    suspend fun getRandomUser(): SimulatedUser?
    suspend fun getRandomActiveAccount(login: String): SimulatedAccount?

    suspend fun addUser(user: SimulatedUser)
    suspend fun addAccount(login: String, account: SimulatedAccount)

    suspend fun increaseBalance(number: String, amount: BigDecimal): Boolean
    suspend fun reserveDebit(accountNumber: String, amount: BigDecimal): Boolean
    suspend fun rollbackDebit(accountNumber: String, amount: BigDecimal): Boolean
    suspend fun completeTransfer(toAccountNumber: String, amount: BigDecimal): Boolean
    suspend fun replaceBalance(accountNumber: String, balance: BigDecimal): Boolean

    suspend fun updateToken(login: String, token: String)
}