package CD.Test

import Helpers.cleanupDatabaseStep
import Helpers.detectChangedServicesStep
import Helpers.markAsLatestTestStep
import Helpers.prepareDatabaseStep
import Helpers.retagUnchangedImageStep
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object BuildBuild : BuildType({
    name = "CD - (test) build and push to registry."

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    val buildApp = "env.BUILD_APP"
    val buildNot = "env.BUILD_NOTIFICATION"
    val buildWeb = "env.BUILD_WEBAPP"
    val buildTra = "env.BUILD_TRAFFIC_GENERATOR"

    steps {
        detectChangedServicesStep()

        prepareDatabaseStep()

        script {
            id = "BUILD_APP"
            name = "Build application"
            workingDir = "app"

            conditions {
                equals(buildApp, "true")
            }

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B -Pdb-codegen
            """.trimIndent()
        }

        dockerCommand {
            id = "DOCKER_BUILD"
            name = "Docker Build"

            conditions {
                equals(buildApp, "true")
            }

            commandType = build {
                source = file {
                    path = "app/Dockerfile"
                }
                contextDir = "app"
                namesAndTags = "%docker.registry%/polybank-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD_WEBAPP"
            name = "Docker Build Webapp"

            conditions {
                equals(buildWeb, "true")
            }

            commandType = build {
                source = file {
                    path = "webapp/Dockerfile"
                }
                contextDir = "webapp"
                namesAndTags = "%docker.registry%/polybank-webapp-test:%build.number%"
            }
        }

        script {
            id = "BUILD_NOTIFICATION"
            name = "Build notification application"
            workingDir = "notification"

            conditions {
                equals(buildNot, "true")
            }

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B -Pdb-codegen
            """.trimIndent()
        }

        dockerCommand {
            id = "DOCKER_BUILD_NOTIFICATION"
            name = "Docker Build Notification"

            conditions {
                equals(buildNot, "true")
            }

            commandType = build {
                source = file {
                    path = "notification/Dockerfile"
                }
                contextDir = "notification"
                namesAndTags = "%docker.registry%/polybank-notification-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD_TRAFFIC_GENERATOR"
            name = "Docker Build Traffic Generator"

            conditions {
                equals(buildTra, "true")
            }

            commandType = build {
                source = file {
                    path = "traffic-generator/Dockerfile"
                }
                contextDir = "traffic-generator"
                namesAndTags = "%docker.registry%/polybank-traffic-generator-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH"
            name = "Docker Push"

            conditions {
                equals(buildApp, "true")
            }

            commandType = push {
                namesAndTags = "%docker.registry%/polybank-test:%build.number%"
            }
        }

        retagUnchangedImageStep(
            id = "RETAG_APP",
            name = "Retag application (unchanged)",
            image = "polybank-test",
            condition = buildApp,
        )

        markAsLatestTestStep(
            id = "MARK_LATEST_APP",
            name = "Mark application as latest-test",
            image = "polybank-test",
        )


        dockerCommand {
            id = "DOCKER_PUSH_WEBAPP"
            name = "Docker Push Webapp"

            conditions {
                equals(buildWeb, "true")
            }

            commandType = push {
                namesAndTags = "%docker.registry%/polybank-webapp-test:%build.number%"
            }
        }

        retagUnchangedImageStep(
            id = "RETAG_WEBAPP",
            name = "Retag webapp (unchanged)",
            image = "polybank-webapp-test",
            condition = buildWeb,
        )

        markAsLatestTestStep(
            id = "MARK_LATEST_WEBAPP",
            name = "Mark webapp as latest-test",
            image = "polybank-webapp-test",
        )

        dockerCommand {
            id = "DOCKER_PUSH_NOTIFICATION"
            name = "Docker Push Notification"

            conditions {
                equals(buildNot, "true")
            }

            commandType = push {
                namesAndTags = "%docker.registry%/polybank-notification-test:%build.number%"
            }
        }

        retagUnchangedImageStep(
            id = "RETAG_NOTIFICATION",
            name = "Retag notification (unchanged)",
            image = "polybank-notification-test",
            condition = buildNot,
        )

        markAsLatestTestStep(
            id = "MARK_LATEST_NOTIFICATION",
            name = "Mark notification as latest-test",
            image = "polybank-notification-test",
        )

        dockerCommand {
            id = "DOCKER_PUSH_TRAFFIC_GENERATOR"
            name = "Docker Push Traffic Generator"

            conditions {
                equals(buildTra, "true")
            }

            commandType = push {
                namesAndTags = "%docker.registry%/polybank-traffic-generator-test:%build.number%"
            }
        }

        retagUnchangedImageStep(
            id = "RETAG_TRAFFIC_GENERATOR",
            name = "Retag traffic generator (unchanged)",
            image = "polybank-traffic-generator-test",
            condition = buildTra,
        )

        markAsLatestTestStep(
            id = "MARK_LATEST_TRAFFIC_GENERATOR",
            name = "Mark traffic generator as latest-test",
            image = "polybank-traffic-generator-test",
        )

        cleanupDatabaseStep()

    }

    triggers {
        vcs {
            branchFilter = """
            +:refs/heads/main
        """.trimIndent()
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = gitlab {
                gitlabApiUrl = "%gitlab.api.url%"
                authType = personalToken {
                    accessToken = "%env.GITLAB_TOKEN%"
                }
            }
        }
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})
