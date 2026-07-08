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
        prepareDatabaseStep()

        script {
            id = "BUILD_APP"
            name = "Build application"

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
                namesAndTags = "%docker.registry%/polybank-webapp:%env.RELEASE_VERSION%"
            }
        }

        dockerCommand {
            id = "DOCKER_BUILD"
            name = "Docker Build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "%docker.registry%/polybank:%env.RELEASE_VERSION%"
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