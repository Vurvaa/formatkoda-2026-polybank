package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long
)