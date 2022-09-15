import org.gradle.api.tasks.javadoc.Javadoc

plugins {
    `java-base`
    signing
    id("org.openrewrite.base")
    id("nebula.maven-publish")
    id("nebula.maven-resolved-dependencies")
    id("nebula.maven-apache-license")
}

plugins.withId("com.github.johnrengelman.shadow") {
    apply(plugin = "nebula.maven-shadow-publish")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

signing {
    setRequired({
        !project.version.toString().endsWith("SNAPSHOT") || project.hasProperty("forceSigning")
    })
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["nebula"])
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")

            pom.withXml {
                (asElement().getElementsByTagName("dependencies").item(0) as org.w3c.dom.Element).let { dependencies ->
                    dependencies.getElementsByTagName("dependency").let { dependencyList ->
                        var i = 0
                        var length = dependencyList.length
                        while (i < length) {
                            (dependencyList.item(i) as org.w3c.dom.Element).let { dependency ->
                                if ((dependency.getElementsByTagName("scope").item(0) as org.w3c.dom.Element).textContent == "provided" ||
                                        (dependency.getElementsByTagName("groupId").item(0) as org.w3c.dom.Element).textContent == "org.projectlombok"
                                ) {
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
