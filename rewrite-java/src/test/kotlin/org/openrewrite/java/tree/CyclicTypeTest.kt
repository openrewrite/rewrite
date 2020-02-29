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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asArray
import org.openrewrite.java.asClass

open class CyclicTypeTest : JavaParser() {

    @Test
    fun genericNesting() {
        parse("""
            import java.util.*;

            public class A {
                B b;
            }

            class B extends G<C> { }

            class C {
                A a;
            }

            class G<T> {}
        """)
    }

    @Test
    fun nestedTypes() {
        parse("""
            public class A {
                B b;
                public static class B {
                    A a;
                }
            }
        """)
    }

    @Test
    fun interdependentTypes() {
        parse("""
            public class A {
                B b;
            }

            class B {
                A a;
            }
        """)
    }

    @Test
    fun cyclicType() {
        parse("""
            public class A<T> {
                A<?> a;
            }
        """)
    }

    @Test
    fun cyclicTypeInArray() {
        val a = parse("""
            public class A {
                A[] nested = new A[0];
            }
        """)
        
        val fieldType = a.classes[0].fields[0].vars[0].type.asArray()
        assertTrue(fieldType is JavaType.Array)

        val elemType = fieldType!!.elemType.asClass()
        assertTrue(elemType is JavaType.Class)

        assertTrue(elemType!!.members[0].type?.asArray()?.elemType is JavaType.Cyclic)
    }
}