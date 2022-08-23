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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RewriteTest

interface TypeUtilsTest : RewriteTest {

    /* isOverride */
    @Test
    fun isOverrideBasicInterface(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            interface Interface {
                void foo();
            }
        """),
        java("""
            class Clazz implements Interface {
                @Override void foo() { }
            }
        """) { s -> s.beforeRecipe { cu ->
            val fooMethodType = (cu.classes[0].body.statements[0] as J.MethodDeclaration).methodType
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent
        }}
    )

    @Test
    fun isOverrideBasicInheritance(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Superclass {
                void foo();
            }
        """),
        java("""
            class Clazz extends Superclass {
                @Override void foo() { }
            }
        """) { s -> s.beforeRecipe { cu ->
            val fooMethodType = (cu.classes[0].body.statements[0] as J.MethodDeclaration).methodType
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1759")
    @Test
    fun isOverrideParameterizedInterface(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            import java.util.Comparator;
            
            class TestComparator implements Comparator<String> {
                @Override public int compare(String o1, String o2) { 
                    return 0; 
                }
            }
        """) { s -> s.beforeRecipe { cu ->
            val fooMethodType = (cu.classes[0].body.statements[0] as J.MethodDeclaration).methodType
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent
        }}
    )

    @Test
    fun isOverrideParameterizedMethod(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            interface Interface {
                <T> void foo(T t);
            }
        """),
        java("""
            class Clazz implements Interface {
                @Override <T> void foo(T t) { }
            }
        """) { s -> s.beforeRecipe { cu ->
            val fooMethodType = (cu.classes[0].body.statements[0] as J.MethodDeclaration).methodType
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    fun isOverrideConsidersTypeParameterPositions(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            interface Interface <T, Y> {
                 void foo(Y y, T t);
            }
        """),
        java("""
            class Clazz implements Interface<Integer, String> {
                
                void foo(Integer t, String y) { }
                
                @Override
                void foo(String y, Integer t) { }
            }
        """) { s -> s.beforeRecipe { cu ->
            val methods = cu.classes[0].body.statements.filterIsInstance<J.MethodDeclaration>()
            assertThat(TypeUtils.findOverriddenMethod((methods[0]).methodType)).isEmpty
            assertThat(TypeUtils.findOverriddenMethod((methods[1]).methodType)).isPresent
        }}
    )

    /* isOfType */
    @Test
    fun isFullyQualifiedOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                Integer integer1;
                Integer integer2;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]
            val variable2 = (cu.classes[0].body.statements[1] as J.VariableDeclarations).variables[0]
            assertThat(variable1.variableType?.type).isInstanceOf(JavaType.Class::class.java)
            assertThat(variable2.variableType?.type).isInstanceOf(JavaType.Class::class.java)
            assertThat(TypeUtils.isOfType(variable1.variableType?.type, variable2.variableType?.type)).isTrue
        }}
    )

    @Test
    fun isParameterizedTypeOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<Integer> integer1;
                java.util.List<Integer> integer2;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]
            val variable2 = (cu.classes[0].body.statements[1] as J.VariableDeclarations).variables[0]

            assertThat(variable1.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            assertThat(variable2.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            assertThat(TypeUtils.isOfType(variable1.variableType?.type, variable2.variableType?.type)).isTrue
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1857")
    @Test
    fun isParameterizedTypeWithShallowClassesOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<Integer> integer1;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable1.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            val shallowParameterizedType = JavaType.Parameterized(null, JavaType.ShallowClass.build("java.util.List"), listOf(JavaType.ShallowClass.build("java.lang.Integer")))
            assertThat(TypeUtils.isOfType(variable1.variableType?.type, shallowParameterizedType)).isTrue()
        }}
    )

    @Test
    fun isMethodTypeIsOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                void stringArg(String arg) {
                    objectArg(arg);
                }
                void integerArg(Integer arg) {
                    objectArg(arg);
                }
                void objectArg(Object arg) {}
            }
        """) { s -> s.beforeRecipe { cu ->
            val methodInvocation1 = (cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.MethodInvocation
            val methodInvocation2 = (cu.classes[0].body.statements[1] as J.MethodDeclaration)
                .body!!.statements[0] as J.MethodInvocation

            assertThat(methodInvocation1.methodType).isInstanceOf(JavaType.Method::class.java)
            assertThat(methodInvocation2.methodType).isInstanceOf(JavaType.Method::class.java)
            assertThat(TypeUtils.isOfType(methodInvocation1.methodType, methodInvocation2.methodType)).isTrue
        }}
    )

    @Test
    fun differentMethodTypeIsOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                void stringArg(String arg) {
                    foo(arg);
                }
                void integerArg(Integer arg) {
                    foo(arg);
                }
                void foo(Integer arg) {}
                void foo(Object arg) {}
            }
        """) { s -> s.beforeRecipe { cu ->
            val methodInvocation1 = (cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.MethodInvocation
            val methodInvocation2 = (cu.classes[0].body.statements[1] as J.MethodDeclaration)
                .body!!.statements[0] as J.MethodInvocation

            assertThat(methodInvocation1.methodType).isInstanceOf(JavaType.Method::class.java)
            assertThat(methodInvocation2.methodType).isInstanceOf(JavaType.Method::class.java)
            assertThat(TypeUtils.isOfType(methodInvocation1.methodType, methodInvocation2.methodType)).isFalse
        }}
    )

    @Test
    fun differentParameterizedTypesIsOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<Integer> integers;
                java.util.List<String> strings;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]
            val variable2 = (cu.classes[0].body.statements[1] as J.VariableDeclarations).variables[0]

            assertThat(variable1.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            assertThat(variable2.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            assertThat(TypeUtils.isOfType(variable1.variableType?.type, variable2.variableType?.type)).isFalse
        }}
    )
    @Test
    fun isGenericTypeOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<? super Number> type1;
                java.util.List<? super Number> type2;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = ((cu.classes[0].body.statements[0] as J.VariableDeclarations)
                .variables[0]?.type as JavaType.Parameterized)
                .typeParameters[0]
            val variable2 = ((cu.classes[0].body.statements[1] as J.VariableDeclarations)
                .variables[0]?.type as JavaType.Parameterized)
                .typeParameters[0]

            assertThat(variable1).isInstanceOf(JavaType.GenericTypeVariable::class.java)
            assertThat(variable2).isInstanceOf(JavaType.GenericTypeVariable::class.java)
            assertThat(TypeUtils.isOfType(variable1, variable2)).isTrue
        }}
    )

    @Test
    fun differentVarianceOfGenericTypeOfType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<? super Number> type1;
                java.util.List<? extends Number> type2;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable1 = ((cu.classes[0].body.statements[0] as J.VariableDeclarations)
                .variables[0]?.type as JavaType.Parameterized)
                .typeParameters[0]
            val variable2 = ((cu.classes[0].body.statements[1] as J.VariableDeclarations)
                .variables[0]?.type as JavaType.Parameterized)
                .typeParameters[0]

            assertThat(variable1).isInstanceOf(JavaType.GenericTypeVariable::class.java)
            assertThat(variable2).isInstanceOf(JavaType.GenericTypeVariable::class.java)
            assertThat(TypeUtils.isOfType(variable1, variable2)).isFalse
        }}
    )

    /* isAssignableTo */
    @Test
    fun isJavaTypeArrayAssignableTo(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("class Test {}"),
        java("""
            class ExtendTest extends Test {
                Test[] extendTestArray = new ExtendTest[0];
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]
            assertThat(TypeUtils.isAssignableTo(variable.variableType?.type, ((variable.initializer as J.NewArray).type))).isTrue
        }}
    )

    /* isOfClassType */
    @Test
    fun isFullyQualifiedTypeOfClassType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                Integer integer;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable.variableType?.type).isInstanceOf(JavaType.Class::class.java)
            assertThat(TypeUtils.isOfClassType(variable.variableType?.type, "java.lang.Integer")).isTrue
        }}
    )

    @Test
    fun isParameterizedTypeOfClassType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<Integer> list;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable.variableType?.type).isInstanceOf(JavaType.Parameterized::class.java)
            assertThat(TypeUtils.isOfClassType(variable.variableType?.type, "java.util.List")).isTrue
        }}
    )

    @Test
    fun isVariableTypeOfClassType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                java.util.List<Integer> list;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable.variableType).isInstanceOf(JavaType.Variable::class.java)
            assertThat(TypeUtils.isOfClassType(variable.variableType, "java.util.List")).isTrue
        }}
    )

    @Test
    fun isArrayTypeOfClassType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                Integer[] integer;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable.variableType?.type).isInstanceOf(JavaType.Array::class.java)
            assertThat(TypeUtils.isOfClassType(variable.variableType?.type, "java.lang.Integer")).isTrue
        }}
    )

    @Test
    fun isPrimitiveTypeOfClassType(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            class Test {
                int i;
            }
        """) { s -> s.beforeRecipe { cu ->
            val variable = (cu.classes[0].body.statements[0] as J.VariableDeclarations).variables[0]

            assertThat(variable.variableType?.type).isInstanceOf(JavaType.Primitive::class.java)
            assertThat(TypeUtils.isOfClassType(variable.variableType?.type, "int")).isTrue
        }}
    )
}
