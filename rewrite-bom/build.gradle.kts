plugins {
    `java-platform`
    id("org.openrewrite.build.publish")
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
