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

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.TypeUtils.TypePosition.Invariant;
import static org.openrewrite.java.tree.TypeUtils.TypePosition.Out;
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
    @MinimumJava11
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
                    if (method.getSimpleName().equals("test")) {
                        J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                        J.TypeCast cast = (J.TypeCast) return_.getExpression();
                        assertThat(TypeUtils.isAssignableTo(cast.getType(), cast.getExpression().getType(), Invariant)).isFalse();
                        assertThat(TypeUtils.isAssignableTo(cast.getType(), cast.getExpression().getType(), Out)).isTrue();
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
                    if (method.getSimpleName().equals("test")) {
                        J.Block block = getCursor().getParentTreeCursor().getValue();
                        J.MethodDeclaration consumeClass = (J.MethodDeclaration) block.getStatements().get(0);
                        J.MethodDeclaration consumeMethod = (J.MethodDeclaration) block.getStatements().get(1);
                        J.VariableDeclarations.NamedVariable list = ((J.VariableDeclarations) method.getBody().getStatements().get(0)).getVariables().get(0);
                        JavaType consumeClassParamType = ((J.VariableDeclarations) consumeClass.getParameters().get(0)).getVariables().get(0).getType();
                        JavaType consumeMethodParamType = ((J.VariableDeclarations) consumeMethod.getParameters().get(0)).getVariables().get(0).getType();

                        assertThat(TypeUtils.isAssignableTo(consumeClassParamType, list.getType(), Out)).isTrue();
                        assertThat(TypeUtils.isAssignableTo(consumeMethodParamType, list.getType(), Out)).isTrue();
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
        EnumSet<JavaType.Primitive> others = EnumSet.complementOf(EnumSet.of(JavaType.Primitive.String));
        for (JavaType.Primitive other : others) {
            assertFalse(TypeUtils.isAssignableTo(JavaType.Primitive.String, other));
        }
        assertTrue(TypeUtils.isAssignableTo(JavaType.Primitive.String, JavaType.Primitive.String));
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

    @Test
    @MinimumJava11
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/5289")
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
}
