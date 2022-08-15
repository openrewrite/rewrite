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
@file:Suppress("GrUnnecessaryPublicModifier", "GrPackage")

package org.openrewrite.groovy.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.groovy.Assertions.groovy
import org.openrewrite.java.asFullyQualified
import org.openrewrite.java.asParameterized
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.test.RewriteTest

class ClassDeclarationTest : RewriteTest {

    @Test
    fun multipleClassDeclarationsInOneCompilationUnit() = rewriteRun(
        groovy(
            """
                public class A {
                    int n
                    
                    def sum(int m) {
                        n+m
                    }
                }
                class B {}
            """
        )
    )

    @Test
    fun implements() = rewriteRun(
        groovy(
            """
                public interface B {}
                interface C {}
                class A implements B, C {}
            """
        )
    )

    @Test
    fun extends() = rewriteRun(
        groovy(
            """
                public class Test {}
                class A extends Test {}
            """
        )
    )

    @Test
    fun modifierOrdering() = rewriteRun(
        groovy(
            """
                public abstract class A {}
            """
        )
    )

    @Test
    fun interfaceExtendsInterface() = rewriteRun(
        groovy(
            """
                interface A {}
                interface C {}
                interface B extends A , C {}
            """
        )
    )

    @Test
    fun transitiveInterfaces() = rewriteRun(
        groovy(
            """
                interface A {}
                interface B extends A {}
                interface C extends B {}
            """
        )
    )

    @Test
    fun annotation() = rewriteRun(
        groovy(
            """
                @interface A{}
            """
        )
    )

    @Test
    fun hasPackage() = rewriteRun(
        groovy(
            """ 
               package org.openrewrite
                
               public class A{}
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1736")
    @Test
    fun parameterizedFieldDoesNotAffectClassType() = rewriteRun(
        groovy(
            """
                class A {
                    List<String> a
                }
            """
        ) { spec ->
            spec.afterRecipe { cu ->
                val aType = cu.classes[0].type
                assertThat(aType).isInstanceOf(JavaType.Class::class.java)
                assertThat((aType as JavaType.Class).fullyQualifiedName).isEqualTo("A")
            }
        }
    )

    @Test
    fun implicitlyPublic() = rewriteRun(
        groovy("class A{}")
    )

    @Test
    fun packagePrivate() = rewriteRun(
        groovy(
            """
                import groovy.transform.PackageScope
                
                @PackageScope 
                class A {}
            """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val clazz = cu.classes[0]
                assertThat(clazz.modifiers)
                    .`as`("Groovy's default visibility is public, applying @PackageScope should prevent the public modifier from being present")
                    .hasSize(0)
                val annotations = cu.classes[0].allAnnotations
                assertThat(annotations).hasSize(1)
            }
        }
    )

    @Test
    fun typeParameters() = rewriteRun(
        groovy(
            """
                class A <T, S extends PT<S> & C> {
                    T t
                    S s
                }
                interface PT<T> {}
                interface C {}
            """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val typeParameters = cu.classes[0].typeParameters
                assertThat(typeParameters!!).hasSize(2)
                val tParam = typeParameters[0]
                assertThat(tParam.bounds).isNull()
                val sParam = typeParameters[1]
                assertThat(sParam.bounds).isNotNull
                assertThat(sParam.bounds).hasSize(2)
                val ptBound = sParam.bounds!![0]
                assertThat(ptBound).isInstanceOf(J.ParameterizedType::class.java)
            }
        }
    )

    @Test
    fun innerClass() = rewriteRun(
        groovy(
            """
                interface C {
                    class Inner {
                    }
                }
            """
        )
    )

    @Test
    fun newParameterizedConstructor() = rewriteRun(
        groovy(
            """
                class Outer {
                    PT<TypeA> parameterizedField = new PT<TypeA>() {
                    }
                }
                interface TypeA {}
                interface PT<T> {
                }
            """
        )
    )

    @Test
    fun parameterizedField() = rewriteRun(
        groovy(
            """
                class A {
                    List<String> a
                    Map<Object, Object> b
                }
            """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val statements = cu.classes[0].body.statements
                assertThat(statements).hasSize(2)
                val a = (statements[0] as J.VariableDeclarations).variables[0]
                assertThat(a.type.asParameterized()!!.toString())
                    .isEqualTo("java.util.List<java.lang.String>")
                val b = (statements[1] as J.VariableDeclarations).variables[0]
                assertThat(b.type.asParameterized()!!.toString())
                    .isEqualTo("java.util.Map<java.lang.Object, java.lang.Object>")
            }
        }
    )

    @Test
    fun singleLineCommentBeforeModifier() = rewriteRun(
        groovy(
            """
                @Deprecated
                // Some comment
                public final class A {}
            """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val annotations = cu.classes[0].allAnnotations
                assertThat(annotations.size).isEqualTo(1)
                val annotation = annotations[0]!!
                val type = annotation.type
                assertThat(type).isNotNull
                assertThat(type).isInstanceOf(JavaType.FullyQualified::class.java)
                assertThat(type.asFullyQualified()!!.fullyQualifiedName).isEqualTo(
                    "java.lang.Deprecated"
                )
            }
        }
    )
}
