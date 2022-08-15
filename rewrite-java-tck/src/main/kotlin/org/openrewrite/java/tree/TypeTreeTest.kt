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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asFullyQualified


interface TypeTreeTest {

    @Test
    fun buildFullyQualifiedClassName(jp: JavaParser) {
        val name = TypeTree.build("java.util.List") as J.FieldAccess

        assertEquals("java.util.List", name.toString())
        assertEquals("List", name.simpleName)
    }

    @Test
    fun buildFullyQualifiedClassNameWithSpacing(jp: JavaParser) {
        val name = TypeTree.build("java . util . List") as J.FieldAccess

        assertEquals("java . util . List", name.toString())
    }

    @Test
    fun buildFullyQualifiedInnerClassName(jp: JavaParser) {
        val name = TypeTree.build("a.Outer.Inner") as J.FieldAccess

        assertEquals("a.Outer.Inner", name.toString())
        assertEquals("Inner", name.simpleName)
        assertEquals("a.Outer.Inner", name.type.asFullyQualified()?.fullyQualifiedName)

        val outer = name.target as J.FieldAccess
        assertEquals("Outer", outer.simpleName)
        assertEquals("a.Outer", outer.type.asFullyQualified()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport(jp: JavaParser) {
        val name = TypeTree.build("a.A.*") as J.FieldAccess

        assertEquals("a.A.*", name.toString())
        assertEquals("*", name.simpleName)
    }
}
