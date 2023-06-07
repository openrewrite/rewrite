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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Parameterized;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.TypeUtils.*;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "StatementWithEmptyBody"})
public class JavaParserTypeMappingTest implements JavaTypeMappingTest, RewriteTest {

    @Language("java")
    private final String goat = StringUtils.readFully(JavaParserTypeMappingTest.class.getResourceAsStream("/JavaTypeGoat.java"));

    private final J.CompilationUnit goatCu = JavaParser.fromJavaVersion().build()
      .parse(goat)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

    @Override
    public JavaType.FullyQualified classType(String fqn) {
        var type = new AtomicReference<JavaType.FullyQualified>();
        new JavaVisitor<>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                if (requireNonNull(classDecl.getType()).getFullyQualifiedName().equals(fqn)) {
                    type.set(classDecl.getType());
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, o);
            }
        }.visit(goatCu, 0);
        return type.get();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2445")
    @Test
    void annotationParameterDefaults() {
        rewriteRun(
          java(
            """
              @AnAnnotation
              class Test {
              }
              @interface AnAnnotation {
                  int scalar() default 1;
                  String[] array() default {"a", "b"};
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Class t = asClass(cu.getClasses().get(0).getAllAnnotations().get(0).getType());
                assertThat(t.getMethods().stream().filter(m -> m.getName().equals("scalar"))
                  .map(JavaType.Method::getDefaultValue).map(dv -> dv.get(0))).containsExactly("1");
                assertThat(t.getMethods().stream().filter(m -> m.getName().equals("array"))
                  .map(JavaType.Method::getDefaultValue).flatMap(dv -> dv.stream())).hasSize(2);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    void parameterizedTypesAreDeeplyBasedOnBounds() {
        // The sources intentionally do not import to prevent the import from being processed first.
        rewriteRun(
          java(
            "abstract class TypeA<T extends Number> extends java.util.ArrayList<T> {}",
            spec -> spec.afterRecipe(cu -> {
                Parameterized typeA = asParameterized(cu.getClasses().get(0).getType());
                assertThat(asGeneric(typeA.getTypeParameters().get(0)).toString())
                  .isEqualTo("Generic{T extends java.lang.Number}");
                Parameterized typeASuperType = asParameterized(typeA.getSupertype());
                assertThat(typeASuperType.toString()).isEqualTo("java.util.ArrayList<Generic{T extends java.lang.Number}>");
                assertThat(asClass(typeASuperType.getType()).getTypeParameters().get(0).toString()).isEqualTo("Generic{E}");
            })
          ),
          java(
            """
              class TypeB extends TypeA<Integer> {
                  // Attempt to force the JavaTypeCache to cache the wrong parameterized super type.
                  java.util.List<String> list = new java.util.ArrayList<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Class typeB = asClass(cu.getClasses().get(0).getType());
                assertThat(typeB.getSupertype().toString()).isEqualTo("TypeA<java.lang.Integer>");
                Parameterized typeBSuperType = asParameterized(typeB.getSupertype());
                assertThat(asClass(typeBSuperType.getType()).getTypeParameters().get(0).toString())
                  .isEqualTo("Generic{T extends java.lang.Number}");
            })
          ),
          java(
            """
              class TypeC<T extends String> extends java.util.ArrayList<T> {
                  // Attempt to force the JavaTypeCache to cache the wrong parameterized super type.
                  java.util.List<Object> list = new java.util.ArrayList<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                Parameterized typeC = asParameterized(cu.getClasses().get(0).getType());
                assertThat(asGeneric(typeC.getTypeParameters().get(0)).toString())
                  .isEqualTo("Generic{T extends java.lang.String}");
                Parameterized typeCSuperType = asParameterized(typeC.getSupertype());
                assertThat(typeCSuperType.toString()).isEqualTo("java.util.ArrayList<Generic{T extends java.lang.String}>");
                assertThat(asClass(typeCSuperType.getType()).getTypeParameters().get(0).toString())
                  .isEqualTo("Generic{E}");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1762")
    @Test
    void methodInvocationWithUnknownTypeSymbol() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
              import java.util.stream.Collectors;
                          
              class Test {
                  class Parent {
                  }
                  class Child extends Parent {
                  }
                          
                  List<Parent> method(List<Parent> values) {
                      return values.stream()
                              .map(o -> {
                                  if (o instanceof Child) {
                                      return new UnknownType(((Child) o).toString());
                                  }
                                  return o;
                              })
                              .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1318")
    @Test
    void methodInvocationOnUnknownType() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              // do not import List to create an UnknownType
                          
              class Test {
                  class Base {
                      private int foo;
                      public boolean setFoo(int foo) {
                          this.foo = foo;
                      }
                      public int getFoo() {
                          return foo;
                      }
                  }
                  List<Base> createUnknownType(List<Integer> values) {
                      List<Base> bases = new ArrayList<>();
                      values.forEach((v) -> {
                          Base b = new Base();
                          b.setFoo(v);
                          bases.add(b);
                      });
                      return bases;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2118")
    @Test
    void variousMethodScopeIdentifierTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Binary visitBinary(J.Binary binary, ExecutionContext executionContext) {
                  if (binary.getLeft() instanceof J.Identifier) {
                      J.Identifier left = (J.Identifier) binary.getLeft();
                      if ("i".equals(left.getSimpleName())) {
                          assertThat(left.getFieldType().getType().toString())
                            .isEqualTo("java.lang.Integer");
                      } else if ("m".equals(left.getSimpleName())) {
                          assertThat(left.getFieldType().getType().toString())
                            .isEqualTo("MakeEasyToFind$MultiMap");
                      }
                  }
                  return binary;
              }

              @Override
              public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                  if (variable.getSimpleName().equals("l")) {
                      assertThat(variable.getType().toString()).isEqualTo("java.lang.Long");
                  }
                  return super.visitVariable(variable, executionContext);
              }
          })),
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;
                            
              @SuppressWarningsWarnings("ALL")
              class MakeEasyToFind {
                  void method(List<MultiMap> multiMaps) {
                      List<Integer> ints;
                      ints.forEach(i -> {
                          if (i != null) {
                          }
                      });
                            
                      multiMaps.forEach(m -> {
                          if (m != null) {
                          }
                      });
                            
                      while (true) {
                          if (multiMaps.isEmpty()) {
                              Long l;
                              break;
                          }
                      }
                  }
                            
                  static class MultiMap {
                      List<Inner> inners;
                      public List<Inner> getInners() {
                          return inners;
                      }
                            
                      static class Inner {
                          List<Number> numbers;
                          public List<Number> getNumbers() {
                              return numbers;
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Issue("https://github.com/openrewrite/rewrite/issues/2118")
    @Test
    void multiMapWithSameLambdaParamNames() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
                  J.Identifier param = ((J.VariableDeclarations) lambda.getParameters().getParameters().get(0))
                    .getVariables().get(0).getName();
                  if ("it1".equals(param.getSimpleName())) {
                      assertThat(param.getType().toString()).isEqualTo("MakeEasyToFind$MultiMap");
                  } else if ("it2".equals(param.getSimpleName())) {
                      assertThat(TypeUtils.asParameterized(param.getType()).getTypeParameters().get(0).toString())
                        .isEqualTo("MakeEasyToFind$MultiMap$Inner");
                  }
                  return super.visitLambda(lambda, executionContext);
              }
          })),
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;
              
              @SuppressWarningsWarnings("ALL")
              class MakeEasyToFind {
                  void method(List<MultiMap> multiMaps) {
                      Object obj = multiMaps.stream()
                          .map(it1 -> it1.getInners())
                          .map(it2 -> it2.stream().map(i -> i.getNumbers()))
                          .collect(Collectors.toList());
                  }
              
                  static class MultiMap {
                      List<Inner> inners;
                      public List<Inner> getInners() {
                          return inners;
                      }
              
                      static class Inner {
                          List<Number> numbers;
                          public List<Number> getNumbers() {
                              return numbers;
                          }
                      }
                  }
              }
              """
          )
        );
    }
}
