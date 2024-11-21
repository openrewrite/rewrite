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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.EnumSet;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
class TypeUtilsTest implements RewriteTest {

    static Consumer<SourceSpec<J.CompilationUnit>> typeIsPresent() {
        return s -> s.afterRecipe(cu -> {
            var fooMethodType = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent();
        });
    }

    @Test
    void isOverrideBasicInterface() {
        rewriteRun(
          java(
            """
              interface Interface {
                  void foo();
              }
              """
          ),
          java(
            """
              class Clazz implements Interface {
                  @Override void foo() { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Test
    void isOverrideBasicInheritance() {
        rewriteRun(
          java(
            """
              class Superclass {
                  void foo() { }
              }
              """
          ),
          java(
            """
              class Clazz extends Superclass {
                  @Override void foo() { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Test
    void isOverrideOnlyVisible() {
        rewriteRun(
          java(
            """
              package foo;
              public class Superclass {
                  void foo() { }
              }
              """
          ),
          java(
            """
              package bar;
              import foo.Superclass;
              class Clazz extends Superclass {
                  public void foo() { }
              }
              """,
            s -> s.afterRecipe(cu -> {
                var fooMethodType = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
                assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1759")
    @Test
    void isOverrideParameterizedInterface() {
        rewriteRun(
          java(
            """
              import java.util.Comparator;

              class TestComparator implements Comparator<String> {
                  @Override public int compare(String o1, String o2) {
                      return 0;
                  }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Test
    void isOverrideParameterizedMethod() {
        rewriteRun(
          java(
            """
              interface Interface {
                  <T> void foo(T t);
              }
              """
          ),
          java(
            """
              class Clazz implements Interface {
                  @Override <T> void foo(T t) { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    void isOverrideConsidersTypeParameterPositions() {
        rewriteRun(
          java(
            """
              interface Interface <T, Y> {
                   void foo(Y y, T t);
              }
              """
          ),
          java(
            """
              class Clazz implements Interface<Integer, String> {
                  void foo(Integer t, String y) { }

                  @Override
                  void foo(String y, Integer t) { }
              }
              """,
            s -> s.afterRecipe(cu -> {
                var methods = cu.getClasses().get(0).getBody().getStatements();
                assertThat(TypeUtils.findOverriddenMethod(((J.MethodDeclaration) methods.get(0)).getMethodType())).isEmpty();
                assertThat(TypeUtils.findOverriddenMethod(((J.MethodDeclaration) methods.get(1)).getMethodType())).isPresent();
            })
          )
        );
    }

    @Test
    void arrayIsFullyQualifiedOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  Integer[][] integer1;
                  Integer[] integer2;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    assertThat(multiVariable.getTypeExpression().getType()).isInstanceOf(JavaType.Array.class);
                    assertThat(TypeUtils.isOfClassType(multiVariable.getTypeExpression().getType(), "java.lang.Integer")).isTrue();
                    return super.visitVariableDeclarations(multiVariable, o);
                }

                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    assertThat(variable.getVariableType().getType()).isInstanceOf(JavaType.Array.class);
                    return super.visitVariable(variable, o);
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isFullyQualifiedOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  Integer integer1;
                  Integer integer2;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    assertThat(variable.getVariableType().getType()).isInstanceOf(JavaType.Class.class);
                    return super.visitVariable(variable, o);
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isParameterizedTypeOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  java.util.List<Integer> integer;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType type = variable.getVariableType().getType();
                    assertThat(type).isInstanceOf(JavaType.Parameterized.class);
                    assertThat(TypeUtils.isAssignableTo("java.util.List", type)).isTrue();
                    assertThat(TypeUtils.isAssignableTo("java.util.List<java.lang.Integer>", type)).isTrue();
                    assertThat(TypeUtils.isAssignableTo("java.util.List<java.lang.String>", type)).isFalse();
                    return super.visitVariable(variable, o);
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isAssignableToWildcard() {
        rewriteRun(
          java(
            """
              class Test {
                  java.util.List<?> l = new java.util.ArrayList<String>();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType type = variable.getVariableType().getType();
                    JavaType exprType = variable.getInitializer().getType();
                    assertThat(type).isInstanceOf(JavaType.Parameterized.class);
                    assertThat(exprType).isInstanceOf(JavaType.Parameterized.class);
                    assertThat(TypeUtils.isAssignableTo(type, exprType)).isTrue();
                    return super.visitVariable(variable, o);
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1857")
    @Test
    void isParameterizedTypeWithShallowClassesOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  java.util.List<Integer> integer1;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType varType = variable.getVariableType().getType();
                    assertThat(varType).isInstanceOf(JavaType.Parameterized.class);
                    var shallowParameterizedType = new JavaType.Parameterized(null, JavaType.ShallowClass.build("java.util.List"),
                      singletonList(JavaType.ShallowClass.build("java.lang.Integer")));
                    assertThat(TypeUtils.isOfType(varType, shallowParameterizedType)).isTrue();
                    return super.visitVariable(variable, o);
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isAssignableToGenericTypeVariable() {
        rewriteRun(
          java(
            """
              import java.util.Map;
              import java.util.function.Supplier;

              class Test {
                  <K, V> void m(Supplier<? extends Map<K, ? extends V>> map) {
                  }
                  void foo() {
                      Map<String, Integer> map = null;
                      m(() -> map);
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Object o) {
                    JavaType paramType = method.getMethodType().getParameterTypes().get(0);
                    assertThat(paramType).isInstanceOf(JavaType.Parameterized.class);
                    JavaType argType = method.getArguments().get(0).getType();
                    assertThat(argType).isInstanceOf(JavaType.Parameterized.class);
                    assertThat(TypeUtils.isAssignableTo(paramType, argType)).isTrue();
                    return method;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isAssignableToLong() {
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Long));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Int));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Double));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Long, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToInt() {
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Long));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Int));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Double));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Int, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToShort() {
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Long));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Int));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Double));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Short, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToChar() {
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Long));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Int));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Char));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Double));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Char, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToByte() {
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Long));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Int));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Short));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Double));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Byte, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToDouble() {
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Long));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Int));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Byte));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Double));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Double, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToFloat() {
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Long));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Int));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Short));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Char));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Byte));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Double));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Float));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Boolean));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.None));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Void));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.String));
        assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Float, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToBoolean() {
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.Boolean));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Boolean, JavaType.Primitive.Boolean));
    }

    @Test
    void isAssignableToNone() {
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.None));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.None, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.None, JavaType.Primitive.None));
    }

    @Test
    void isAssignableToVoid() {
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.Void));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.Void, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.Void, JavaType.Primitive.Void));
    }

    @Test
    void isAssignableToString() {
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.String));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.String, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.String, JavaType.Primitive.String));
    }

    @SuppressWarnings("RedundantCast")
    @Test
    void isAssignableFromIntersection() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;

              class Test {
                  Object o1 = (Serializable & Runnable) null;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType variableType = variable.getVariableType().getType();
                    assertThat(variableType).satisfies(
                      type -> assertThat(type).isInstanceOf(JavaType.Class.class),
                      type -> assertThat(((JavaType.Class) type).getFullyQualifiedName()).isEqualTo("java.lang.Object")
                    );
                    J.TypeCast typeCast = (J.TypeCast) variable.getInitializer();
                    assertThat(typeCast.getType()).satisfies(
                      type -> assertThat(type).isInstanceOf(JavaType.Intersection.class),
                      type -> assertThat(((JavaType.Intersection) type).getBounds()).satisfiesExactly(
                        bound -> assertThat(((JavaType.Class) bound).getFullyQualifiedName()).isEqualTo("java.io.Serializable"),
                        bound -> assertThat(((JavaType.Class) bound).getFullyQualifiedName()).isEqualTo("java.lang.Runnable")
                      ),
                      type -> assertThat(((JavaType.Intersection) type).getBounds()).allSatisfy(
                        bound -> {
                            assertThat(TypeUtils.isAssignableTo(bound, type)).isTrue();
                            assertThat(TypeUtils.isAssignableTo(((JavaType.FullyQualified) bound).getFullyQualifiedName(), type)).isTrue();
                        }
                      ),
                      type -> assertThat(TypeUtils.isAssignableTo(JavaType.ShallowClass.build("java.lang.Object"), type)).isTrue(),
                      type -> assertThat(TypeUtils.isAssignableTo("java.lang.Object", type)).isTrue()
                    );
                    return variable;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4405")
    void isWellFormedType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  assertThat(cu.getTypesInUse().getTypesInUse()).allMatch(TypeUtils::isWellFormedType);
                  return cu;
              }
          })),
          java(
            """
              import java.io.Serializable;

              class Test {
                  static <T extends Serializable &
                          Comparable<T>> T method0() {
                      return null;
                  }

                  static <T extends Serializable> T method1() {
                      return null;
                  }
              }
              """
          )
        );
    }
}
