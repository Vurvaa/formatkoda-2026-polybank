package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable
import ru.formatkoda.trafficgenerator.domain.TransactionStatus

@Serializable
data class TransactionResponse(
    val id: Long,
    val fromAccountNumber: String? = null,
    val toAccountNumber: String? = null,
    val amount: String,
    val type: String,
    val status: TransactionStatus,
    val createdAt: String
)
