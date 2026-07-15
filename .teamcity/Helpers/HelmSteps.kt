package Helpers

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.script

data class HelmDeployParams(
    val namespaceParam: String,
    val valuesFiles: List<String>,
    val imageRepoParam: String,
    val imageTagParam: String,
    val webappImageRepoParam: String,
    val webappImageTagParam: String,
    val notificationImageRepoParam: String,
    val notificationImageTagParam: String,
    val trafficGenImageRepoParam: String,
    val trafficGenImageTagParam: String,
    val pgUserParam: String,
    val pgPasswordParam: String,
    val pgDatabaseParam: String,
    val kafkaBootstrapServers: String = "%kafka.bootstrap.servers%",
    val mailHost: String = "%mail.host%",
    val mailPort: String = "%mail.port%",
    val extraSetArgs: String = "",
)

fun BuildSteps.helmDeployStep(p: HelmDeployParams) {
    script {
        id = "HELM_DEPLOY"
        name = "Helm Deploy"
        scriptContent = """
            #!/bin/sh
            set -e

            export KUBECONFIG=%k8s.kubeconfig%
            kubectl get nodes

            NEEDS_RECREATE=0
            if ! kubectl get secret polybank-backend-secret -n ${p.namespaceParam} \
                -o jsonpath='{.metadata.labels.app\.kubernetes\.io/managed-by}' 2>/dev/null \
                | grep -q "Helm"; then
                NEEDS_RECREATE=1
            fi

            if kubectl get secret polybank-backend-secret -n ${p.namespaceParam} >/dev/null 2>&1; then
                if ! kubectl get secret polybank-backend-secret -n ${p.namespaceParam} \
                    -o jsonpath='{.data.replication-password}' 2>/dev/null | grep -q .; then
                    NEEDS_RECREATE=1
                fi
            fi

            if [ "${'$'}NEEDS_RECREATE" = "1" ]; then
                kubectl delete secret polybank-backend-secret -n ${p.namespaceParam} --ignore-not-found=true
            fi

            helm dependency build %helm.chart.path%/polybank/

            HELM_APP_VAR_ARGS=""
            for name in ${'$'}(echo "%env.app.vars.names%" | tr ',' ' '); do
                val="${'$'}(printenv "${'$'}name" || true)"
                HELM_APP_VAR_ARGS="${'$'}HELM_APP_VAR_ARGS --set-string appVars.${'$'}{name}.value=${'$'}val"
            done

            helm upgrade polybank \
                -n ${p.namespaceParam} \
                -i \
                --create-namespace \
                --kubeconfig %k8s.kubeconfig% \
                --wait \
                --atomic \
                --timeout 30m0s \
                %helm.chart.path%/polybank/ \
                ${p.valuesFiles.joinToString(" ") { "-f %helm.chart.path%/polybank/$it" }} \
                --set-string image.repository=${p.imageRepoParam} \
                --set-string image.tag=${p.imageTagParam} \
                --set-string webapp.image.repository=${p.webappImageRepoParam} \
                --set-string webapp.image.tag=${p.webappImageTagParam} \
                --set-string notification.image.repository=${p.notificationImageRepoParam} \
                --set-string notification.image.tag=${p.notificationImageTagParam} \
                --set-string trafficGenerator.image.repository=${p.trafficGenImageRepoParam} \
                --set-string trafficGenerator.image.tag=${p.trafficGenImageTagParam} \
                --set-string postgresAuth.username="${'$'}(printenv ${p.pgUserParam})" \
                --set-string postgresAuth.password="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresAuth.postgresPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresql.auth.password="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresql.auth.postgresPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresql.auth.replicationPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string global.postgresql.auth.password="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string global.postgresql.auth.postgresPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string global.postgresql.auth.replicationPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresAuth.database="${'$'}(printenv ${p.pgDatabaseParam})" \
                --set-string notification.kafka.bootstrapServers="${p.kafkaBootstrapServers}" \
                --set-string notification.mail.host="${p.mailHost}" \
                --set-string notification.mail.port="${p.mailPort}" \
                ${'$'}HELM_APP_VAR_ARGS \
                ${p.extraSetArgs} \
                --debug

            kubectl get pods -n ${p.namespaceParam}
        """.trimIndent()
    }
}
