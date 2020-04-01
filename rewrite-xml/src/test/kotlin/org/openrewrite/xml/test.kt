package org.openrewrite.xml

import org.junit.jupiter.api.Assertions.assertEquals
import org.openrewrite.xml.tree.Xml

fun assertRefactored(doc: Xml.Document, refactored: String) {
    assertEquals(refactored.trimIndent(), doc.printTrimmed())
}