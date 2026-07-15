package ru.formatkoda.trafficgenerator.domain

import java.math.BigDecimal

data class SimulatedAccount(
    val id: Long?,
    val number: String,
    var balance: BigDecimal,
    val type: AccountType,
    var status: AccountStatus
)
