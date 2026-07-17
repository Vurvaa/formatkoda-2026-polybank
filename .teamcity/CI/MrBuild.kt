//Мистер Билд. =)
package CI

import Helpers.cleanupDatabaseStep
import Helpers.detectChangedServicesStep
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
    val pomp = "app/pom.xml"
    val notificationPomp = "notification/pom.xml"

    val buildApp = "env.BUILD_APP"
    val buildNot = "env.BUILD_NOTIFICATION"
    val buildWeb = "env.BUILD_WEBAPP"
    val buildTra = "env.BUILD_TRAFFIC_GENERATOR"

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    steps {
        detectChangedServicesStep(baseRef = "main")

        prepareDatabaseStep()

        maven {
            id = "COMPILE"
            name = "Compile"
            pomLocation = pomp
            goals = "clean compile"
            runnerArgs = "-B -Pdb-codegen"
            jdkHome = javap

            conditions {
                equals("env.BUILD_APP", "true")
            }
        }

        maven {
            id = "UNIT_TESTS"
            name = "Unit Tests"
            pomLocation = pomp
            goals = "test"
            runnerArgs = "-Dsurefire.failIfNoSpecifiedTests=false"
            jdkHome = javap
            conditions {
                equals(buildApp, "true")
            }
        }

        maven {
            id = "STYLE_CHECK"
            name = "Checkstyle"
            pomLocation = pomp
            goals = "checkstyle:checkstyle"
            jdkHome = javap
            conditions {
                equals(buildApp, "true")
            }
        }

        maven {
            id = "NOTIFICATION_COMPILE"
            name = "Notification Compile"
            pomLocation = notificationPomp
            goals = "clean compile"
            runnerArgs = "-B -Pdb-codegen"
            jdkHome = javap

            conditions {
                equals(buildNot, "true")
            }
        }

        maven {
            id = "NOTIFICATION_UNIT_TESTS"
            name = "Notification Unit Tests"
            pomLocation = notificationPomp
            goals = "test"
            runnerArgs = "-Dsurefire.failIfNoSpecifiedTests=false"
            jdkHome = javap

            conditions {
                equals(buildNot, "true")
            }
        }

        maven {
            id = "NOTIFICATION_STYLE_CHECK"
            name = "Notification Checkstyle"
            pomLocation = notificationPomp
            goals = "checkstyle:checkstyle"
            jdkHome = javap

            conditions {
                equals(buildNot, "true")
            }
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

            conditions {
                equals(buildWeb, "true")
            }
        }

        script {
            id = "WEBAPP_LINT"
            name = "Webapp Lint"
            workingDir = "webapp"

            scriptContent = """
                #!/bin/sh
                set -e

                npx eslint .
            """.trimIndent()

            conditions {
                equals(buildWeb, "true")
            }
        }

        script {
            id = "TRAFFIC_GENERATOR_TESTS"
            name = "Traffic Generator Unit Tests"
            workingDir = "traffic-generator"

            scriptContent = """
                #!/bin/sh
                set -e

                ./gradlew test --no-daemon
            """.trimIndent()

            conditions {
                equals(buildTra, "true")
            }
        }

        script {
            id = "TRAFFIC_GENERATOR_BUILD"
            name = "Traffic Generator Build"
            workingDir = "traffic-generator"

            scriptContent = """
                #!/bin/sh
                set -e

                ./gradlew build -x test --no-daemon
            """.trimIndent()

            conditions {
                equals(buildTra, "true")
            }
        }

        maven {
            id = "SONARQUBE_APP"
            name = "SonarQube Analysis App"
            pomLocation = pomp
            goals = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar"
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

            conditions {
                equals(buildApp, "true")
            }
        }

        maven {
            id = "SONARQUBE_NOTIFICATION"
            name = "SonarQube Analysis Notification"
            pomLocation = notificationPomp
            goals = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar"
            runnerArgs = """
                -Dsonar.host.url=%sonar.host.url%
                -Dsonar.token=%env.SONAR_TOKEN%
                -Dsonar.projectKey=polybank-notification
                -Dsonar.projectName=Polybank Notification
                -Dsonar.projectVersion=%teamcity.build.branch%-%build.number%
                -Dsonar.java.binaries=target/classes
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                -Dsonar.qualitygate.wait=true
            """.trimIndent().replace("\n", " ")
            jdkHome = javap

            conditions {
                equals(buildNot, "true")
            }
        }

        script {
            id = "SONARQUBE_WEBAPP"
            name = "SonarQube Analysis Webapp"
            workingDir = "webapp"
            scriptContent = """
                #!/bin/sh
                set -e
                npx sonarqube-scanner \
                  -Dsonar.host.url=%sonar.host.url% \
                  -Dsonar.token=%env.SONAR_TOKEN% \
                  -Dsonar.projectKey=polybank-webapp \
                  -Dsonar.projectName=Polybank Webapp \
                  -Dsonar.projectVersion=%teamcity.build.branch%-%build.number% \
                  -Dsonar.sources=src \
                  -Dsonar.qualitygate.wait=true
            """.trimIndent()

            conditions {
                equals(buildWeb, "true")
            }
        }

        script {
            id = "SONARQUBE_TRAFFIC"
            name = "SonarQube Analysis Traffic Generator"
            workingDir = "traffic-generator"
            scriptContent = """
                #!/bin/sh
                set -e
                ./gradlew sonarqube --no-daemon \
                  -Dsonar.host.url=%sonar.host.url% \
                  -Dsonar.token=%env.SONAR_TOKEN% \
                  -Dsonar.projectKey=polybank-traffic \
                  -Dsonar.projectName=Polybank Traffic Generator \
                  -Dsonar.projectVersion=%teamcity.build.branch%-%build.number% \
                  -Dsonar.qualitygate.wait=true
            """.trimIndent()

            conditions {
                equals(buildTra, "true")
            }
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
        executionTimeoutMin = 30
    }

})
