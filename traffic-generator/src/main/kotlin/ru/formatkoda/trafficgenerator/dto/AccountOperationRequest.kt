package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountOperationRequest(
    val accountNumber: String,
    val amount: String
)
