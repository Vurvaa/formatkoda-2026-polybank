package ru.formatkoda.model

data class SimulatedUser(
    val login: String,
    val password: String,
    var token: String,
    val userSegment: UserSegment,
    val accounts: MutableList<SimulatedAccount> = mutableListOf()
) {
    fun randomActiveAccount() =
        accounts
            .filter { it.status == AccountStatus.ACTIVE }
            .randomOrNull()

    fun deepCopy() =
        copy(
            accounts = accounts
                .map { it.copy() }
                .toMutableList()
        )
}
