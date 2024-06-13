/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class VariableNameUtilsTest implements RewriteTest {

    private static Consumer<RecipeSpec> baseTest(String scope, String expected) {
        return spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
                //noinspection RedundantCast
                assertThat(getCursor().getMessage("variables", emptySet()))
                  .containsExactlyInAnyOrder((Object[]) expected.split(","));
                return c;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext p) {
                if (identifier.getSimpleName().equals(scope)) {
                    HashSet<String> variables = getCursor().getNearestMessage("variables", new HashSet<>());
                    variables.addAll(VariableNameUtils.findNamesInScope(getCursor()));
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "variables", variables);
                }
                return identifier;
            }
        }));
    }

    @Disabled
    @Test
    void doNotAddPackagePrivateNameFromSuperClass() {
        rewriteRun(
          baseTest("classBlock", "classBlock"),
          java(
            """
              package foo;
              public class Super {
                  boolean pkgPrivate;
              }
              """
          ),
          java(
            """
              package bar;
                            
              import foo.Super;
                            
              class Test extends Super {
                  boolean classBlock;
              }
              """
          )
        );
    }

    @Test
    void staticImportedFieldNames() {
        rewriteRun(
          baseTest("classBlock", "classBlock,UTF_8"),
          java(
            """
              import static java.nio.charset.StandardCharsets.UTF_8;
              import static java.util.Collections.emptyList;
                            
              class Test {
                  boolean classBlock;
              }
              """
          )
        );
    }

    @Test
    void allClassFieldsAreFound() {
        rewriteRun(
          baseTest("methodBlock", "methodBlock,classBlockA,classBlockB"),
          java(
            """
              class Test {
                  boolean classBlockA;
                  void method() {
                      boolean methodBlock;
                  }
                  boolean classBlockB;
              }
              """
          )
        );
    }

    @Test
    void findNamesAvailableFromBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                  J.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
                  assertThat(getCursor().getMessage("variables", emptySet()))
                    .containsExactlyInAnyOrder("classFieldA", "classFieldB", "methodBlockA", "methodBlockB", "methodParam", "control", "forBlock", "ifBlock");
                  return c;
              }

              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext p) {
                  if (identifier.getSimpleName().equals("methodBlockA")) {
                      HashSet<String> variables = getCursor().getNearestMessage("variables", new HashSet<>());
                      variables.addAll(VariableNameUtils.findNamesInScope(getCursor().dropParentUntil(J.Block.class::isInstance)));
                      getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "variables", variables);
                  }
                  return identifier;
              }
          })),
          java(
            """
              class Test {
                  boolean classFieldA;
                  void method (boolean methodParam) {
                      boolean methodBlockA;
                      for (int control = 0; control < 10; control++) {
                          boolean forBlock;
                          if (control == 5) {
                              boolean ifBlock;
                          }
                      }
                      boolean methodBlockB;
                  }
                  boolean classFieldB;
              }
              """
          )
        );
    }

    @Test
    void detectMethodParam() {
        rewriteRun(
          baseTest("ifBlock", "ifBlock,methodBlockA,methodParam"),
          java(
            """
              class Test {
                  void method(boolean methodParam) {
                      boolean methodBlockA;
                      if (methodParam) {
                          boolean ifBlock;
                      }
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @Test
    void detectInstanceOf() {
        rewriteRun(
          baseTest("ifBlock", "ifBlock,pattern,methodParam"),
          java(
            """
              class Test {
                  void method(Object methodParam) {
                      if (methodParam instanceof String pattern) {
                          boolean ifBlock;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          baseTest("innerClassBlock", "classBlockA,classBlockB,innerClassBlock"),
          java(
            """
              class Test {
                  boolean classBlockA;
                  void method() {
                      boolean methodBlock;
                  }
                  boolean classBlockB;
                  class Inner {
                      boolean innerClassBlock;
                  }
              }
              """
          )
        );

    }

    @ParameterizedTest
    @CsvSource(value = {
      "control:control,methodBlockA",
      "forBlock:forBlock,control,methodBlockA"
    }, delimiter = ':')
    void forLoop(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              class Test {
                  void method() {
                      boolean methodBlockA;
                      for (int control = 0; control < 10; control++) {
                          boolean forBlock;
                      }
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
      "ifScope:ifScope,methodParam,methodBlockA",
      "elseIfScope:elseIfScope,methodParam,methodBlockA",
      "elseScope:elseScope,methodParam,methodBlockA"
    }, delimiter = ':')
    void ifElse(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              class Test {
                  void method(short methodParam) {
                      boolean methodBlockA;
                      if (methodParam == 0) {
                          boolean ifScope;
                      } else if (methodParam == 1) {
                          boolean elseIfScope;
                      } else {
                          boolean elseScope;
                      }
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "Convert2Lambda"})
    @ParameterizedTest
    @CsvSource(value = {
      "supplier:supplier,methodBlockA",
      "anonMethodBlock:anonMethodBlock,methodBlockA,supplier"
    }, delimiter = ':')
    void lambda(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              import java.util.function.Supplier;
                            
              class Test {
                  void method() {
                      boolean methodBlockA;
                      Supplier<Integer> supplier = new Supplier<>() {
                          @Override
                          public Integer get() {
                              int anonMethodBlock;
                              return anonMethodBlock;
                          }
                      };
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void superClass() {
        rewriteRun(
          baseTest("classBlock", "classBlock,superPublic,superProtected,superPackagePrivate,superSuperPublic,superSuperProtected,superSuperPackagePrivate"),
          java(
            """
              package foo.bar;
                            
              class SuperSuper {
                  public int superSuperPublic;
                  protected int superSuperProtected;
                  private int superSuperPrivate;
                  int superSuperPackagePrivate;
              }
              """
          ),
          java(
            """
              package foo.bar;
                            
              class Super extends SuperSuper {
                  public int superPublic;
                  protected int superProtected;
                  private int superPrivate;
                  int superPackagePrivate;
              }
              """
          ),
          java(
            """
              package foo.bar;
                            
              class Test extends Super {
                  boolean classBlock;
              }
              """
          )
        );
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @ParameterizedTest
    @CsvSource(value = {
      "caseA:caseA,methodParam,methodBlockA",
      "caseB:caseB,methodParam,methodBlockA",
      "defaultBlock:defaultBlock,methodParam,methodBlockA"
    }, delimiter = ':')
    void switchStatement(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              class Test {
                  void method(short methodParam) {
                      boolean methodBlockA;
                      switch (methodParam) {
                          case 0:
                              boolean caseA;
                              break;
                          case 1:
                              boolean caseB;
                              break;
                          default:
                              boolean defaultBlock;
                              break;
                      }
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
      "resourceA:resourceA,methodBlockA",
      "tryBlock:tryBlock,methodBlockA,resourceA,resourceB",
      "catchControl:catchControl,methodBlockA,resourceA,resourceB",
      "catchBlock:catchBlock,methodBlockA,resourceA,resourceB,catchControl",
      "finallyBlock:finallyBlock,methodBlockA,resourceA,resourceB"
    }, delimiter = ':')
    void tryCatchFinally(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              import java.io.*;
                            
              class Test {
                  void method() {
                      File methodBlockA = new File("file.txt");
                      try (FileInputStream resourceA = new FileInputStream(methodBlockA); FileInputStream resourceB = new FileInputStream(methodBlockA)) {
                          boolean tryBlock;
                      } catch (RuntimeException | IOException catchControl) {
                          boolean catchBlock;
                      } finally {
                          boolean finallyBlock;
                      }
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
      "whileBlock:whileBlock,methodParam,methodBlockA",
      "doWhileBlock:doWhileBlock,methodParam,methodBlockA"
    }, delimiter = ':')
    void whileLoops(String scope, String result) {
        rewriteRun(
          baseTest(scope, result),
          java(
            """
              import java.io.*;
                            
              class Test {
                  void method(short methodParam) {
                      boolean methodBlockA;
                      while (methodParam < 10) {
                          boolean whileBlock;
                          methodParam++;
                      }
                      do {
                          boolean doWhileBlock;
                          methodParam--;
                      } while (methodParam > 0);
                      boolean methodBlockB;
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void incrementExistingNumberPostFix() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext executionContext) {
                  if (identifier.getSimpleName().equals("name2")) {
                      return identifier.withSimpleName(VariableNameUtils.generateVariableName("name1", getCursor(), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER));
                  }
                  return identifier;
              }
          })).typeValidationOptions(TypeValidation.builder().variableDeclarations(false).identifiers(false).build()),
          java(
            """
              @SuppressWarnings("all")
              class Test {
                  int name = 0;
                  void method(int name1) {
                      int name2 = 0;
                  }
              }
              """,
            """
              @SuppressWarnings("all")
              class Test {
                  int name = 0;
                  void method(int name1) {
                      int name3 = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void conflictsInModifiedContents() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                  lambda = super.visitLambda(lambda, ctx);
                  if (((J.VariableDeclarations) lambda.getParameters().getParameters().get(0)).getVariables().get(0).getSimpleName().startsWith("i")) {
                      J.VariableDeclarations declarations = (J.VariableDeclarations) lambda.getParameters().getParameters().get(0);
                      J.VariableDeclarations.NamedVariable variable = declarations.getVariables().get(0);
                      variable = variable.withName(variable.getName().withSimpleName(VariableNameUtils.generateVariableName("j", new Cursor(getCursor(), lambda), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER)));
                      lambda = lambda.withParameters(lambda.getParameters().withParameters(List.of(declarations.withVariables(List.of(variable)))));
                  }
                  return lambda;
              }
          })).typeValidationOptions(TypeValidation.builder().variableDeclarations(false).identifiers(false).build()),
          java(
            """
              import java.util.function.Consumer;
              
              @SuppressWarnings("all")
              class Test {
                  void m() {
                      Consumer<Integer> c1 = i1 -> {
                          Consumer<Integer> c2 = i2 -> {
                            Consumer<Integer> c3 = i3 -> {
                            };
                          };
                      };
                  }
              }
              """,
            """
              import java.util.function.Consumer;
              
              @SuppressWarnings("all")
              class Test {
                  void m() {
                      Consumer<Integer> c1 = j2 -> {
                          Consumer<Integer> c2 = j1 -> {
                            Consumer<Integer> c3 = j -> {
                            };
                          };
                      };
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @Issue("https://github.com/openrewrite/rewrite/issues/1937")
    @Test
    void generateUniqueNameWithIncrementedNumber() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext executionContext) {
                  if (identifier.getSimpleName().equals("ex")) {
                      return identifier.withSimpleName(VariableNameUtils.generateVariableName("ignored", getCursor(), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER));
                  }
                  return identifier;
              }
          })).typeValidationOptions(TypeValidation.builder().variableDeclarations(false).identifiers(false).build()),
          java(
            """
              @SuppressWarnings("all")
              class Test {
                  int ignored = 0;
                  void method(int ignored1) {
                      int ignored2 = 0;
                      for (int ignored3 = 0; ignored3 < 10; ignored3++) { // scope does not apply.
                          int ignored4 = 0; // scope does not apply.
                      }
                      if (ignored1 > 0) {
                          int ignored5 = 0; // scope does not apply.
                      }
                      try {
                          int ignored6 = 0; // scope does not apply.
                      } catch (Exception ex) {
                      }
                  }
              }
              """,
            """
              @SuppressWarnings("all")
              class Test {
                  int ignored = 0;
                  void method(int ignored1) {
                      int ignored2 = 0;
                      for (int ignored3 = 0; ignored3 < 10; ignored3++) { // scope does not apply.
                          int ignored4 = 0; // scope does not apply.
                      }
                      if (ignored1 > 0) {
                          int ignored5 = 0; // scope does not apply.
                      }
                      try {
                          int ignored6 = 0; // scope does not apply.
                      } catch (Exception ignored3) {
                      }
                  }
              }
              """
          )
        );
    }
}
