package CD.Test

import Helpers.HelmDeployParams
import Helpers.helmDeployStep
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object DeployBuild : BuildType({
    name = "CD - (test) deploy on test stand."

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    dependencies {
        snapshot(BuildBuild) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    steps {
        helmDeployStep(
            HelmDeployParams(
                namespaceParam = "%k8s.namespace.test%",
                valuesFiles = listOf("values.yaml", "values-test.yaml"),
                imageRepoParam = "%docker.registry%/polybank-test",
                imageTagParam = "%dep.${BuildBuild.id}.build.number%",
                webappImageRepoParam = "%docker.registry%/polybank-webapp-test",
                webappImageTagParam = "%dep.${BuildBuild.id}.build.number%",
                pgUserParam = "POSTGRES_USER_TEST",
                pgPasswordParam = "POSTGRES_PASSWORD_TEST",
                pgDatabaseParam = "POSTGRES_DB_TEST",
            )
        )
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildBuild.id}"
            successfulOnly = true
        }
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})
