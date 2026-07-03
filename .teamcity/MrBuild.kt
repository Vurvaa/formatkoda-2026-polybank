import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object MrBuild : BuildType({
    name = "Checks on MR."

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
                UNIQUE_DB="mr_%build.number%"
                export PGPASSWORD="${'$'}{POSTGRES_PASSWORD}"

                psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "CREATE DATABASE \"${'$'}{UNIQUE_DB}\";"

                POSTGRES_URL="jdbc:postgresql://${'$'}{POSTGRES_HOST}:${'$'}{POSTGRES_PORT}/${'$'}{UNIQUE_DB}"

                ./mvnw process-resources liquibase:update -B \
                    -Dliquibase.url=${'$'}{POSTGRES_URL} \
                    -Dliquibase.username=${'$'}POSTGRES_USER \
                    -Dliquibase.password=${'$'}POSTGRES_PASSWORD \
                    -Dliquibase.changeLogFile=db/changelog/db.changelog-master.xml \
                    -Dliquibase.searchPath=src/main/resources

                echo "##teamcity[setParameter name='env.POSTGRES_URL' value='${'$'}{POSTGRES_URL}']"
                echo "##teamcity[setParameter name='env.UNIQUE_DB' value='${'$'}{UNIQUE_DB}']"
            """.trimIndent()
        }

        maven {
            id = "COMPILE"
            name = "Compile"
            goals = "clean compile"
            runnerArgs = "-B -Pdb-codegen"
            jdkHome = "%java.home%"
        }

        maven {
            id = "UNIT_TESTS"
            name = "Unit Tests"
            goals = "test"
            runnerArgs = "-Dsurefire.failIfNoSpecifiedTests=false"
            jdkHome = "%java.home%"
        }

        script {
            name = "Cleanup database"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/sh

                UNIQUE_DB="mr_%build.number%"
                export PGPASSWORD="${'$'}{POSTGRES_PASSWORD}"
                psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${'$'}{UNIQUE_DB}' AND pid <> pg_backend_pid();" || true
                psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "DROP DATABASE IF EXISTS \"${'$'}{UNIQUE_DB}\";"
            """.trimIndent()
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

    failureConditions {
        executionTimeoutMin = 15
    }

})