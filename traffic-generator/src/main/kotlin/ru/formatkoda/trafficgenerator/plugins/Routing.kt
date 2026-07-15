package ru.formatkoda.trafficgenerator.plugins

import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import ru.formatkoda.trafficgenerator.controller.generatorRoutes
import ru.formatkoda.trafficgenerator.service.GeneratorService

fun Application.configureRouting(service: GeneratorService) {
    routing {
        generatorRoutes(service)

        staticResources("/", "web", "index.html")

        swaggerUI(path = "openapi") {
            info = OpenApiInfo("Traffic Generator API", "1.0.0")
            source = OpenApiDocSource.Routing {
                routingRoot.descendants()
            }
        }
    }
}