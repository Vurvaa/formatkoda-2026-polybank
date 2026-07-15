package ru.formatkoda.trafficgenerator.config

import ru.formatkoda.trafficgenerator.domain.AccountType
import ru.formatkoda.trafficgenerator.domain.Action
import ru.formatkoda.trafficgenerator.domain.UserSegment

val growthConfig = BehaviorConfig(
    withdrawAmountPercentage = 5..10,
    transferAmountPercentage = 30..40,
    accountTypeProbability = mapOf(
        AccountType.CURRENT to 10,
        AccountType.FIXED_DEPOSIT to 30,
        AccountType.SAVINGS to 50,
        AccountType.CREDIT to 10
    ),
    topUpRange = mapOf(
        UserSegment.ACTIVE to 5000..10000,
        UserSegment.PASSIVE to 1000..2000,
        UserSegment.STUDENT to 2000..4000,
        UserSegment.BUSINESS to 100000..200000
    ),
    userSegmentProbability = mapOf(
        UserSegment.ACTIVE to 50,
        UserSegment.PASSIVE to 5,
        UserSegment.STUDENT to 10,
        UserSegment.BUSINESS to 35
    ),
    actionProbability = mapOf(
        Action.CREATE_USER to 15,
        Action.CREATE_ACCOUNT to 25,
        Action.TOP_UP to 50,
        Action.WITHDRAW to 5,
        Action.TRANSFER to 5
    )
)
