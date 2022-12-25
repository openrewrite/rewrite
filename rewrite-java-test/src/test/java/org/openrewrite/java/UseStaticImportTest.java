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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UseStaticImportTest implements RewriteTest {
    @Test
    void replaceWithStaticImports() {
        rewriteRun(
          spec -> spec.recipe(new UseStaticImport("asserts.Assert assert*(..)")),
          java(
            """
              package asserts;

              public class Assert {
                  public static void assertTrue(boolean b) {}
                  public static void assertFalse(boolean b) {}
                  public static void assertEquals(int m, int n) {}
              }
              """
          ),
          java(
            """
              package test;
                            
              import asserts.Assert;
                            
              class Test {
                  void test() {
                      Assert.assertTrue(true);
                      Assert.assertEquals(1, 2);
                      Assert.assertFalse(false);
                  }
              }
              """,
            """
              package test;
                            
              import static asserts.Assert.*;
                            
              class Test {
                  void test() {
                      assertTrue(true);
                      assertEquals(1, 2);
                      assertFalse(false);
                  }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationsHavingNullSelect() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  return super.visitClassDeclaration(classDecl, ctx).withExtends(null);
              }

              @Override
              public J.Import visitImport(J.Import _import, ExecutionContext executionContext) {
                  //noinspection ConstantConditions
                  return null;
              }

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  return super.visitMethodInvocation(method, executionContext)
                    .withDeclaringType(JavaType.ShallowClass.build("asserts.Assert"));
              }

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                  doAfterVisit(new UseStaticImport("asserts.Assert assert*(..)"));
                  return super.visitCompilationUnit(cu, executionContext);
              }
          })).cycles(2).expectedCyclesThatMakeChanges(2),
          java(
            """
              package asserts;

              public class Assert {
                  public static void assertTrue(boolean b) {}
                  public static void assertEquals(int m, int n) {}
              }

              public class MyAssert {
                  public void assertTrue(boolean b) {Assert.assertTrue(b);}
                  public void assertEquals(int m, int n) {Assert.assertEquals(m, n);}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package test;

              import asserts.MyAssert;

              class Test extends MyAssert {
                  void test() {
                      assertTrue(true);
                      assertEquals(1, 2);
                  }
              }
              """,
            """
              package test;

              import static asserts.Assert.assertEquals;
              import static asserts.Assert.assertTrue;

              class Test {
                  void test() {
                      assertTrue(true);
                      assertEquals(1, 2);
                  }
              }
              """
          )
        );
    }

    @Test
    void junit5Assertions() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
            .recipe(new UseStaticImport("org.junit.jupiter.api.Assertions assert*(..)")),
          java(
            """
              package org.openrewrite;

              import org.junit.jupiter.api.Test;
              import org.junit.jupiter.api.Assertions;

              class SampleTest {
                  @Test
                  void sample() {
                      Assertions.assertEquals(42, 21*2);
                  }
              }
              """,
            """
              package org.openrewrite;

              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.assertEquals;

              class SampleTest {
                  @Test
                  void sample() {
                      assertEquals(42, 21*2);
                  }
              }
              """
          )
        );
    }
}
