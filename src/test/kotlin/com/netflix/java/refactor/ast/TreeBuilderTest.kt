/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.java.refactor.ast

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeBuilderTest {
    val cache = TypeCache.new()

    @Test
    fun buildFullyQualifiedClassName() {
        val name = TreeBuilder.buildName(cache, "java.util.List", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("java.util.List", name.printTrimmed())
        assertEquals("List", name.name.name)
    }

    @Test
    fun buildFullyQualifiedInnerClassName() {
        val name = TreeBuilder.buildName(cache, "a.Outer.Inner", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("a.Outer.Inner", name.printTrimmed())
        assertEquals("Inner", name.name.name)
        assertEquals("a.Outer.Inner", name.type.asClass()?.fullyQualifiedName)

        val outer = name.target as Tr.FieldAccess
        assertEquals("Outer", outer.name.name)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport() {
        val name = TreeBuilder.buildName(cache, "a.A.*", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("a.A.*", name.printTrimmed())
        assertEquals("*", name.name.name)
    }
}