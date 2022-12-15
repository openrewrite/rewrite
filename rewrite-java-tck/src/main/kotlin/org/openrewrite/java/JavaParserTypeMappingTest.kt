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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils

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

    @Issue("https://github.com/openrewrite/rewrite/issues/2445")
    @Test
    fun annotationParameterDefaults() {
        val source = """
            @AnAnnotation
            class Test {
            }
            @interface AnAnnotation {
                int scalar() default 1;
                String[] array() default {"a", "b"};
            }
        """
        val cu = parser.parse(source)[0]
        val t = TypeUtils.asClass(cu.classes[0].allAnnotations[0].type)!!
        assertThat(t.methods.find { it.name == "scalar"}!!.defaultValue!![0]).isEqualTo("1")
        val array = t.methods.find { it.name == "array"}!!
        assertThat(array.defaultValue!!.find { it == "a" }).isNotNull
        assertThat(array.defaultValue!!.find { it == "b" }).isNotNull
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
                    // Attempt to force the JavaTypeCache to cache the wrong parameterized super type.
                    java.util.List<String> list = new java.util.ArrayList<>();
                }
            """.trimIndent(),
            """
                class TypeC<T extends String> extends java.util.ArrayList<T> {
                    // Attempt to force the JavaTypeCache to cache the wrong parameterized super type.
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2118")
    @Test
    fun variousMethodScopeIdentifierTypes() {
        val source = """
            import java.util.List;
            import java.util.stream.Collectors;
            
            @SuppressWarnings("ALL")
            class MakeEasyToFind {
                void method(List<MultiMap> multiMaps) {
                    List<Integer> ints;
                    ints.forEach(it -> {
                        if (it != null) {
                        }
                    });
            
                    multiMaps.forEach(it -> {
                        if (it != null) {
                        }
                    });
            
                    while (true) {
                        if (multiMaps.isEmpty()) {
                            Long it;
                            break;
                        }
                    }
                }
            
                static class MultiMap {
                    List<Inner> inners;
                    public List<Inner> getInners() {
                        return inners;
                    }
            
                    static class Inner {
                        List<Number> numbers;
                        public List<Number> getNumbers() {
                            return numbers;
                        }
                    }
                }
            }
        """.trimIndent()

        val methodBody = (((parser.parse(InMemoryExecutionContext { t -> fail(t) }, source)[0] as J.CompilationUnit)
            .classes[0] as J.ClassDeclaration)
            .body.statements[0] as J.MethodDeclaration)
            .body!!

        val intsItType = ((((((((methodBody
            .statements[1] as J.MethodInvocation)
            .arguments[0] as J.Lambda)
            .body as J.Block)
            .statements[0] as J.If)
            .ifCondition as J.ControlParentheses)
            .tree as J.Binary)
            .left as J.Identifier)
            .fieldType as JavaType.Variable)
            .type
        assertThat(intsItType.toString()).isEqualTo("java.lang.Integer")

        val multiMapItType = ((((((((methodBody
            .statements[2] as J.MethodInvocation)
            .arguments[0] as J.Lambda)
            .body as J.Block)
            .statements[0] as J.If)
            .ifCondition as J.ControlParentheses)
            .tree as J.Binary)
            .left as J.Identifier)
            .fieldType as JavaType.Variable)
            .type
        assertThat(multiMapItType.toString()).isEqualTo("MakeEasyToFind${'$'}MultiMap")

        val whileLoopItType = (((((((methodBody
            .statements[3] as J.WhileLoop)
            .body as J.Block)
            .statements[0] as J.If)
            .thenPart as J.Block)
            .statements[0] as J.VariableDeclarations)
            .variables[0] as J.VariableDeclarations.NamedVariable)
            .name as J.Identifier)
            .type!!
        assertThat(whileLoopItType.toString()).isEqualTo("java.lang.Long")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2118")
    @Test
    fun multiMapWithSameLambdaParamNames() {
        val source = """
            import java.util.List;
            import java.util.stream.Collectors;
            
            @SuppressWarnings("ALL")
            class MakeEasyToFind {
                void method(List<MultiMap> multiMaps) {
                    Object obj = multiMaps.stream()
                        .map(it -> it.getInners())
                        .map(it -> it.stream().map(i -> i.getNumbers()))
                        .collect(Collectors.toList());
                }
            
                static class MultiMap {
                    List<Inner> inners;
                    public List<Inner> getInners() {
                        return inners;
                    }
            
                    static class Inner {
                        List<Number> numbers;
                        public List<Number> getNumbers() {
                            return numbers;
                        }
                    }
                }
            }
        """.trimIndent()

        val methodBody = (((parser.parse(InMemoryExecutionContext { t -> fail(t) }, source)[0] as J.CompilationUnit)
            .classes[0] as J.ClassDeclaration)
            .body.statements[0] as J.MethodDeclaration)
            .body!!

        val multiLambda = ((((methodBody
            .statements[0] as J.VariableDeclarations)
            .variables[0] as J.VariableDeclarations.NamedVariable)
            .initializer as J.MethodInvocation)
            .select as J.MethodInvocation)

        val firstMultiMapLambdaParamItType = (((((multiLambda
            .select as J.MethodInvocation)
            .arguments[0] as J.Lambda)
            .parameters.parameters[0] as J.VariableDeclarations)
            .variables[0] as J.VariableDeclarations.NamedVariable)
            .name as J.Identifier)
            .type!!
        assertThat(firstMultiMapLambdaParamItType.toString()).isEqualTo("MakeEasyToFind${'$'}MultiMap")

        val secondMultiMapLambdaParamItType = ((((multiLambda
            .arguments[0] as J.Lambda)
            .parameters.parameters[0] as J.VariableDeclarations)
            .variables[0] as J.VariableDeclarations.NamedVariable)
            .name as J.Identifier)
            .type!!
        assertThat(secondMultiMapLambdaParamItType.toString()).isEqualTo("java.util.List<MakeEasyToFind${'$'}MultiMap${'$'}Inner>")
    }
}
