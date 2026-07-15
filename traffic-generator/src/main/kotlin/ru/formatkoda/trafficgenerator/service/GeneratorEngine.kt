package ru.formatkoda.trafficgenerator.service

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import ru.formatkoda.trafficgenerator.config.BehaviorConfig
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
    private val repository: GeneratorRepository
) {
    suspend fun warmUpTick(config: BehaviorConfig) =
        randomCreateUserWithAccounts(config = config, topUp = true)

    suspend fun tick(config: BehaviorConfig) {
        val action = chooseWeighted(config.actionProbability)
        when (action) {
            Action.CREATE_USER -> randomCreateUserWithAccounts(config = config)
            Action.CREATE_ACCOUNT -> randomCreateAccount(config = config)
            Action.TOP_UP -> randomTopUp(config = config)
            Action.WITHDRAW -> randomWithdraw(config = config)
            Action.TRANSFER -> randomTransfer(config = config)
        }
    }

    private suspend fun randomCreateUserWithAccounts(config: BehaviorConfig, topUp: Boolean = false) {
        val user = createUser(config)
        val accountsCount = config.accountPerUserCountRange.random()

        repeat(accountsCount) {
            val account = withTokenRefresh(user.id!!) { token ->
                createAccount(config, user.id, token)
            }
            if (topUp)
                topUpAccount(config, user, account)
        }
    }

    private suspend fun randomCreateAccount(config: BehaviorConfig) {
        val user = repository.getRandomUser() ?: return

        withTokenRefresh(user.id!!) { token ->
            createAccount(config, user.id, token)
        }
    }

    private suspend fun randomTopUp(config: BehaviorConfig) {
        val randomAccount = repository.getAccountForTopUp() ?: return
        val user = repository.getUserByAccountId(randomAccount.id!!) ?: return

        topUpAccount(config, user, randomAccount)
    }

    private suspend fun randomWithdraw(config: BehaviorConfig) {
        val randomAccount = repository.getAccountForWithdraw() ?: return
        val user = repository.getUserByAccountId(randomAccount.id!!) ?: return

        withdrawAccount(config, user, randomAccount)
    }

    private suspend fun randomTransfer(config: BehaviorConfig) {
        val accountFrom = repository.getAccountForWithdraw() ?: return
        val userFrom = repository.getUserByAccountId(accountFrom.id!!) ?: return
        val accountTo = repository.getAccountForTopUp() ?: return
        val userTo = repository.getUserByAccountId(accountTo.id!!) ?: return

        transfer(config, userFrom, userTo, accountFrom, accountTo)
    }

    private suspend fun createUser(config: BehaviorConfig): SimulatedUser {
        val userSegment = chooseWeighted(config.userSegmentProbability)
        val request = UserRegistrationRequest()
        val response = bankClient.register(request)

        val user = SimulatedUser(null, request.login, request.password, response.token, userSegment)

        return repository.addUser(user)
    }

    private suspend fun createAccount(config: BehaviorConfig, userId: Long, token: String): SimulatedAccount {
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

    private suspend fun topUpAccount(config: BehaviorConfig, user: SimulatedUser, account: SimulatedAccount) {
        val amount = getIncreaseAmount(config, user.userSegment)

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

    private suspend fun withdrawAccount(config: BehaviorConfig, user: SimulatedUser, account: SimulatedAccount) {
        if (account.balance.signum() == 0)
            return

        val amount = getDecreaseAmount(
            balance = account.balance,
            decreasePercentageRange = config.withdrawAmountPercentage,
            minAmount = config.minimumWithdrawAmount
        )


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
            reconcileAccount(user.id!!, account.number)
            throw error
        }
    }

    private suspend fun transfer(
        config: BehaviorConfig,
        userFrom: SimulatedUser,
        userTo: SimulatedUser,
        accountFrom: SimulatedAccount,
        accountTo: SimulatedAccount
    ) {
        if (accountFrom.number == accountTo.number)
            return

        if (accountFrom.balance.signum() == 0)
            return

        val amount = getDecreaseAmount(
            balance = accountFrom.balance,
            decreasePercentageRange = config.transferAmountPercentage,
            minAmount = config.minimumWithdrawAmount
        )

        if (amount < BigDecimal.ONE)
            return

        val reserved = repository.reserveDebit(accountFrom.number, amount)

        if (!reserved)
            return

        try {
            val transaction = withTokenRefresh(userFrom.id!!) { token ->
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
            reconcileAccount(userFrom.id!!, accountFrom.number)
            reconcileAccount(userTo.id!!, accountTo.number)

            throw error
        }
    }

    private fun getIncreaseAmount(config: BehaviorConfig, userSegment: UserSegment) =
        config.topUpRange
            .getValue(userSegment)
            .random()
            .toBigDecimal()
            .setScale(2, RoundingMode.HALF_UP)

    private fun getDecreaseAmount(
        balance: BigDecimal,
        decreasePercentageRange: IntRange,
        minAmount: BigDecimal
    ): BigDecimal {
        val percentage = decreasePercentageRange.random()
        val amount = balance.percentage(percentage)

        if (amount <= minAmount)
            return minAmount.min(balance)

        return amount
    }

    private fun BigDecimal.percentage(value: Int): BigDecimal {
        require(value > 0)
        return this
            .multiply(value.toBigDecimal())
            .divide(BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun <T> chooseWeighted(weights: Map<T, Int>): T {
        require(weights.isNotEmpty())
        require(weights.values.all { it >= 0 })

        val positiveWeights = weights.filterValues { it > 0 }

        require(positiveWeights.isNotEmpty()) {
            "at least one weight must be positive"
        }

        val totalWeight = positiveWeights.values.sum()
        var roll = Random.nextInt(totalWeight)

        for ((value, weight) in positiveWeights) {
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
