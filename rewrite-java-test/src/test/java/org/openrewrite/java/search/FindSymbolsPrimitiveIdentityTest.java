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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.SymbolsTable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.PathUtils.separatorsToSystem;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.xml.Assertions.xml;

class FindSymbolsPrimitiveIdentityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindSymbols(null));
    }

    @Test
    void recipeRunRetainsOriginalJavaAndNonJavaReferences() {
        String path = separatorsToSystem("src/main/java/org/example/PrimitiveIdentity.java");
        String siblingPath = separatorsToSystem("src/main/java/org/example/Sibling.java");
        String xmlPath = separatorsToSystem("src/main/resources/control.xml");
        String owner = "org.example.PrimitiveIdentity";
        String sibling = "org.example.Sibling";
        List<SymbolsTable.Row> expected = Arrays.asList(
          new SymbolsTable.Row(path, "CLASS", "PrimitiveIdentity", owner, owner, "PACKAGE_PRIVATE"),
          new SymbolsTable.Row(path, "FIELD", "label", owner, " String", "PRIVATE"),
          new SymbolsTable.Row(path, "CONSTRUCTOR", "PrimitiveIdentity", owner,
            "PrimitiveIdentity PrimitiveIdentity(int)", "PACKAGE_PRIVATE"),
          new SymbolsTable.Row(path, "METHOD", "add", owner, "void add(String, int)", "PUBLIC"),
          new SymbolsTable.Row(path, "METHOD", "add", owner, "void add(String, long)", "PUBLIC"),
          new SymbolsTable.Row(path, "METHOD", "add", owner, "void add(String, double)", "PUBLIC"),
          new SymbolsTable.Row(path, "METHOD", "add", owner, "void add(String, boolean)", "PUBLIC"),
          new SymbolsTable.Row(siblingPath, "CLASS", "Sibling", sibling, sibling, "PACKAGE_PRIVATE"),
          new SymbolsTable.Row(siblingPath, "METHOD", "copy", sibling, "String copy(String)", "PROTECTED")
        );
        rewriteRun(
          spec -> spec.beforeRecipe(sources -> {
              Map<String, Integer> observations = new LinkedHashMap<>();
              // Observe real Recipe.run separately, keeping the normal harness's quality probes enabled.
              RecipeRun observed = new FindSymbols(null).run(new InMemoryLargeSourceSet(sources) {
                  @Override
                  public LargeSourceSet edit(UnaryOperator<SourceFile> map) {
                      return super.edit(before -> {
                          SourceFile after = map.apply(before);
                          assertThat(after).as("original reference for %s", before.getSourcePath()).isSameAs(before);
                          observations.merge(before.getSourcePath().toString(), 1, Integer::sum);
                          return after;
                      });
                  }
              }, new InMemoryExecutionContext(error -> {
                  throw new AssertionError(error);
              }));
              assertThat(observations).containsOnlyKeys(path, siblingPath, xmlPath);
              assertThat(observations.values()).allMatch(count -> count > 0);
              assertThat(observed.getChangeset().getAllResults()).isEmpty();
              try (Stream<SymbolsTable.Row> rows = observed.getDataTableStore().getRows(SymbolsTable.class)) {
                  assertThat(rows.collect(toList())).containsExactlyInAnyOrderElementsOf(expected);
              }
          }).dataTable(SymbolsTable.Row.class,
            rows -> assertThat(rows).containsExactlyInAnyOrderElementsOf(expected)),
          java(
            """
              package org.example;

              class PrimitiveIdentity {
                  private String label;

                  PrimitiveIdentity(int value) {
                      label = "";
                  }

                  public void add(String name, int value) {
                  }

                  public void add(String name, long value) {
                  }

                  public void add(String name, double value) {
                  }

                  public void add(String name, boolean value) {
                  }
              }
              """,
            source -> source.path(path).beforeRecipe(cu -> {
                List<String> declarations = new ArrayList<>();
                new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      List<String> attributed) {
                        JavaType.Method type = method.getMethodType();
                        assertThat(type).as("normally attributed %s", method.getSimpleName()).isNotNull();
                        assertThat(type.getDeclaringType().getFullyQualifiedName()).isEqualTo(owner);
                        if (method.isConstructor()) {
                            assertThat(type.getParameterTypes()).containsExactly(JavaType.Primitive.Int);
                            attributed.add("constructor:int");
                        } else {
                            assertThat(type.getParameterTypes()).hasSize(2);
                            assertThat(TypeUtils.isOfClassType(type.getParameterTypes().get(0),
                              "java.lang.String")).isTrue();
                            assertThat(type.getParameterTypes().get(1)).isIn(
                              JavaType.Primitive.Int, JavaType.Primitive.Long,
                              JavaType.Primitive.Double, JavaType.Primitive.Boolean);
                            assertThat(type.getReturnType()).isEqualTo(JavaType.Primitive.Void);
                            attributed.add("add:" + type.getParameterTypes().get(1));
                        }
                        return super.visitMethodDeclaration(method, attributed);
                    }
                }.visit(cu, declarations);
                assertThat(declarations).containsExactlyInAnyOrder(
                  "constructor:int", "add:int", "add:long", "add:double", "add:boolean");
            })
          ),
          java(
            """
              package org.example;

              class Sibling {
                  protected String copy(String value) {
                      return value;
                  }
              }
              """,
            source -> source.path(siblingPath)
          ),
          xml("<control/>", source -> source.path(xmlPath))
        );
    }
}
