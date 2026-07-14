package ru.formatkoda.trafficgenerator.service

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import ru.formatkoda.trafficgenerator.dto.UserRegistrationRequest
import ru.formatkoda.trafficgenerator.domain.AccountStatus
import ru.formatkoda.trafficgenerator.domain.Action
import ru.formatkoda.trafficgenerator.domain.SimulatedAccount
import ru.formatkoda.trafficgenerator.domain.SimulatedUser
import ru.formatkoda.trafficgenerator.domain.TransactionStatus
import ru.formatkoda.trafficgenerator.domain.UserSegment
import ru.formatkoda.trafficgenerator.repository.GeneratorRepository
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.collections.iterator
import kotlin.random.Random

class GeneratorEngine(
    private val bankClient: BankClient,
    private val repository: GeneratorRepository,
    private val config: GeneratorConfig
) {
    suspend fun warmUpTick() =
        randomCreateUserWithAccountsAndTopUp()

    suspend fun tick() {
        val action = chooseWeighted(config.actionProbability)
        when (action) {
            Action.CREATE_USER -> randomCreateUserWithAccountsAndTopUp()
            Action.CREATE_ACCOUNT -> randomCreateAccountAndTopUp()
            Action.TOP_UP -> randomTopUp()
            Action.WITHDRAW -> randomWithdraw()
            Action.TRANSFER -> randomTransfer()
        }
    }

    private suspend fun randomCreateUserWithAccountsAndTopUp() {
        val user = createUser()
        val accountsCount = config.initialAccountPerUserCountRange.random()

        repeat(accountsCount) {
            val account = withTokenRefresh(user.id!!) { token ->
                createAccount(user.id, token)
            }
            topUpAccount(user, account)
        }
    }

    private suspend fun randomCreateAccountAndTopUp() {
        val user = repository.getRandomUser() ?: return

        val account = withTokenRefresh(user.id!!) { token ->
            createAccount(user.id, token)
        }
        topUpAccount(user, account)
    }

    private suspend fun randomTopUp() {
        val user = repository.getRandomUser() ?: return
        val randomAccount = repository.getRandomActiveAccount(user.id!!) ?: return

        topUpAccount(user, randomAccount)
    }

    private suspend fun randomWithdraw() {
        val user = repository.getRandomUser() ?: return
        val randomAccount = repository.getRandomActiveAccount(user.id!!) ?: return

        withdrawAccount(user, randomAccount)
    }

    private suspend fun randomTransfer() {
        val userFrom = repository.getRandomUser() ?: return
        val accountFrom = repository.getRandomActiveAccount(userFrom.id!!) ?: return
        val userTo = repository.getRandomUser() ?: return
        val accountTo = repository.getRandomActiveAccount(userTo.id!!) ?: return

        if (accountFrom.number == accountTo.number)
            return

        val amount = getDecreaseAmount(
            balance = accountFrom.balance,
            multiplier = config.transferAmountFactor
        ) ?: return

        val reserved = repository.reserveDebit(accountFrom.number, amount)

        if (!reserved)
            return

        try {
            val transaction = withTokenRefresh(userFrom.id) { token ->
                bankClient.transfer(
                    token = token,
                    fromAccountNumber = accountFrom.number,
                    toAccountNumber = accountTo.number,
                    amount = amount
                )
            }

            if (transaction.status == TransactionStatus.COMPLETED)
                check(repository.increaseBalance(accountTo.number, amount)) {
                    "target account ${accountTo.number} is absent"
                }
            else
                check(repository.increaseBalance(accountFrom.number, amount)) {
                    "source account ${accountFrom.number} is absent during rollback"
                }
        } catch (error: Exception) {
            reconcileAccount(userFrom.id, accountFrom.number)
            reconcileAccount(userTo.id, accountTo.number)

            throw error
        }
    }

    private suspend fun createUser(): SimulatedUser {
        val userSegment = chooseWeighted(config.userSegmentProbability)
        val request = UserRegistrationRequest()
        val response = bankClient.register(request)

        val user = SimulatedUser(null, request.login, request.password, response.token, userSegment)

        return repository.addUser(user)
    }

    private suspend fun createAccount(userId: Long, token: String): SimulatedAccount {
        val accountType = chooseWeighted(config.accountTypeProbability)
        val response = bankClient.createAccount(token, accountType)

        val account = SimulatedAccount(
            id = null,
            number = response.number,
            balance = response.balance.toBigDecimal(),
            type = accountType,
            status = AccountStatus.valueOf(response.status)
        )

        return repository.addAccount(userId, account)
    }

    private suspend fun topUpAccount(user: SimulatedUser, account: SimulatedAccount) {
        val amount = getIncreaseAmount(user.userSegment)

        try {
            val transaction = withTokenRefresh(user.id!!) { token ->
                bankClient.topUp(token, account.number, amount)
            }

            if (transaction.status == TransactionStatus.COMPLETED)
                check(repository.increaseBalance(account.number, amount)) {
                    "account ${account.number} is absent"
                }
        } catch (error: Exception) {
            reconcileAccount(user.id!!, account.number)

            throw error
        }
    }

    private suspend fun withdrawAccount(user: SimulatedUser, account: SimulatedAccount) {
        val amount = getDecreaseAmount(
            balance = account.balance,
            multiplier = config.withdrawAmountFactor
        ) ?: return

        val reserved = repository.reserveDebit(account.number, amount)

        if (!reserved)
            return

        try {
            val transaction = withTokenRefresh(user.id!!) { token ->
                bankClient.withdraw(token, account.number, amount)
            }

            if (transaction.status != TransactionStatus.COMPLETED)
                check(repository.increaseBalance(account.number, amount)) {
                    "account ${account.number} is absent during rollback"
                }
        } catch (error: Exception) {
            reconcileAccount(
                id = user.id!!,
                accountNumber = account.number
            )

            throw error
        }
    }

    private fun getIncreaseAmount(userSegment: UserSegment) =
        config.topUpRange
            .getValue(userSegment)
            .random()
            .toBigDecimal()
            .setScale(2, RoundingMode.HALF_UP)

    private fun getDecreaseAmount(balance: BigDecimal, multiplier: BigDecimal): BigDecimal? {
        val maxCents = balance
            .multiply(multiplier)
            .movePointRight(2)
            .setScale(0, RoundingMode.DOWN)
            .longValueExact()

        if (maxCents < 1)
            return null

        val cents = Random.nextLong(1, maxCents + 1)

        return BigDecimal.valueOf(cents, 2)
    }

    private fun <T> chooseWeighted(weights: Map<T, Int>): T {
        require(weights.isNotEmpty())
        require(weights.values.all { it > 0 })

        val totalWeight = weights.values.sum()
        var roll = Random.nextInt(totalWeight)

        for ((value, weight) in weights) {
            if (roll < weight)
                return value

            roll -= weight
        }

        error("unreachable")
    }

    private suspend fun reconcileAccount(id: Long, accountNumber: String) {
        val account = withTokenRefresh(id) { token ->
            bankClient.getAccount(token, accountNumber)
        }

        check(
            repository.replaceBalance(
                accountNumber = accountNumber,
                balance = account.balance.toBigDecimal()
            )
        ) {
            "account $accountNumber is absent in generator repository"
        }
    }

    private suspend fun <T> withTokenRefresh(id: Long, operation: suspend (String) -> T): T {
        val user = repository.getUser(id) ?: error("user $id is not present in generator repository")

        return try {
            operation(user.token)
        } catch (error: ClientRequestException) {
            if (error.response.status != HttpStatusCode.Unauthorized)
                throw error

            val auth = bankClient.login(user.login, user.password)
            check(repository.updateToken(id, auth.token)) {
                "user $id is absent"
            }

            operation(auth.token)
        }
    }
}
