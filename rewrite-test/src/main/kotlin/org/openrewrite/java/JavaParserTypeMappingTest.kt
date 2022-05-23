/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.JavaType

interface JavaParserTypeMappingTest : JavaTypeMappingTest {

    companion object {
        val parser: JavaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
    }

    @BeforeEach
    fun beforeRecipe() {
        parser.reset()
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    fun parameterizedTypesAreDeeplyBasedOnBounds() {
        // The sources intentionally do not import to prevent the import from being processed first.

        //language=java
        val sources = arrayOf(
            """
                abstract class TypeA<T extends Number> extends java.util.ArrayList<T> {}
            """.trimIndent(),
            """
                class TypeB extends TypeA<Integer> {
                    // Attempt to force a race condition in the JavaTypeCache.
                    java.util.List<String> list = new java.util.ArrayList<>();
                }
            """.trimIndent(),
            """
                class TypeC<T extends String> extends java.util.ArrayList<T> {
                    // Attempt to force a race condition in the JavaTypeCache.
                    java.util.List<Object> list = new java.util.ArrayList<>();
                }
            """.trimIndent()
        )

        val cus = parser.parse(InMemoryExecutionContext { t -> fail(t) }, *sources)

        val typeA = cus[0].classes[0].type as JavaType.Parameterized
        assertThat((typeA.typeParameters[0] as JavaType.GenericTypeVariable).toString()).isEqualTo("Generic{T extends java.lang.Number}")
        val typeASuperType = typeA.supertype as JavaType.Parameterized
        assertThat(typeASuperType.toString()).isEqualTo("java.util.ArrayList<Generic{T extends java.lang.Number}>")
        assertThat(((typeASuperType).type as JavaType.Class).typeParameters[0].toString()).isEqualTo("Generic{E}")

        val typeB = cus[1].classes[0].type as JavaType.Class
        assertThat(typeB.supertype!!.toString()).isEqualTo("TypeA<java.lang.Integer>")
        val typeBSuperType = typeB.supertype as JavaType.Parameterized
        assertThat(((typeBSuperType).type as JavaType.Class).typeParameters[0].toString()).isEqualTo("Generic{T extends java.lang.Number}")

        val typeC = cus[2].classes[0].type as JavaType.Parameterized
        assertThat((typeC.typeParameters[0] as JavaType.GenericTypeVariable).toString()).isEqualTo("Generic{T extends java.lang.String}")
        val typeCSuperType = typeC.supertype as JavaType.Parameterized
        assertThat(typeCSuperType.toString()).isEqualTo("java.util.ArrayList<Generic{T extends java.lang.String}>")
        assertThat(((typeCSuperType).type as JavaType.Class).typeParameters[0].toString()).isEqualTo("Generic{E}")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1762")
    @Test
    fun methodInvocationWithUnknownTypeSymbol() {
        val source = """
            import java.util.ArrayList;
            import java.util.List;
            import java.util.stream.Collectors;
            
            class Test {
                class Parent {
                }
                class Child extends Parent {
                }
            
                List<Parent> method(List<Parent> values) {
                    return values.stream()
                            .map(o -> {
                                if (o instanceof Child) {
                                    return new UnknownType(((Child) o).toString());
                                }
                                return o;
                            })
                            .collect(Collectors.toList());
                }
            }
            """.trimIndent()
        val cu = parser.parse(InMemoryExecutionContext { t -> fail(t) }, source)
        assertThat(cu).isNotNull
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
        val cu = parser.parse(InMemoryExecutionContext { t -> fail(t) }, source)
        assertThat(cu).isNotNull
    }
}
