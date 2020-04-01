package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asArray

open class NewArrayTest : JavaParser() {
    
    @Test
    fun newArray() {
        val a = parse("""
            public class A {
                int[] n = new int[0];
            }
        """)
        
        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertNull(newArr.initializer)
        assertTrue(newArr.type is JavaType.Array)
        assertTrue(newArr.type.asArray()?.elemType is JavaType.Primitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0].size is J.Literal)
    }

    @Test
    fun newArrayWithInitializers() {
        val a = parse("""
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertTrue(newArr.dimensions[0].size is J.Empty)
        assertTrue(newArr.type is JavaType.Array)
        assertTrue(newArr.type.asArray()?.elemType is JavaType.Primitive)
        assertEquals(3, newArr.initializer?.elements?.size)
    }

    @Test
    fun formatWithDimensions() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ 1 ];
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ 1 ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithEmptyDimension() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ ];
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithInitializers() {
        val a = parse("""
            public class A {
                int[] m = new int[] { 0 };
                int[][] n = new int [ ] [ ] { m, m, m };
            }
        """)

        val newArr = a.classes[0].fields[1].vars[0].initializer as J.NewArray
        assertEquals("new int [ ] [ ] { m, m, m }", newArr.printTrimmed())
    }

    @Test
    fun newArrayShortcut() {
        val produces = """
            import java.lang.annotation.*;
            @Target({ElementType.TYPE})
            public @interface Produces {
                String[] value() default "*/*";
            }
        """

        val a = parse("""@Produces({"something"}) class A {}""", produces)
        val arr = a.classes[0].annotations[0].args!!.args[0] as J.NewArray

        assertNull(arr.typeExpr)
        assertEquals("""{"something"}""", arr.printTrimmed())
    }
}