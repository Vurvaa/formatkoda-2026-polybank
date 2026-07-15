package ru.formatkoda.trafficgenerator.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ru.formatkoda.trafficgenerator.config.crisisConfig
import ru.formatkoda.trafficgenerator.config.defaultConfig
import ru.formatkoda.trafficgenerator.config.growthConfig
import ru.formatkoda.trafficgenerator.dto.MetricsResponseDto
import ru.formatkoda.trafficgenerator.service.GeneratorService

fun Route.generatorRoutes(
    service: GeneratorService
) {
    route("/generator") {
        get("/status") {
            call.respond(
                HttpStatusCode.OK,
                service.status.value
            )
        }

        get("/metrics") {
            val snapshot = service.metricsSnapshot.value

            call.respond(MetricsResponseDto(
                snapshot.requestsPerSecond,
                snapshot.ticksPerSecond,
                snapshot.sentRequests,
                snapshot.failedTicks
            ))
        }

        post("/start") {
            val started = service.start()

            if (started)
                call.respond(
                    HttpStatusCode.OK,
                    "generator successfully started"
                )
            else
                call.respond(
                    HttpStatusCode.Conflict,
                    "generator is already running"
                )
        }

        post("/stop") {
            val stopped = service.stop()

            if (stopped)
                call.respond(
                    HttpStatusCode.OK,
                    "generator successfully stopped"
                )
            else
                call.respond(
                    HttpStatusCode.Conflict,
                    "generator is already stopped"
                )
        }

        put("/config/{configName}") {
            val configName = call.requirePathParameter("configName").lowercase()
            val newConfig = when (configName) {
                "growth" -> growthConfig
                "default" -> defaultConfig
                "crisis" -> crisisConfig
                else -> null
            } ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "unknown config"
                )
                return@put
            }

            service.updateBehaviorConfig(newConfig)

            call.respond(
                HttpStatusCode.OK,
                "config is changed to $configName"
            )
        }
    }
}