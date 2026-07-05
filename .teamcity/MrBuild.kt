import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher

object MrBuild : BuildType({
    name = "Checks on MR."

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

        maven {
            id = "SONARQUBE"
            name = "SonarQube Analysis"
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
            jdkHome = "%java.home%"
        }

        cleanupDatabaseStep()
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