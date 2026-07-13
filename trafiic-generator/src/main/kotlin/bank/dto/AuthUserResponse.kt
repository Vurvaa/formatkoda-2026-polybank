package ru.formatkoda.bank.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthUserResponse(
    val token: String
)
