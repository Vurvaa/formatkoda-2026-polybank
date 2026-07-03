import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object DeployBuild : BuildType({
    name = "Deploy on server."

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

                kubectl get secret polybank-backend-secret -n %k8s.namespace% -o jsonpath='{.metadata.labels.app\.kubernetes\.io/managed-by}' | grep -q "Helm" || \
                kubectl delete secret polybank-backend-secret -n %k8s.namespace% --ignore-not-found=true

                helm dependency build %helm.chart.path%/polybank/

                SET_ARGS="
                  image.repository=%docker.registry%/polybank
                  image.tag=%dep.${BuildBuild.id}.build.number%
                  global.postgresql.auth.username=%env.POSTGRES_USER%
                  global.postgresql.auth.password=%env.POSTGRES_PASSWORD%
                  global.postgresql.auth.postgresPassword=%env.POSTGRES_PASSWORD%
                  global.postgresql.auth.database=%env.POSTGRES_DB%
                  jwt.secret=%env.JWT_SECRET%
                "

                SET_STRING_FLAGS=""
                for kv in ${'$'}SET_ARGS; do
                  SET_STRING_FLAGS="${'$'}SET_STRING_FLAGS --set-string ${'$'}kv"
                done

                helm upgrade polybank \
                  -n %k8s.namespace% \
                  -i \
                  --create-namespace \
                  --kubeconfig %k8s.kubeconfig% \
                  --wait \
                  --atomic \
                  --timeout 30m0s \
                  %helm.chart.path%/polybank/ \
                  -f %helm.chart.path%/polybank/values.yaml \
                  ${'$'}SET_STRING_FLAGS \
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

    failureConditions {
        executionTimeoutMin = 30
    }
})