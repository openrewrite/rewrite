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
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.TypeUtils.ComparisonContext.BOUND;
import static org.openrewrite.java.tree.TypeUtils.ComparisonContext.INFER;
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
    void methodWithAnnotationsIsOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  @Deprecated
                  void foo() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    assertThat(TypeUtils.isOfType(method.getMethodType(), method.getMethodType())).isTrue();
                    assertThat(TypeUtils.isOfType(method.getMethodType().withAnnotations(emptyList()), method.getMethodType())).isTrue();
                    assertThat(TypeUtils.isOfType(method.getMethodType(), method.getMethodType().withAnnotations(emptyList()))).isTrue();
                    return method;
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
                  java.util.List<Integer> li;
                  java.util.List<Object> lo;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    J.VariableDeclarations.NamedVariable li = ((J.VariableDeclarations) classDecl.getBody().getStatements().get(0)).getVariables().get(0);
                    J.VariableDeclarations.NamedVariable lo = ((J.VariableDeclarations) classDecl.getBody().getStatements().get(1)).getVariables().get(0);
                    JavaType.Parameterized listIntegerType = ((JavaType.Parameterized) li.getVariableType().getType());
                    JavaType.Parameterized listObjectType = ((JavaType.Parameterized) lo.getVariableType().getType());

                    assertThat(TypeUtils.isAssignableTo(listIntegerType.getType(), listIntegerType)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(listObjectType, listIntegerType)).isFalse();
                    assertThat(TypeUtils.isAssignableTo(listIntegerType, listObjectType)).isFalse();
                    return classDecl;
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
                    JavaType.Parameterized wildcardList = (JavaType.Parameterized) variable.getVariableType().getType();
                    JavaType.FullyQualified rawList = wildcardList.getType();
                    JavaType.Parameterized stringArrayList = (JavaType.Parameterized) variable.getInitializer().getType();
                    assertThat(TypeUtils.isAssignableTo(wildcardList, stringArrayList)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(wildcardList, stringArrayList)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(wildcardList, rawList)).isTrue();
                    return variable;
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
    void isAssignableToGenericTypeVariable1() {
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

    @MinimumJava11
    @Test
    void isAssignableToGenericTypeVariable2() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.List;

              class Test {
                  public <T extends Collection<String>> T test() {
                      return (T) get();
                  }
                  public List<String> get() {
                      return List.of("a", "b", "c");
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    if ("test".equals(method.getSimpleName())) {
                        J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                        J.TypeCast cast = (J.TypeCast) return_.getExpression();
                        assertThat(TypeUtils.isAssignableTo(cast.getType(), cast.getExpression().getType(), BOUND)).isFalse();
                        assertThat(TypeUtils.isAssignableTo(cast.getType(), cast.getExpression().getType(), INFER)).isTrue();
                    }
                    return method;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Test
    void isAssignableToGenericTypeVariable3() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.List;

              import static java.util.Collections.singletonList;

              class Test<T extends Collection<String>> {

                  void consumeClass(T collection) {
                  }

                  <T extends Collection<String>> void consumeMethod(T collection) {
                  }

                  void test() {
                      List<String> list = singletonList("hello");
                      consumeMethod(null);
                      consumeClass(null);
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    if ("test".equals(method.getSimpleName())) {
                        J.Block block = getCursor().getParentTreeCursor().getValue();
                        J.MethodDeclaration consumeClass = (J.MethodDeclaration) block.getStatements().get(0);
                        J.MethodDeclaration consumeMethod = (J.MethodDeclaration) block.getStatements().get(1);
                        J.VariableDeclarations.NamedVariable list = ((J.VariableDeclarations) method.getBody().getStatements().get(0)).getVariables().get(0);
                        JavaType consumeClassParamType = ((J.VariableDeclarations) consumeClass.getParameters().get(0)).getVariables().get(0).getType();
                        JavaType consumeMethodParamType = ((J.VariableDeclarations) consumeMethod.getParameters().get(0)).getVariables().get(0).getType();

                        assertThat(TypeUtils.isAssignableTo(consumeClassParamType, list.getType(), INFER)).isTrue();
                        assertThat(TypeUtils.isAssignableTo(consumeMethodParamType, list.getType(), INFER)).isTrue();
                    }
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
    void arrayIsAssignableToObject() {
        rewriteRun(
          java(
            """
              class Test {
                  Object o;
                  Object[] oa;
                  String[] sa;
                  int[] ia;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    J.VariableDeclarations o = (J.VariableDeclarations) classDecl.getBody().getStatements().get(0);
                    J.VariableDeclarations oa = (J.VariableDeclarations) classDecl.getBody().getStatements().get(1);
                    J.VariableDeclarations sa = (J.VariableDeclarations) classDecl.getBody().getStatements().get(2);
                    J.VariableDeclarations ia = (J.VariableDeclarations) classDecl.getBody().getStatements().get(3);
                    JavaType object = o.getType();
                    JavaType objectArray = oa.getType();
                    JavaType stringArray = sa.getType();
                    JavaType intArray = ia.getType();
                    assertThat(TypeUtils.isAssignableTo(object, objectArray)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(object, stringArray)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(objectArray, stringArray)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(stringArray, objectArray)).isFalse();
                    assertThat(TypeUtils.isAssignableTo(object, intArray)).isTrue();
                    assertThat(TypeUtils.isAssignableTo(objectArray, intArray)).isFalse();
                    return classDecl;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
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
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.String, JavaType.Primitive.Null));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.String, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.String, JavaType.Primitive.String));
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.String, JavaType.Primitive.Null));
    }

    @Test
    void isAssignableToPrimitiveArrays() {
        JavaType.Array intArray = new JavaType.Array(null, JavaType.Primitive.Int, null);
        JavaType.Array longArray = new JavaType.Array(null, JavaType.Primitive.Long, null);
        assertTrue(TypeUtils.isAssignableTo(intArray, intArray));
        assertFalse(TypeUtils.isAssignableTo(longArray, intArray));
        assertFalse(TypeUtils.isAssignableTo(intArray, longArray));
    }

    @Test
    void isAssignableToNonPrimitiveArrays() {
        rewriteRun(
          java(
            """
              class Test {
                  Object[] oa;
                  String[] sa;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    J.VariableDeclarations oa = (J.VariableDeclarations) classDecl.getBody().getStatements().get(0);
                    J.VariableDeclarations sa = (J.VariableDeclarations) classDecl.getBody().getStatements().get(1);
                    JavaType objectArray = oa.getType();
                    JavaType stringArray = sa.getType();
                    assertTrue(TypeUtils.isAssignableTo(objectArray, objectArray));
                    assertFalse(TypeUtils.isAssignableTo(stringArray, objectArray));
                    assertTrue(TypeUtils.isAssignableTo(objectArray, stringArray));
                    return classDecl;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
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

    @Issue("https://github.com/openrewrite/rewrite/issues/4405")
    @Test
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

    @Test
    void typeToString() {
        rewriteRun(
          java(
            """
              import java.io.*;
              import java.util.*;

              @SuppressWarnings("all")
              public class Test<A extends B, B extends Number, C extends Comparable<? super C> & Serializable> {

                  // Plain generics
                  A a;
                  B b;
                  C c;

                  // Parameterized
                  Optional<A> oa;
                  Optional<B> ob;
                  Optional<C> oc;

                  // Wildcards
                  Optional<?> ow;
                  Optional<? extends A> oea;
                  Optional<? extends B> oeb;
                  Optional<? extends C> oec;
                  Optional<? super A> osa;
                  Optional<? super B> osb;
                  Optional<? super C> osc;

                  // === Raw types ===
                  List rawList;
                  Map rawMap;

                  // === Recursive generic ===
                  static class Recursive<T extends Comparable<T>> {}
                  Recursive<Recursive<String>> rec;

                  // === Arrays ===
                  int[] intArray;
                  boolean[] boolArray;
                  String[] stringArray;
                  Map<?, String>[][] wildcardArray;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Object o) {
                    try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                        assertions.toGenericTypeString("A").isEqualTo("A extends B");
                        assertions.toGenericTypeString("B").isEqualTo("B extends java.lang.Number");
                        assertions.toGenericTypeString("C").isEqualTo("C extends java.lang.Comparable<? super C> & java.io.Serializable");

                        assertions.toString("int").isEqualTo("int");
                        assertions.toString("long").isEqualTo("long");
                        assertions.toString("double").isEqualTo("double");
                        assertions.toString("boolean").isEqualTo("boolean");

                        assertions.toString("A").isEqualTo("A");
                        assertions.toString("B").isEqualTo("B");
                        assertions.toString("C").isEqualTo("C");

                        assertions.toString("Optional<A>").isEqualTo("java.util.Optional<A>");
                        assertions.toString("Optional<B>").isEqualTo("java.util.Optional<B>");
                        assertions.toString("Optional<C>").isEqualTo("java.util.Optional<C>");

                        assertions.toString("Optional<?>").isEqualTo("java.util.Optional<?>");
                        assertions.toString("Optional<? extends A>").isEqualTo("java.util.Optional<? extends A>");
                        assertions.toString("Optional<? extends B>").isEqualTo("java.util.Optional<? extends B>");
                        assertions.toString("Optional<? extends C>").isEqualTo("java.util.Optional<? extends C>");
                        assertions.toString("Optional<? super A>").isEqualTo("java.util.Optional<? super A>");
                        assertions.toString("Optional<? super B>").isEqualTo("java.util.Optional<? super B>");
                        assertions.toString("Optional<? super C>").isEqualTo("java.util.Optional<? super C>");

                        assertions.toString("List").isEqualTo("java.util.List");
                        assertions.toString("Map").isEqualTo("java.util.Map");

                        assertions.toString("Recursive<Recursive<String>>").isEqualTo("Test$Recursive<Test$Recursive<java.lang.String>>");

                        assertions.toString("int[]").isEqualTo("int[]");
                        assertions.toString("boolean[]").isEqualTo("boolean[]");
                        assertions.toString("String[]").isEqualTo("java.lang.String[]");
                        assertions.toString("Map<?, String>[][]").isEqualTo("java.util.Map<?, java.lang.String>[][]");
                    }
                    return cu;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @MinimumJava11
    @Test
    void typeToString2() {
        rewriteRun(
          java(
            """
              import java.io.*;
              import java.util.*;

              @SuppressWarnings("all")
              public class Test {
                  void test() {
                      var intersection = (Cloneable & Serializable) null;
                      try {} catch (NullPointerException | IllegalArgumentException exception) {}
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Object o) {
                    try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                        assertions.toString("intersection").isEqualTo("java.lang.Cloneable & java.io.Serializable");
                        assertions.toString("exception").isEqualTo("java.lang.RuntimeException");
                        assertions.toString("NullPointerException | IllegalArgumentException").isEqualTo("java.lang.NullPointerException | java.lang.IllegalArgumentException");
                    }
                    return cu;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5289")
    @Test
    void toStringRecursiveType() {
        rewriteRun(
          java(
            """
              import java.io.*;
              import java.util.*;

              abstract class Rec<T extends Rec<T>> {}

              abstract class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              abstract class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}

              @SuppressWarnings("all")
              public class Test {
                  void run(Rec<?> r, One<?, ?> m) {
                      Optional.of(r).get();
                      Optional.of(m).get();

                      Optional.of(r).ifPresent(sr -> {});
                      Optional.of(m).ifPresent(sm -> {});
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Object o) {
                    try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                        assertions.toString("r").isEqualTo("Rec<?>");
                        assertions.toString("Optional.of(r)").isEqualTo("java.util.Optional<Rec<?>>");
                        assertions.toString("Optional.of(r).get()").isEqualTo("Rec<?>");
                        assertions.toString("sr").isEqualTo("Rec<?>");

                        assertions.toString("m").isEqualTo("One<?, ?>");
                        assertions.toString("Optional.of(m)").isEqualTo("java.util.Optional<One<?, ?>>");
                        assertions.toString("Optional.of(m).get()").isEqualTo("One<?, ?>");
                        assertions.toString("sm").isEqualTo("One<?, ?>");
                    }
                    return cu;
                }
            }.visit(cu, new InMemoryExecutionContext()))
          )
        );
    }

    @SuppressWarnings("rawtypes")
    @Test
    void isOfType() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Map;

              class Test<T extends Number, U extends List<String>, V extends U, X> {
                  Integer integer;
                  int[] intArray;
                  Integer[] integerArray;
                  String[] stringArray;
                  List<String>[] genericArray;
                  Integer[][] nestedArray;
                  T[] tArray;
                  U[] uArray;
                  V[] vArray;
                  X[] xArray;

                  T numberType;
                  U listType;
                  V nestedListType;
                  X generic;

                  List<T> numberList;
                  List<String> listString;
                  Map<String, T> stringToNumberMap;
                  Map<String, X> stringToGenericMap;

                  List<? extends Number> extendsNumberList;
                  List<? super Integer> superIntegerList;

                  Map<String, List<Map<Integer, String>>> complexNested;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Primitive exact matches
                      assertions.isOfType("int", "int").isTrue();
                      assertions.isOfType("int", "Integer").isFalse();
                      assertions.isOfType("Integer", "int").isFalse();

                      // 2. Array matches
                      assertions.isOfType("int[]", "int[]").isTrue();
                      assertions.isOfType("Integer[]", "Integer[]").isTrue();
                      assertions.isOfType("Integer[]", "int[]").isFalse();
                      assertions.isOfType("int[]", "Integer[]").isFalse();
                      assertions.isOfType("Integer[][]", "Integer[][]").isTrue();
                      assertions.isOfType("List<String>[]", "List<String>[]").isTrue();
                      assertions.isOfType("List<String>[]", "String[]").isFalse();
                      assertions.isOfType("int[]", "String[]").isFalse();
                      assertions.isOfType("List<String>[]", "String[]").isFalse();

                      // 3. Generic array matches
                      assertions.isOfType("T[]", "T[]").isTrue();
                      assertions.isOfType("U[]", "U[]").isTrue();
                      assertions.isOfType("T[]", "Integer[]").isFalse();
                      assertions.isOfType("U[]", "List<String>[]").isFalse();
                      assertions.isOfType("Integer[][]", "T[]").isFalse();
                      assertions.isOfType("T[]", "Integer[][]").isFalse();
                      assertions.isOfType("U[]", "Integer[][]").isFalse();
                      assertions.isOfType("U[]", "V[]").isFalse();
                      assertions.isOfType("V[]", "U[]").isFalse();
                      assertions.isOfType("Integer[][]", "int[]").isFalse();

                      // 4. Type variable matches <T extends Number, U extends List<String>, V extends U>
                      assertions.isOfType("T", "T").isTrue();
                      assertions.isOfType("U", "U").isTrue();
                      assertions.isOfType("V", "V").isTrue();
                      assertions.isOfType("T", "Integer").isFalse();
                      assertions.isOfType("T", "Integer").isFalse();
                      assertions.isOfType("U", "V").isFalse();
                      assertions.isOfType("T", "U").isFalse();

                      // 5. Parameterized types
                      assertions.isOfType("List<T>", "List<T>").isTrue();
                      assertions.isOfType("List<? extends Number>", "List<? extends Number>").isTrue();
                      assertions.isOfType("Map<String, List<Map<Integer, String>>>", "Map<String, List<Map<Integer, String>>>").isTrue();
                      assertions.isOfType("List<T>", "List<? extends Number>").isFalse();
                      assertions.isOfType("List<? extends Number>", "List<T>").isFalse();

                      // 6. With INFER mode <T extends Number, U extends List<String>, V extends U>
                      assertions.isOfType("T", "Integer", INFER).isTrue();
                      assertions.isOfType("U", "Integer", INFER).isFalse();
                      assertions.isOfType("U", "List<String>", INFER).isTrue();
                      assertions.isOfType("V", "List<String>", INFER).isTrue();
                      assertions.isOfType("T", "Integer[]", INFER).isFalse();
                      assertions.isOfType("X", "Integer[]", INFER).isTrue();
                      assertions.isOfType("T", "int[]", INFER).isFalse();
                      assertions.isOfType("X", "int[]", INFER).isTrue();
                      assertions.isOfType("T[]", "int[]", INFER).isFalse();
                      assertions.isOfType("X[]", "int[]", INFER).isFalse();
                      assertions.isOfType("T[]", "Integer[]", INFER).isTrue();
                      assertions.isOfType("X[]", "Integer[]", INFER).isTrue();
                      assertions.isOfType("U[]", "List<String>[]", INFER).isTrue();
                      assertions.isOfType("V[]", "List<String>[]", INFER).isTrue();
                      assertions.isOfType("Integer[][]", "T[]", INFER).isFalse();
                      assertions.isOfType("X[]", "Integer[][]", INFER).isTrue();
                      assertions.isOfType("T[]", "Integer[][]", INFER).isFalse();
                      assertions.isOfType("U[]", "V[]", INFER).isTrue();
                      assertions.isOfType("V[]", "U[]", INFER).isTrue();
                      assertions.isOfType("Integer[][]", "int[]", INFER).isFalse();
                      assertions.isOfType("Map<String, T>", "Map<String, List<Map<Integer, String>>>", INFER).isFalse();
                      assertions.isOfType("Map<String, X>", "Map<String, List<Map<Integer, String>>>", INFER).isTrue();
                      assertions.isOfType("Map<String, List<Map<Integer, String>>>", "Map<String, T>", INFER).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isClassAssignableTo() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
              import java.util.ArrayList;
              import java.util.Collection;
              import java.util.List;

              @SuppressWarnings("all")
              class Test<T extends Number & Serializable, U> {
                  Integer integer;
                  Boolean bool;
                  Double bool;
                  Number number;
                  Cloneable cloneable;
                  Serializable serializable;
                  String[] array;

                  Object obj;
                  String str;
                  List listRaw;
                  Collection collectionRaw;
                  ArrayList arrayListRaw;
                  List<String> listString;
                  T genericBounded;
                  U generic;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Boxed from primitives
                      assertions.isAssignableTo("Integer", "int").isTrue();
                      assertions.isAssignableTo("Number", "int").isTrue();
                      assertions.isAssignableTo("Serializable", "int").isTrue();
                      assertions.isAssignableTo("Boolean", "boolean").isTrue();
                      assertions.isAssignableTo("Number", "boolean").isFalse();
                      assertions.isAssignableTo("Serializable", "boolean").isTrue();
                      assertions.isAssignableTo("Double", "double").isTrue();
                      assertions.isAssignableTo("Number", "double").isTrue();
                      assertions.isAssignableTo("Serializable", "double").isTrue();
                      assertions.isAssignableTo("String", "int").isFalse();

                      // FullyQualified direct
                      assertions.isAssignableTo("Object", "String").isTrue();
                      assertions.isAssignableTo("String", "Object").isFalse();
                      assertions.isAssignableTo("List", "String").isFalse();

                      // Null type (assignable to any reference type)
                      assertions.isAssignableTo("String", "null").isTrue();
                      assertions.isAssignableTo("List", "null").isTrue();

                      // Parameterized type to raw type
                      assertions.isAssignableTo("List", "List<String>").isTrue();

                      // Class to interface
                      assertions.isAssignableTo("Serializable", "String").isTrue();
                      assertions.isAssignableTo("Collection", "ArrayList").isTrue();

                      // Interface to class
                      assertions.isAssignableTo("String", "Serializable").isFalse();

                      // Array assignability
                      assertions.isAssignableTo("Object", "String[]").isTrue();
                      assertions.isAssignableTo("Cloneable", "String[]").isTrue();
                      assertions.isAssignableTo("Serializable", "String[]").isTrue();

                      // Generic type <T extends Number & Serializable, U>
                      assertions.isAssignableTo("Serializable", "T").isTrue();
                      assertions.isAssignableTo("Number", "T").isTrue();
                      assertions.isAssignableTo("String", "T").isFalse();
                      assertions.isAssignableTo("Object", "T").isTrue();
                      assertions.isAssignableTo("Number", "U").isFalse();
                      assertions.isAssignableTo("Number", "U").isFalse();
                      assertions.isAssignableTo("Object", "U").isTrue();
                  }
              }
            )
          )
        );
    }

    @SuppressWarnings("rawtypes")
    @Test
    void isParameterizedAssignableTo() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.function.Supplier;

              class Test<T, U extends T, N extends Number, CS extends CharSequence> {
                  ArrayList v1;
                  Comparable<?> v2;
                  Comparable<ImplementsComparable> v3;
                  Comparable<Number> v4;
                  Comparable<String> v5;
                  ComparableSupplier<String, Number> v6;
                  ExtendsComparable v7;
                  List v8;
                  List<? extends CharSequence> v9;
                  List<? extends List<? extends CharSequence>> v10;
                  List<? super String> v11;
                  List<? super CharSequence> v25;
                  List<? super T> v26;
                  List<? super U> v27;
                  List<?> v12;
                  List<CS> v13;
                  List<CharSequence> v14;
                  List<List<? extends CharSequence>> v15;
                  List<List<String>> v16;
                  List<N> v17;
                  List<String> v18;
                  List<T> v19;
                  List<U> v20;
                  MySupplier<Number> v21;
                  Supplier<Number> v22;
                  Supplier<String> v23;
                  ImplementsComparable v24;
                  Map<N, N> mapNN;
                  Map<String, String> mapSS;
                  Map<Integer, Integer> mapII;
                  Map<Long, Integer> mapLI;

                  static abstract class ImplementsComparable implements Comparable<ImplementsComparable> {}
                  static abstract class ExtendsComparable extends ImplementsComparable {}
                  static abstract class MySupplier<T> implements Supplier<T> {}
                  static abstract class ComparableSupplier<T, U> extends MySupplier<U> implements Comparable<T> {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Generic Variance
                      assertions.isAssignableTo("List<? extends CharSequence>", "List<String>").isTrue();
                      assertions.isAssignableTo("List<String>", "List<? extends CharSequence>").isFalse();
                      assertions.isAssignableTo("List<? super String>", "List<CharSequence>").isTrue();

                      // 2. Wildcards and Raw Types
                      assertions.isAssignableTo("List<?>", "List<String>").isTrue();
                      assertions.isAssignableTo("List<?>", "ArrayList").isTrue();
                      assertions.isAssignableTo("List<String>", "List").isFalse(); // We don't allow unsafe assignments
                      assertions.isAssignableTo("List<?>", "List").isTrue(); // Except for wildcards

                      // 3. Type Hierarchy with Generics (String, Number)
                      assertions.isAssignableTo("Comparable<?>", "ImplementsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<ImplementsComparable>", "ImplementsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<ImplementsComparable>", "ExtendsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<?>", "ExtendsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<String>", "ExtendsComparable").isFalse();

                      assertions.isAssignableTo("Comparable<String>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Comparable<Number>", "ComparableSupplier<String, Number>").isFalse();
                      assertions.isAssignableTo("Supplier<Number>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Supplier<String>", "ComparableSupplier<String, Number>").isFalse();
                      assertions.isAssignableTo("MySupplier<Number>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Comparable<?>", "ComparableSupplier<String, Number>").isTrue();

                      // 4. Type Variables
                      assertions.isAssignableTo("List<T>", "List<String>").isFalse();
                      assertions.isAssignableTo("List<T>", "List<U>").isFalse();
                      assertions.isAssignableTo("List<? extends CharSequence>", "List<CS>").isTrue();
                      assertions.isAssignableTo("List<? super U>", "List<? super T>").isTrue();
                      assertions.isAssignableTo("List<? super String>", "List<? super CharSequence>").isTrue();
                      assertions.isAssignableTo("List<? super T>", "List<? super U>").isFalse();
                      assertions.isAssignableTo("List<? super CharSequence>", "List<? super String>").isFalse();

                      // 5. Edge Cases
                      assertions.isAssignableTo("List<? extends List<? extends CharSequence>>", "List<List<String>>").isTrue();
                      assertions.isAssignableTo("List<List<? extends CharSequence>>", "List<List<String>>").isFalse();

                      // 6. Inference Mode
                      assertions.isAssignableTo("List<T>", "List<String>", INFER).isTrue();
                      assertions.isAssignableTo("List<CS>", "List<String>", INFER).isTrue();
                      assertions.isAssignableTo("List<N>", "List<String>", INFER).isFalse();
                      assertions.isAssignableTo("List<? super T>", "List<? super String>", INFER).isTrue();
                      assertions.isAssignableTo("Map<N, N>", "Map<String, String>", INFER).isFalse();
                      assertions.isAssignableTo("Map<N, N>", "Map<Integer, Integer>", INFER).isTrue();
                      assertions.isAssignableTo("Map<N, N>", "Map<Long, Integer>", INFER).isTrue(); // This should be false
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToArray() {
        rewriteRun(
          java(
            """
              class Test<T extends CharSequence, U, V extends Number> {
                  Object[] objectArray;
                  String[] stringArray;
                  CharSequence[] charSequenceArray;
                  int[] intArray;
                  double[] doubleArray;
                  Integer[] integerArray;
                  Double[][] double2DArray;
                  Number[][] number2DArray;
                  Object[][] object2DArray;
                  String[][] string2DArray;
                  T[] genericCsArray;
                  U[] genericArray;
                  V[] genericNumericArray;
                  U generic;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Identity and exact match
                      assertions.isAssignableTo("String[]", "String[]").isTrue();

                      // Covariant assignability of reference types
                      assertions.isAssignableTo("Object[]", "String[]").isTrue();
                      assertions.isAssignableTo("CharSequence[]", "String[]").isTrue();

                      // Reverse should fail
                      assertions.isAssignableTo("String[]", "Object[]").isFalse();
                      assertions.isAssignableTo("String[]", "CharSequence[]").isFalse();

                      // Primitive arrays are not assignable to Object[]
                      assertions.isAssignableTo("Object[]", "int[]").isFalse();
                      assertions.isAssignableTo("Object[]", "Integer[]").isTrue();

                      // Primitive identity
                      assertions.isAssignableTo("int[]", "int[]").isTrue();
                      assertions.isAssignableTo("int[]", "Integer[]").isFalse();
                      assertions.isAssignableTo("Integer[]", "int[]").isFalse();

                      // Different primitives are not assignable
                      assertions.isAssignableTo("int[]", "double[]").isFalse();

                      // 2D array covariance
                      assertions.isAssignableTo("Object[][]", "String[][]").isTrue();
                      assertions.isAssignableTo("Number[][]", "Double[][]").isTrue();
                      assertions.isAssignableTo("Double[][]", "Number[][]").isFalse();

                      // Incompatible inner dimension
                      assertions.isAssignableTo("Number[][]", "Integer[]").isFalse();

                      // Generics: <T extends CharSequence, U>
                      assertions.isAssignableTo("T[]", "String[]").isFalse();
                      assertions.isAssignableTo("T[]", "CharSequence[]").isFalse();
                      assertions.isAssignableTo("Object[]", "T[]").isTrue();
                      assertions.isAssignableTo("CharSequence[]", "T[]").isTrue();
                      assertions.isAssignableTo("U[]", "CharSequence[]").isFalse();
                      assertions.isAssignableTo("Object[]", "U[]").isTrue();

                      // Infer mode: <T extends CharSequence, U>
                      assertions.isAssignableTo("T[]", "String[]", INFER).isTrue();
                      assertions.isAssignableTo("T[]", "CharSequence[]", INFER).isTrue();
                      assertions.isAssignableTo("T[]", "String[][]", INFER).isFalse();

                      assertions.isAssignableTo("U", "String[]", INFER).isTrue();
                      assertions.isAssignableTo("U", "CharSequence[]", INFER).isTrue();
                      assertions.isAssignableTo("U", "String[][]", INFER).isTrue();
                      assertions.isAssignableTo("U", "int[]", INFER).isTrue();
                      assertions.isAssignableTo("U", "double[]", INFER).isTrue();

                      assertions.isAssignableTo("U[]", "String[]", INFER).isTrue();
                      assertions.isAssignableTo("U[]", "CharSequence[]", INFER).isTrue();
                      assertions.isAssignableTo("U[]", "String[][]", INFER).isTrue();
                      assertions.isAssignableTo("U[]", "int[]", INFER).isFalse();
                      assertions.isAssignableTo("V[]", "int[]", INFER).isFalse();
                      assertions.isAssignableTo("V[]", "Integer[]", INFER).isTrue();
                      assertions.isAssignableTo("U[]", "double[]", INFER).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToPrimitive() {
        rewriteRun(
          java(
            """
              class Test<T, U extends Number> {
                  Byte boxedByte;
                  Character boxedChar;
                  Short boxedShort;
                  Integer boxedInt;
                  Long boxedLong;
                  Float boxedFloat;
                  Double boxedDouble;
                  Boolean boxedBoolean;

                  T genericT;
                  U genericU;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Direct primitive assignability
                      assertions.isAssignableTo("int", "byte").isTrue();
                      assertions.isAssignableTo("int", "char").isTrue();
                      assertions.isAssignableTo("int", "short").isTrue();
                      assertions.isAssignableTo("int", "int").isTrue();
                      assertions.isAssignableTo("int", "long").isFalse();
                      assertions.isAssignableTo("float", "int").isTrue();
                      assertions.isAssignableTo("double", "float").isTrue();
                      assertions.isAssignableTo("float", "double").isFalse();

                      // Boolean isn't compatible with numeric types
                      assertions.isAssignableTo("int", "boolean").isFalse();
                      assertions.isAssignableTo("boolean", "boolean").isTrue();
                      assertions.isAssignableTo("boolean", "int").isFalse();

                      // Auto-unboxing
                      assertions.isAssignableTo("int", "Integer").isTrue();
                      assertions.isAssignableTo("double", "Double").isTrue();
                      assertions.isAssignableTo("boolean", "Boolean").isTrue();
                      assertions.isAssignableTo("Integer", "int").isTrue();
                      assertions.isAssignableTo("Double", "double").isTrue();
                      assertions.isAssignableTo("Boolean", "boolean").isTrue();

                      // Mismatched boxed types
                      assertions.isAssignableTo("int", "Boolean").isFalse();
                      assertions.isAssignableTo("boolean", "Integer").isFalse();
                      assertions.isAssignableTo("Boolean", "int").isFalse();
                      assertions.isAssignableTo("Integer", "boolean").isFalse();

                      // Generics <T, U extends Number>
                      assertions.isAssignableTo("T", "byte", INFER).isTrue();
                      assertions.isAssignableTo("T", "short", INFER).isTrue();
                      assertions.isAssignableTo("T", "char", INFER).isTrue();
                      assertions.isAssignableTo("T", "int", INFER).isTrue();
                      assertions.isAssignableTo("T", "long", INFER).isTrue();
                      assertions.isAssignableTo("T", "float", INFER).isTrue();
                      assertions.isAssignableTo("T", "double", INFER).isTrue();
                      assertions.isAssignableTo("T", "boolean", INFER).isTrue();

                      assertions.isAssignableTo("U", "byte", INFER).isTrue();
                      assertions.isAssignableTo("U", "short", INFER).isTrue();
                      assertions.isAssignableTo("U", "int", INFER).isTrue();
                      assertions.isAssignableTo("U", "long", INFER).isTrue();
                      assertions.isAssignableTo("U", "float", INFER).isTrue();
                      assertions.isAssignableTo("U", "double", INFER).isTrue();
                      assertions.isAssignableTo("U", "char", INFER).isFalse();
                      assertions.isAssignableTo("U", "boolean", INFER).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToGenericTypeVariable() {
        rewriteRun(
          java(
            """
              class Test {
                  class A<T, U extends T, V extends U, X> {
                      T t;
                      U u;
                      V v;
                      X x;
                  }

                  class B<T, U extends T, V extends U, X> {
                      T t;
                      U u;
                      V v;
                      X x;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Same variable name
                      assertions.isAssignableTo("T", "T").isTrue();
                      assertions.isAssignableTo("U", "U").isTrue();
                      assertions.isOfType("T", "T").isTrue();

                      // 2. Different variables with compatible bounds
                      // class <T, U extends T, V extends U>
                      assertions.isAssignableTo("T", "U").isTrue(); // U is assignable to T
                      assertions.isAssignableTo("U", "T").isFalse(); // T not assignable to U (U more specific)
                      assertions.isAssignableTo("T", "V").isTrue(); // V -> U -> T
                      assertions.isAssignableTo("U", "V").isTrue(); // V -> U
                      assertions.isAssignableTo("V", "T").isFalse(); // T is more general

                      // 3. Unrelated variables
                      // class <T, X>
                      assertions.isAssignableTo("T", "X").isFalse();
                      assertions.isAssignableTo("X", "T").isFalse();

                      // 4. isOfType tests for completeness
                      assertions.isOfType("T", "T").isTrue();
                      assertions.isOfType("U", "U").isTrue();
                      assertions.isOfType("T", "U").isFalse();
                      assertions.isOfType("U", "T").isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void recursiveTypes() {
        rewriteRun(
          java(
            """
              abstract class Comp implements Comparable<Comp> {}
              abstract class Ext extends Comp {}
              enum EnumType { A, B, C }
              abstract class CompT<T extends CompT<T>> implements Comparable<T> {}
              abstract class ExtT<T> extends CompT<ExtT<T>> {}
              abstract class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              abstract class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              class OneType extends One<TwoType, OneType> {}
              class TwoType extends Two<TwoType, OneType> {}

              class Test<E extends Enum<E>, C extends Comparable<? super C>, T> {
                  E e;
                  C c;
                  T free;
                  Comp comp;
                  Ext ext;
                  EnumType enumType;
                  Comparable<Comp> comparable;
                  CompT<?> compT;
                  CompT<ExtT<Integer>> compExtT;
                  ExtT<Integer> extT;
                  One<?, ?> oneWildcard;
                  Two<?, ?> twoWildcard;
                  OneType oneType;
                  TwoType twoType;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      assertions.isOfType("Comp", "Comp").isTrue();
                      assertions.isOfType("Ext", "Ext").isTrue();
                      assertions.isOfType("EnumType", "EnumType").isTrue();

                      assertions.isOfType("CompT<?>", "CompT<?>").isTrue();
                      assertions.isOfType("CompT<ExtT<Integer>>", "CompT<ExtT<Integer>>").isTrue();
                      assertions.isOfType("ExtT<Integer>", "ExtT<Integer>").isTrue();
                      assertions.isOfType("CompT<ExtT<Integer>>", "ExtT<Integer>").isFalse();

                      assertions.isOfType("OneType", "OneType").isTrue();
                      assertions.isOfType("TwoType", "TwoType").isTrue();

                      assertions.isAssignableTo("E", "EnumType", BOUND).isFalse();
                      assertions.isAssignableTo("E", "EnumType", INFER).isTrue();

                      assertions.isAssignableTo("C", "Comp", BOUND).isFalse();
                      assertions.isAssignableTo("C", "Ext", BOUND).isFalse();

                      assertions.isAssignableTo("C", "Comp", INFER).isTrue();
                      assertions.isAssignableTo("C", "Ext", INFER).isTrue();

                      assertions.isAssignableTo("C", "Comparable<Comp>", BOUND).isFalse();
                      assertions.isAssignableTo("C", "Comparable<Comp>", INFER).isTrue();

                      assertions.isAssignableTo("Comparable<Comp>", "Comp").isTrue();
                      assertions.isAssignableTo("Comparable<Comp>", "Ext").isTrue();

                      assertions.isAssignableTo("CompT<?>", "CompT<ExtT<Integer>>").isTrue();
                      assertions.isAssignableTo("CompT<ExtT<Integer>>", "CompT<ExtT<Integer>>").isTrue();
                      assertions.isAssignableTo("CompT<ExtT<Integer>>", "CompT<?>").isFalse();
                      assertions.isAssignableTo("CompT<?>", "ExtT<Integer>").isTrue();
                      assertions.isAssignableTo("CompT<ExtT<Integer>>", "ExtT<Integer>").isTrue();
                      assertions.isAssignableTo("ExtT<Integer>", "ExtT<Integer>").isTrue();

                      assertions.isAssignableTo("One<?, ?>", "OneType").isTrue();
                      assertions.isAssignableTo("Two<?, ?>", "TwoType").isTrue();
                      assertions.isAssignableTo("OneType", "OneType").isTrue();
                      assertions.isAssignableTo("TwoType", "TwoType").isTrue();
                  }
              }
            )
          )
        );
    }

    @MinimumJava11
    @Test
    void intersectionTypes() {
        rewriteRun(
          java(
            """
              import java.io.*;
              import java.util.*;

              @SuppressWarnings("all")
              public class Test {
                  void test() {
                      var intersection1 = (Cloneable & Serializable) null;
                      var intersection2 = (Serializable & Cloneable) null;
                      Serializable serializable;
                      Cloneable cloneable;
                      int[] arrayPrimitive;
                      DuplicateFormatFlagsException extendIllegal;
                      RuntimeException exception;
                      try {} catch (NullPointerException | IllegalArgumentException exception1) {}
                      try {} catch (IllegalArgumentException | NullPointerException exception2) {}
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      assertions.isOfType("intersection1", "intersection2").isTrue();
                      assertions.isAssignableTo("intersection1", "int[]").isTrue();
                      assertions.isAssignableTo("int[]", "intersection1").isFalse();
                      assertions.isAssignableTo("Serializable", "intersection1").isTrue();
                      assertions.isAssignableTo("Cloneable", "intersection1").isTrue();

                      assertions.isOfType("NullPointerException | IllegalArgumentException", "IllegalArgumentException | NullPointerException").isTrue();
                      assertions.isAssignableTo("NullPointerException | IllegalArgumentException", "DuplicateFormatFlagsException").isTrue();
                      assertions.isAssignableTo("DuplicateFormatFlagsException", "NullPointerException | IllegalArgumentException").isFalse();
                      assertions.isAssignableTo("NullPointerException | IllegalArgumentException", "RuntimeException").isFalse();
                      assertions.isAssignableTo("RuntimeException", "NullPointerException | IllegalArgumentException").isTrue();
                      assertions.isAssignableTo("exception2", "NullPointerException | IllegalArgumentException").isTrue();
                  }
              }
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6140")
    @Test
    void isAssignableToWithCircularTypeReference() {
        // Create a mock type with a circular supertype reference
        JavaType.Class circularType = JavaType.ShallowClass.build("com.example.CircularType");
        JavaType.Class withCircularSupertype = new JavaType.Class(
            null,
            0L,
            "com.example.CircularType",
            JavaType.Class.Kind.Class,
            (JavaType[]) null, // typeParameters
            circularType, // Set supertype to itself - this creates a cycle
            null, // owningClass
            (JavaType.FullyQualified[]) null, // annotations
            (JavaType.FullyQualified[]) null, // interfaces
            (JavaType.Variable[]) null, // members
            (JavaType.Method[]) null  // methods
        );
        
        // This should not cause a StackOverflowError
        assertFalse(TypeUtils.isAssignableTo("java.lang.String", withCircularSupertype));
        // Even though it has itself as supertype, Object match should work
        assertTrue(TypeUtils.isAssignableTo("java.lang.Object", withCircularSupertype));
    }
}
