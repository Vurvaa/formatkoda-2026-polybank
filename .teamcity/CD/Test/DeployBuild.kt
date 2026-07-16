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
                namespace = "%k8s.namespace.test%",
                valuesFiles = listOf("values.yaml", "values-test.yaml"),
                imageEnvSuffix = "-test",
                imageTag = "%dep.${BuildBuild.id}.build.number%",
                envSuffix = "_TEST",
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