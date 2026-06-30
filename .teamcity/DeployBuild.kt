import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object DeployBuild : BuildType({
    name = "CD - Deploy"

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
        script {
            id = "HELM_DEPLOY"
            name = "Helm Deploy"
            scriptContent = """
                #!/bin/sh
                set -e

                export KUBECONFIG=%k8s.kubeconfig%

                kubectl get nodes

                helm dependency build %helm.chart.path%/polybank/

                helm upgrade polybank \
                  -n %k8s.namespace% \
                  -i \
                  --kubeconfig %k8s.kubeconfig% \
                  --wait \
                  --atomic \
                  --timeout 5m0s \
                  %helm.chart.path%/polybank/ \
                  -f %helm.chart.path%/polybank/values.yaml \
                  --set-string image.repository=%docker.registry%/polybank \
                  --set-string image.tag=%dep.${BuildBuild.id}.build.number% \
                  --debug

                kubectl get pods -n %k8s.namespace%
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildBuild.id}"
            successfulOnly = true
        }
    }

    params {
        param("docker.registry", "192.168.130.81:5000")
    }

    failureConditions {
        executionTimeoutMin = 30
    }
})