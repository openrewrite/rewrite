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
            "rewrite-java-11",
            "rewrite-java-17",
            "rewrite-java-21"
    )
}

// ---------------------------------------------------------------
// ------ Gradle Enterprise Configuration ------------------------
// ---------------------------------------------------------------

plugins {
    id("com.gradle.enterprise") version "3.13.3"
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
