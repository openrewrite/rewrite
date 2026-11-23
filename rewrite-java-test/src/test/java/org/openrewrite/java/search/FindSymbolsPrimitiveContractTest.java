/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.SymbolsTable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.PathUtils.separatorsToSystem;
import static org.openrewrite.java.Assertions.java;

class FindSymbolsPrimitiveContractTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindSymbols(null));
    }

    @Test
    void preserveReferenceBoxedGenericAndArrayPolicyWithSiblingRows() {
        String path = separatorsToSystem("src/main/java/sample/ReferencePolicy.java");
        String owner = "sample.ReferencePolicy";
        String sibling = "sample.Sibling";
        rewriteRun(
          spec -> spec.dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
            new SymbolsTable.Row(path, "CLASS", "ReferencePolicy", owner, owner, "PUBLIC"),
            new SymbolsTable.Row(path, "FIELD", "first", owner, " String", "PRIVATE"),
            new SymbolsTable.Row(path, "FIELD", "second", owner, " String", "PRIVATE"),
            new SymbolsTable.Row(path, "METHOD", "references", owner,
              "String references(Integer, Map.Entry, List, Void)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "opaque", owner,
              "Object opaque(?, ?, ?, ?, ?)", "PROTECTED"),
            new SymbolsTable.Row(path, "METHOD", "count", owner, "int count()", "PUBLIC"),
            new SymbolsTable.Row(path, "CLASS", "Sibling", sibling, sibling, "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "FIELD", "ready", sibling, " boolean", "PRIVATE"),
            new SymbolsTable.Row(path, "METHOD", "reset", sibling, "void reset()", "PUBLIC")
          )),
          java(
            """
              package sample;

              public class ReferencePolicy<T> {
                  private String first, second;

                  public String references(Integer boxed, java.util.Map.Entry<String, Long> entry,
                                           java.util.List<String> items, Void empty) {
                      return "";
                  }

                  protected <U> Object opaque(T classValue, U methodValue, int[] ints,
                                              String[][] words, Long... rest) {
                      return null;
                  }

                  public int count() {
                      return 0;
                  }
              }

              class Sibling {
                  private boolean ready;

                  public void reset() {
                  }
              }
              """,
            source -> source.path(path).beforeRecipe(cu -> {
                List<String> attributed = new ArrayList<>();
                new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      List<String> declarations) {
                        JavaType.Method type = method.getMethodType();
                        assertThat(type).as("normally attributed %s", method.getSimpleName()).isNotNull();
                        if ("references".equals(method.getSimpleName())) {
                            assertThat(type.getParameterTypes()).hasSize(4)
                              .allSatisfy(parameter -> assertThat(parameter)
                                .isInstanceOf(JavaType.FullyQualified.class));
                            assertThat(type.getReturnType()).isInstanceOf(JavaType.FullyQualified.class);
                        } else if ("opaque".equals(method.getSimpleName())) {
                            assertThat(type.getParameterTypes()).hasSize(5);
                            assertThat(type.getParameterTypes().get(0))
                              .isInstanceOf(JavaType.GenericTypeVariable.class);
                            assertThat(type.getParameterTypes().get(1))
                              .isInstanceOf(JavaType.GenericTypeVariable.class);
                            assertThat(type.getParameterTypes().subList(2, 5))
                              .allSatisfy(parameter -> assertThat(parameter).isInstanceOf(JavaType.Array.class));
                        } else if ("count".equals(method.getSimpleName())) {
                            assertThat(type.getReturnType()).isEqualTo(JavaType.Primitive.Int);
                        } else if ("reset".equals(method.getSimpleName())) {
                            assertThat(type.getReturnType()).isEqualTo(JavaType.Primitive.Void);
                        }
                        declarations.add(method.getSimpleName());
                        return super.visitMethodDeclaration(method, declarations);
                    }
                }.visit(cu, attributed);
                assertThat(attributed).containsExactlyInAnyOrder("references", "opaque", "count", "reset");
            })
          )
        );
    }

    @Test
    void retainParameterOrderAroundPrimitiveAndBoxedTypes() {
        String path = separatorsToSystem("src/main/java/sample/MixedParameters.java");
        String owner = "sample.MixedParameters";
        rewriteRun(
          spec -> spec.dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
            new SymbolsTable.Row(path, "CLASS", "MixedParameters", owner, owner, "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "CONSTRUCTOR", "MixedParameters", owner,
              "MixedParameters MixedParameters(String, int, Long)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "mix", owner,
              "void mix(String, int, BigDecimal)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "mix", owner,
              "void mix(long, String, Boolean)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "mix", owner,
              "void mix(Integer, String, boolean)", "PUBLIC")
          )),
          java(
            """
              package sample;

              class MixedParameters {
                  public MixedParameters(String before, int value, Long after) {
                  }

                  public void mix(String before, int value, java.math.BigDecimal after) {
                  }

                  public void mix(long value, String middle, Boolean after) {
                  }

                  public void mix(Integer before, String middle, boolean value) {
                  }
              }
              """,
            source -> source.path(path).beforeRecipe(cu -> {
                List<String> primitivePositions = new ArrayList<>();
                new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      List<String> positions) {
                        JavaType.Method type = method.getMethodType();
                        assertThat(type).as("normally attributed %s", method.getSimpleName()).isNotNull();
                        assertThat(type.getParameterTypes()).hasSize(3);
                        int scalarCount = 0;
                        for (int i = 0; i < type.getParameterTypes().size(); i++) {
                            JavaType parameter = type.getParameterTypes().get(i);
                            if (parameter instanceof JavaType.Primitive) {
                                assertThat(parameter).isIn(JavaType.Primitive.Int,
                                  JavaType.Primitive.Long, JavaType.Primitive.Boolean);
                                positions.add(method.getSimpleName() + ":" + i + ":" + parameter);
                                scalarCount++;
                            } else {
                                assertThat(parameter).isInstanceOf(JavaType.FullyQualified.class);
                            }
                        }
                        assertThat(scalarCount).isEqualTo(1);
                        return super.visitMethodDeclaration(method, positions);
                    }
                }.visit(cu, primitivePositions);
                assertThat(primitivePositions).containsExactlyInAnyOrder(
                  "MixedParameters:1:int", "mix:1:int", "mix:0:long", "mix:2:boolean");
            })
          )
        );
    }

    @Test
    void sourcePathSelectsTheExactFileAndRetainsItsCompleteRows() {
        String path = separatorsToSystem("src/main/java/sample/Scoped.java");
        String owner = "sample.Scoped";
        rewriteRun(
          spec -> spec.recipe(new FindSymbols(path))
            .dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
              new SymbolsTable.Row(path, "CLASS", "Scoped", owner, owner, "PUBLIC"),
              new SymbolsTable.Row(path, "FIELD", "label", owner, " String", "PRIVATE"),
              new SymbolsTable.Row(path, "METHOD", "mix", owner, "void mix(int)", "PUBLIC")
            )),
          java(
            """
              package sample;

              public class Scoped {
                  private String label;

                  public void mix(int value) {
                  }
              }
              """,
            source -> source.path(path)
              .beforeRecipe(cu -> assertSinglePrimitiveMethod(cu, JavaType.Primitive.Int))
          ),
          java(
            """
              package other;

              public class Scoped {
                  private long marker;

                  public void mix(boolean value) {
                  }
              }
              """,
            source -> source.path("src/main/java/other/Scoped.java")
              .beforeRecipe(cu -> assertSinglePrimitiveMethod(cu, JavaType.Primitive.Boolean))
          )
        );
    }

    @Test
    void emptySourcePathIncludesEveryFile() {
        String first = separatorsToSystem("src/main/java/sample/First.java");
        String second = separatorsToSystem("src/main/java/sample/Second.java");
        rewriteRun(
          spec -> spec.recipe(new FindSymbols(""))
            .dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
              new SymbolsTable.Row(first, "CLASS", "First", "sample.First", "sample.First", "PUBLIC"),
              new SymbolsTable.Row(first, "METHOD", "visit", "sample.First", "void visit(String)", "PUBLIC"),
              new SymbolsTable.Row(second, "CLASS", "Second", "sample.Second", "sample.Second", "PUBLIC"),
              new SymbolsTable.Row(second, "METHOD", "visit", "sample.Second", "void visit(Integer)", "PUBLIC")
            )),
          java(
            """
              package sample;

              public class First {
                  public void visit(String value) {
                  }
              }
              """,
            source -> source.path(first)
          ),
          java(
            """
              package sample;

              public class Second {
                  public void visit(Integer value) {
                  }
              }
              """,
            source -> source.path(second)
          )
        );
    }

    @Test
    void retainExactlyTheSixPayloadColumns() {
        assertThat(new FindSymbols(null).getDescriptor().getDataTables())
          .filteredOn(table -> SymbolsTable.class.getName().equals(table.getName()))
          .singleElement().satisfies(table -> assertThat(table.getColumns())
            .extracting(ColumnDescriptor::getName)
            .containsExactly("sourcePath", "kind", "name", "parentFqn", "signature", "visibility"));
    }

    private static void assertSinglePrimitiveMethod(J.CompilationUnit cu, JavaType.Primitive parameterType) {
        List<String> methods = new ArrayList<>();
        new JavaIsoVisitor<List<String>>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, List<String> declarations) {
                JavaType.Method type = method.getMethodType();
                assertThat(type).as("normally attributed %s", method.getSimpleName()).isNotNull();
                assertThat(type.getParameterTypes()).containsExactly(parameterType);
                assertThat(type.getReturnType()).isEqualTo(JavaType.Primitive.Void);
                declarations.add(method.getSimpleName());
                return super.visitMethodDeclaration(method, declarations);
            }
        }.visit(cu, methods);
        assertThat(methods).containsExactly("mix");
    }
}
