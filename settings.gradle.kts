plugins {
    id("com.gradle.enterprise") version "3.1"
}

apply(from = "gradle/build-cache-configuration.settings.gradle.kts")

include("rewrite-core", "rewrite-java", "rewrite-xml")
//, "rewrite-gradle-enterprise")