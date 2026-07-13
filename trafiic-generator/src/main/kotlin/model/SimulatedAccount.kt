package ru.formatkoda.model

import java.math.BigDecimal

data class SimulatedAccount(
    val number: String,
    var balance: BigDecimal,
    val type: AccountType,
    var status: AccountStatus
)
