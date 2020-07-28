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
package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface FindTypeTest {
    companion object {
        private const val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
            }
        """
    }

    @Test
    fun simpleName(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            
            public class B extends A1 {}
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun fullyQualifiedName(jp: JavaParser) {
        val b = jp.parse("public class B extends a.A1 {}", a1)
        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun annotation(jp: JavaParser) {
        val a1 = "public @interface A1 {}"
        val b = jp.parse("@A1 public class B {}", a1)
        assertEquals(1, b.findType("A1").size)
    }

    @Test
    fun array(jp: JavaParser) { // array types and new arrays
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1[] a = new A1[0];
            }
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun classDecl(jp: JavaParser) {
        val i1 = "public interface I1 {}"

        val b = jp.parse("""
            import a.A1;
            public class B extends A1 implements I1 {}
        """, a1, i1)

        assertEquals(1, b.findType("a.A1").size)
        assertEquals(1, b.findType("I1").size)
    }

    @Test
    fun method(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               public A1 foo() throws A1 { return null; }
            }
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            import java.util.List;
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
               public void test() {
                   A1.stat();
                   this.<A1>generic(null, null);
               }
            }
        """, a1)

        assertEquals(4, b.findType("a.A1").size)
    }

    @Test
    fun multiCatch(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               public void test() {
                   try {}
                   catch(A1 | RuntimeException e) {}
               }
            }
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun multiVariable(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 f1, f2;
            }
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun newClass(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 a = new A1();
            }
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun paramaterizedType(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               Map<A1, A1> m;
            }
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun typeCast(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 a = (A1) null;
            }
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }
}
