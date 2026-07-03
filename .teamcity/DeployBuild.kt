import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object DeployBuild : BuildType({
    name = "Deploy on server"

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
                  --set-string image.repository=%docker.registry%/polybank \
                  --set-string image.tag=%dep.${BuildBuild.id}.build.number% \
                  --set-string postgresAuth.username=%env.POSTGRES_USER% \
                  --set-string postgresAuth.password=%env.POSTGRES_PASSWORD% \
                  --set-string postgresAuth.postgresPassword=%env.POSTGRES_PASSWORD% \
                  --set-string postgresAuth.database=%env.POSTGRES_DB% \
                  --set-string postgresql.auth.username=%env.POSTGRES_USER% \
                  --set-string postgresql.auth.password=%env.POSTGRES_PASSWORD% \
                  --set-string postgresql.auth.postgresPassword=%env.POSTGRES_PASSWORD% \
                  --set-string postgresql.auth.database=%env.POSTGRES_DB% \
                  --set-string global.postgresql.auth.username=%env.POSTGRES_USER% \
                  --set-string global.postgresql.auth.password=%env.POSTGRES_PASSWORD% \
                  --set-string global.postgresql.auth.postgresPassword=%env.POSTGRES_PASSWORD% \
                  --set-string global.postgresql.auth.database=%env.POSTGRES_DB% \
                  --set-string jwt.secret=%env.JWT_SECRET% \
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