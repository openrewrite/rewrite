/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class ShortenFullyQualifiedTypeReferencesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferences());
    }

    @DocumentExample
    @Test
    void redundantImport() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              class T {
                  java.util.List<String> list;
              }
              """,
            """
              import java.util.List;
                            
              class T {
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    void nestedTypeReference() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.Map.Entry<String, String> mapEntry;
              }
              """,
            """
              import java.util.Map;
              
              class T {
                  Map.Entry<String, String> mapEntry;
              }
              """
          )
        );
    }

    @Test
    void conflictingTypeInSamePackage() {
        rewriteRun(
          java(
            """
              package a;

              class String {
              }
              """
          ),
          java(
            """
              package a;

              class T {
                  String s1;
                  java.lang.String s2;
              }
              """
          )
        );
    }

    @Test
    void withinStaticFieldAccess() {
        rewriteRun(
          java(
            """
              class T {
                  int dotall = java.util.regex.Pattern.DOTALL;
              }
              """,
            """
              import java.util.regex.Pattern;
                            
              class T {
                  int dotall = Pattern.DOTALL;
              }
              """
          )
        );
    }

    @Test
    void inGenericTypeParameter() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              class T {
                  List<java.util.List<String>> list;
              }
              """,
            """
              import java.util.List;
                            
              class T {
                  List<List<String>> list;
              }
              """
          )
        );
    }

    @Test
    void noImport() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
              }
              """,
            """
              import java.util.List;
                                
              class T {
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    void multipleConflictingTypesWithoutImport() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
                  java.awt.List list2;
              }
              """,
            """
              import java.util.List;
                                
              class T {
                  List<String> list;
                  java.awt.List list2;
              }
              """
          )
        );
    }

    @Test
    void visitSubtreeOnly() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              @SuppressWarnings("DataFlowIssue")
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("m1")) {
                      return (J.MethodDeclaration) new ShortenFullyQualifiedTypeReferences().getVisitor().visit(method, ctx);
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }
          })),
          java(
            """
              class T {
                  void m1(java.util.List<String> list) {
                  }
                  void m2(java.util.List<String> list) {
                  }
              }
              """,
            """
              class T {
                  void m1(List<String> list) {
                  }
                  void m2(java.util.List<String> list) {
                  }
              }
              """
          )
        );
    }

    @Test
    void conflictWithLocalType() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
                  class List {
                  }
              }
              """
          )
        );
    }

    @Test
    void dontModifyThisReference() {
        rewriteRun(
          java(
            """
              class T {
                  Object f;
                  T() {
                      this.f = null;
                  }
              }
              """
          )
        );
    }

    @Test
    void dontModifyJavadoc() {
        rewriteRun(
          java(
            """
              /**
               * See {@link java.util.List}.
               *
               * @see java.util.List
               */
              class T {
              }
              """
          )
        );
    }

    @Test
    void dontModifyQualifiedJavaLangTypes() {
        rewriteRun(
          java(
            """
              class T {
                  java.lang.String s;
                  java.lang.Integer i;
                  java.lang.Object o;
              }
              """
          )
        );
    }

    @Test
    void conflictExistingImport() {
        rewriteRun(
          java(
            """
              import java.awt.List;
              class T {
                  java.util.List<String> list;
              }
              """
          )
        );
    }

    @Test
    void conflictGenericVariable() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class T<String> {
                  java.lang.String s;
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("TypeParameterHidesVisibleType")
    void conflictGenericVariableOnMethod() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class T {
                  <String> void m(java.lang.String s, List<String> list) {
                  }
              }
              """
          )
        );
    }
}
