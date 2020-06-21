package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import org.openrewrite.Tree.randomId
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.tree.Xml

class MavenTagInsertionComparatorTest {
    @Test
    fun betweenElementsInOrder() {
        val a = XmlParser().parse("""
            <project>
                <groupId>com.group</group>
                <version>1</version>
            </project>
        """.trimIndent())

        val existing = a.root.content.map(Xml.Tag::class.java::cast)

        val insert = Xml.Tag(randomId(), "artifactId", emptyList(), emptyList(),
                null, "", Formatting.EMPTY)

        assertThat(
                (existing + insert)
                        .sortedWith(MavenTagInsertionComparator(existing))
                        .map { it.name }
        ).containsExactly("groupId", "artifactId", "version")
    }
}
