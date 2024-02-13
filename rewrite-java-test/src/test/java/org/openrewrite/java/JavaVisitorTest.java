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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaVisitorTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("RedundantThrows")
    @Test
    void javaVisitorHandlesPaddedWithNullElem() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipes(
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                      var mi = super.visitMethodInvocation(method, p);
                      if ("removeMethod".equals(mi.getSimpleName())) {
                          //noinspection ConstantConditions
                          return null;
                      }
                      return mi;
                  }
              }),
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                      if (method.getSimpleName().equals("allTheThings")) {
                          return JavaTemplate.builder("Exception").contextSensitive().build()
                            .apply(getCursor(), method.getCoordinates().replaceThrows());
                      }
                      return method;
                  }
              })
            ),
          java(
            """
              class A {
                  void allTheThings() {
                      doSomething();
                      removeMethod();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """,
            """
              class A {
                  void allTheThings() throws Exception {
                      doSomething();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """
          )
        );
    }

    @Test
    void topVisitor() {
        final JavaIsoVisitor<ExecutionContext> afterVisitor = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                for (Cursor parent = getCursor().getParent(); parent != null; parent = parent.getParent()) {
                    if (method == parent.getValue()) {
                        fail("Duplicate cursor: %s".formatted(getCursor()));
                    }
                }
                return super.visitMethodDeclaration(method, p);
            }
        };
        rewriteRun(
          spec -> spec.recipe(
            toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                    var md = super.visitMethodDeclaration(method, p);
                    if ("myMethod".equals(md.getSimpleName())) {
                        //noinspection ConstantConditions
                        return (J.MethodDeclaration) new JavaIsoVisitor<ExecutionContext>() {
                            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                                doAfterVisit(afterVisitor);
                                return super.visitMethodDeclaration(method, p);
                            }
                        }.visit(md, p, getCursor().getParent());
                    }
                    return md;
                }
            })
          ),
          java("""
           class A {
             public void method1() {
             }
             
             @Deprecated
             public String myMethod() {
               return "hello";
             }
             
             public void method2() {
             }
           }
           """)
        );
    }
}
