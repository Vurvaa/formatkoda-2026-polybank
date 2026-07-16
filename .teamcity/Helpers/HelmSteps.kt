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
    val postgresHost: String,
    val postgresPort: String = "5432",
    val appJwtExpirationMinutesParam: String = "APP_JWT_EXPIRATION_MINUTES",
    val appCorsAllowedOriginParam: String = "APP_CORS_ALLOWED_ORIGIN",
    val appJwtSecretParam: String = "APP_JWT_SECRET",
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
                --set-string postgresAuth.database="${'$'}(printenv ${p.pgDatabaseParam})" \
                --set-string notification.kafka.bootstrapServers="${p.kafkaBootstrapServers}" \
                --set-string notification.mail.host="${p.mailHost}" \
                --set-string notification.mail.port="${p.mailPort}" \
                --set-string postgresql.host="${p.postgresHost}" \
                --set-string postgresql.port="${p.postgresPort}" \
                --set-string appVars.appJwtExpirationMinutes="${'$'}(printenv ${p.appJwtExpirationMinutesParam})" \
                --set-string appVars.appCorsAllowedOrigin="${'$'}(printenv ${p.appCorsAllowedOriginParam} | sed 's/,/\\,/g')" \
                --set-string appVars.appJwtSecret="${'$'}(printenv ${p.appJwtSecretParam})" \
                ${p.extraSetArgs} \
                --debug

            kubectl get pods -n ${p.namespaceParam}
        """.trimIndent()
    }
}