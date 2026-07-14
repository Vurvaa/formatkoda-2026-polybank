package ru.formatkoda.trafficgenerator.repository

import ru.formatkoda.trafficgenerator.domain.SimulatedAccount
import ru.formatkoda.trafficgenerator.domain.SimulatedUser
import java.math.BigDecimal

interface GeneratorRepository {
    suspend fun getUser(id: Long): SimulatedUser?
    suspend fun getRandomUser(): SimulatedUser?
    suspend fun getRandomActiveAccount(userId: Long): SimulatedAccount?

    suspend fun addUser(user: SimulatedUser): SimulatedUser
    suspend fun addAccount(userId: Long, account: SimulatedAccount): SimulatedAccount

    suspend fun increaseBalance(accountNumber: String, amount: BigDecimal): Boolean
    suspend fun reserveDebit(accountNumber: String, amount: BigDecimal): Boolean
    suspend fun replaceBalance(accountNumber: String, balance: BigDecimal): Boolean

    suspend fun updateToken(id: Long, token: String): Boolean
}