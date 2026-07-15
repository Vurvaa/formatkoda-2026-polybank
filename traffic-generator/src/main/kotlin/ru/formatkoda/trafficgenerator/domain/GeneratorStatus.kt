package ru.formatkoda.trafficgenerator.domain

enum class GeneratorStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED
}