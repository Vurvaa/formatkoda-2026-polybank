package CD.Prod

import Helpers.HelmDeployParams
import Helpers.helmDeployStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object ProdDeployBuild : BuildType({
    name = "CD - (prod) deploy on prod."

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    dependencies {
        snapshot(ProdBuild) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${ProdBuild.id}"
            successfulOnly = true
        }
    }

    steps {
        helmDeployStep(
            HelmDeployParams(
                namespace = "%k8s.namespace%",
                valuesFiles = listOf("values.yaml"),
                imageTag = "%dep.${ProdBuild.id}.env.RELEASE_VERSION%",
            )
        )
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})