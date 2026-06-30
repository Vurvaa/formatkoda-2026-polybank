import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object MrBuild : BuildType({
    name = "CI - MR_checks"

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
        maven {
            id = "COMPILE"
            name = "Compile"
            goals = "clean compile"
            runnerArgs = "-B -DPOSTGRES_URL=%env.POSTGRES_URL% -DPOSTGRES_USER=%env.POSTGRES_USER% -DPOSTGRES_PASSWORD=%env.POSTGRES_PASSWORD%"
            jdkHome = "%java.home%"
        }
        /*maven {
            id = "UNIT_TESTS"
            name = "Unit Tests"
            goals = "test"
            runnerArgs = "-Dsurefire.failIfNoSpecifiedTests=false"
            jdkHome = "%java.home%"
        }*/
        script {
            name = "Shutdown DB"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = "docker compose down --volumes"
        }
    }

    triggers {
        vcs {
            branchFilter = "+:refs/merge-requests/*"
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
        param("java.home", "/opt/java/openjdk")
    }

    failureConditions {
        executionTimeoutMin = 15
    }

})