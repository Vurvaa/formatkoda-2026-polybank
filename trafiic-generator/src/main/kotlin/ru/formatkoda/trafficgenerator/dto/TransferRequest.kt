package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransferRequest(
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: String
)
