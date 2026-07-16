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
                namespaceParam = "%k8s.namespace%",
                valuesFiles = listOf("values.yaml"),
                imageRepoParam = "%docker.registry%/polybank",
                imageTagParam = "%dep.${ProdBuild.id}.env.RELEASE_VERSION%",
                webappImageRepoParam = "%docker.registry%/polybank-webapp",
                webappImageTagParam = "%dep.${ProdBuild.id}.env.RELEASE_VERSION%",
                notificationImageRepoParam = "%docker.registry%/polybank-notification",
                notificationImageTagParam = "%dep.${ProdBuild.id}.env.RELEASE_VERSION%",
                trafficGenImageRepoParam = "%docker.registry%/polybank-traffic-generator",
                trafficGenImageTagParam = "%dep.${ProdBuild.id}.env.RELEASE_VERSION%",
                pgUserParam = "POSTGRES_USER",
                pgPasswordParam = "POSTGRES_PASSWORD",
                pgDatabaseParam = "POSTGRES_DB",
                postgresHost = "polybank-pg-rw",
            )
        )
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})