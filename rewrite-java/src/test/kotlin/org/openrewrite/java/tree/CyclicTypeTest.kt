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