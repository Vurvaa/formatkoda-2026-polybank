package CI

import Helpers.cleanupDatabaseStep
import Helpers.prepareDatabaseStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object MrBuild : BuildType({
    name = "CI - all checks."

    val javap = "%java.home%"
    val addD = "app"
    val pomL = "app/pom.xml"

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    steps {
        prepareDatabaseStep()

        maven {
            id = "COMPILE"
            name = "Compile"
            goals = "clean compile"
            pomLocation = pomL
            workingDir = addD
            runnerArgs = "-B -Pdb-codegen"
            jdkHome = javap
        }

        maven {
            id = "UNIT_TESTS"
            name = "Unit Tests"
            goals = "test"
            pomLocation = pomL
            workingDir = addD
            runnerArgs = "-Dsurefire.failIfNoSpecifiedTests=false"
            jdkHome = javap
        }

        maven {
            id = "STYLE_CHECK"
            name = "Checkstyle"
            goals = "checkstyle:checkstyle"
            pomLocation = pomL
            workingDir = addD
            jdkHome = javap
        }

        maven {
            id = "SONARQUBE"
            name = "SonarQube Analysis"
            goals = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar"
            pomLocation = pomL
            workingDir = addD
            runnerArgs = """
                -Dsonar.host.url=%sonar.host.url%
                -Dsonar.token=%env.SONAR_TOKEN%
                -Dsonar.projectKey=polybank
                -Dsonar.projectName=Polybank
                -Dsonar.projectVersion=%teamcity.build.branch%-%build.number%
                -Dsonar.java.binaries=target/classes
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                -Dsonar.qualitygate.wait=true
            """.trimIndent().replace("\n", " ")
            jdkHome = javap
        }

        script {
            id = "WEBAPP_CHECK"
            name = "Webapp Build"
            workingDir = "webapp"
            scriptContent = """
                #!/bin/sh
                set -e

                npm ci
                npm run build
            """.trimIndent()
        }

        cleanupDatabaseStep()
    }

    triggers {
        vcs {
            branchFilter = "+:feature/*"
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