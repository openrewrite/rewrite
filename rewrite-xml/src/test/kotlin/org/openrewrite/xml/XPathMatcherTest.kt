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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.xml.tree.Xml

class XPathMatcherTest : XmlParser() {
    val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependency>
        """.trimIndent())

    @Test
    fun matchAbsolute() {
        assertTrue(visitor("/dependencies/dependency").visit(x))
        assertTrue(visitor("/dependencies/*").visit(x))
        assertFalse(visitor("/dependency/dne").visit(x))
    }

    @Test
    fun matchAbsoluteAttribute() {
        assertTrue(visitor("/dependencies/dependency/artifactId/@scope").visit(x))
        assertTrue(visitor("/dependencies/dependency/artifactId/@*").visit(x))
    }

    @Test
    fun matchRelative() {
        assertTrue(visitor("dependency").visit(x))
        assertTrue(visitor("//dependency").visit(x))
        assertTrue(visitor("dependency/*").visit(x))
        assertFalse(visitor("dne").visit(x))
    }

    @Test
    fun matchRelativeAttribute() {
        assertTrue(visitor("artifactId/@scope").visit(x))
        assertTrue(visitor("artifactId/@*").visit(x))
        assertTrue(visitor("//artifactId/@scope").visit(x))
    }

    private fun visitor(xPath: String): XmlSourceVisitor<Boolean> {
        val matcher = XPathMatcher(xPath)

        return object : XmlSourceVisitor<Boolean>() {
            override fun defaultTo(t: Tree?) = false
            override fun reduce(r1: Boolean, r2: Boolean) = r1 || r2
            override fun isCursored(): Boolean = true

            override fun visitTag(tag: Xml.Tag?) =
                    super.visitTag(tag) || matcher.matches(cursor)

            override fun visitAttribute(attribute: Xml.Attribute?) =
                    super.visitAttribute(attribute) || matcher.matches(cursor)
        }
    }
}