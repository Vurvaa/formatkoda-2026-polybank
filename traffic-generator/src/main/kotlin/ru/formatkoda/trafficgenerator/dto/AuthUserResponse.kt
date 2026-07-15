package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthUserResponse(
    val token: String
)
