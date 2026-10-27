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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.SymbolsTable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.PathUtils.separatorsToSystem;
import static org.openrewrite.java.Assertions.java;

class FindSymbolsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindSymbols(null));
    }

    @Test
    void distinguishPrimitiveBuilderOverloads() {
        String path = separatorsToSystem("src/main/java/org/example/NullAwareJsonObjectBuilder.java");
        String owner = "org.example.NullAwareJsonObjectBuilder";
        String builder = "org.example.JsonObjectBuilder";
        rewriteRun(
          spec -> spec.dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
            new SymbolsTable.Row(path, "CLASS", "NullAwareJsonObjectBuilder", owner, owner, "PUBLIC"),
            new SymbolsTable.Row(path, "FIELD", "delegate", owner, " JsonObjectBuilder", "PRIVATE"),
            new SymbolsTable.Row(path, "CONSTRUCTOR", "NullAwareJsonObjectBuilder", owner,
              "NullAwareJsonObjectBuilder NullAwareJsonObjectBuilder()", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "add", owner, "JsonObjectBuilder add(String, int)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "add", owner, "JsonObjectBuilder add(String, long)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "add", owner, "JsonObjectBuilder add(String, double)", "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "add", owner, "JsonObjectBuilder add(String, boolean)", "PUBLIC"),
            new SymbolsTable.Row(path, "INTERFACE", "JsonObjectBuilder", builder, builder, "PACKAGE_PRIVATE"),
            // Preserve the recipe's current treatment of implicit interface visibility.
            new SymbolsTable.Row(path, "METHOD", "add", builder,
              "JsonObjectBuilder add(String, int)", "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "METHOD", "add", builder,
              "JsonObjectBuilder add(String, long)", "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "METHOD", "add", builder,
              "JsonObjectBuilder add(String, double)", "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "METHOD", "add", builder,
              "JsonObjectBuilder add(String, boolean)", "PACKAGE_PRIVATE")
          )),
          java(
            """
              package org.example;

              public class NullAwareJsonObjectBuilder implements JsonObjectBuilder {
                  private final JsonObjectBuilder delegate;

                  public NullAwareJsonObjectBuilder() {
                      delegate = this;
                  }

                  @Override
                  public JsonObjectBuilder add(String name, int value) {
                      return delegate;
                  }

                  @Override
                  public JsonObjectBuilder add(String name, long value) {
                      return delegate;
                  }

                  @Override
                  public JsonObjectBuilder add(String name, double value) {
                      return delegate;
                  }

                  @Override
                  public JsonObjectBuilder add(String name, boolean value) {
                      return delegate;
                  }
              }

              interface JsonObjectBuilder {
                  JsonObjectBuilder add(String name, int value);
                  JsonObjectBuilder add(String name, long value);
                  JsonObjectBuilder add(String name, double value);
                  JsonObjectBuilder add(String name, boolean value);
              }
              """,
            source -> source.path(path).beforeRecipe(cu -> {
                List<String> overloads = new ArrayList<>();
                new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      List<String> attributed) {
                        JavaType.Method type = method.getMethodType();
                        assertThat(type).as("normally attributed %s", method.getSimpleName()).isNotNull();
                        if (!method.isConstructor()) {
                            assertThat(type.getParameterTypes()).hasSize(2);
                            assertThat(TypeUtils.isOfClassType(type.getParameterTypes().get(0),
                              "java.lang.String")).isTrue();
                            assertThat(type.getParameterTypes().get(1)).isIn(
                              JavaType.Primitive.Int, JavaType.Primitive.Long,
                              JavaType.Primitive.Double, JavaType.Primitive.Boolean);
                            assertThat(TypeUtils.isOfClassType(type.getReturnType(), builder)).isTrue();
                            attributed.add(type.getDeclaringType().getFullyQualifiedName() + ":" +
                              type.getParameterTypes().get(1));
                        } else {
                            assertThat(type.getParameterTypes()).isEmpty();
                            assertThat(type.getDeclaringType().getFullyQualifiedName()).isEqualTo(owner);
                        }
                        return super.visitMethodDeclaration(method, attributed);
                    }
                }.visit(cu, overloads);
                assertThat(overloads).containsExactlyInAnyOrder(
                  owner + ":int", owner + ":long", owner + ":double", owner + ":boolean",
                  builder + ":int", builder + ":long", builder + ":double", builder + ":boolean");
            })
          )
        );
    }

    @Test
    void retainEveryScalarPrimitiveInMethodsAndConstructors() {
        String path = separatorsToSystem("src/main/java/org/example/PrimitiveParameters.java");
        String owner = "org.example.PrimitiveParameters";
        rewriteRun(
          spec -> spec.dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
            new SymbolsTable.Row(path, "CLASS", "PrimitiveParameters", owner, owner, "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "CONSTRUCTOR", "PrimitiveParameters", owner,
              "PrimitiveParameters PrimitiveParameters(boolean, byte, char, double, float, int, long, short)",
              "PUBLIC"),
            new SymbolsTable.Row(path, "METHOD", "values", owner,
              "int values(boolean, byte, char, double, float, int, long, short)", "PROTECTED")
          )),
          java(
            """
              package org.example;

              class PrimitiveParameters {
                  public PrimitiveParameters(boolean b, byte by, char c, double d, float f, int i, long l, short s) {
                  }

                  protected int values(boolean b, byte by, char c, double d, float f, int i, long l, short s) {
                      return i;
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
                        assertThat(type.getParameterTypes()).containsExactly(
                          JavaType.Primitive.Boolean, JavaType.Primitive.Byte, JavaType.Primitive.Char,
                          JavaType.Primitive.Double, JavaType.Primitive.Float, JavaType.Primitive.Int,
                          JavaType.Primitive.Long, JavaType.Primitive.Short);
                        if (method.isConstructor()) {
                            assertThat(TypeUtils.isOfClassType(type.getReturnType(), owner)).isTrue();
                        } else {
                            assertThat(type.getReturnType()).isEqualTo(JavaType.Primitive.Int);
                        }
                        attributed.add(method.getSimpleName());
                        return super.visitMethodDeclaration(method, attributed);
                    }
                }.visit(cu, declarations);
                assertThat(declarations).containsExactlyInAnyOrder("PrimitiveParameters", "values");
            })
          )
        );
    }

    @Test
    void distinguishPrimitiveConstructorOverloads() {
        String path = separatorsToSystem("src/main/java/org/example/Overloaded.java");
        String owner = "org.example.Overloaded";
        rewriteRun(
          spec -> spec.dataTable(SymbolsTable.Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
            new SymbolsTable.Row(path, "CLASS", "Overloaded", owner, owner, "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "CONSTRUCTOR", "Overloaded", owner,
              "Overloaded Overloaded(int)", "PACKAGE_PRIVATE"),
            new SymbolsTable.Row(path, "CONSTRUCTOR", "Overloaded", owner,
              "Overloaded Overloaded(long)", "PRIVATE")
          )),
          java(
            """
              package org.example;

              class Overloaded {
                  Overloaded(int value) {
                  }

                  private Overloaded(long value) {
                  }
              }
              """,
            source -> source.path(path).beforeRecipe(cu -> {
                List<String> parameters = new ArrayList<>();
                new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      List<String> attributed) {
                        assertThat(method.isConstructor()).isTrue();
                        JavaType.Method type = method.getMethodType();
                        assertThat(type).as("normally attributed constructor").isNotNull();
                        assertThat(type.getDeclaringType().getFullyQualifiedName()).isEqualTo(owner);
                        assertThat(TypeUtils.isOfClassType(type.getReturnType(), owner)).isTrue();
                        assertThat(type.getParameterTypes()).hasSize(1);
                        assertThat(type.getParameterTypes().get(0)).isIn(
                          JavaType.Primitive.Int, JavaType.Primitive.Long);
                        attributed.add(type.getParameterTypes().get(0).toString());
                        return super.visitMethodDeclaration(method, attributed);
                    }
                }.visit(cu, parameters);
                assertThat(parameters).containsExactlyInAnyOrder("int", "long");
            })
          )
        );
    }
}
