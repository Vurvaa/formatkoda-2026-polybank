package ru.formatkoda.trafficgenerator.config

import ru.formatkoda.trafficgenerator.domain.AccountType
import ru.formatkoda.trafficgenerator.domain.Action
import ru.formatkoda.trafficgenerator.domain.UserSegment

val defaultConfig = BehaviorConfig(
    withdrawAmountPercentage = 40..50,
    transferAmountPercentage = 40..50,
    accountTypeProbability = mapOf(
        AccountType.CURRENT to 50,
        AccountType.FIXED_DEPOSIT to 20,
        AccountType.SAVINGS to 20,
        AccountType.CREDIT to 10
    ),
    topUpRange = mapOf(
        UserSegment.ACTIVE to 2000..5000,
        UserSegment.PASSIVE to 100..1000,
        UserSegment.STUDENT to 500..1500,
        UserSegment.BUSINESS to 20000..40000
    ),
    userSegmentProbability = mapOf(
        UserSegment.ACTIVE to 40,
        UserSegment.PASSIVE to 20,
        UserSegment.STUDENT to 20,
        UserSegment.BUSINESS to 20
    ),
    actionProbability = mapOf(
        Action.CREATE_USER to 10,
        Action.CREATE_ACCOUNT to 10,
        Action.TOP_UP to 20,
        Action.WITHDRAW to 20,
        Action.TRANSFER to 40
    )
)
