package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.fields

open class PrimitiveTest : JavaParser() {

    @Test
    fun primitiveField() {
        val a = parse("""
            public class A {
                int n = 0;
                char c = 'a';
            }
        """)

        assertThat(a.fields(0..1).map { it.typeExpr?.type })
                .containsExactlyInAnyOrder(JavaType.Primitive.Int, JavaType.Primitive.Char)
    }
}