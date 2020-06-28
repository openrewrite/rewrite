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
package org.openrewrite.xml.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.tree.Xml

class FindTagsTest : XmlParser() {
    private val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency/>
                    <dependency/>
                </dependency>
            </project>
        """.trimIndent())

    private val deep = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencyManagement>
                    <dependencies>
                        <dependency/>
                        <dependency/>
                    </dependency>
                </dependencyManagement>
            </project>
        """.trimIndent())

    @Test
    fun findAbsolute() {
        assertThat(FindTags("/project/dependencies/dependency").visit(x).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }

    @Test
    fun findRelative() {
        assertThat(FindTags("dependencies/dependency").visit(x.root).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }

    @Test
    fun findRelativeWildcard() {
        assertThat(FindTags("dependencies/*").visit(x.root).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }

    @Test
    fun dontFindRelativeMatchDeeperThanRoot() {
        assertThat(FindTags("dependencies/*").visit(deep.root)).isEmpty()
    }

    @Test
    fun findRelativeMatchBelowDocumentRoot() {
        val dm = deep.root.content[0]
        assertThat((dm as Xml.Tag).name).isEqualTo("dependencyManagement")
        assertThat(FindTags("dependencies/dependency").visit(dm).map { it.name })
                .hasSize(2)
                .containsExactly("dependency", "dependency")
    }
}
