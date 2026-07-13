package ru.formatkoda.generator

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import ru.formatkoda.bank.BankClient
import ru.formatkoda.bank.dto.UserRegistrationRequest
import ru.formatkoda.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class GeneratorEngine(
    private val bankClient: BankClient,
    private val state: GeneratorState,
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
        state.addUser(user)
        val accountsCount = config.initialAccountPerUserCountRange.random()

        repeat(accountsCount) {
            val account = withTokenRefresh(user.login) { token ->
                createAccount(token)
            }
            state.addAccount(user.login, account)
            topUpAccount(user, account)
        }
    }

    private suspend fun randomCreateAccountAndTopUp() {
        val user = state.getRandomUser() ?: return

        val account = withTokenRefresh(user.login) { token ->
            createAccount(token)
        }

        state.addAccount(user.login, account)
        topUpAccount(user, account)
    }

    private suspend fun randomTopUp() {
        val user = state.getRandomUser() ?: return
        val randomAccount = user.randomActiveAccount() ?: return

        topUpAccount(user, randomAccount)
    }

    private suspend fun randomWithdraw() {
        val user = state.getRandomUser() ?: return
        val randomAccount = user.randomActiveAccount() ?: return

        withdrawAccount(user, randomAccount)
    }

    private suspend fun randomTransfer() {
        val userFrom = state.getRandomUser() ?: return
        val accountFrom = state.getRandomActiveAccount(userFrom.login) ?: return
        val userTo = state.getRandomUser() ?: return
        val accountTo = state.getRandomActiveAccount(userTo.login) ?: return

        if (accountFrom.number == accountTo.number)
            return

        val amount = getDecreaseAmount(
            balance = accountFrom.balance,
            multiplier = config.transferAmountFactor
        ) ?: return

        val reserved = state.reserveDebit(accountFrom.number, amount)

        if (!reserved)
            return

        try {
            val transaction = withTokenRefresh(userFrom.login) { token ->
                bankClient.transfer(
                    token = token,
                    fromAccountNumber = accountFrom.number,
                    toAccountNumber = accountTo.number,
                    amount = amount
                )
            }

            if (transaction.status == TransactionStatus.COMPLETED)
                check(state.completeTransfer(accountTo.number, amount)) {
                    "target account ${accountTo.number} is absent"
                }
            else
                state.rollbackDebit(accountFrom.number, amount)
        } catch (error: Exception) {
            reconcileAccount(userFrom.login, accountFrom.number)
            reconcileAccount(userTo.login, accountTo.number)

            throw error
        }
    }

    private suspend fun createUser(): SimulatedUser {
        val userSegment = chooseWeighted(config.userSegmentProbability)
        val request = UserRegistrationRequest()
        val response = bankClient.register(request)

        return SimulatedUser(request.login, request.password, response.token, userSegment)
    }

    private suspend fun createAccount(token: String): SimulatedAccount {
        val accountType = chooseWeighted(config.accountTypeProbability)
        val response = bankClient.createAccount(token, accountType)

        return SimulatedAccount(
            number = response.number,
            balance = response.balance.toBigDecimal(),
            type = accountType,
            status = AccountStatus.valueOf(response.status)
        )
    }

    private suspend fun topUpAccount(user: SimulatedUser, account: SimulatedAccount) {
        val amount = getIncreaseAmount(user.userSegment)

        try {
            val transaction = withTokenRefresh(user.login) { token ->
                bankClient.topUp(token, account.number, amount)
            }

            if (transaction.status == TransactionStatus.COMPLETED)
                check(state.increaseBalance(account.number, amount)) {
                    "account ${account.number} is absent"
                }
        } catch (error: Exception) {
            reconcileAccount(user.login, account.number)

            throw error
        }
    }

    private suspend fun withdrawAccount(user: SimulatedUser, account: SimulatedAccount) {
        val amount = getDecreaseAmount(
            balance = account.balance,
            multiplier = config.withdrawAmountFactor
        ) ?: return

        val reserved = state.reserveDebit(account.number, amount)

        if (!reserved)
            return

        try {
            val transaction = withTokenRefresh(user.login) { token ->
                bankClient.withdraw(token, account.number, amount)
            }

            if (transaction.status != TransactionStatus.COMPLETED)
                state.rollbackDebit(account.number, amount)
        } catch (error: Exception) {
            reconcileAccount(
                login = user.login,
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

    private suspend fun <T> withTokenRefresh(
        login: String,
        operation: suspend (String) -> T
    ): T {
        val user = state.getUser(login) ?: error("user $login is not present in generator state")

        return try {
            operation(user.token)
        } catch (error: ClientRequestException) {
            if (error.response.status != HttpStatusCode.Unauthorized)
                throw error

            val auth = bankClient.login(login, user.password)
            state.updateToken(user.login, auth.token)

            operation(auth.token)
        }
    }

    private suspend fun reconcileAccount(login: String, accountNumber: String) {
        val account = withTokenRefresh(login) { token ->
            bankClient.getAccount(token, accountNumber)
        }

        check(
            state.replaceBalance(
                accountNumber = accountNumber,
                balance = account.balance.toBigDecimal()
            )
        ) {
            "account $accountNumber is absent in generator state"
        }
    }
}
