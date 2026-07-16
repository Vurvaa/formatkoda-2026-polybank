package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class UserRegistrationRequest(
    val login: String = "login_${System.currentTimeMillis()}_${Random.nextInt(10_000)}",
    val email: String = "email_${System.currentTimeMillis()}_${Random.nextInt(10_000)}@example.com",
    val name: String = "John_${Random.nextInt(10_000)}",
    val lastName: String = "Doe_${Random.nextInt(10_000)}",
    val password: String = "password1234"
)
