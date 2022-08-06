pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

includeBuild(
    "build-src"
)

include(
    "rewrite-core",
    "rewrite-gradle",
    "rewrite-groovy",
    "rewrite-hcl",
    "rewrite-java",
    "rewrite-java-8",
    "rewrite-java-11",
    "rewrite-java-17",
    "rewrite-json",
    "rewrite-maven",
    "rewrite-properties",
    "rewrite-protobuf",
    "rewrite-xml",
    "rewrite-yaml",
    "rewrite-test",
    "rewrite-bom",
    "rewrite-benchmarks"
)

plugins {
    id("com.gradle.enterprise") version "latest.release"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "latest.release"
    id("com.gradle.enterprise.test-distribution") version "latest.release"
}

gradleEnterprise {
    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    server = "https://ge.openrewrite.org/"
    val gradleCacheRemoteUsername: String? = System.getenv("GRADLE_ENTERPRISE_CACHE_USERNAME")
    val gradleCacheRemotePassword: String? = System.getenv("GRADLE_ENTERPRISE_CACHE_PASSWORD")

    buildCache {
        remote(HttpBuildCache::class) {
            url = uri("https://ge.openrewrite.org/cache/")
            isPush = isCiServer
            if (!gradleCacheRemoteUsername.isNullOrBlank() && !gradleCacheRemotePassword.isNullOrBlank()) {
                credentials {
                    username = gradleCacheRemoteUsername
                    password = gradleCacheRemotePassword
                }
            }
        }
    }

    buildScan {
        capture {
            isTaskInputFiles = true
        }

        isUploadInBackground = !isCiServer

        publishAlways()
        this as com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures
        publishIfAuthenticated()
    }
}
