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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.openrewrite.java.Assertions.java;

public class GoToLineTest implements RewriteTest {

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class GoToLineRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Go to line";
        }

        @Override
        public String getDescription() {
            return "Finds a line in a file. Will add LST markers to all AST nodes on the line (and column if specified).";
        }

        @Option(
          displayName = "Line number",
          description = "The line number to go to. 1-based.",
          example = "1"
        )
        Integer lineNumber;

        @Option(
          displayName = "Column number",
          description = "The column number to go to. 1-based.\nIf not specified, all AST nodes on the line will be selected.",
          example = "1",
          required = false
        )
        @Nullable
        Integer columnNumber;

        @Override
        public Validated<Object> validate() {
            Validated<Object> validated = super.validate();
            //noinspection ConstantValue
            if (lineNumber != null && lineNumber < 1) {
                validated = validated.and(Validated.invalid("lineNumber", lineNumber, "lineNumber must be greater than 0"));
            }
            if (columnNumber != null && columnNumber < 1) {
                validated = validated.and(Validated.invalid("columnNumber", columnNumber, "columnNumber must be greater than 0"));
            }
            return validated;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                    Collection<J> found;
                    if (columnNumber != null) {
                        found = GoToLine.findLineColumn(cu, lineNumber, columnNumber);
                    } else {
                        found = GoToLine.findLine(cu, lineNumber);
                    }
                    if (found.isEmpty()) {
                        return cu;
                    }
                    Set<J> foundSet = Collections.newSetFromMap(new IdentityHashMap<>());
                    foundSet.addAll(found);

                    //noinspection DataFlowIssue
                    return (J.CompilationUnit) cu.accept(new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public @Nullable J preVisit(J tree, ExecutionContext executionContext) {
                            J t = tree;
                            if (foundSet.remove(t)) {
                                t = SearchResult.found(t, getSimpleNameWithParent(tree.getClass()));
                            }
                            if (foundSet.isEmpty()) {
                                stopAfterPreVisit();
                            }
                            return t;
                        }
                    }, executionContext);
                }
            };
        }

        private static String getSimpleNameWithParent(Class<?> clazz) {
            if (clazz.getEnclosingClass() != null) {
                return getSimpleNameWithParent(clazz.getEnclosingClass()) + "." + clazz.getSimpleName();
            }
            return clazz.getSimpleName();
        }
    }

    private static Recipe locateLine(int line) {
        return new GoToLineRecipe(line, null);
    }

    private static Recipe locateLineColumn(int line, int column) {
        return new GoToLineRecipe(line, column);
    }

    @Test
    void findLineColumnPassedString() {
        rewriteRun(
          spec -> spec.recipe(locateLineColumn(3, 28)),
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      System.out.println(/*~~(J.Literal)~~>*/"Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findFindLineColumnMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(locateLineColumn(3, 20)),
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      System.out./*~~(J.Identifier)~~>*/println("Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findLineColumnStaticImportMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(locateLineColumn(4, 9)),
          java(
            """
              import static java.lang.Thread.sleep;
              class Test {
                  void test() {
                      sleep(1000);
                  }
              }
              """,
            """
              import static java.lang.Thread.sleep;
              class Test {
                  void test() {
                      /*~~(J.MethodInvocation)~~>*//*~~(J.Identifier)~~>*/sleep(1000);
                  }
              }
              """
          )
        );
    }

    @Test
    void findLineColumnStaticImportMethodInvocationWithComment() {
        rewriteRun(
          spec -> spec.recipe(locateLineColumn(5, 9)),
          java(
            """
              import static java.lang.Thread.sleep;
              class Test {
                  void test() {
                      // comment
                      sleep(1000);
                  }
              }
              """,
            """
              import static java.lang.Thread.sleep;
              class Test {
                  void test() {
                      // comment
                      /*~~(J.MethodInvocation)~~>*//*~~(J.Identifier)~~>*/sleep(1000);
                  }
              }
              """
          )
        );
    }


    @Test
    void findMethodCallLineNumber() {
        rewriteRun(
          spec -> spec.recipe(locateLine(3)),
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~(J.MethodInvocation)~~>*//*~~(J.FieldAccess)~~>*//*~~(J.Identifier)~~>*/System./*~~(J.Identifier)~~>*/out./*~~(J.Identifier)~~>*/println(/*~~(J.Literal)~~>*/"Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassHeaderLine() {
        rewriteRun(
          spec -> spec.recipe(locateLine(1)),
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              /*~~(J.ClassDeclaration)~~>*/class /*~~(J.Identifier)~~>*/Test /*~~(J.Block)~~>*/{
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassHeaderWithMultilneCommentLine() {
        rewriteRun(
          spec -> spec.recipe(locateLine(1)),
          java(
            """
              class Test /*
              */ {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              /*~~(J.ClassDeclaration)~~>*/class /*~~(J.Identifier)~~>*/Test /*
              */ {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassHeaderWithSingleLineCommentLine() {
        rewriteRun(
          spec -> spec.recipe(locateLine(1)),
          java(
            """
              class Test // comment
              {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              /*~~(J.ClassDeclaration)~~>*/class /*~~(J.Identifier)~~>*/Test // comment
              {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassHeaderWithJavadocLine() {
        rewriteRun(
          spec -> spec.recipe(locateLine(4)),
          java(
            """
              /**
               * Javadoc
               */
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              /**
               * Javadoc
               */
              /*~~(J.ClassDeclaration)~~>*/class /*~~(J.Identifier)~~>*/Test /*~~(J.Block)~~>*/{
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassHeaderWithJavadocLineAndMultilineComment() {
        rewriteRun(
          spec -> spec.recipe(locateLine(5)),
          java(
            """
              /**
               * Javadoc
               */
              /* comment */
              class Test {
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """,
            """
              /**
               * Javadoc
               */
              /* comment */
              /*~~(J.ClassDeclaration)~~>*/class /*~~(J.Identifier)~~>*/Test /*~~(J.Block)~~>*/{
                  void test() {
                      System.out.println("Hello World!");
                  }
              }
              """
          )
        );
    }
}
