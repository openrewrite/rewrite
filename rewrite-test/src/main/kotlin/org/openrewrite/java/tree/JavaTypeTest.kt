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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass

interface JavaTypeTest {

    @Test
    fun memberVisibility(jp: JavaParser) {
        val b = """
            public class B {
                protected int n;
            }
        """.trimIndent()

        val a = jp.parse("""
            public class A extends B {
            }
        """.trimIndent(), b)[0]

        assertTrue(a.classes[0].type.asClass()!!.supertype!!.members[0].flags.contains(Flag.Protected))
    }

    @Disabled("flaky on collection.isAssignableFrom(list)")
    @Test
    fun isAssignableFrom(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            import java.util.Collection;
            public class A {
                public List[] listArr;
                public Collection[] collArr;
            }
        """.trimIndent())[0]

        val (list, collection) = a.imports.map { TypeUtils.asClass(it.elem.qualid.type)!! }
        val (listArr, collectionArr) = a.classes[0].fields.map { TypeUtils.asClass(it.typeExpr!!.type)!! }

        assertTrue(collection.isAssignableFrom(list))
        assertFalse(list.isAssignableFrom(collection))
        assertTrue(JavaType.Class.OBJECT.isAssignableFrom(collection))

        assertTrue(collectionArr.isAssignableFrom(listArr))
        assertFalse(listArr.isAssignableFrom(collectionArr))
    }

    @Test
    fun innerClassType(jp: JavaParser) {
        val t = JavaType.Class.build("com.foo.Foo.Bar")
        assertEquals("com.foo.Foo.Bar", t.fullyQualifiedName)
        assertEquals("com.foo", t.packageName)
    }

    @Test
    fun packageName(jp: JavaParser) {
        val t = JavaType.Class.build("com.foo.Foo")
        assertEquals("com.foo", t.packageName)

        val a = JavaType.Class.build("a.A1")
        assertEquals("a", a.packageName)

        val b = JavaType.Class.build("a.A.B")
        assertEquals("a", b.packageName)

        val c = JavaType.Class.build("A.C")
        assertEquals("", c.packageName)
    }

    @Test
    fun selfReferentialTypeIsShared(jp: JavaParser) {
        val a = jp.parse("public class A { A a; }")[0]
        val outerType = a.classes[0].type
        val fieldType = a.classes[0].fields[0].typeExpr?.type
        assertTrue(outerType === fieldType)
    }

    @Test
    fun typeFlyweightsAreSharedBetweenParsers(jp: JavaParser) {
        val a = jp.parse("public class A {}")[0]
        val a2 = jp.reset().parse("public class A {}")[0]

        val t1 = a.classes[0].type
        val t2 = a2.classes[0].type

        assertThat(t1).isSameAs(t2)
    }

    @Test
    fun sameFullyQualifiedNameWithDifferentMembers(jp: JavaParser) {
        val a = jp.parse("public class A { String foo; }")[0]
        val a2 = jp.reset().parse("public class A { String bar; }")[0]

        assertTrue(a.classes[0].type !== a2.classes[0].type)
    }

    @Test
    fun sameFullyQualifiedNameWithDifferentTypeHierarchy(jp: JavaParser) {
        val a = jp.parse("""
            public class A extends B {}
            class B {}
        """)[0]

        val a2 = jp.reset().parse("""
            public class A {}
            class B {}
        """)[0]

        assertTrue(a.classes[0].type !== a2.classes[0].type)
    }
}
