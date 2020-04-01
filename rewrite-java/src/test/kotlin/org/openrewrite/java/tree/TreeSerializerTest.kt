package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

/**
 * Test that flyweights survive a serialization/deserialization cycle
 */
open class TreeSerializerTest : JavaParser() {

    private val serializer = TreeSerializer()

    private val aSource = """
            public class A {
                A a = foo();
                A a2 = foo();

                public A foo() { return null; }
            }
        """

    @Test
    fun `round trip serialization of AST preserves flyweights`() {
        val a = parse(aSource)
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
    fun `round trip serialization of AST list`() {
        val a = parse(aSource)
        val aBytes = serializer.write(listOf(a))
        assertEquals(a, serializer.readList(aBytes)[0])
    }
}
