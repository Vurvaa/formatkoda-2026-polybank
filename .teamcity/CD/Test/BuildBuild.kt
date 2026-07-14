package CD.Test

import Helpers.cleanupDatabaseStep
import Helpers.prepareDatabaseStep
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

    steps {
        prepareDatabaseStep()

        script {
            id = "BUILD_APP"
            name = "Build application"
            workingDir = "app"

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B -Pdb-codegen
            """.trimIndent()
        }

        dockerCommand {
            id = "DOCKER_BUILD_WEBAPP"
            name = "Docker Build Webapp"
            commandType = build {
                source = file {
                    path = "webapp/Dockerfile"
                }
                contextDir = "webapp"
                namesAndTags = "%docker.registry%/polybank-webapp-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD"
            name = "Docker Build"
            commandType = build {
                source = file {
                    path = "app/Dockerfile"
                }
                contextDir = "app"
                namesAndTags = "%docker.registry%/polybank-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD_TRAFFIC_GENERATOR"
            name = "Docker Build Traffic Generator"
            commandType = build {
                source = file {
                    path = "trafiic-generator/Dockerfile"
                }
                contextDir = "trafiic-generator"
                namesAndTags = "%docker.registry%/polybank-traffic-generator-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH"
            name = "Docker Push"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH_WEBAPP"
            name = "Docker Push Webapp"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-webapp-test:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH_TRAFFIC_GENERATOR"
            name = "Docker Push Traffic Generator"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-traffic-generator-test:%build.number%"
            }
        }

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