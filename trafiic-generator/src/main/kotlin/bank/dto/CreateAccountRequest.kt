package ru.formatkoda.bank.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val accountType: String
)
