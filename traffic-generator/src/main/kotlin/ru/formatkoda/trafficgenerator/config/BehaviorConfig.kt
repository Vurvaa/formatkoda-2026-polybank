package ru.formatkoda.trafficgenerator.config

import ru.formatkoda.trafficgenerator.domain.AccountType
import ru.formatkoda.trafficgenerator.domain.Action
import ru.formatkoda.trafficgenerator.domain.UserSegment
import java.math.BigDecimal

data class BehaviorConfig(
    val accountPerUserCountRange: IntRange = 5..15,

    val minimumWithdrawAmount: BigDecimal = BigDecimal.TEN,
    val withdrawAmountPercentage: IntRange = 20..30,
    val transferAmountPercentage: IntRange = 30..40,

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
        require(!accountPerUserCountRange.isEmpty()) {
            "accountPerUserCountRange must not be empty"
        }

        require(accountPerUserCountRange.first >= 0) {
            "account count must not be negative"
        }

        validatePercentageRange(
            "withdrawAmountPercentage",
            withdrawAmountPercentage
        )

        validatePercentageRange(
            "transferAmountPercentage",
            transferAmountPercentage
        )

        validateProbabilityMap(accountTypeProbability)
        validateProbabilityMap(userSegmentProbability)
        validateProbabilityMap(actionProbability)

        require(topUpRange.values.all { range ->
            !range.isEmpty() && range.first > 0
        })
    }

    private fun validatePercentageRange(
        name: String,
        range: IntRange
    ) {
        require(!range.isEmpty()) {
            "$name must not be empty"
        }

        require(range.first >= 1 && range.last <= 100) {
            "$name must be between 1 and 100"
        }
    }

    private fun validateProbabilityMap(map: Map<*, Int>) {
        require(map.isNotEmpty())
        require(map.values.all { it >= 0 })
        require(map.values.any { it > 0 })
    }
}
