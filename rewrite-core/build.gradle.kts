import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")

    api("com.fasterxml.jackson.core:jackson-databind:latest.release")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:latest.release")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:latest.release")

    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
}

tasks.withType<ShadowJar> {
    configurations = listOf(project.configurations.compileClasspath.get())
    archiveClassifier.set(null as String?)
    dependencies {
        include(dependency("org.eclipse.jgit:"))
    }
    relocate("org.eclipse.jgit", "org.openrewrite.shaded.jgit")
    metaInf {
        from("$rootDir/LICENSE")
        from("$rootDir/NOTICE")
    }
}

tasks.named("jar") {
    enabled = false;
    dependsOn(tasks.named("shadowJar"))
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            artifact(tasks.withType<ShadowJar>().first())

            pom.withXml {
                asElement().removeChild(asElement().getElementsByTagName("packaging").item(0))
                (asElement().getElementsByTagName("dependencies").item(0) as org.w3c.dom.Element).let { dependencies ->
                    dependencies.getElementsByTagName("dependency").let { dependencyList ->
                        var i = 0
                        var length = dependencyList.length
                        while (i < length) {
                            (dependencyList.item(i) as org.w3c.dom.Element).let { dependency ->
                                if (((dependency.getElementsByTagName("groupId").item(0) as org.w3c.dom.Element).textContent ==
                                            "org.eclipse.jgit")) {
                                    dependencies.removeChild(dependency)
                                    i--
                                    length--
                                }
                            }
                            i++
                        }
                    }
                }
            }
        }
    }
}
