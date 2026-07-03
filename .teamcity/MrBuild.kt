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

        cleanupDatabaseStep()
    }

    triggers {
        vcs {
            branchFilter = "+:mr-*"
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

        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = gitlab {
                authType = token {
                    token = "%env.GITLAB_TOKEN%"
                }
            }
        }
    }

    failureConditions {
        executionTimeoutMin = 15
    }

})
