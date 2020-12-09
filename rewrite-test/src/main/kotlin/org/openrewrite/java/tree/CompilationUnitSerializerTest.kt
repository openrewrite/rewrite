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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.marker.Markers
import org.openrewrite.TreeSerializer
import org.openrewrite.git.Git
import org.openrewrite.java.JavaParser

/**
 * Test that flyweights survive a serialization/deserialization cycle
 */
interface CompilationUnitSerializerTest {

    companion object {
        private val serializer = TreeSerializer<J.CompilationUnit>()

        private const val aSource = """
            public class A {
                A a = foo();
                A a2 = foo();

                public A foo() { return null; }
            }
        """
    }

    @Test
    fun roundTripSerializationPreservesFlyweights(jp: JavaParser) {
        val a = jp.parse(aSource)[0].withMarkers(Markers(listOf(Git().apply {
            headCommitId = "123"
        })))

        val aBytes = serializer.write(a)
        val aDeser = serializer.read(aBytes)

        assertEquals(a, aDeser)
        assertTrue(a.classes[0].type === aDeser.classes[0].type)
        assertTrue((a.classes[0].fields + aDeser.classes[0].fields)
                .map { it.vars[0].initializer?.type }
                .toSet()
                .size == 1)
    }

    @Test
    fun roundTripSerializationOfList(jp: JavaParser) {
        val a = jp.parse(aSource)[0]
        val aBytes = serializer.write(listOf(a))
        assertEquals(a, serializer.readList(aBytes)[0])
    }
}
