import jetbrains.buildServer.configs.kotlin.*

version = "2026.1"

data class AppEnvVar(
    val name: String,
    val value: String,
    val secret: Boolean = false,
)

val appEnvVars = listOf(
    AppEnvVar(name = "APP_JWT_EXPIRATION_MINUTES", value = "60"),
    AppEnvVar(
        name = "APP_JWT_SECRET",
        value = "credentialsJSON:12db1afd-9baf-4d3e-873b-07ae63f3bd22",
        secret = true,
    ),
)

project {
    description = "Polybank"

    params {
        param("docker.registry", "192.168.130.81:5000")
        param("k8s.namespace", "polybank")
        param("k8s.kubeconfig", "/opt/buildagent/work/.kube/polybank-rke2.conf")
        param("helm.chart.path", "%teamcity.build.checkoutDir%/helm")
        param("gitlab.api.url", "https://gitlab.com/api/v4")
        param("java.home", "/opt/java/openjdk")

        password("env.GITLAB_TOKEN", "credentialsJSON:18b94050-b187-44a5-a494-e37022a420b5")

        param("env.POSTGRES_HOST", "192.168.130.82")
        param("env.POSTGRES_PORT", "5432")
        param("env.POSTGRES_DB", "polybank_db")
        param("env.POSTGRES_USER", "polybank_user")
        password("env.POSTGRES_PASSWORD", "credentialsJSON:20e6cb45-b464-447c-9363-aef8c4ccf22f")

        param("sonar.host.url", "http://192.168.130.82:9000")
        password("env.SONAR_TOKEN", "credentialsJSON:2172e929-59f3-4d2c-8142-9c45447289c5")

        appEnvVars.forEach { v ->
            if (v.secret) password("env.${v.name}", v.value) else param("env.${v.name}", v.value)
        }
        param("env.app.config.vars", appEnvVars.filter { !it.secret }.joinToString(",") { it.name })
        param("env.app.secret.vars", appEnvVars.filter { it.secret }.joinToString(",") { it.name })
    }

    buildType(MrBuild)
    buildType(BuildBuild)
    buildType(DeployBuild)
}
