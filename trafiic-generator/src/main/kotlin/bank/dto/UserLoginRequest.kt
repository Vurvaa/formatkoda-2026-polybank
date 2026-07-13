package ru.formatkoda.bank.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginRequest(
    val login: String,
    val password: String
)
