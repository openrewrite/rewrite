/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class JavaTypeTest implements RewriteTest {

    @Test
    void withFlagsKeepsTheMarkOfAnExtensionMethod() {
        // C# marks an extension method with a bit that isn't one of the flags
        JavaType.Method extension = new JavaType.Method(null, 1L << 20, JavaType.ShallowClass.build("A"), "m",
          JavaType.Primitive.Void, List.of(), List.of(), List.of(), List.of(), null, List.of());
        assertThat(extension.withFlags(Set.of(Flag.Public)).isExtensionMethod()).isTrue();
    }

    @Test
    void recursiveGenericEqualityContract() {
        var first = recursiveGeneric("T", "example.Left");
        var second = recursiveGeneric("T", "example.Left");
        var third = recursiveGeneric("T", "example.Left");

        assertThat(first).isEqualTo(first).isEqualTo(second);
        assertThat(second).isEqualTo(first).isEqualTo(third);
        assertThat(first).isEqualTo(third);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(new HashSet<>(List.of(first))).contains(second);
        assertThat(first).isNotEqualTo(recursiveGeneric("U", "example.Left"));
        assertThat(first).isNotEqualTo(second.withVariance(JavaType.GenericTypeVariable.Variance.CONTRAVARIANT));
        // The recursive edge is equal; a later bound must still be checked.
        assertThat(first).isNotEqualTo(recursiveGeneric("T", "example.Right"));
        assertThat(first).isNotEqualTo(second.withBounds(second.getBounds().subList(0, 1)));
        assertThat(first).isEqualTo(second);
    }

    @Test
    void genericEqualityPreservesEmptyBoundAndVarianceSemantics() {
        for (var variance : JavaType.GenericTypeVariable.Variance.values()) {
            var first = new JavaType.GenericTypeVariable(null, "T", variance, List.of());
            var second = new JavaType.GenericTypeVariable(null, "T", variance, List.of());
            assertThat(first.equals(second)).isEqualTo(variance == JavaType.GenericTypeVariable.Variance.INVARIANT);
            assertThat(first).isEqualTo(first);
            assertThat(first).isNotEqualTo(second.withName("U"));
        }
    }

    @Test
    void genericEqualityComparesClassBoundTypeParameters() {
        var leftBound = JavaType.ShallowClass.build("example.Bound").withTypeParameters(List.of(JavaType.Primitive.Int));
        var rightBound = JavaType.ShallowClass.build("example.Bound").withTypeParameters(List.of(JavaType.Primitive.Long));
        var left = new JavaType.GenericTypeVariable(null, "T",
          JavaType.GenericTypeVariable.Variance.COVARIANT, List.of(leftBound));
        var right = new JavaType.GenericTypeVariable(null, "T",
          JavaType.GenericTypeVariable.Variance.COVARIANT, List.of(rightBound));
        assertThat(left).isNotEqualTo(right);
        assertThat(right).isNotEqualTo(left);
    }

    private static JavaType.GenericTypeVariable recursiveGeneric(String name, String leaf) {
        var variable = new JavaType.GenericTypeVariable(null, name,
          JavaType.GenericTypeVariable.Variance.COVARIANT, List.of());
        var bound = new JavaType.Parameterized(null, JavaType.ShallowClass.build("example.Recursive"), List.of(variable));
        return variable.unsafeSet(name, JavaType.GenericTypeVariable.Variance.COVARIANT,
          List.of(bound, JavaType.ShallowClass.build(leaf)));
    }

    @Test
    void methodOverridesOfGenericParameters() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(java.util.List<Integer> l) {
                      l.add(0);
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                    var matcher = new MethodMatcher("java.util.List add(..)");
                    JavaType.Method methodType = method.getMethodType();
                    assertThat(matcher.matches(methodType)).isTrue();
                    assertThat(methodType.getOverride()).isNotNull();
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void resolvedSignatureOfGenericMethodDeclarations() {
        rewriteRun(
          java(
            """
              import java.util.ListIterator;
              import static java.util.Collections.singletonList;

              interface MyList<E> {
                  ListIterator<E> listIterator();
              }

              class Test {
                  ListIterator<Integer> s = singletonList(1).listIterator();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    assertThat(method.getMethodType().getReturnType()).isInstanceOf(JavaType.Parameterized.class);
                    return method;
                }

                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType returnType = ((J.MethodInvocation) variable.getInitializer()).getMethodType().getReturnType();
                    assertThat(TypeUtils.asFullyQualified(TypeUtils.asParameterized(returnType).getTypeParameters().getFirst())
                      .getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                    return variable;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/762")
    @Test
    void annotationsOnTypeAttribution() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  Consumer<String> c;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    List<JavaType.FullyQualified> annotations = multiVariable.getTypeAsFullyQualified().getAnnotations();
                    assertThat(annotations).hasSize(1);
                    assertThat(annotations.getFirst().getFullyQualifiedName()).isEqualTo("java.lang.FunctionalInterface");
                    return multiVariable;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1267")
    @Test
    void noStackOverflow() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  Map<String, Map<String, Map<Integer, String>>> overflowMap = new HashMap<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Object o) {
                    assertThat(cu.getTypesInUse().getTypesInUse()).isNotEmpty();
                    return super.visitCompilationUnit(cu, o);
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    assertThat(classDecl.getSimpleName()).isEqualTo("A");
                    return classDecl;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2493")
    @Test
    void noNewMethodType() {
        rewriteRun(
          java(
            """
              public class Test {
              }
              """
          ),
          java(
            """
              public class A {
                  void method() {
                      Test a = test(null);
                  }

                  Test test(Test test) {
                      return test;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Object o) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, o);
                    if (m.getName().getPrefix().getWhitespace().isEmpty()) {
                        m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("  ")));
                    }
                    assertThat(m.getName().getType()).isSameAs(m.getMethodType());
                    return m;
                }
            })
          )
        );
    }

    @Issue("https://github.com/apache/maven/pull/2291")
    @Test
    void overrideFromClass() {
        rewriteRun(
          java(
            """
              import java.io.FileNotFoundException;
              class A {
                  @Override
                  public String toString() {
                      return "A";
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(
              ((J.MethodDeclaration)
                cu.getClasses().getFirst()
                  .getBody().getStatements().getFirst())
                .getMethodType().isOverride())
              .isTrue())
          )
        );
    }

    @Test
    void shallowClassOwningClassInference() {
        JavaType.ShallowClass nested = JavaType.ShallowClass.build("java.util.Map.Entry");
        assertThat(nested.getOwningClass()).isNotNull();
        assertThat(nested.getOwningClass().getFullyQualifiedName()).isEqualTo("java.util.Map");

        // Lowercase class names (e.g. Python's builtins.list) are package-qualified, not nested
        JavaType.ShallowClass lowercase = JavaType.ShallowClass.build("builtins.list");
        assertThat(lowercase.getOwningClass()).isNull();
        assertThat(lowercase.getPackageName()).isEqualTo("builtins");
        assertThat(lowercase.getClassName()).isEqualTo("list");

        assertThat(JavaType.ShallowClass.build("Foo").getOwningClass()).isNull();
        assertThat(JavaType.ShallowClass.build("list").getOwningClass()).isNull();

        // Default-package nested names get no owning class: ChangeType's plain-rename
        // path depends on this (see ChangeTypeTest.doesNotModifyInnerClassesIfIgnoreDefinitionTrue)
        assertThat(JavaType.ShallowClass.build("Test.InnerB").getOwningClass()).isNull();
    }
}
