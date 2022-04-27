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
