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
            id = "prepare_database"
            name = "Prepare database"
            scriptContent = """
                #!/bin/sh
                set -e

                UNIQUE_DB="build_%build.number%"

                docker run --rm --network host postgres:16-alpine \
                  sh -c "PGPASSWORD=${'$'}{POSTGRES_PASSWORD} psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c 'CREATE DATABASE ${'$'}{UNIQUE_DB};'"

                LIQUIBASE_URL="jdbc:postgresql://${'$'}{POSTGRES_HOST}:${'$'}{POSTGRES_PORT}/${'$'}{UNIQUE_DB}"

                ./mvnw liquibase:update -B \
                  -Dliquibase.url=${'$'}{LIQUIBASE_URL} \
                  -Dliquibase.username=${'$'}POSTGRES_USER \
                  -Dliquibase.password=${'$'}POSTGRES_PASSWORD \
                  -Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml

                echo "##teamcity[setParameter name='env.UNIQUE_DB' value='${'$'}{UNIQUE_DB}']"
                echo "##teamcity[setParameter name='env.POSTGRES_URL' value='${'$'}{LIQUIBASE_URL}']"
            """.trimIndent()
        }

        script {
            id = "BUILD_APP"
            name = "Build application"

            scriptContent = """
                #!/bin/sh
                set -e

                ./mvnw clean package -DskipTests -B -Pdb-codegen \
                  -DPOSTGRES_URL=${'$'}{POSTGRES_URL} \
                  -DPOSTGRES_USER=${'$'}{POSTGRES_USER} \
                  -DPOSTGRES_PASSWORD=${'$'}{POSTGRES_PASSWORD}
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
            name = "Cleanup database"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/sh
                docker run --rm --network host postgres:16-alpine \
                  sh -c "PGPASSWORD=${'$'}{POSTGRES_PASSWORD} psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${'$'}{UNIQUE_DB}' AND pid <> pg_backend_pid();\"" || true

                docker run --rm --network host postgres:16-alpine \
                  sh -c "PGPASSWORD=${'$'}{POSTGRES_PASSWORD} psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c 'DROP DATABASE IF EXISTS ${'$'}{UNIQUE_DB};'"
            """.trimIndent()
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