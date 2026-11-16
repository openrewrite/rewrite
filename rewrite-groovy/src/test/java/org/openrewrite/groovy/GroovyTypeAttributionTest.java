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

package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessaryPublicModifier", "ConstantConditions", "GrMethodMayBeStatic"})
class GroovyTypeAttributionTest implements RewriteTest {

    @DocumentExample
    @Test
    void typeFromClasspathResources() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("org.junit.jupiter.api.Test", null))
            .parser(GroovyParser.builder()
                .classpathFromResource(new InMemoryExecutionContext(), "junit-jupiter-api")),
          groovy(
                """
            import org.junit.jupiter.api.Test
            class A {
                Test t
            }
            """,
            """
            import org.junit.jupiter.api.Test
            class A {
                /*~~>*/Test t
            }
            """
          )
        );
    }

    @Test
    void defTypeAttributed() {
        rewriteRun(
          groovy(
            """
              class Test {
                  static void test() {
                      def l = new ArrayList()
                  }
              }
              """,
            isAttributed(true)
          )
        );
    }

    @Test
    void defFieldNotTypeAttributed() {
        rewriteRun(
          groovy(
            """
              class Test {
                  def l = new ArrayList()
              }
              """,
            isAttributed(false)
          )
        );
    }

    @Test
    void globalTypeAttributed() {
        rewriteRun(
          groovy(
            """
              def l = new ArrayList()
              """,
            isAttributed(true)
          )
        );
    }

    @Test
    void closureImplicitParameterAttributed() {
        rewriteRun(
          groovy(
                """
              public <T> T register(String name, Class<T> type, @groovy.transform.stc.ClosureParams(groovy.transform.stc.SecondParam.FirstGenericType) Closure<T> configurationAction) {
                  return null
              }
              register("testTask", String) {
                  it.substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var it = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody())
                  .getStatements().getFirst()).getExpression();
                assertThat(TypeUtils.asFullyQualified(it.getSelect().getType()).getFullyQualifiedName()).isEqualTo("java.lang.String");
                assertThat(it.getMethodType().getName()).isEqualTo("substring");
                assertThat(it.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void closureImplicitParameterAttributedZeroArgMethod() {
        rewriteRun(
          groovy(
                """
              public <T> T register(String name, Class<T> type, @groovy.transform.stc.ClosureParams(groovy.transform.stc.SecondParam.FirstGenericType) Closure<T> configurationAction) {
                  return null
              }
              register("testTask", Integer) {
                  it.byteValue()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var it = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody())
                  .getStatements().getFirst()).getExpression();
                assertThat(TypeUtils.asFullyQualified(it.getSelect().getType()).getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                assertThat(it.getMethodType().getName()).isEqualTo("byteValue");
                assertThat(it.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.Integer");
            })
          )
        );
    }

    @Test
    void closureNamedParameterAttributed() {
        rewriteRun(
          groovy(
                """
              public <T> T register(String name, Class<T> type, @groovy.transform.stc.ClosureParams(groovy.transform.stc.SecondParam.FirstGenericType) Closure<T> configurationAction) {
                  return null
              }
              register("testTask", String) { foo ->
                  foo
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var foo = (J.Identifier) ((G.ExpressionStatement) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody()).getStatements().getFirst())
                  .getExpression()).getExpression();
                assertThat(TypeUtils.asFullyQualified(foo.getType()).getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void closureWithDelegate() {
        rewriteRun(
          groovy(
                """
              public String register(@DelegatesTo(String) Closure stringAction) {
                  return null
              }
              register {
                  substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(1);
                assertThat(m.getArguments().getFirst()).isInstanceOf(J.Lambda.class);
                var substring = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().getFirst()).getBody()).getStatements().getFirst()).getExpression();
                assertThat(substring.getMethodType().getName()).isEqualTo("substring");
                assertThat(substring.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @ExpectedToFail
    @SuppressWarnings({"GrPackage", "OptionalGetWithoutIsPresent"})
    @Test
    void infersDelegateViaSimilarGradleApi() {
        rewriteRun(
          groovy(
                """
              package org.gradle.api

              interface Action<T> {
                  void execute(T t);
              }
              void register(Action<String> stringAction) {
              }
              void register(Closure stringAction) {
              }
              register {
                  substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().stream()
                  .filter(s -> s instanceof J.MethodInvocation)
                  .findFirst()
                  .get();
                assertThat(m.getArguments()).hasSize(1);
                assertThat(m.getArguments().getFirst()).isInstanceOf(J.Lambda.class);
                var substring = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().getFirst()).getBody()).getStatements().getFirst()).getExpression();
                assertThat(substring.getMethodType().getName()).isEqualTo("substring");
                assertThat(substring.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void closureParameterTypedByTypeChecker() {
        rewriteRun(
          groovy(
            """
              def upper = ["a"].collect { it.toUpperCase() }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    if ("toUpperCase".equals(method.getSimpleName())) {
                        assertThat(TypeUtils.isOfClassType(method.getSelect().getType(), "java.lang.String")).isTrue();
                    }
                    return super.visitMethodInvocation(method, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void newClassOfSourceDeclaredClassHasClassType() {
        rewriteRun(
          groovy(
            """
              class Foo {}
              def foo = new Foo()
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                    assertThat(TypeUtils.isOfClassType(newClass.getType(), "Foo")).isTrue();
                    assertThat(TypeUtils.isOfClassType(newClass.getConstructorType().getReturnType(), "Foo")).isTrue();
                    return newClass;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void noOverloadGuessedWhenTypeCheckerCannotChoose() {
        rewriteRun(
          groovy(
            """
              def find(String s, def needle) {
                  s.indexOf(needle)
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    assertThat(method.getMethodType()).isNull();
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void groovyMethodOnArrayDeclaredByItsExtensionClass() {
        rewriteRun(
          groovy(
            """
              String[] parts = "a,b".split(",")
              parts.each { println it }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    if ("each".equals(method.getSimpleName())) {
                        assertThat(method.getMethodType().getDeclaringType().getFullyQualifiedName())
                          .isEqualTo("org.codehaus.groovy.runtime.DefaultGroovyMethods");
                    }
                    return super.visitMethodInvocation(method, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void parameterReferenceSharesTheParameterVariableType() {
        rewriteRun(
          groovy(
            """
              class A {
                  int m(String s) {
                      s.length()
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Variable[] declared = new JavaType.Variable[1];
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                        declared[0] = variable.getVariableType();
                        assertThat(variable.getName().getFieldType()).isSameAs(declared[0]);
                        return variable;
                    }

                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if ("s".equals(identifier.getSimpleName()) && getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation) {
                            assertThat(identifier.getFieldType()).isSameAs(declared[0]);
                        }
                        return identifier;
                    }
                }.visit(cu, 0);
                assertThat(TypeUtils.isOfClassType(declared[0].getType(), "java.lang.String")).isTrue();
                assertThat(declared[0].getOwner()).isInstanceOf(JavaType.Method.class);
                assertThat(((JavaType.Method) declared[0].getOwner()).getName()).isEqualTo("m");
            })
          )
        );
    }

    @Test
    void sameNamedLocalsInDifferentMethodsAreDistinctVariables() {
        rewriteRun(
          groovy(
            """
              class A {
                  void a() {
                      def x = 1
                      x
                  }
                  void b() {
                      String x = ""
                      x
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<JavaType.Variable> references = new ArrayList<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if ("x".equals(identifier.getSimpleName()) && getCursor().getParentTreeCursor().getValue() instanceof G.ExpressionStatement) {
                            references.add(identifier.getFieldType());
                        }
                        return identifier;
                    }
                }.visit(cu, 0);
                assertThat(references).hasSize(2);
                assertThat(references.get(0).getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(TypeUtils.isOfClassType(references.get(1).getType(), "java.lang.String")).isTrue();
            })
          )
        );
    }

    @Test
    void everyKindOfLocalDeclarationHasAVariableType() {
        rewriteRun(
          groovy(
            """
              for (String s in ["a"]) {
              }
              try {
              } catch (IOException e) {
              }
              ["a"].each { v -> v }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    assertThat(variable.getVariableType()).as(variable.getSimpleName()).isNotNull();
                    assertThat(variable.getVariableType().getType()).as(variable.getSimpleName()).isNotInstanceOf(JavaType.Unknown.class);
                    return variable;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void propertyAndAttributeAccessHaveTheFieldType() {
        rewriteRun(
          groovy(
            """
              class Foo {
                  String name
              }
              def f = new Foo()
              f.name.length()
              f.@name.length()
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer p) {
                    assertThat(TypeUtils.isOfClassType(fieldAccess.getType(), "java.lang.String")).isTrue();
                    assertThat(TypeUtils.isOfClassType(fieldAccess.getName().getType(), "java.lang.String")).isTrue();
                    assertThat(fieldAccess.getName().getFieldType().getName()).isEqualTo("name");
                    assertThat(TypeUtils.isOfClassType(fieldAccess.getName().getFieldType().getOwner(), "Foo")).isTrue();
                    return fieldAccess;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void importedClassIsTyped() {
        rewriteRun(
          groovy(
            """
              import java.util.concurrent.ConcurrentHashMap
              """,
            spec -> spec.afterRecipe(cu -> {
                J.FieldAccess qualid = cu.getImports().getFirst().getQualid();
                assertThat(TypeUtils.isOfClassType(qualid.getType(), "java.util.concurrent.ConcurrentHashMap")).isTrue();
                assertThat(TypeUtils.isOfClassType(qualid.getName().getType(), "java.util.concurrent.ConcurrentHashMap")).isTrue();
            })
          )
        );
    }

    @Test
    void sameNamedCatchParametersAreDistinctVariables() {
        rewriteRun(
          groovy(
            """
              class A {
                  void m() {
                      try { } catch (IOException e) { }
                      try { } catch (IOException e) { }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<JavaType.Variable> declared = new ArrayList<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                        declared.add(variable.getVariableType());
                        return variable;
                    }
                }.visit(cu, 0);
                assertThat(declared).hasSize(2);
                assertThat(declared.get(0)).isNotSameAs(declared.get(1));
            })
          )
        );
    }

    @Test
    void multiCatchVariableHasTheAlternativesCommonSuperclass() {
        rewriteRun(
          groovy(
            """
              try { } catch (IOException | IllegalStateException e) { }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    assertThat(TypeUtils.isOfClassType(variable.getVariableType().getType(), "java.lang.Exception")).isTrue();
                    return variable;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void quotedPropertyNameHasTheField() {
        rewriteRun(
          groovy(
            """
              class Foo {
                  String name
              }
              def f = new Foo()
              f.'name'.length()
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer p) {
                    assertThat(fieldAccess.getName().getFieldType().getName()).isEqualTo("name");
                    return fieldAccess;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void onlyMethodOfMatchingArityIsChosenForAnUntypedArgument() {
        rewriteRun(
          groovy(
            """
              def first(String s, def i) { s.charAt(i) }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    assertThat(method.getMethodType()).isNotNull();
                    assertThat(method.getMethodType().getParameterTypes()).containsExactly(JavaType.Primitive.Int);
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void explicitlyTypedLocalHasItsDeclaredType() {
        rewriteRun(
          groovy(
            """
              Object o = "a"
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    assertThat(TypeUtils.isOfClassType(variable.getVariableType().getType(), "java.lang.Object")).isTrue();
                    return variable;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void accessorBackedPropertyIsAVariableDistinctFromASameNamedPrivateField() {
        rewriteRun(
          groovy(
            """
              class Folder {
                  private File root
                  File getRoot() { root }
                  void setLabel(String label) {}
              }
              def f = new Folder()
              f.root
              f.label = "a"
              """,
            spec -> spec.afterRecipe(cu -> {
                List<JavaType.Variable> properties = new ArrayList<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer p) {
                        properties.add(fieldAccess.getName().getFieldType());
                        return fieldAccess;
                    }
                }.visit(cu, 0);
                assertThat(properties).satisfiesExactly(
                  root -> {
                      assertThat(root.getName()).isEqualTo("root");
                      assertThat(TypeUtils.isOfClassType(root.getOwner(), "Folder")).isTrue();
                      assertThat(TypeUtils.isOfClassType(root.getType(), "java.io.File")).isTrue();
                      assertThat(root.hasFlags(Flag.Private)).isFalse();
                  },
                  label -> {
                      assertThat(label.getName()).isEqualTo("label");
                      assertThat(TypeUtils.isOfClassType(label.getType(), "java.lang.String")).isTrue();
                  }
                );
            })
          )
        );
    }

    @Test
    void mapPropertyIsAKeyNotAField() {
        rewriteRun(
          groovy(
            """
              def m = new HashMap()
              m.threshold
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer p) {
                    assertThat(fieldAccess.getName().getFieldType()).isNull();
                    return fieldAccess;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void fieldInitializerInScriptIsNotOwnedByTheScript() {
        rewriteRun(
          groovy(
            """
              class B {
                  def c = { String q -> q }
              }
              println 1
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    if ("q".equals(variable.getSimpleName())) {
                        assertThat(variable.getVariableType().getOwner()).isNotInstanceOf(JavaType.Method.class);
                    }
                    return super.visitVariable(variable, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void unaryOperatorsHaveTheirResultType() {
        rewriteRun(
          groovy(
            """
              int i = 0
              i++
              --i
              ~i
              -i
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.Unary visitUnary(J.Unary unary, Integer p) {
                    assertThat(unary.getType()).as(unary.getOperator().toString()).isEqualTo(JavaType.Primitive.Int);
                    return unary;
                }
            }.visit(cu, 0))
          )
        );
    }

    private Consumer<SourceSpec<G.CompilationUnit>> isAttributed(boolean attributed) {
        return spec -> spec.afterRecipe(cu -> new JavaVisitor<Integer>() {
            @Override
            @SuppressWarnings("ConstantConditions")
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                assertThat(TypeUtils.asFullyQualified(variable.getVariableType().getType())
                  .getFullyQualifiedName())
                  .isEqualTo(attributed ? "java.util.ArrayList" : "java.lang.Object");
                return variable;
            }
        }.visit(cu, 0));
    }
}
