package ru.formatkoda.trafficgenerator.config

import ru.formatkoda.trafficgenerator.domain.AccountType
import ru.formatkoda.trafficgenerator.domain.Action
import ru.formatkoda.trafficgenerator.domain.UserSegment

val crisisConfig = BehaviorConfig(
    withdrawAmountPercentage = 90..100,
    transferAmountPercentage = 50..60,
    accountTypeProbability = mapOf(
        AccountType.CURRENT to 20,
        AccountType.FIXED_DEPOSIT to 5,
        AccountType.SAVINGS to 5,
        AccountType.CREDIT to 70
    ),
    topUpRange = mapOf(
        UserSegment.ACTIVE to 500..1000,
        UserSegment.PASSIVE to 10..100,
        UserSegment.STUDENT to 200..500,
        UserSegment.BUSINESS to 3000..5000
    ),
    userSegmentProbability = mapOf(
        UserSegment.ACTIVE to 20,
        UserSegment.PASSIVE to 50,
        UserSegment.STUDENT to 25,
        UserSegment.BUSINESS to 5
    ),
    actionProbability = mapOf(
        Action.CREATE_USER to 1,
        Action.CREATE_ACCOUNT to 1,
        Action.TOP_UP to 20,
        Action.WITHDRAW to 20,
        Action.TRANSFER to 40
    )
)
