package ru.formatkoda.trafficgenerator.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.formatkoda.trafficgenerator.dto.AccountOperationRequest
import ru.formatkoda.trafficgenerator.dto.AccountResponse
import ru.formatkoda.trafficgenerator.dto.AuthUserResponse
import ru.formatkoda.trafficgenerator.dto.CreateAccountRequest
import ru.formatkoda.trafficgenerator.dto.PageRequest
import ru.formatkoda.trafficgenerator.dto.TransactionResponse
import ru.formatkoda.trafficgenerator.dto.PageResponse
import ru.formatkoda.trafficgenerator.dto.TransferRequest
import ru.formatkoda.trafficgenerator.dto.UserLoginRequest
import ru.formatkoda.trafficgenerator.dto.UserRegistrationRequest
import ru.formatkoda.trafficgenerator.domain.AccountType
import java.math.BigDecimal
import java.math.RoundingMode

class BankClient(
    private val baseUrl: String,
    private val metrics: GeneratorMetrics
) {
    private val requestMetricsPlugin = createClientPlugin(
        name = "RequestMetrics"
    ) {
        onRequest { _, _ ->
            metrics.requestSent()
        }
    }

    private val http = HttpClient(CIO) {
        expectSuccess = true

        install(requestMetricsPlugin)

        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 10_000
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }

        install(Logging) {
            logger = Logger.EMPTY
            level = LogLevel.NONE

            sanitizeHeader { header ->
                header == HttpHeaders.Authorization
            }
        }
    }

    suspend fun register(request: UserRegistrationRequest): AuthUserResponse {
        return http.post("$baseUrl/user/sign-up") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(login: String, password: String): AuthUserResponse {
        return http.post("$baseUrl/user/sign-in") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(login, password))
        }.body()
    }

    suspend fun getAccounts(token: String): List<AccountResponse> {
        return http.get("$baseUrl/account") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getAccount(token: String, accountNumber: String): AccountResponse {
        return http.get("$baseUrl/account/$accountNumber") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createAccount(token: String, accountType: AccountType): AccountResponse {
        return http.post("$baseUrl/account") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateAccountRequest(accountType.name))
        }.body()
    }

    suspend fun getTransactions(
        token: String,
        accountNumber: String,
        pageRequest: PageRequest
    ): PageResponse<TransactionResponse> {
        return http.get("$baseUrl/transaction/$accountNumber") {
            bearerAuth(token)
            parameter("page", pageRequest.page)
            parameter("size", pageRequest.size)
        }.body()
    }

    suspend fun topUp(token: String, accountNumber: String, amount: BigDecimal): TransactionResponse {
        validateAmount(amount)

        return http.post("$baseUrl/transaction/top-up") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AccountOperationRequest(accountNumber, amount.toPlainString()))
        }.body()
    }

    suspend fun withdraw(token: String, accountNumber: String, amount: BigDecimal): TransactionResponse {
        validateAmount(amount)

        return http.post("$baseUrl/transaction/withdraw") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AccountOperationRequest(accountNumber, amount.toPlainString()))
        }.body()
    }

    suspend fun transfer(
        token: String,
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: BigDecimal
    ): TransactionResponse {
        validateAmount(amount)

        return http.post("$baseUrl/transaction/transfer") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(TransferRequest(fromAccountNumber, toAccountNumber, amount.toPlainString()))
        }.body()
    }

    fun close() {
        http.close()
    }

    private fun validateAmount(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) {
            "amount must be positive, got $amount"
        }

        require(
            runCatching {
                amount.setScale(2, RoundingMode.UNNECESSARY)
            }.isSuccess
        ) {
            "amount must have no more than 2 fraction digits, got $amount"
        }
    }
}