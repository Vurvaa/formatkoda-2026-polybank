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
    val pgUserParam: String,
    val pgPasswordParam: String,
    val pgDatabaseParam: String,
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

            kubectl get secret polybank-backend-secret -n ${p.namespaceParam} \
                -o jsonpath='{.metadata.labels.app\.kubernetes\.io/managed-by}' 2>/dev/null \
                | grep -q "Helm" \
                || kubectl delete secret polybank-backend-secret -n ${p.namespaceParam} --ignore-not-found=true

            helm dependency build %helm.chart.path%/polybank/

            HELM_APP_VAR_ARGS=""
            for name in %env.app.vars.names%; do
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
                --set-string postgresAuth.username="${'$'}(printenv ${p.pgUserParam})" \
                --set-string postgresAuth.password="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresAuth.postgresPassword="${'$'}(printenv ${p.pgPasswordParam})" \
                --set-string postgresAuth.database="${'$'}(printenv ${p.pgDatabaseParam})" \
                ${'$'}HELM_APP_VAR_ARGS \
                ${p.extraSetArgs} \
                --debug

            kubectl get pods -n ${p.namespaceParam}
        """.trimIndent()
    }
}
