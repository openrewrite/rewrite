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
import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest
import org.openrewrite.TreePrinter
import org.openrewrite.marker.SearchResult
import org.openrewrite.xml.XmlParser

class FindTagTest : RecipeTest {
    override val parser = XmlParser()

    override val treePrinter: TreePrinter<*>?
        get() = SearchResult.PRINTER

    @Test
    fun simpleElement() = assertChanged(
        parser,
        FindTag().apply { setXPath("/dependencies/dependency") },
        before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    <dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """,
        after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    ~~><dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """
    )

    @Test
    fun wildcard() = assertChanged(
        parser,
        FindTag().apply { setXPath("/dependencies/*") },
        before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    <dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """,
        after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    ~~><dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """
    )

    @Test
    fun noMatch() = assertUnchanged(
        parser,
        FindTag().apply { setXPath("/dependencies/dne") },
        before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    <dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """
    )

    @Test
    fun staticFind() {
        val before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependencies>
                    <dependency>
                        <artifactId scope="compile">org.openrewrite</artifactId>
                    </dependency>
                </dependency>
            """
        val source = parser.parse(*(arrayOf(before.trimIndent()))).iterator().next()
        val matchingTags = FindTag.find(source, "/dependencies/dependency")
        assertThat(matchingTags).isNotNull.isNotEmpty
    }

}
