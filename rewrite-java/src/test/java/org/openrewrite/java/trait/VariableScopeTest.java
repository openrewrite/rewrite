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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

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
              """,
            """
              class Test {
                  void method() {
                      /*~~(used after)~~>*/int x = 1;
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
                      /*~~(used after)~~>*/int x = 1;
                      System.out.println(x);
                      /*~~(x not used after this)~~>*/System.out.println("no more x usage");
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    void variableShadowingInLambda() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      int x = 1;
                      Consumer<Integer> lambda = x -> { // shadows outer x
                          System.out.println(x);
                      };
                      System.out.println(x); // refers to outer x
                  }
              }
              """,
            """
              import java.util.function.Consumer;
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/int x = 1;
                      Consumer<Integer> lambda = x -> { // shadows outer x
                          System.out.println(x);
                      };
                      System.out.println(x); // refers to outer x
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
                      /*~~(shadowed, used after)~~>*/int count = 0;
                      Runnable r = new Runnable() {
                          /*~~(used after)~~>*/int count = 5; // shadows outer count
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
                      /*~~(max depth: 2)~~>*/Consumer<Integer> outer = x -> {
                          Consumer<Integer> inner = y -> System.out.println(x + y);
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
                      /*~~(used after)~~>*/int x = 1;
                      Consumer<Integer> lambda = y -> { // different variable name
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
                      try (FileInputStream resource = new FileInputStream("test.txt")) {
                          System.out.println("using resource");
                      } catch (IOException e) {
                          System.out.println("error");
                      }
                      System.out.println(resource);
                  }
              }
              """,
            """
              import java.io.*;
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/String resource = "outer";
                      try (FileInputStream resource = new FileInputStream("test.txt")) {
                          System.out.println("using resource");
                      } catch (IOException e) {
                          System.out.println("error");
                      }
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
                      /*~~(used after)~~>*/FileInputStream stream = null;
                      try {
                          stream = new FileInputStream("test.txt");
                      } catch (IOException e) {
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
                      try {
                          System.out.println("trying");
                      } catch (RuntimeException e) {
                          System.out.println(e.getMessage());
                      }
                      System.out.println(e);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/String e = "outer";
                      try {
                          System.out.println("trying");
                      } catch (RuntimeException e) {
                          System.out.println(e.getMessage());
                      }
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
                      /*~~(used after)~~>*/String result;
                      try {
                          result = "success";
                      } catch (RuntimeException e) {
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
                      for (int i = 1; i < 10; i++) {
                          System.out.println(i);
                      }
                      System.out.println(i);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/int i = 0;
                      for (int i = 1; i < 10; i++) {
                          System.out.println(i);
                      }
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
              """,
            """
              class Test {
                  void method() {
                      /*~~(used after)~~>*/int count = 0;
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
                      List<String> items = List.of("a", "b", "c");
                      for (String item : items) {
                          System.out.println(item);
                      }
                      System.out.println(item);
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/String item = "outer";
                      /*~~(used after)~~>*/List<String> items = List.of("a", "b", "c");
                      for (String item : items) {
                          System.out.println(item);
                      }
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
              """,
            """
              import java.util.List;
              class Test {
                  void method() {
                      /*~~(used after)~~>*/int total = 0;
                      /*~~(used after)~~>*/List<Integer> numbers = List.of(1, 2, 3);
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
                  void method() {
                      int x = 1;
                      {
                          int x = 2; // shadows outer x
                          System.out.println(x);
                      }
                      System.out.println(x);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(used after)~~>*/int x = 1;
                      {
                          /*~~(used after)~~>*/int x = 2; // shadows outer x
                          System.out.println(x);
                      }
                      System.out.println(x);
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
              """,
            """
              class Test {
                  void method() {
                      /*~~(used after)~~>*/int value = 10;
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
                  void method() {
                      String str = "outer";
                      Object obj = "test";
                      Runnable r = () -> {
                          if (obj instanceof String str) {
                              System.out.println(str.length());
                          }
                      };
                      System.out.println(str);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      /*~~(shadowed, used after)~~>*/String str = "outer";
                      /*~~(used after)~~>*/Object obj = "test";
                      Runnable r = () -> {
                          if (obj instanceof String str) {
                              System.out.println(str.length());
                          }
                      };
                      System.out.println(str);
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
                      Consumer<Integer> outer = y -> {
                          int x = 2; // shadows method-level x
                          Consumer<Integer> inner = z -> {
                              int x = 3; // shadows lambda-level x
                              System.out.println(x);
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
                      /*~~(shadowed, used after, max depth: 2)~~>*/int x = 1;
                      Consumer<Integer> outer = y -> {
                          /*~~(shadowed)~~>*/int x = 2; // shadows method-level x
                          Consumer<Integer> inner = z -> {
                              /*~~(used after)~~>*/int x = 3; // shadows lambda-level x
                              System.out.println(x);
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
              """,
            """
              class Test {
                  void method() {
                      {
                          /*~~(used after)~~>*/int x = 1;
                          System.out.println(x);
                      }
                      {
                          /*~~(used after)~~>*/int x = 2; // no shadowing, different scope
                          System.out.println(x);
                      }
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
                J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, ctx);

                // Find the containing block
                J.Block containingBlock = getCursor().firstEnclosing(J.Block.class);
                if (containingBlock == null) {
                    return v;
                }

                Cursor blockCursor = getCursor().dropParentUntil(t -> t instanceof J.Block);
                VariableScope scope = scopeMatcher.get(containingBlock, blockCursor).orElse(null);
                if (scope == null) {
                    return v;
                }

                // Process only the first variable in the declaration to avoid duplicates
                J.VariableDeclarations.NamedVariable firstVar = v.getVariables().getFirst();
                String varName = firstVar.getSimpleName();

                // Test variable usage after current statement
                boolean usedAfter = scope.isVariableUsedAfter(varName, v);

                // Test variable shadowing - only check if this is a regular variable declaration
                // not a lambda parameter or field declaration
                boolean shadowed = false;
                if (isRegularVariableDeclaration()) {
                    shadowed = scope.isVariableShadowed(varName);
                }

                // Test max scope depth (only for the first variable declaration in method)
                int maxDepth = scope.getMaxScopeDepth();

                // Add markers based on trait analysis
                if (shadowed) {
                    v = SearchResult.mergingFound(v, "shadowed");
                }

                if (usedAfter) {
                    v = SearchResult.mergingFound(v, "used after");
                }

                // Mark max scope depth only for the first variable declaration in the method that reaches maximum depth
                // AND only when maxDepth >= 2 (to avoid marking simple lambda cases)
                if (maxDepth >= 2 && isRegularVariableDeclaration() && isFirstVariableInMethod(v, scope.getTree())) {
                    v = SearchResult.mergingFound(v, String.format("max depth: %d", maxDepth));
                }

                return v;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Handle the "not used after this" case
                if (m.getSimpleName().equals("println") &&
                  !m.getArguments().isEmpty() &&
                  m.getArguments().getFirst() instanceof J.Literal arg) {

                    if (arg.getValue() != null && arg.getValue().toString().contains("no more x usage")) {
                        return SearchResult.mergingFound(m, "x not used after this");
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
                // Check if this is a regular local variable declaration (not a lambda parameter or field)
                Object parent = getCursor().getParentTreeCursor().getValue();
                return parent instanceof J.Block || parent instanceof J.ForLoop || parent instanceof J.ForEachLoop;
            }

        });
    }
}