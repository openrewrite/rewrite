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
package org.openrewrite.xml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.xml.tree.Xml

class XPathMatcherTest : XmlParser() {

    private val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependency>
        """.trimIndent()).iterator().next()


    private fun visit(xpath: String) : Boolean {
        val matchElements = mutableListOf<Xml>()
        visitor(xpath).visit(x, matchElements)
        return matchElements.isNotEmpty()
    }

    @Test
    fun matchAbsolute() {
        assertThat(visit("/dependencies/dependency")).isTrue
        assertThat(visit("/dependencies/*")).isTrue
        assertThat(visit("/dependencies/dne")).isFalse
    }

    @Test
    fun matchAbsoluteAttribute() {
        assertThat(visit("/dependencies/dependency/artifactId/@scope")).isTrue
        assertThat(visit("/dependencies/dependency/artifactId/@scope")).isTrue
        assertThat(visit("/dependencies/dependency/artifactId/@*")).isTrue
    }

    @Test
    fun matchRelative() {
        assertThat(visit("dependency")).isTrue
        assertThat(visit("//dependency")).isTrue
        assertThat(visit("dependency/*")).isTrue
        assertThat(visit("dne")).isFalse
    }

    @Test
    fun matchRelativeAttribute() {
        assertThat(visit("dependency/artifactId/@scope")).isTrue
        assertThat(visit("dependency/artifactId/@*")).isTrue
        assertThat(visit("//dependency/artifactId/@scope")).isTrue
    }

    private fun visitor(xPath: String): XmlProcessor<MutableList<Xml>> {
        val matcher = XPathMatcher(xPath)

        return object : XmlProcessor<MutableList<Xml>>() {
            init {
                setCursoringOn()
            }

            override fun visitTag(tag: Xml.Tag, p: MutableList<Xml>): Xml {
                if (matcher.matches(cursor)) {
                    p.add(tag)
                }
                return super.visitTag(tag, p)
            }

            override fun visitAttribute(attribute: Xml.Attribute, p: MutableList<Xml>): Xml {
                if (matcher.matches(cursor)) {
                    p.add(attribute)
                }
                return super.visitAttribute(attribute, p)
            }
        }
    }
}
