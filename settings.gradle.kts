pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "rewrite-kotlin"

plugins {
    id("com.gradle.enterprise") version "latest.release"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "latest.release"
}

gradleEnterprise {
    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    server = "https://ge.openrewrite.org/"

    buildCache {
        remote(gradleEnterprise.buildCache) {
            isEnabled = true
            val accessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY")
            isPush = isCiServer && !accessKey.isNullOrBlank()
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