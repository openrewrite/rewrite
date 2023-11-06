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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType.ShallowClass;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ImplementInterfaceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                doAfterVisit(new ImplementInterface<>(classDecl, "b.B"));
                return classDecl;
            }
        }));
    }

    @Language("java")
    String b = "package b;\npublic interface B {}";

    @Language("java")
    String c = "package c;\npublic interface C {}";

    @Test
    void firstImplementedInterface() {
        rewriteRun(
          java(b, SourceSpec::skip),
          java(
            """
              class A {
              }
              """,
            """
              import b.B;
                            
              class A implements B {
              }
              """
          )
        );
    }

    @Test
    void addAnImplementedInterface() {
        rewriteRun(
          java(b, SourceSpec::skip),
          java(c, SourceSpec::skip),
          java(
            """
              import c.C;
                            
              class A implements C {
              }
              """,
            """
              import b.B;
              import c.C;
                            
              class A implements C, B {
              }
              """
          )
        );
    }

    @Test
    void addAnImplementedInterfaceWithTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  doAfterVisit(new ImplementInterface<>(
                    classDecl,
                    "b.B",
                    Arrays.asList(
                      new J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        "String",
                        ShallowClass.build("java.lang.String"),
                        null
                      ),
                      new J.Identifier(
                        randomId(),
                        Space.build(" ", emptyList()),
                        Markers.EMPTY,
                        emptyList(),
                        "LocalDate",
                        ShallowClass.build("java.time.LocalDate"),
                        null
                      )
                    )
                  ));
                  return classDecl;
              }
          })),
          java(b, SourceSpec::skip),
          java(c, SourceSpec::skip),
          java(
            """
              import c.C;
                              
              class A implements C {
              }
              """,
            """
              import b.B;
              import c.C;
                              
              import java.time.LocalDate;
                              
              class A implements C, B<String, LocalDate> {
              }
              """
          )
        );
    }
}
