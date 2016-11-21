package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class NewArrayTest(p: Parser): Parser by p {
    
    @Test
    fun newArray() {
        val a = parse("""
            public class A {
                int[] n = new int[0];
            }
        """)
        
        val newArr = a.fields()[0].vars[0].initializer as Tr.NewArray
        assertNull(newArr.initializer)
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0].size is Tr.Literal)
    }

    @Test
    fun newArrayWithInitializers() {
        val a = parse("""
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """)

        val newArr = a.fields()[0].vars[0].initializer as Tr.NewArray
        assertTrue(newArr.dimensions[0].size is Tr.Empty)
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(3, newArr.initializer?.elements?.size)
    }

    @Test
    fun formatWithDimensions() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ 1 ];
            }
        """)

        val newArr = a.fields()[0].vars[0].initializer as Tr.NewArray
        assertEquals("new int [ 0 ] [ 1 ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithEmptyDimension() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ ];
            }
        """)

        val newArr = a.fields()[0].vars[0].initializer as Tr.NewArray
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

        val newArr = a.classes[0].fields()[1].vars[0].initializer as Tr.NewArray
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

        val a = parse("""@Produces({"something"}) class A {}""", whichDependOn = produces)
        val arr = a.classes[0].annotations[0].args!!.args[0] as Tr.NewArray

        assertNull(arr.typeExpr)
        assertEquals("""{"something"}""", arr.printTrimmed())
    }
}