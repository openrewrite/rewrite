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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ShortenFullyQualifiedTypeReferencesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferences());
    }

    @DocumentExample
    @Test
    void redundantImport() {
        rewriteRun(
          //language=java
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
          //language=java
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
          //language=java
          java(
            """
              package a;

              class String {
              }
              """
          ),
          //language=java
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
          //language=java
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
          //language=java
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
    void equalType() {
        rewriteRun(
          java(
            //language=java
            """
              import java.util.List;
                            
              class T {
                  java.util.List list;
              }
              """,
            """
              import java.util.List;
                            
              class T {
                  List list;
              }
              """,
            spec -> spec.mapBeforeRecipe(cu -> (J.CompilationUnit) new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    return multiVariable.withType(JavaType.ShallowClass.build("java.util.List"));
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void noImport() {
        rewriteRun(
          //language=java
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
          //language=java
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
                      return (J.MethodDeclaration) ShortenFullyQualifiedTypeReferences.modifyOnly(method).visit(method, ctx, getCursor().getParent());
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }
          })),
          //language=java
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
          //language=java
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
          //language=java
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
          //language=java
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
          //language=java
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
    void shortenQualifiedJavaLangTypesWhenAlreadyPresentElsewhere() {
        rewriteRun(
          //language=java
          java(
            """
              class T {
                  java.lang.String s1;
                  String s2;
              }
              """,
            """
              class T {
                  String s1;
                  String s2;
              }
              """
          )
        );
    }

    @Test
    void conflictExistingImport() {
        rewriteRun(
          //language=java
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
          //language=java
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
          //language=java
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

    @Test
    void qualifiedMethodReference() {
        rewriteRun(
          java(
            //language=java
            """
              import java.util.Collection;
              import java.util.function.Function;

              class T {
                  Function<Collection<?>, Integer> m() {
                      return java.util.Collection::size;
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.function.Function;

              class T {
                  Function<Collection<?>, Integer> m() {
                      return Collection::size;
                  }
              }
              """
          )
        );
    }

    @Test
    void subtree() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("m1")) {
                      doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(method));
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }
          })),
          java(
            """
              import java.util.Collection;
              import java.util.function.Function;

              class T {
                  Function<Collection<?>, Integer> m() {
                      return java.util.Collection::size;
                  }
                  Function<Collection<?>, Integer> m1() {
                      return java.util.Collection::size;
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.function.Function;

              class T {
                  Function<Collection<?>, Integer> m() {
                      return java.util.Collection::size;
                  }
                  Function<Collection<?>, Integer> m1() {
                      return Collection::size;
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedReferenceCollision() {
        rewriteRun(
          java(
            """            
              class List {
                  class A {
                  }
              }
              """),
          java(
            """           
              import java.util.ArrayList;
                        
              class Test {
                  void test(List.A l1) {
                      java.util.List<Integer> l2 = new ArrayList<>();
                  }
              }
              """)
        );
    }

    @Test
    void deeperNestedReferenceCollision() {
        rewriteRun(
          java(
            """            
              class List {
                  class A {
                      class B {
                      }
                  }
              }
              """),
          java(
            """           
              import java.util.ArrayList;
                        
              class Test {
                  void test(List.A.B l1) {
                      java.util.List<Integer> l2 = new ArrayList<>();
                  }
              }
              """)
        );
    }

    @Test
    void importWithLeadingComment() {
        rewriteRun(
          java(
            """
              package foo;
                            
              /* comment */
              import java.util.List;
                            
              class Test {
                  List<String> l = new java.util.ArrayList<>();
              }
              """,
            """
              package foo;
                            
              /* comment */
              import java.util.ArrayList;
              import java.util.List;
                            
              class Test {
                  List<String> l = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void annotatedFieldAccess() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;
                            
              class Test {
                  java.util. @Anno List<String> l;
              }
              @Target(ElementType.TYPE_USE)
              @interface Anno {}
              """,
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;
              import java.util.List;
                                
              class Test {
                  @Anno List<String> l;
              }
              @Target(ElementType.TYPE_USE)
              @interface Anno {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3870")
    @Test
    void typeFullyQualifiedAnnotatedField() {
        rewriteRun(
          java(
            """
              import java.sql.DatabaseMetaData;
              import java.util.List;
              import java.lang.annotation.*;

              class TypeAnnotationTest {
                  protected java.sql.@A DatabaseMetaData metadata;

                  @Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
                  private @interface A {
                  }
              }
              """,
            """
              import java.sql.DatabaseMetaData;
              import java.util.List;
              import java.lang.annotation.*;

              class TypeAnnotationTest {
                  protected @A DatabaseMetaData metadata;

                  @Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
                  private @interface A {
                  }
              }
              """
          )
        );
    }
}
