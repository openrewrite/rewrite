package org.openrewrite.xml.search

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser

class FindTagTest : XmlParser() {
    private val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependency>
        """.trimIndent())

    @Test
    fun matchAbsolute() {
        assertNotNull(FindTag("/dependencies/dependency").visit(x))
        assertNotNull(FindTag("/dependencies/*").visit(x))
        assertNull(FindTag("/dependency/dne").visit(x))
    }
}