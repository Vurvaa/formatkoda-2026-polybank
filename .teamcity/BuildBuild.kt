import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object BuildBuild : BuildType({
    name = "CI - Build & Push"

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    steps {
        script {
            id = "prepare database"
            name = "Prepare database"
            scriptContent = """
            #!/bin/sh
            set -e

            docker compose up -d postgres
            until docker compose exec -T postgres pg_isready -U "${'$'}{POSTGRES_USER}"; do
                sleep 2
            done

            ./mvnw liquibase:update -B \
              -Dliquibase.url="${'$'}{POSTGRES_URL}" \
              -Dliquibase.username="${'$'}{POSTGRES_USER}" \
              -Dliquibase.password="${'$'}{POSTGRES_PASSWORD}" \
              -Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml
        """.trimIndent()
        }

        script {
            id = "BUILD_APP"
            name = "Build application"

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B
            """.trimIndent()
        }

        dockerCommand {
            id = "DOCKER_BUILD"
            name = "Docker Build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "%docker.registry%/polybank:%build.number%"
            }
        }

        dockerCommand {
            id = "DOCKER_PUSH"
            name = "Docker Push"
            commandType = push {
                namesAndTags = "%docker.registry%/polybank:%build.number%"
            }
        }

        script {
            name = "Shutdown DB"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = "docker compose down --volumes"
        }

    }

    triggers {
        vcs {
            branchFilter = "+:main"
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

    params {
        param("docker.registry", "192.168.130.81:5000")
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})