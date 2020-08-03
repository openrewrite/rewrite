/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        """.trimIndent())[0]

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
