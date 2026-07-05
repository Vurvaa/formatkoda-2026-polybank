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
                kubectl get secret polybank-backend-secret -n %k8s.namespace% \
                    -o jsonpath='{.metadata.labels.app\.kubernetes\.io/managed-by}' 2>/dev/null \
                    | grep -q "Helm" \
                    || kubectl delete secret polybank-backend-secret -n %k8s.namespace% --ignore-not-found=true

                helm dependency build %helm.chart.path%/polybank/

                APP_SET_ARGS=""
                OLD_IFS="${'$'}IFS"
                IFS=','

                for v in %env.app.config.vars%; do
                    val="${'$'}(printenv "${'$'}v" || true)"
                    APP_SET_ARGS="${'$'}APP_SET_ARGS --set-string configEnv.${'$'}v=${'$'}val"
                done

                for v in %env.app.secret.vars%; do
                    val="${'$'}(printenv "${'$'}v" || true)"
                    APP_SET_ARGS="${'$'}APP_SET_ARGS --set-string secretEnv.${'$'}v=${'$'}val"
                done

                IFS="${'$'}OLD_IFS"

                PG_USER="$(printenv POSTGRES_USER)"
                PG_PASS="$(printenv POSTGRES_PASSWORD)"
                PG_DB="$(printenv POSTGRES_DB)"

                PG_SET_ARGS=""
                for prefix in postgresAuth postgresql.auth global.postgresql.auth; do
                    PG_SET_ARGS="${'$'}PG_SET_ARGS --set-string ${'$'}{prefix}.username=${'$'}PG_USER"
                    PG_SET_ARGS="${'$'}PG_SET_ARGS --set-string ${'$'}{prefix}.password=${'$'}PG_PASS"
                    PG_SET_ARGS="${'$'}PG_SET_ARGS --set-string ${'$'}{prefix}.postgresPassword=${'$'}PG_PASS"
                    PG_SET_ARGS="${'$'}PG_SET_ARGS --set-string ${'$'}{prefix}.database=${'$'}PG_DB"
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
                    --set-string image.repository=%docker.registry%/polybank \
                    --set-string image.tag=%dep.${BuildBuild.id}.build.number% \
                    ${'$'}PG_SET_ARGS \
                    ${'$'}APP_SET_ARGS \
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