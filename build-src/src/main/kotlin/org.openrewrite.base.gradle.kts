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
        if(System.getProperty("org.openrewrite.maven.localRepo") == null) {
            mavenLocal()
        } else {
            maven {
                url = uri(System.getProperty("org.openrewrite.maven.localRepo"))
            }
        }
        if(System.getProperty("org.openrewrite.maven.snapshotRepo") == null) {
            maven {
                url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            }
        } else {
            maven {
                url = uri(System.getProperty("org.openrewrite.maven.snapshotRepo"))
            }
        }
    }
    if(System.getProperty("org.openrewrite.maven.remoteRepo") == null) {
        mavenCentral()
    } else {
        maven {
            url = uri(System.getProperty("org.openrewrite.maven.remoteRepo"))
        }
    }
}

configure<ContactsExtension> {
    val j = Contact("team@moderne.io")
    j.moniker("Moderne")

    people["team@moderne.io"] = j
}
