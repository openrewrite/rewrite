plugins {
    `java-platform`
    id("org.openrewrite.build.publish")
    id("org.openrewrite.build.metadata")
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter {
                it != project &&
                        !it.name.contains("benchmark") &&
                        !it.name.contains("tck") &&
                        it.name != "tools"
            }
            .sortedBy { it.name }
            .forEach { api(it) }
    }
}
