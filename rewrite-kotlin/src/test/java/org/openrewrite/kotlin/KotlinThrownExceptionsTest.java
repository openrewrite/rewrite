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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinThrownExceptionsTest implements RewriteTest {

    private static List<String> thrown(JavaType.Method type) {
        List<String> names = new ArrayList<>();
        for (JavaType t : type.getThrownExceptions()) {
            names.add(((JavaType.FullyQualified) t).getFullyQualifiedName());
        }
        return names;
    }

    @Test
    void javaMethodResolvedFromTheClasspath() {
        rewriteRun(
          kotlin(
            """
              fun test(ms: Long) {
                  Thread.sleep(ms)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> sleep = new AtomicReference<>();
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        sleep.set(method.getMethodType());
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                assertThat(thrown(sleep.get())).containsExactly("java.lang.InterruptedException");
                // The declaring type enumerates its own members through a separate mapping path.
                JavaType.Method member = sleep.get().getDeclaringType().getMethods().stream()
                  .filter(m -> "sleep".equals(m.getName()) && m.getParameterTypes().size() == 1)
                  .filter(m -> m.getParameterTypes().getFirst() == JavaType.Primitive.Long)
                  .findFirst()
                  .orElseThrow();
                assertThat(thrown(member)).containsExactly("java.lang.InterruptedException");
            })
          )
        );
    }

    @Test
    void javaConstructorResolvedFromTheClasspath() {
        rewriteRun(
          kotlin(
            """
              import java.io.FileDescriptor
              import java.io.FileInputStream

              fun test(name: String, fd: FileDescriptor) {
                  FileInputStream(name)
                  FileInputStream(fd)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<JavaType.Method> constructors = new ArrayList<>();
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                        constructors.add(newClass.getMethodType());
                        return super.visitNewClass(newClass, p);
                    }
                }.visit(cu, 0);
                assertThat(thrown(constructors.get(0))).containsExactly("java.io.FileNotFoundException");
                // Only the `String` overload declares an exception; a name-keyed lookup would share it.
                assertThat(thrown(constructors.get(1))).isEmpty();
            })
          )
        );
    }

    @Test
    void kotlinFunctionAnnotatedWithThrows() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              @Throws(IOException::class)
              fun risky() {
              }

              fun test() {
                  risky()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> declaration = new AtomicReference<>();
                AtomicReference<JavaType.Method> invocation = new AtomicReference<>();
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                        if ("risky".equals(method.getSimpleName())) {
                            declaration.set(method.getMethodType());
                        }
                        return super.visitMethodDeclaration(method, p);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        invocation.set(method.getMethodType());
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                assertThat(thrown(declaration.get())).containsExactly("java.io.IOException");
                assertThat(thrown(invocation.get())).containsExactly("java.io.IOException");
            })
          )
        );
    }

    @Test
    void bareKotlinFunctionDeclaresNothing() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              fun bare() {
                  throw IOException()
              }

              fun test() {
                  bare()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> declaration = new AtomicReference<>();
                AtomicReference<JavaType.Method> invocation = new AtomicReference<>();
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                        if ("bare".equals(method.getSimpleName())) {
                            declaration.set(method.getMethodType());
                        }
                        return super.visitMethodDeclaration(method, p);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        invocation.set(method.getMethodType());
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                // Without @Throws the compiled method has no `throws` clause, however it may throw.
                assertThat(thrown(declaration.get())).isEmpty();
                assertThat(thrown(invocation.get())).isEmpty();
            })
          )
        );
    }
}
