package ru.formatkoda.trafficgenerator.plugins

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase(): Database {
    val config = environment.config

    return Database.connect(
        url = config.property("database.url").getString(),
        driver = "org.postgresql.Driver",
        user = config.property("database.user").getString(),
        password = config.property("database.password").getString()
    )
}
