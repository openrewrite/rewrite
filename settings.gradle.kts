pluginManagement {
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
        "rewrite-java-lombok",
        "rewrite-java-17", // remove this when rewrite recipe gradle plugin moves to 21
        "rewrite-java-21",
        "rewrite-json",
        "rewrite-maven",
        "rewrite-properties",
        "rewrite-protobuf",
        "rewrite-test",
        "rewrite-xml",
        "rewrite-yaml",
)

val includedProjects = file("IDE.properties").let {
    if (it.exists() && (System.getProperty("idea.active") != null || System.getProperty("idea.sync.active") != null)) {
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

gradle.allprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            allProjects
                    .minus(includedProjects)
                    .minus(arrayOf("rewrite-bom"))
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
            "rewrite-java-11",
            "rewrite-java-17",
            "rewrite-java-21"
    )
}

// ---------------------------------------------------------------
// ------ Gradle Develocity Configuration ------------------------
// ---------------------------------------------------------------

plugins {
    id("com.gradle.develocity") version "latest.release"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "latest.release"
}

develocity {
    val isCiServer = System.getenv("CI")?.equals("true") ?: false
    server = "https://ge.openrewrite.org/"
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
        publishing {
            onlyIf {
                authenticated
            }
        }

        uploadInBackground = !isCiServer
    }
}
