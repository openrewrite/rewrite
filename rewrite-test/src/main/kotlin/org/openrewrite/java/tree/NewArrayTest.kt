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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asArray

interface NewArrayTest {
    
    @Test
    fun newArray(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int[] n = new int[0];
            }
        """)[0]

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertNull(newArr.initializer)
        assertTrue(newArr.type is JavaType.Array)
        assertTrue(newArr.type.asArray()?.elemType is JavaType.Primitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0].size is J.Literal)
    }

    @Test
    fun newArrayWithInitializers(jp: JavaParser) {
        val aSrc = """
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """.trimIndent()

        val a = jp.parse(aSrc)[0]

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertTrue(newArr.dimensions[0].size is J.Empty)
        assertTrue(newArr.type is JavaType.Array)
        assertTrue(newArr.type.asArray()?.elemType is JavaType.Primitive)
        assertEquals(3, newArr.initializer?.elements?.size)
        assertEquals(a.print(), aSrc)
    }

    @Test
    fun formatWithDimensions(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int[][] n = new int [ 0 ] [ 1 ];
            }
        """)[0]

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ 1 ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithEmptyDimension(jp: JavaParser) {
        val aSrc = """
            public class A {
                int[][] n = new int [ 0 ] [ ];
            }
        """.trimIndent()

        val a = jp.parse(aSrc)[0]

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ ]", newArr.printTrimmed())
        assertEquals(a.print(), aSrc)
    }

    @Test
    fun formatWithInitializers(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int[] m = new int[] { 0 };
                int[][] n = new int [ ] [ ] { m, m, m };
            }
        """)[0]

        val newArr = a.classes[0].fields[1].vars[0].initializer as J.NewArray
        assertEquals("new int [ ] [ ] { m, m, m }", newArr.printTrimmed())
    }

    @Test
    fun newArrayShortcut(jp: JavaParser) {
        val produces = """
            import java.lang.annotation.*;
            @Target({ElementType.TYPE})
            public @interface Produces {
                String[] value() default "*/*";
            }
        """

        val a = jp.parse("""@Produces({"something"}) class A {}""", produces)[0]
        val arr = a.classes[0].annotations[0].args!!.args[0] as J.NewArray

        assertNull(arr.typeExpr)
        assertEquals("""{"something"}""", arr.printTrimmed())
    }
}
