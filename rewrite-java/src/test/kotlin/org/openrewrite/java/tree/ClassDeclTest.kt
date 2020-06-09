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

interface ClassDeclTest {
    
    @Test
    fun multipleClassDeclarationsInOneCompilationUnit(jp: JavaParser) {
        val a = jp.parse("""
            public class A {}
            class B {}
        """)

        assertEquals(listOf("A", "B"), a.classes.map { it.simpleName }.sorted())
    }

    @Test
    fun modifiers(jp: JavaParser) {
        val a = jp.parse("public class A {}")

        assertTrue(a.classes[0].hasModifier("public"))
    }
    
    @Test
    fun fields(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
                List l;
            }
        """)

        assertEquals(1, a.classes[0].fields.size)
    }

    @Test
    fun methods(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void fun() {}
            }
        """)

        assertEquals(1, a.classes[0].methods.size)
    }
    
    @Test
    fun implements(jp: JavaParser) {
        val b = "public interface B {}"
        val a = "public class A implements B {}"
        
        assertEquals(1, jp.parse(a, b).classes[0].implements?.from?.size)
    }

    @Test
    fun extends(jp: JavaParser) {
        val b = "public class B {}"
        val a = "public class A extends B {}"

        val aClass = jp.parse(a, b).classes[0]
        assertNotNull(aClass.extends)
    }

    @Test
    fun format(jp: JavaParser) {
        val b = "public class B<T> {}"
        val a = "@Deprecated public class A < T > extends B < T > {}"
        assertEquals(a, jp.parse(a, b).classes.find { it.simpleName == "A" }?.printTrimmed())
    }

    @Test
    fun formatInterface(jp: JavaParser) {
        val b = "public interface B {}"
        val a = "public interface A extends B {}"
        assertEquals(a, jp.parse(a, b).classes.find { it.simpleName == "A" }?.printTrimmed())
    }

    @Test
    fun formatAnnotation(jp: JavaParser) {
        val a = "public @interface Produces { }"
        assertEquals(a, jp.parse(a).classes[0].printTrimmed())
    }

    @Test
    fun enumWithParameters(jp: JavaParser) {
        val aSrc = """
            public enum A {
                ONE(1),
                TWO(2);
            
                A(int n) {}
            }
        """.trimIndent()

        val a = jp.parse(aSrc)

        assertTrue(a.classes[0].kind is J.ClassDecl.Kind.Enum)
        assertEquals("ONE(1),\nTWO(2);", a.classes[0].enumValues?.printTrimmed())
        assertEquals(aSrc, a.printTrimmed())
    }

    @Test
    fun enumWithoutParameters(jp: JavaParser) {
        val a = jp.parse("public enum A { ONE, TWO }")
        assertEquals("public enum A { ONE, TWO }", a.classes[0].printTrimmed())
        assertEquals("ONE, TWO", a.classes[0].enumValues?.printTrimmed())
    }

    @Test
    fun enumUnnecessarilyTerminatedWithSemicolon(jp: JavaParser) {
        val a = jp.parse("public enum A { ONE ; }")
        assertEquals("{ ONE ; }", a.classes[0].body.printTrimmed())
    }

    @Test
    fun enumWithEmptyParameters(jp: JavaParser) {
        val a = jp.parse("public enum A { ONE ( ), TWO ( ) }")
        assertEquals("public enum A { ONE ( ), TWO ( ) }", a.classes[0].printTrimmed())
        assertEquals("ONE ( ), TWO ( )", a.classes[0].enumValues?.printTrimmed())
    }

    /**
     * OpenJDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering(jp: JavaParser) {
        val a = jp.parse("public /* abstract */ final abstract class A {}")
        assertEquals("public /* abstract */ final abstract class A {}", a.printTrimmed())
    }

    @Test
    fun innerClass(jp: JavaParser) {
        val aSrc = """
            public class A {
                public enum B {
                    ONE,
                    TWO
                }
            
                private B b;
            }
        """.trimIndent()

        assertEquals(aSrc, jp.parse(aSrc).printTrimmed())
    }

    @Test
    fun strictfpClass(jp: JavaParser) {
        val a = jp.parse("public strictfp class A {}")
        assertEquals(a.classes[0].printTrimmed(), "public strictfp class A {}")
    }
}