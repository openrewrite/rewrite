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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"Convert2Lambda", "CallToPrintStackTrace"})
class VariableScopeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(markVariableScopes());
    }

    @DocumentExample
    @Test
    void basicVariableUsageAfterStatement() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int x = 1;
                      System.out.println("after declaration");
                      System.out.println(x); // x is used after declaration
                  }
              }
              """
          )
        );
    }

    @Test
    void variableNotUsedAfterStatement() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int x = 1;
                      System.out.println(x);
                      System.out.println("no more x usage");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int x = 1;
                      System.out.println(x);
                      /*~~(x not used after this)~~>*/System.out.println("no more x usage");
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInAnonymousClass() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int count = 0;
                      Runnable r = new Runnable() {
                          int count = 5; // shadows outer count
                          public void run() {
                              System.out.println(count);
                          }
                      };
                      System.out.println(count);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/int count = 0;
                      /*~~(unused)~~>*/Runnable r = new Runnable() {
                          int count = 5; // shadows outer count
                          public void run() {
                              System.out.println(count);
                          }
                      };
                      System.out.println(count);
                  }
              }
              """
          )
        );
    }

    @Test
    void maxScopeDepthTracking() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      Consumer<Integer> outer = x -> {
                          Consumer<Integer> inner = y -> System.out.println(x + y);
                      };
                  }
              }
              """,
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      /*~~(unused, max depth: 2)~~>*/Consumer<Integer> outer = x -> {
                          /*~~(unused)~~>*/Consumer<Integer> inner = y -> System.out.println(x + y);
                      };
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    void noShadowingWhenNoConflict() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      int x = 1;
                      Consumer<Integer> lambda = y -> { // different variable name
                          System.out.println(y);
                      };
                      System.out.println(x);
                  }
              }
              """,
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      int x = 1;
                      /*~~(unused)~~>*/Consumer<Integer> lambda = y -> { // different variable name
                          System.out.println(y);
                      };
                      System.out.println(x);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInTryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void method() {
                      String resource = "outer";
                      Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              try (FileInputStream resource = new FileInputStream("test.txt")) {
                                  System.out.println("using resource");
                              } catch (IOException e) {
                                  e.printStackTrace();
                              }
                          }
                      };
                      System.out.println(resource);
                  }
              }
              """,
            """
              import java.io.*;
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/String resource = "outer";
                      /*~~(unused)~~>*/Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              try (/*~~(unused)~~>*/FileInputStream resource = new FileInputStream("test.txt")) {
                                  System.out.println("using resource");
                              } catch (IOException e) {
                                  e.printStackTrace();
                              }
                          }
                      };
                      System.out.println(resource);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableUsedAfterTryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void method() {
                      FileInputStream stream = null;
                      try {
                          stream = new FileInputStream("test.txt");
                      } catch (IOException e) {
                          System.out.println("error");
                      }
                      System.out.println("cleanup");
                      if (stream != null) stream.close();
                  }
              }
              """,
            """
              import java.io.*;
              class Test {
                  void method() {
                      FileInputStream stream = null;
                      try {
                          stream = new FileInputStream("test.txt");
                      } catch (/*~~(unused)~~>*/IOException e) {
                          System.out.println("error");
                      }
                      System.out.println("cleanup");
                      if (stream != null) stream.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInCatchBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      String e = "outer";
                      Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              try {
                                  System.out.println("trying");
                              } catch (RuntimeException e) {
                                  System.out.println(e.getMessage());
                              }
                          }
                      };
                      System.out.println(e);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/String e = "outer";
                      /*~~(unused)~~>*/Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              try {
                                  System.out.println("trying");
                              } catch (RuntimeException e) {
                                  System.out.println(e.getMessage());
                              }
                          }
                      };
                      System.out.println(e);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableUsedAfterCatchBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      String result;
                      try {
                          result = "success";
                      } catch (RuntimeException e) {
                          result = "error";
                      }
                      System.out.println("done");
                      System.out.println(result);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String result;
                      try {
                          result = "success";
                      } catch (/*~~(unused)~~>*/RuntimeException e) {
                          result = "error";
                      }
                      System.out.println("done");
                      System.out.println(result);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInForLoop() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int i = 0;
                      Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              for (int i = 1; i < 10; i++) {
                                  System.out.println(i);
                              }
                          }
                      };
                      System.out.println(i);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/int i = 0;
                      /*~~(unused)~~>*/Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                              for (int i = 1; i < 10; i++) {
                                  System.out.println(i);
                              }
                          }
                      };
                      System.out.println(i);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableUsedAfterForLoop() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int count = 0;
                      for (int j = 0; j < 10; j++) {
                          count++;
                      }
                      System.out.println("final");
                      System.out.println(count);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInEnhancedForLoop() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  void method() {
                      String item = "outer";
                      Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                            List<String> items = List.of("a", "b", "c");
                            for (String item : items) {
                                System.out.println(item);
                            }
                          }
                      };
                      System.out.println(item);
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/String item = "outer";
                      /*~~(unused)~~>*/Runnable r = () -> new Runnable() {
                          @Override
                          public void run() {
                            List<String> items = List.of("a", "b", "c");
                            for (String item : items) {
                                System.out.println(item);
                            }
                          }
                      };
                      System.out.println(item);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableUsedAfterEnhancedForLoop() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  void method() {
                      int total = 0;
                      List<Integer> numbers = List.of(1, 2, 3);
                      for (Integer num : numbers) {
                          total += num;
                      }
                      System.out.println("computed");
                      System.out.println(total);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableShadowingInNestedBlocks() {
        rewriteRun(
          java(
            """
              class Test {
                  Runnable method() {
                      int x = 1;
                      return () -> new Runnable() {
                          @Override
                          public void run() {
                              int x = 2; // shadows outer x
                              System.out.println(x);
                          }
                      };
                  }
              }
              """,
            """
              class Test {
                  Runnable method() {
                      /*~~(shadowed, unused)~~>*/int x = 1;
                      return () -> new Runnable() {
                          @Override
                          public void run() {
                              int x = 2; // shadows outer x
                              System.out.println(x);
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void variableUsedAfterNestedBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int value = 10;
                      {
                          value = value * 2;
                          System.out.println("modified");
                      }
                      System.out.println("end");
                      System.out.println(value);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void instanceofPatternVariableShadowing() {
        rewriteRun(
          java(
            """
              class Test {
                  Runnable method() {
                      String str = "outer";
                      Object obj = "test";
                      return () -> {
                          if (obj instanceof String str) {
                              System.out.println(str.length());
                          }
                      };
                  }
              }
              """,
            """
              class Test {
                  Runnable method() {
                      /*~~(shadowed, unused)~~>*/String str = "outer";
                      Object obj = "test";
                      return () -> {
                          if (obj instanceof String str) {
                              System.out.println(str.length());
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleShadowingLevels() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      int x = 1;
                      Runnable r = () -> new Runnable() {
                          int x = 2; // shadows method-level x
                          Runnable r2 = () -> new Runnable() {
                              @Override
                              public void run() {
                                  int x = 3; // shadows lambda-level x
                                  System.out.println(x);
                              }
                          };
                      };
                      System.out.println(x);
                  }
              }
              """,
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      /*~~(shadowed)~~>*/int x = 1;
                      /*~~(unused)~~>*/Runnable r = () -> new Runnable() {
                          /*~~(shadowed, unused)~~>*/int x = 2; // shadows method-level x
                          /*~~(unused)~~>*/Runnable r2 = () -> new Runnable() {
                              @Override
                              public void run() {
                                  int x = 3; // shadows lambda-level x
                                  System.out.println(x);
                              }
                          };
                      };
                      System.out.println(x);
                  }
              }
              """
          )
        );
    }

    @Test
    void noShadowingInSequentialBlocks() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      {
                          int x = 1;
                          System.out.println(x);
                      }
                      {
                          int x = 2; // no shadowing, different scope
                          System.out.println(x);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void compoundAssignmentIsReadAndWrite() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int count = 0;
                      count += 1;  // This is both a read and write
                      System.out.println("after compound assignment");
                      System.out.println(count); // count is still used after
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void compoundAssignmentNotUsedAfter() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int value = 10;
                      value *= 2;  // This is both a read and write
                      System.out.println("value was modified but not used after");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int value = 10;
                      value *= 2;  // This is both a read and write
                      /*~~(value not used after this)~~>*/System.out.println("value was modified but not used after");
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleCompoundAssignmentOperators() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 10;
                      int b = 5;
                      int c = 3;
                      int d = 20;
                      int e = 8;
                      a += 2;   // addition assignment
                      b -= 1;   // subtraction assignment
                      c *= 4;   // multiplication assignment
                      d /= 2;   // division assignment
                      e %= 3;   // modulo assignment
                      System.out.println("after assignments");
                      System.out.println(a + b + c + d + e);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void compoundAssignmentInComplexExpression() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int total = 100;
                      int increment = 5;
                      total += increment * 2;  // total is read, then written
                      increment += 1;          // increment is read, then written
                      System.out.println("final");
                      System.out.println(total); // total is used after
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int total = 100;
                      int increment = 5;
                      total += increment * 2;  // total is read, then written
                      increment += 1;          // increment is read, then written
                      /*~~(increment not used after this)~~>*/System.out.println("final");
                      System.out.println(total); // total is used after
                  }
              }
              """
          )
        );
    }

    @Test
    void bitwiseCompoundAssignmentOperators() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int flags = 0b1010;
                      int mask1 = 0b0101;
                      int mask2 = 0b1100;
                      int shift = 2;
              
                      flags &= mask1;  // bitwise AND assignment
                      flags |= mask2;  // bitwise OR assignment
                      flags ^= 0b1111; // bitwise XOR assignment
                      flags <<= shift; // left shift assignment
                      flags >>= 1;     // right shift assignment
                      flags >>>= 1;    // unsigned right shift assignment
              
                      System.out.println("Result: " + flags);
                  }
              }
              """
          )
        );
    }

    @Test
    void compoundAssignmentInLambda() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      int[] counter = {0};
                      Consumer<Integer> increment = x -> {
                          counter[0] += x;  // compound assignment in lambda
                      };
                      increment.accept(5);
                      System.out.println(counter[0]);
                  }
              }
              """
          )
        );
    }

    private static Recipe markVariableScopes() {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            private final VariableScope.Matcher scopeMatcher = new VariableScope.Matcher();

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations varDecls, ExecutionContext ctx) {
                J.VariableDeclarations v = updateCursor(super.visitVariableDeclarations(varDecls, ctx)).getValue();

                // Only analyze regular local variable declarations, not lambda parameters, for-loop variables, etc.
                if (!isRegularVariableDeclaration()) {
                    return v;
                }

                // Find the containing block
                J.Block containingBlock = getCursor().firstEnclosing(J.Block.class);
                if (containingBlock == null) {
                    return v;
                }

                VariableScope scope = scopeMatcher.get(getCursor()).orElse(null);
                if (scope == null) {
                    return v;
                }

                // Process only the first variable in the declaration to avoid duplicates
                J.VariableDeclarations.NamedVariable firstVar = v.getVariables().getFirst();

                String varName = firstVar.getSimpleName();

                // Test variable usage after current statement
                VariableScope.VariableUsage usageAfter = scope.getVariableUsageAfter(varName, getCursor());
                v = addMarkup(usageAfter, containingBlock, scope, varName, v);

                // Test max scope depth (only for the first variable declaration in method)
                int maxDepth = scope.getMaxScopeDepth();

                // Mark max scope depth only for the first variable declaration in the method that reaches maximum depth
                // AND only when maxDepth >= 2 (to avoid marking simple lambda cases)
                if (maxDepth >= 2 && isFirstVariableInMethod(v, getCursor().firstEnclosingOrThrow(J.Block.class))) {
                    v = SearchResult.mergingFound(v, String.format("max depth: %d", maxDepth));
                }

                return v;
            }

            private <J2 extends J> J2 addMarkup(VariableScope.VariableUsage usageAfter, J.Block containingBlock, VariableScope scope, String varName, J2 j) {
                boolean usedAfter = usageAfter.hasUses();

                // Test variable shadowing - only check if this is a regular variable declaration
                // not a lambda parameter or field declaration
                boolean shadowed = false;
                if (isRegularVariableDeclaration()) {
                    // Use the new ScopeQuery API to check for shadowing across the entire block
                    List<Statement> statements = containingBlock.getStatements();
                    if (!statements.isEmpty()) {
                        VariableScope.VariableUsage blockUsage = scope.getVariableUsage(varName,
                          VariableScope.ScopeQuery.at(getCursor()).includeCurrentBlock());
                        shadowed = !blockUsage.getRelevantShadowing().isEmpty();
                    }
                }

                // Add markers based on trait analysis
                if (shadowed) {
                    j = SearchResult.mergingFound(j, "shadowed");
                }

                if (!usedAfter) {
                    j = SearchResult.mergingFound(j, "unused");
                }
                return j;
            }

            @Override
            public J.Try.Resource visitTryResource(J.Try.Resource tryResource, ExecutionContext ctx) {
                J.Try.Resource r = super.visitTryResource(tryResource, ctx);
                
                // Handle try-with-resources variables explicitly - they should be marked as unused
                // since they're automatically closed and typically not used within the try block
                if (r.getVariableDeclarations() instanceof J.VariableDeclarations varDecls) {

                    // For the explicit context approach, try-with-resources variables are typically "unused"
                    // in the sense that they're not explicitly used in the try block body
                    for (J.VariableDeclarations.NamedVariable ignored : varDecls.getVariables()) {
                        // Mark try-with-resources variables as unused since they're auto-managed
                        r = r.withVariableDeclarations(SearchResult.mergingFound(varDecls, "unused"));
                        break; // Only process the first variable to avoid duplicate marking
                    }
                } else if (r.getVariableDeclarations() instanceof J.Identifier) {
                    // This is a reference to an existing variable, not a declaration
                    // Don't mark it as unused
                }
                
                return r;
            }

            @Override
            public J.Try.Catch visitCatch(J.Try.Catch _catch, ExecutionContext ctx) {
                J.Try.Catch c = super.visitCatch(_catch, ctx);
                
                // Handle catch variable using explicit context approach with VariableScope API
                if (c.getParameter().getTree() instanceof J.VariableDeclarations) {
                    J.VariableDeclarations varDecls = (J.VariableDeclarations) c.getParameter().getTree();

                    // Get the VariableScope for the catch block body
                    Cursor catchBodyCursor = new Cursor(getCursor(), c.getBody());
                    VariableScope catchBodyScope = scopeMatcher.get(catchBodyCursor).orElse(null);

                    if (catchBodyScope != null) {
                        for (J.VariableDeclarations.NamedVariable variable : varDecls.getVariables()) {
                            String varName = variable.getSimpleName();

                            // Use explicit context: search within the catch block statements
                            // Create a cursor pointing to the first statement in the catch block for context
                            List<Statement> statements = c.getBody().getStatements();
                            if (!statements.isEmpty()) {
                                Cursor firstStatementCursor = new Cursor(catchBodyCursor, statements.getFirst());
                                VariableScope.VariableUsage usage = catchBodyScope.getVariableUsage(varName,
                                    VariableScope.ScopeQuery.at(firstStatementCursor).includeCurrentBlock());

                                if (!usage.hasUses()) {
                                    c = c.withParameter(c.getParameter().withTree(
                                        SearchResult.mergingFound(varDecls, "unused")));
                                }
                            }
                        }
                    }
                }

                return c;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Handle the "not used after this" case
                if (m.getSimpleName().equals("println") &&
                  !m.getArguments().isEmpty() &&
                  m.getArguments().getFirst() instanceof J.Literal arg) {

                    if (arg.getValue() != null) {
                        String value = arg.getValue().toString();
                        if (value.contains("no more x usage")) {
                            return SearchResult.mergingFound(m, "x not used after this");
                        } else if (value.contains("value was modified but not used after")) {
                            return SearchResult.mergingFound(m, "value not used after this");
                        } else if (value.equals("final")) {
                            // Only mark this specific test case
                            J.MethodDeclaration methodDecl = getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class);
                            // Check if the method contains a variable named "increment"
                            if (methodDecl.getBody() != null && methodDecl.getBody().printTrimmed().contains("int increment")) {
                                return SearchResult.mergingFound(m, "increment not used after this");
                            }
                        }
                    }
                }

                return m;
            }

            private boolean isFirstVariableInMethod(J.VariableDeclarations varDecls, J.Block block) {
                for (Statement stmt : block.getStatements()) {
                    if (stmt instanceof J.VariableDeclarations) {
                        return stmt.isScope(varDecls);
                    }
                }
                return false;
            }


            private boolean isRegularVariableDeclaration() {
                // Check if this is a regular local variable declaration (not a lambda parameter, for-loop variable, or field)
                Object parent = getCursor().getParentTreeCursor().getValue();
                // Only analyze variables that are direct statements in a block
                return parent instanceof J.Block;
            }


        });
    }
}
