pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "rewrite-kotlin"

plugins {
    id("com.gradle.develocity") version "latest.release"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "latest.release"
}

develocity {
    server = "https://ge.openrewrite.org/"

    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    val accessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY")
    val authenticated = !accessKey.isNullOrBlank()
    buildCache {
        remote(develocity.buildCache) {
            isEnabled = true
            isPush = isCiServer && authenticated
        }
    }

    buildScan {
        capture {
            fileFingerprints = true
        }

        uploadInBackground = !isCiServer
    }
}
