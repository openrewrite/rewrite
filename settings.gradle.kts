pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(
    "rewrite-core",
    "rewrite-gradle",
    "rewrite-groovy",
    "rewrite-hcl",
    "rewrite-java",
    "rewrite-java-8",
    "rewrite-java-11",
    "rewrite-json",
    "rewrite-maven",
    "rewrite-properties",
    "rewrite-xml",
    "rewrite-yaml",
    "rewrite-test",
    "rewrite-benchmarks"
)

plugins {
    id("com.gradle.enterprise") version "3.7"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.4.2"
}

gradleEnterprise {
    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    server = "https://ge.openrewrite.org/"

    buildCache {
        local {
            isEnabled = false
        }
        remote(HttpBuildCache::class) {
            url = uri("https://ge.openrewrite.org/cache/")
            isPush = isCiServer
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
