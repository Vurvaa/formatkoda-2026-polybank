package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val accountType: String
)
