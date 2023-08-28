package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.openrewrite.java.Assertions.java;

class ChangeClassInheritanceTest implements RewriteTest {

    @Nested
    class WithExtendsTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(RewriteTest.toRecipe(() -> new JavaVisitor<>() {
                private final J.Identifier arrayList = new J.Identifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, ArrayList.class.getName(), JavaType.buildType(ArrayList.class.getName()), null);

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                    if (classDecl.getExtends() != null && Objects.equals(arrayList.getType(), classDecl.getExtends().getType())) {
                        return classDecl;
                    }
                    return classDecl.withExtends(arrayList);
                }
            }));
        }

        @Test
        void addExtends() {
            java(
              """
              package de.example;
              
              class CustomList {
              }
              """,
              """
              package de.example;
              
              class CustomList extends java.util.ArrayList {
              }
              """
            );
        }

        @Test
        void replaceExtends() {
            rewriteRun(
              java(
                """
                package de.example;
                
                class CustomList extends java.util.HashMap {
                }
                """,
                """
                package de.example;
                
                class CustomList extends java.util.ArrayList {
                }
                """
              )
            );
        }

        @Test
        void addExtendsOnExistingImplements() {
            java(
              """
              package de.example;
              
              class CustomList implements java.lang.Cloneable {
              }
              """,
              """
              package de.example;
              
              class CustomList extends java.util.ArrayList implements java.lang.Cloneable {
              }
              """
            );
        }
    }

    @Nested
    class WithImplementsTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(RewriteTest.toRecipe(() -> new JavaVisitor<>() {
                private final J.Identifier serializable = new J.Identifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, Serializable.class.getName(), JavaType.buildType(Serializable.class.getName()), null);

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                    if (classDecl.getImplements() != null && classDecl.getImplements().stream().anyMatch(e -> Objects.equals(e.getType(), serializable.getType()))) {
                        return classDecl;
                    }
                    return classDecl.withImplements(List.of(serializable));
                }
            }));
        }

        @Test
        void addImplements() {
            rewriteRun(
              java(
                """
                  package de.example;
                              
                  class CustomList {
                  }
                  """,
                """
                  package de.example;
                              
                  class CustomList implements java.io.Serializable {
                  }
                  """
              )
            );
        }

        @Test
        void replaceImplements() {
            rewriteRun(
              java(
                """
                  package de.example;
                              
                  class CustomList implements java.lang.Cloneable {
                  }
                  """,
                """
                  package de.example;
                              
                  class CustomList implements java.io.Serializable {
                  }
                  """
              )
            );
        }

        @Test
        void addImplementsOnExistingExtends() {
            rewriteRun(
              java(
                """
                  package de.example;
                              
                  class CustomList extends java.util.HashMap {
                  }
                  """,
                """
                  package de.example;
                              
                  class CustomList extends java.util.HashMap implements java.io.Serializable {
                  }
                  """
              )
            );
        }
    }
}
