package Helpers

import jetbrains.buildServer.configs.kotlin.BuildStep
import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.script

fun BuildSteps.prepareDatabaseStep(
    id: String = "prepare_database",
    name: String = "Prepare database",
    workingDir: String = "app",
    uniqueDbPrefix: String = "mr",
    urlParamName: String = "env.POSTGRES_URL",
    extraLiquibaseDirs: List<String> = emptyList(),
) {
    script {
        this.id = id
        this.name = name
        this.workingDir = workingDir
        scriptContent = """
            #!/bin/sh
            set -e
            UNIQUE_DB="${uniqueDbPrefix}_%build.number%"
            export PGPASSWORD="${'$'}{POSTGRES_PASSWORD}"

            psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "CREATE DATABASE \"${'$'}{UNIQUE_DB}\";"

            POSTGRES_URL="jdbc:postgresql://${'$'}{POSTGRES_HOST}:${'$'}{POSTGRES_PORT}/${'$'}{UNIQUE_DB}"

            ./mvnw process-resources liquibase:update -B \
                -Dliquibase.url=${'$'}{POSTGRES_URL} \
                -Dliquibase.username=${'$'}POSTGRES_USER \
                -Dliquibase.password=${'$'}POSTGRES_PASSWORD \
                -Dliquibase.changeLogFile=db/changelog/db.changelog-master.xml \
                -Dliquibase.searchPath=src/main/resources
            ${extraLiquibaseDirs.joinToString("") { dir ->
                """

            (
                cd ../$dir
                ./mvnw process-resources liquibase:update -B \
                    -Dliquibase.url=${'$'}{POSTGRES_URL} \
                    -Dliquibase.username=${'$'}POSTGRES_USER \
                    -Dliquibase.password=${'$'}POSTGRES_PASSWORD \
                    -Dliquibase.changeLogFile=db/changelog/db.changelog-master.xml \
                    -Dliquibase.searchPath=src/main/resources
            )"""
            }}

            echo "##teamcity[setParameter name='${urlParamName}' value='${'$'}{POSTGRES_URL}']"
            echo "##teamcity[setParameter name='env.UNIQUE_DB' value='${'$'}{UNIQUE_DB}']"
        """.trimIndent()
    }
}

fun BuildSteps.cleanupDatabaseStep(
    name: String = "Cleanup database",
    uniqueDbPrefix: String = "mr",
) {
    script {
        this.name = name
        executionMode = BuildStep.ExecutionMode.ALWAYS
        scriptContent = """
            #!/bin/sh

            UNIQUE_DB="${uniqueDbPrefix}_%build.number%"
            export PGPASSWORD="${'$'}{POSTGRES_PASSWORD}"
            psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${'$'}{UNIQUE_DB}' AND pid <> pg_backend_pid();" || true
            psql -h ${'$'}{POSTGRES_HOST} -p ${'$'}{POSTGRES_PORT} -U ${'$'}{POSTGRES_USER} -d postgres -c "DROP DATABASE IF EXISTS \"${'$'}{UNIQUE_DB}\";"
        """.trimIndent()
    }
}