package org.openrewrite.xml.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser

class FindTagsTest : XmlParser() {
    private val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependencies>
                <dependency/>
                <dependency/>
            </dependency>
        """.trimIndent())

    @Test
    fun matchAbsolute() {
        assertThat(FindTags("/dependencies/dependency").visit(x).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }

    @Test
    fun matchRelative() {
        assertThat(FindTags("dependencies/dependency").visit(x.root).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }

    @Test
    fun matchRelativeWildcard() {
        assertThat(FindTags("dependencies/*").visit(x.root).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }
}
