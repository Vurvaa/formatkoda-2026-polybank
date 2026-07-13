package ru.formatkoda.bank.dto

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class UserRegistrationRequest(
    val login: String = "login_${System.currentTimeMillis()}_${Random.nextInt(10_000)}",
    val name: String = "John",
    val lastName: String = "Doe",
    val password: String = "password1234"
)
