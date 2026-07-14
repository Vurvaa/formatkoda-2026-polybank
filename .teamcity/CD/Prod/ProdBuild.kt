package CD.Prod

import Helpers.cleanupDatabaseStep
import Helpers.prepareDatabaseStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object ProdBuild : BuildType({
    name = "CD - (prod) build and push to registry."

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    params {
        param("env.RELEASE_VERSION", "%teamcity.build.branch%")
    }

    steps {
        prepareDatabaseStep(extraLiquibaseDirs = listOf("notification"))

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
            id = "DOCKER_BUILD"
            name = "Docker Build"
            commandType = build {
                source = file {
                    path = "app/Dockerfile"
                }
                contextDir = "app"
                namesAndTags = "%docker.registry%/polybank:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD_WEBAPP"
            name = "Docker Build Webapp"
            commandType = build {
                source = file {
                    path = "webapp/Dockerfile"
                }
                contextDir = "webapp"
                namesAndTags = "%docker.registry%/polybank-webapp:%env.RELEASE_VERSION%"
            }
        }

        script {
            id = "BUILD_NOTIFICATION"
            name = "Build notification application"
            workingDir = "notification"

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B -Pdb-codegen
            """.trimIndent()
        }

        dockerCommand {
            id = "DOCKER_BUILD_NOTIFICATION"
            name = "Docker Build Notification"
            commandType = build {
                source = file {
                    path = "notification/Dockerfile"
                }
                contextDir = "notification"
                namesAndTags = "%docker.registry%/polybank-notification:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD_TRAFFIC_GENERATOR"
            name = "Docker Build Traffic Generator"
            commandType = build {
                source = file {
                    path = "traffic-generator/Dockerfile"
                }
                contextDir = "traffic-generator"
                namesAndTags = "%docker.registry%/polybank-traffic-generator:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH"
            name = "Docker Push"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH_WEBAPP"
            name = "Docker Push Webapp"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-webapp:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH_NOTIFICATION"
            name = "Docker Push Notification"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-notification:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH_TRAFFIC_GENERATOR"
            name = "Docker Push Traffic Generator"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank-traffic-generator:%env.RELEASE_VERSION%"
            }
        }

        cleanupDatabaseStep()
    }

    triggers {
        vcs {
            branchFilter = """
            +:regexp:^\d+\.\d+\.\d+$
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