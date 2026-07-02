import jetbrains.buildServer.configs.kotlin.*

version = "2026.1"

project {
    description = "Polybank"

    params {
        param("docker.registry", "192.168.130.81:5000")
        param("k8s.namespace", "polybank")
        param("k8s.environment", "prod")
        param("k8s.kubeconfig", "/opt/buildagent/work/.kube/polybank-rke2.conf")
        param("helm.chart.path", "%teamcity.build.checkoutDir%/helm")
        param("gitlab.api.url", "https://gitlab.com/api/v4")
        param("java.home", "/opt/java/openjdk")

        param("env.POSTGRES_HOST", "192.168.130.82")
        param("env.POSTGRES_PORT", "5432")
        param("env.POSTGRES_DB", "polybank_db")
        param("env.POSTGRES_USER", "polybank_user")

        param("env.JWT_EXPIRATION_MINUTES", "60")

        password("env.JWT_SECRET", "credentialsJSON:12db1afd-9baf-4d3e-873b-07ae63f3bd22")
        password("env.POSTGRES_PASSWORD", "credentialsJSON:20e6cb45-b464-447c-9363-aef8c4ccf22f")
        password("env.GITLAB_TOKEN", "credentialsJSON:18b94050-b187-44a5-a494-e37022a420b5")
    }

    buildType(MrBuild)
    buildType(BuildBuild)
    buildType(DeployBuild)
}
