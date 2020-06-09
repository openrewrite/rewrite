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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass

interface FindMethodTest {

    @Test
    fun findStaticMethodCalls(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.Collections;
            public class A {
               Object o = Collections.emptyList();
            }
        """)
        
        val m = a.classes[0].findMethodCalls("java.util.Collections emptyList()").first()
        
        assertEquals("emptyList", m.simpleName)
        assertEquals("Collections.emptyList()", m.printTrimmed())
    }

    @Test
    fun findStaticallyImportedMethodCalls(jp: JavaParser) {
        val a = jp.parse("""
            import static java.util.Collections.emptyList;
            public class A {
               Object o = emptyList();
            }
        """)

        val m = a.classes[0].findMethodCalls("java.util.Collections emptyList()").firstOrNull()
        assertEquals("java.util.Collections", m?.type?.declaringType.asClass()?.fullyQualifiedName)
    }

    @Test
    fun matchVarargs(jp: JavaParser) {
        val a = """
            public class A {
                public void foo(String s, Object... o) {}
            }
        """

        val b = """
            public class B {
               public void test() {
                   new A().foo("s", "a", 1);
               }
            }
        """

        assertTrue(jp.parse(b, a).classes[0].findMethodCalls("A foo(String, Object...)").isNotEmpty())
    }

    @Test
    fun matchOnInnerClass(jp: JavaParser) {
        val b = """
            public class B {
               public static class C {
                   public void foo() {}
               }
            }
        """

        val a = """
            public class A {
               void test() {
                   new B.C().foo();
               }
            }
        """

        assertEquals(1, jp.parse(a, b).classes[0].findMethodCalls("B.C foo()").size)
    }
}
