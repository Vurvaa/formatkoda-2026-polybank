package ru.formatkoda.generator

import ru.formatkoda.model.AccountType
import ru.formatkoda.model.Action
import ru.formatkoda.model.UserSegment
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GeneratorConfig(
    val workers: Int = 5,
    val tickDelay: Duration = 100.milliseconds,

    val withdrawAmountFactor: BigDecimal = BigDecimal("0.2"),
    val transferAmountFactor: BigDecimal = BigDecimal("0.3"),

    val warmupUserCountRange: IntRange = 10..20,
    val initialAccountPerUserCountRange: IntRange = 1..5,

    val accountTypeProbability: Map<AccountType, Int> = mapOf(
        AccountType.CURRENT to 50,
        AccountType.FIXED_DEPOSIT to 20,
        AccountType.SAVINGS to 20,
        AccountType.CREDIT to 10
    ),

    val topUpRange: Map<UserSegment, IntRange> = mapOf(
        UserSegment.ACTIVE to 1500..5000,
        UserSegment.PASSIVE to 10..100,
        UserSegment.STUDENT to 500..1000,
        UserSegment.BUSINESS to 10000..20000
    ),

    val userSegmentProbability: Map<UserSegment, Int> = mapOf(
        UserSegment.ACTIVE to 40,
        UserSegment.PASSIVE to 20,
        UserSegment.STUDENT to 25,
        UserSegment.BUSINESS to 15
    ),

    val actionProbability: Map<Action, Int> = mapOf(
        Action.CREATE_USER to 5,
        Action.CREATE_ACCOUNT to 10,
        Action.TOP_UP to 35,
        Action.WITHDRAW to 20,
        Action.TRANSFER to 30
    )
) {
    init {
        require(workers > 0) {
            "workers must be positive"
        }

        require(!tickDelay.isNegative()) {
            "tickDelay must not be negative"
        }
    }
}
