package ru.formatkoda.trafficgenerator.domain

data class SimulatedUser(
    val id: Long?,
    val login: String,
    val password: String,
    var token: String,
    val userSegment: UserSegment,
    val accounts: MutableList<SimulatedAccount> = mutableListOf()
)
