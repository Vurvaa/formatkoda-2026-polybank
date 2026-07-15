package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    val number: String,
    val balance: String,
    val type: String,
    val status: String,
    val createdAt: String
)
