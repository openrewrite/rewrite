import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension

apply(plugin = "org.openrewrite.license")
apply(plugin = "org.openrewrite.dependency-check")
apply(plugin = "nebula.contacts")
apply(plugin = "nebula.info")

group = "org.openrewrite"
description = "Eliminate tech-debt. Automatically."

configure<ContactsExtension> {
    val j = Contact("team@moderne.io")
    j.moniker("Moderne")

    people["team@moderne.io"] = j
}
