pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace != null && requested.id.namespace!!.startsWith("org.openrewrite.build")) {
                useVersion("1.7.3")
            }
        }
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

// ---------------------------------------------------------------
// ------ Included Projects --------------------------------------
// ---------------------------------------------------------------

val allProjects = listOf(
        "rewrite-benchmarks",
        "rewrite-bom",
        "rewrite-core",
        "rewrite-gradle",
        "rewrite-groovy",
        "rewrite-hcl",
        "rewrite-java",
        "rewrite-java-tck",
        "rewrite-java-test",
        "rewrite-java-17",
        "rewrite-json",
        "rewrite-maven",
        "rewrite-properties",
        "rewrite-protobuf",
        "rewrite-test",
        "rewrite-xml",
        "rewrite-yaml",
)

val includedProjects = file("IDE.properties").let {
    if (it.exists()) {
        val props = java.util.Properties()
        it.reader().use { reader ->
            props.load(reader)
        }
        allProjects.intersect(props.keys)
    } else {
        allProjects
    }
}.toSet()

if(!file("IDE.properties").exists() || includedProjects.contains("tools")) {
    includeBuild("tools")
}

include(*allProjects.toTypedArray())

allProjects.minus(includedProjects).forEach {
    // sinkhole this project to a directory that intentionally doesn't exist, so that it
    // can be efficiently substituted for a module dependency below
    project(":$it").projectDir = file("sinkhole-$it")
}

gradle.allprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            allProjects
                    .minus(includedProjects)
                    .minus(arrayOf("rewrite-benchmarks", "rewrite-bom"))
                    .forEach {
                        substitute(project(":$it"))
                                .using(module("org.openrewrite:$it:latest.integration"))
                    }
        }
    }
}

if (System.getProperty("idea.active") == null &&
        System.getProperty("idea.sync.active") == null) {
    include(
            "rewrite-java-8",
            "rewrite-java-11"
    )
}

// ---------------------------------------------------------------
// ------ Gradle Enterprise Configuration ------------------------
// ---------------------------------------------------------------

plugins {
    id("com.gradle.enterprise") version "3.11"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "latest.release"
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
