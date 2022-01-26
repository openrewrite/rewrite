/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

class Java11TypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val goat = Java11TypeMappingTest::class.java.getResourceAsStream("/JavaTypeGoat.java")!!
            .bufferedReader().readText()
    }

    override fun goatType(): JavaType.Parameterized = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .build()
        .parse(InMemoryExecutionContext { t -> fail(t) }, goat)[0]
        .classes[0]
        .type
        .asParameterized()!!

    @Test
    fun noStackOverflowOnCapturedType() {
        val source = """
            import java.util.AbstractList;
            import java.util.List;
            
            class Test {
                void method(PT<?, ?> capturedType) {
                    //noinspection StatementWithEmptyBody, ConstantConditions
                    if (capturedType instanceof PT) { // type is Type.CapturedType symbol
                    }
                }
                class PT<S, T extends PT<S, T>> {
                }
            }
        """.trimIndent()
        val pt: JavaType.Parameterized = (((((JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(false)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, source)[0]
            .classes[0]).body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.If)
            .ifCondition.tree as J.InstanceOf).expression as J.Identifier)
            .type as JavaType.Parameterized
        Assertions.assertThat(pt).isNotNull
    }

    @Test
    fun noStackOverflowOnRecursiveIntersectionType() {
        val source = """
            class Test {
                abstract static class Extension<E extends Extension<E>> {
                }
                interface Intersection<E extends Extension<E> & Intersection<E>> {
                    E getIntersectionType();
                }
            }
        """.trimIndent()
        val cu = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(false)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, source)
        Assertions.assertThat(cu).isNotNull
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1318")
    @Test
    fun methodInvocationOnUnknownType() {
        val source = """
            import java.util.ArrayList;
            // do not import List to create an UnknownType
            
            class Test {
                class Base {
                    private int foo;
                    public boolean setFoo(int foo) {
                        this.foo = foo;
                    }
                    public int getFoo() {
                        return foo;
                    }
                }
                List<Base> createUnknownType(List<Integer> values) {
                    List<Base> bases = new ArrayList<>();
                    values.forEach((v) -> {
                        Base b = new Base();
                        b.setFoo(v);
                        bases.add(b);
                    });
                    return bases;
                }
            }
        """.trimIndent()
        val cu = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, source)
        Assertions.assertThat(cu).isNotNull
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1327")
    @Test
    fun consistentSuperType() {
        val sources: Array<String> = arrayOf("""
            public abstract class TypeA<String> extends java.util.AbstractList<String> {}
            """.trimIndent(), """
            public abstract class TypeB<Integer> extends java.util.AbstractList<Integer> {}
            """.trimIndent()
        )
        val cus = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, *sources)

        Assertions.assertThat(cus).isNotNull
        val typeA = cus[0].classes[0].type!! as JavaType.Parameterized
        val typeASuperType = typeA.supertype
        val typeASuperSuperType = typeASuperType.supertype
        val typeB = cus[1].classes[0].type!! as JavaType.Parameterized
        val typeBSuperType = typeB.supertype
        val typeBSuperSuperType = typeBSuperType.supertype

        Assertions.assertThat(typeASuperSuperType).isNotNull
        Assertions.assertThat(typeASuperSuperType!!.toString()).isEqualTo("java.util.AbstractCollection<Generic{E}>")
        Assertions.assertThat(typeBSuperSuperType).isEqualTo(typeBSuperSuperType)
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/1349")
    @Test
    fun consistentEnumSuperType() {
        val sources: Array<String> = arrayOf("""
            public enum TypeA {}
            """.trimIndent(), """
            public enum TypeB {}
            """.trimIndent()
        )
        val cus = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, *sources)

        Assertions.assertThat(cus).isNotNull
        val typeA = cus[0].classes[0].type!! as JavaType.Class
        val typeASuperType = typeA.supertype
        val typeB = cus[1].classes[0].type!! as JavaType.Class
        val typeBSuperType = typeB.supertype

        Assertions.assertThat(typeASuperType).isNotNull
        Assertions.assertThat(typeASuperType).isEqualTo(typeBSuperType)
    }
}
