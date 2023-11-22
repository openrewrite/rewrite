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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ChangeFieldNameTest implements RewriteTest {
    static Recipe changeFieldName(String enclosingClassFqn, String from, String to) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                doAfterVisit(new ChangeFieldName<>(enclosingClassFqn, from, to));
                return super.visitCompilationUnit(cu, ctx);
            }
        });
    }

    @DocumentExample
    @SuppressWarnings("rawtypes")
    @Test
    void changeFieldName() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("Test", "collection", "list")),
          java(
            """
              import java.util.List;
              class Test {
                 List collection = null;
              }
              """,
            """
              import java.util.List;
              class Test {
                 List list = null;
              }
              """
          )
        );
    }

    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions"})
    @Test
    void changeFieldNameReferences() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("Test", "n", "n1")),
          java(
            """
              class Test {
                 int n;
                 
                 {
                     n = 1;
                     n /= 2;
                     if(n + 1 == 2) {}
                     n++;
                 }
                 
                 public int foo(int n) {
                     return n + this.n;
                 }
              }
              """,
            """
              class Test {
                 int n1;
                 
                 {
                     n1 = 1;
                     n1 /= 2;
                     if(n1 + 1 == 2) {}
                     n1++;
                 }
                 
                 public int foo(int n) {
                     return n + this.n1;
                 }
              }
              """
          )
        );
    }

    @Test
    void changeFieldNameReferencesInOtherClass() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("Test", "n", "n1")),
          java(
            """
              class Caller {
                  Test t = new Test();
                  {
                      t.n = 1;
                  }
              }
              """,
            """
              class Caller {
                  Test t = new Test();
                  {
                      t.n1 = 1;
                  }
              }
              """
          ),
          java(
            """
              class Test {
                 int n;
              }
              """,
            SourceSpec::skip
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/632")
    void changeFieldNameReferencesInOtherClassUsingStaticImport() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("com.example.Test", "IMPORT_ME_STATICALLY", "IMPORT_ME_STATICALLY_1")),
          java(
            """
              package com.example;

              public class Test {
                  public static final int IMPORT_ME_STATICALLY = 0;
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.openrewrite.test;

              import static com.example.Test.IMPORT_ME_STATICALLY;

              public class Caller {
                  int e = IMPORT_ME_STATICALLY;
              }
              """,
            """
              package org.openrewrite.test;

              import static com.example.Test.IMPORT_ME_STATICALLY_1;

              public class Caller {
                  int e = IMPORT_ME_STATICALLY_1;
              }
              """
          )
        );
    }

    @SuppressWarnings("rawtypes")
    @Test
    void dontChangeNestedFieldsWithSameName() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("Test", "collection", "list")),
          java("class A { Object collection; }"),
          java(
            """
              import java.util.List;
              class Test {
                  List collection = null;
                  class Nested {
                      Object collection = Test.this.collection;
                      Object collection2 = new A().collection;
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  List list = null;
                  class Nested {
                      Object collection = Test.this.list;
                      Object collection2 = new A().collection;
                  }
              }
              """
          )
        );
    }

    @Test
    void dontChangeFieldsInConstructor() {
        rewriteRun(
          spec -> spec.recipe(changeFieldName("Test", "a", "b")),
          java(
            """
              class Test {
                  String a;
                  public Test(String a) {
                      this.a = a;
                  }
              }
              """,
            """
              class Test {
                  String b;
                  public Test(String a) {
                      this.b = a;
                  }
              }
              """
          )
        );
    }
}
