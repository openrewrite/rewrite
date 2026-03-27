/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

/**
 * Tests that type attribution is working for Scala constructs.
 */
class ScalaTypeAttributionTest implements RewriteTest {

    @Test
    void valWithTypeAnnotationHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val x: Int = 42
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> identType = new AtomicReference<>();
                AtomicReference<JavaType.Variable> fieldType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                        if ("x".equals(ident.getSimpleName())) {
                            identType.set(ident.getType());
                            fieldType.set(ident.getFieldType());
                        }
                        return super.visitIdentifier(ident, p);
                    }
                }.visit(cu, 0);

                assertThat(identType.get())
                    .as("Identifier 'x' should have type int")
                    .isEqualTo(JavaType.Primitive.Int);

                assertThat(fieldType.get())
                    .as("Identifier 'x' should have a variable type")
                    .isNotNull();
                assertThat(fieldType.get().getName()).isEqualTo("x");
            })
          )
        );
    }

    @Test
    void methodInvocationHasType() {
        rewriteRun(
          scala(
            """
            import java.util.ArrayList

            class MyClass {
              val list = new ArrayList[String]()
              list.add("hello")
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> methodType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        if ("add".equals(method.getSimpleName())) {
                            methodType.set(method.getMethodType());
                        }
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);

                assertThat(methodType.get())
                    .as("Method invocation 'add' should have a method type")
                    .isNotNull();
                assertThat(methodType.get().getName()).isEqualTo("add");
                assertThat(methodType.get().getDeclaringType().getFullyQualifiedName())
                    .isEqualTo("java.util.ArrayList");
            })
          )
        );
    }

    @Test
    void newClassHasType() {
        rewriteRun(
          scala(
            """
            import java.util.ArrayList

            class MyClass {
              val list = new ArrayList[String]()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> newClassType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                        newClassType.set(newClass.getType());
                        return super.visitNewClass(newClass, p);
                    }
                }.visit(cu, 0);

                assertThat(newClassType.get())
                    .as("NewClass should have a type")
                    .isNotNull();
                assertThat(newClassType.get())
                    .isInstanceOf(JavaType.FullyQualified.class);
                assertThat(((JavaType.FullyQualified) newClassType.get()).getFullyQualifiedName())
                    .isEqualTo("java.util.ArrayList");
            })
          )
        );
    }

    @Test
    void fieldAccessHasType() {
        rewriteRun(
          scala(
            """
            class MyClass {
              val s = "hello"
              val len = s.length
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> fieldAccessType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                        if ("length".equals(ident.getSimpleName()) && ident.getType() != null) {
                            fieldAccessType.set(ident.getType());
                        }
                        return super.visitIdentifier(ident, p);
                    }
                }.visit(cu, 0);

                assertThat(fieldAccessType.get())
                    .as("'length' should have a type")
                    .isNotNull();
            })
          )
        );
    }

    @Test
    void classDeclarationHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> classType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        classType.set(classDecl.getType());
                        return super.visitClassDeclaration(classDecl, p);
                    }
                }.visit(cu, 0);

                assertThat(classType.get())
                    .as("ClassDeclaration should have a type")
                    .isNotNull();
                assertThat(classType.get())
                    .isInstanceOf(JavaType.Class.class);
                assertThat(((JavaType.Class) classType.get()).getFullyQualifiedName())
                    .isEqualTo("Foo");
            })
          )
        );
    }
}
