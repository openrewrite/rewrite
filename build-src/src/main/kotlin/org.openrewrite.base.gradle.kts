import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension

plugins {
    base
    id("org.openrewrite.license")
    id("org.openrewrite.dependency-check")
    id("nebula.contacts")
    id("nebula.info")
}

group = "org.openrewrite"
description = "Eliminate tech-debt. Automatically."

repositories {
    if (!project.hasProperty("releasing")) {
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }
    mavenCentral()
}

configure<ContactsExtension> {
    val j = Contact("team@moderne.io")
    j.moniker("Moderne")

    people["team@moderne.io"] = j
}
