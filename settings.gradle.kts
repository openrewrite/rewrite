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
}

gradleEnterprise {
    server = "https://ge.openrewrite.org/"

    buildCache {
        local {
            isEnabled = true
        }

        remote(HttpBuildCache::class) {
            isPush = true
            url = uri("https://ge.openrewrite.org/cache/")
        }

    }

    buildScan {
        publishAlways()
    }
}
