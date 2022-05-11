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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RewriteTest

interface TypeUtilsTest : RewriteTest {

    @Test
    fun isOverrideBasicInterface(jp: JavaParser) = rewriteRun(
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
    fun isOverrideBasicInheritance(jp: JavaParser) = rewriteRun(
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
    fun isOverrideParameterizedInterface(jp: JavaParser) = rewriteRun(
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
    fun isOverrideParameterizedMethod(jp: JavaParser) = rewriteRun(
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

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    fun isOverrideConsidersTypeParameterPositions(jp: JavaParser) = rewriteRun(
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
            assertThat(TypeUtils.findOverriddenMethod((methods[0]).methodType)).isPresent
        }}
    )
}
