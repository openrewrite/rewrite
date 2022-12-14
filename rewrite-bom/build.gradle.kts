plugins {
    `java-platform`
    id("org.openrewrite.build.publish")
    id("org.openrewrite.build.metadata")
}

dependencies {
    constraints {
        rootProject.subprojects.filter { it != project && !it.name.contains("benchmark") }.sortedBy { it.name }.forEach {
            api(it)
        }
    }
}

publishing {
    publications {
        named("nebula", MavenPublication::class.java) {
            from(components["javaPlatform"])
        }
    }
}
