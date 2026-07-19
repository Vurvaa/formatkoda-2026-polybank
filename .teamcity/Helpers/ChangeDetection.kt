package Helpers

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.script

val SERVICE_DIRS = listOf("app", "webapp", "notification", "traffic-generator")

fun BuildSteps.detectChangedServicesStep(
    baseRef: String? = null,
) {
    script {
        id = "DETECT_CHANGED_SERVICES"
        name = "Detect changed services"
        scriptContent = """
            #!/bin/sh
            set -e

            ${if (baseRef != null) """
            git fetch origin $baseRef 2>/dev/null || true
            PREV_SHA=${'$'}(git merge-base HEAD "origin/$baseRef" 2>/dev/null || git merge-base HEAD "$baseRef" 2>/dev/null || true)
            """.trimIndent() else """
            PREV_SHA=${'$'}(git rev-parse HEAD^ 2>/dev/null || true)
            """.trimIndent()}

            if [ -z "${'$'}PREV_SHA" ]; then
                echo "No base commit found, treating everything as changed"
                CHANGED_FILES="ALL"
            else
                CHANGED_FILES=${'$'}(git diff --name-only "${'$'}PREV_SHA" HEAD)
                echo "Changed files (vs ${'$'}PREV_SHA):"
                echo "${'$'}CHANGED_FILES"
            fi

            BUILD_APP=false
            BUILD_WEBAPP=false
            BUILD_NOTIFICATION=false
            BUILD_TRAFFIC_GENERATOR=false
            BUILD_ALL=false

            if [ "${'$'}CHANGED_FILES" = "ALL" ]; then
                BUILD_ALL=true
            else
                OTHER_CHANGES=${'$'}(echo "${'$'}CHANGED_FILES" | grep -v -E '^(app|webapp|notification|traffic-generator)/' || true)
                if [ -n "${'$'}OTHER_CHANGES" ]; then
                    echo "Changes outside known service directories detected:"
                    echo "${'$'}OTHER_CHANGES"
                    BUILD_ALL=true
                fi
            fi

            if [ "${'$'}BUILD_ALL" = "true" ]; then
                BUILD_APP=true
                BUILD_WEBAPP=true
                BUILD_NOTIFICATION=true
                BUILD_TRAFFIC_GENERATOR=true
            else
                echo "${'$'}CHANGED_FILES" | grep -q '^app/' && BUILD_APP=true || true
                echo "${'$'}CHANGED_FILES" | grep -q '^webapp/' && BUILD_WEBAPP=true || true
                echo "${'$'}CHANGED_FILES" | grep -q '^notification/' && BUILD_NOTIFICATION=true || true
                echo "${'$'}CHANGED_FILES" | grep -q '^traffic-generator/' && BUILD_TRAFFIC_GENERATOR=true || true
            fi

            echo "BUILD_APP=${'$'}BUILD_APP"
            echo "BUILD_WEBAPP=${'$'}BUILD_WEBAPP"
            echo "BUILD_NOTIFICATION=${'$'}BUILD_NOTIFICATION"
            echo "BUILD_TRAFFIC_GENERATOR=${'$'}BUILD_TRAFFIC_GENERATOR"

            echo "##teamcity[setParameter name='env.BUILD_APP' value='${'$'}BUILD_APP']"
            echo "##teamcity[setParameter name='env.BUILD_WEBAPP' value='${'$'}BUILD_WEBAPP']"
            echo "##teamcity[setParameter name='env.BUILD_NOTIFICATION' value='${'$'}BUILD_NOTIFICATION']"
            echo "##teamcity[setParameter name='env.BUILD_TRAFFIC_GENERATOR' value='${'$'}BUILD_TRAFFIC_GENERATOR']"
        """.trimIndent()
    }
}

fun BuildSteps.retagUnchangedImageStep(
    id: String,
    name: String,
    image: String,
    condition: String,
    dockerfilePath: String,
    contextDir: String,
    mavenWorkingDir: String? = null,
) {
    script {
        this.id = id
        this.name = name
        scriptContent = """
            #!/bin/sh
            set -e

            if [ "%$condition%" = "true" ]; then
                echo "Service changed, skipping retag (will be rebuilt)"
                exit 0
            fi

            IMAGE="%docker.registry%/$image"
            PREV_BUILD=${'$'}(( %build.number% - 1 ))

            if docker pull "${'$'}IMAGE:${'$'}PREV_BUILD" 2>/dev/null; then
                echo "Found previous build image ${'$'}IMAGE:${'$'}PREV_BUILD, retagging as %build.number%"
                docker tag "${'$'}IMAGE:${'$'}PREV_BUILD" "${'$'}IMAGE:%build.number%"
                docker push "${'$'}IMAGE:%build.number%"
                exit 0
            fi

            echo "No image tagged ${'$'}PREV_BUILD, looking up latest tag via registry API"
            LATEST_TAG=${'$'}(curl -sf "http://%docker.registry%/v2/$image/tags/list" \
                | tr ',' '\n' | grep -o '"[0-9]\{1,\}"' | tr -d '"' | sort -n | tail -1 || true)

            if [ -n "${'$'}LATEST_TAG" ]; then
                echo "Found latest existing tag ${'$'}LATEST_TAG, retagging as %build.number%"
                docker pull "${'$'}IMAGE:${'$'}LATEST_TAG"
                docker tag "${'$'}IMAGE:${'$'}LATEST_TAG" "${'$'}IMAGE:%build.number%"
                docker push "${'$'}IMAGE:%build.number%"
                exit 0
            fi

            echo "No existing image found in registry for $image, building from scratch"
            ${if (mavenWorkingDir != null) """
            (cd $mavenWorkingDir && ./mvnw clean package -DskipTests -B -Pdb-codegen)
            """.trimIndent() else ""}
            docker build -f $dockerfilePath -t "${'$'}IMAGE:%build.number%" $contextDir
            docker push "${'$'}IMAGE:%build.number%"
        """.trimIndent()
    }
}
