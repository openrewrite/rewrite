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

class XPathMatcherTest {

    private val x = XmlParser().parse(
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <dependencies>
                <dependency>
                    <groupId>org.openrewrite</groupId>
                    <artifactId scope="compile">rewrite-xml</artifactId>
                </dependency>
                <dependency>
                    <artifactId scope="test">assertj-core</artifactId>
                </dependency>
            </dependencies>
        """.trimIndent()
    ).iterator().next()

    private fun visit(xpath: String): Boolean {
        val matchElements = mutableListOf<Xml>()
        visitor(xpath).visit(x, matchElements)
        return matchElements.isNotEmpty()
    }

    @Test
    // if the path starts with a single slash, it always represents an absolute path to an element
    fun matchAbsolute() {
        assertThat(visit("/dependencies/dependency")).isTrue
        assertThat(visit("/dependencies/*/artifactId")).isTrue
        assertThat(visit("/dependencies/*")).isTrue
        assertThat(visit("/dependencies/dne")).isFalse
    }

    @Test
    fun matchAbsoluteAttribute() {
        assertThat(visit("/dependencies/dependency/artifactId/@scope")).isTrue
        assertThat(visit("/dependencies/dependency/artifactId/@scope")).isTrue
        assertThat(visit("/dependencies/dependency/artifactId/@*")).isTrue
        assertThat(visit("/dependencies/dependency/groupId/@*")).isFalse
    }

    @Test
    fun matchRelative() {
        assertThat(visit("dependencies")).isTrue
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

    /**
     * handful of tests which are valid xpath syntax, but not currently implemented in our xpath matcher syntax
     * xpathDivergences
     * todo
     */
    @Test
    fun matchAnywhereInBetweenDoubleSlash() {
        // selects all artifactId elements that are descendant of the dependencies element, no matter where they are under the dependencies element
        assertThat(visit("/dependencies//artifactId")).isTrue
    }

    @Test
    fun matchChildrenWithPredicateAttribute() {
        // selects the artifactId element value of any artifactId with a "scope" attribute of "compile"
        assertThat(visit("""/dependencies/dependency/artifactId[@scope="compile"]""")).isTrue
    }

    @Test
    fun matchChildrenWithPredicate() {
        // selects the groupId value of any "dependency" element which has an "artifactId" child element equal to "rewrite-xml"
        assertThat(visit("""/dependencies/dependency[artifactId="rewrite-xml"]/groupId""")).isTrue
    }

    private fun visitor(xPath: String): XmlVisitor<MutableList<Xml>> {
        val matcher = XPathMatcher(xPath)

        return object : XmlVisitor<MutableList<Xml>>() {

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
