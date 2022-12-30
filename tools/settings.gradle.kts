pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace!!.startsWith("org.openrewrite.build")) {
                useVersion("1.4.0")
            }
        }
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include("language-parser-builder")
