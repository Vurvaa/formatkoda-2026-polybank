package ru.formatkoda.trafficgenerator.plugins

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configurePrometheus(
    registry: PrometheusMeterRegistry
) {
    routing {
        get("/metrics") {
            call.respondText(
                text = registry.scrape(),
                contentType = ContentType.parse(
                    "text/plain; version=0.0.4; charset=utf-8"
                )
            )
        }
    }
}