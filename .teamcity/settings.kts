import CD.Prod.ProdBuild
import CD.Prod.ProdDeployBuild
import CD.Test.BuildBuild
import CD.Test.DeployBuild
import CI.MrBuild
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
        param("k8s.kubeconfig", "/opt/buildagent/work/.kube/polybank-rke2.conf")
        param("helm.chart.path", "%teamcity.build.checkoutDir%/helm")
        param("gitlab.api.url", "https://gitlab.com/api/v4")
        param("java.home", "/opt/java/openjdk")

        password("env.GITLAB_TOKEN", "credentialsJSON:18b94050-b187-44a5-a494-e37022a420b5")

        param("k8s.namespace", "polybank")
        param("env.POSTGRES_DB", "polybank_db")
        param("env.POSTGRES_USER", "polybank_user")
        password("env.POSTGRES_PASSWORD", "credentialsJSON:20e6cb45-b464-447c-9363-aef8c4ccf22f")

        param("env.POSTGRES_HOST", "192.168.130.82")
        param("env.POSTGRES_PORT", "5432")

        param("k8s.namespace.test", "polybank-test")
        param("env.POSTGRES_DB_TEST", "polybank_test_db")
        param("env.POSTGRES_USER_TEST", "polybank_test_user")
        password("env.POSTGRES_PASSWORD_TEST", "credentialsJSON:debaa4c2-5b4e-4354-b881-78845df7998c")

        param("sonar.host.url", "http://192.168.130.84:9000")
        password("env.SONAR_TOKEN", "credentialsJSON:17fe68f4-ab0c-4290-b334-0b78b080d20b")

        param("kafka.bootstrap.servers", "192.168.130.82:9092")
        param("mail.host", "192.168.130.81")
        param("mail.port", "1025")

        appEnvVars.forEach { v ->
            if (v.secret) password("env.${v.name}", v.value) else param("env.${v.name}", v.value)
        }
        param("env.app.vars.names", appEnvVars.joinToString(",") { it.name })
    }

    buildType(MrBuild)
    buildType(BuildBuild)
    buildType(DeployBuild)
    buildType(ProdBuild)
    buildType(ProdDeployBuild)
}