package Helpers

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.script

data class HelmDeployParams(
    val namespace: String,
    val valuesFiles: List<String>,
    val imageRegistry: String = "%docker.registry%",
    val imageEnvSuffix: String = "",
    val imageTag: String,
    val envSuffix: String = "",
    val postgresHost: String = "polybank-pg-rw",
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
                -n ${p.namespace} \
                -i \
                --create-namespace \
                --kubeconfig %k8s.kubeconfig% \
                --wait \
                --atomic \
                --timeout 30m0s \
                %helm.chart.path%/polybank/ \
                ${p.valuesFiles.joinToString(" ") { "-f %helm.chart.path%/polybank/$it" }} \
                --set-string image.repository=${p.imageRegistry}/polybank${p.imageEnvSuffix} \
                --set-string image.tag=${p.imageTag} \
                --set-string webapp.image.repository=${p.imageRegistry}/polybank-webapp${p.imageEnvSuffix} \
                --set-string webapp.image.tag=${p.imageTag} \
                --set-string notification.image.repository=${p.imageRegistry}/polybank-notification${p.imageEnvSuffix} \
                --set-string notification.image.tag=${p.imageTag} \
                --set-string trafficGenerator.image.repository=${p.imageRegistry}/polybank-traffic-generator${p.imageEnvSuffix} \
                --set-string trafficGenerator.image.tag=${p.imageTag} \
                --set-string postgresAuth.username="${'$'}(printenv POSTGRES_USER${p.envSuffix})" \
                --set-string postgresAuth.password="${'$'}(printenv POSTGRES_PASSWORD${p.envSuffix})" \
                --set-string postgresAuth.postgresPassword="${'$'}(printenv POSTGRES_PASSWORD${p.envSuffix})" \
                --set-string postgresAuth.database="${'$'}(printenv POSTGRES_DB${p.envSuffix})" \
                --set-string notification.kafka.bootstrapServers="%kafka.bootstrap.servers%" \
                --set-string notification.mail.host="%mail.host%" \
                --set-string notification.mail.port="%mail.port%" \
                --set-string postgresql.host="${p.postgresHost}" \
                --set-string postgresql.port="5432" \
                --set-string appVars.appJwtExpirationMinutes="${'$'}(printenv APP_JWT_EXPIRATION_MINUTES)" \
                --set-string appVars.appCorsAllowedOrigin="${'$'}(printenv APP_CORS_ALLOWED_ORIGIN | sed 's/,/\\,/g')" \
                --set-string appVars.appJwtSecret="${'$'}(printenv APP_JWT_SECRET)" \
                --set-string redis.auth.password="${'$'}(printenv REDIS_PASSWORD)" \
                --set-string redis.auth.user="${'$'}(printenv REDIS_USER)" \
                --set-string redis.auth.database="${'$'}(printenv REDIS_DATABASE)" \
                --debug

            kubectl get pods -n ${p.namespace}
        """.trimIndent()
    }
}
