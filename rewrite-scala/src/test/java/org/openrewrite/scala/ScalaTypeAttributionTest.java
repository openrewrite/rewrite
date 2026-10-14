/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

/**
 * Tests that type attribution is working for Scala constructs.
 */
class ScalaTypeAttributionTest implements RewriteTest {

    @Test
    void valWithTypeAnnotationHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val x: Int = 42
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> identType = new AtomicReference<>();
                AtomicReference<JavaType.Variable> fieldType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                        if ("x".equals(ident.getSimpleName())) {
                            identType.set(ident.getType());
                            fieldType.set(ident.getFieldType());
                        }
                        return super.visitIdentifier(ident, p);
                    }
                }.visit(cu, 0);

                assertThat(identType.get())
                    .as("Identifier 'x' should have type int")
                    .isEqualTo(JavaType.Primitive.Int);

                assertThat(fieldType.get())
                    .as("Identifier 'x' should have a variable type")
                    .isNotNull();
                assertThat(fieldType.get().getName()).isEqualTo("x");
            })
          )
        );
    }

    @Test
    void methodInvocationHasType() {
        rewriteRun(
          scala(
            """
            import java.util.ArrayList

            class MyClass {
              val list = new ArrayList[String]()
              list.add("hello")
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> methodType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        if ("add".equals(method.getSimpleName())) {
                            methodType.set(method.getMethodType());
                        }
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);

                assertThat(methodType.get())
                    .as("Method invocation 'add' should have a method type")
                    .isNotNull();
                assertThat(methodType.get().getName()).isEqualTo("add");
                assertThat(methodType.get().getDeclaringType().getFullyQualifiedName())
                    .isEqualTo("java.util.ArrayList");
            })
          )
        );
    }

    @Test
    void newClassHasType() {
        rewriteRun(
          scala(
            """
            import java.util.ArrayList

            class MyClass {
              val list = new ArrayList[String]()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> newClassType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                        newClassType.set(newClass.getType());
                        return super.visitNewClass(newClass, p);
                    }
                }.visit(cu, 0);

                assertThat(newClassType.get())
                    .as("NewClass should have a type")
                    .isNotNull();
                assertThat(newClassType.get())
                    .isInstanceOf(JavaType.FullyQualified.class);
                assertThat(((JavaType.FullyQualified) newClassType.get()).getFullyQualifiedName())
                    .isEqualTo("java.util.ArrayList");
            })
          )
        );
    }

    @Test
    void fieldAccessHasType() {
        rewriteRun(
          scala(
            """
            class MyClass {
              val s = "hello"
              val len = s.length
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> fieldAccessType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                        if ("length".equals(ident.getSimpleName()) && ident.getType() != null) {
                            fieldAccessType.set(ident.getType());
                        }
                        return super.visitIdentifier(ident, p);
                    }
                }.visit(cu, 0);

                assertThat(fieldAccessType.get())
                    .as("'length' should have a type")
                    .isNotNull();
            })
          )
        );
    }

    @Test
    void classDeclarationHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType> classType = new AtomicReference<>();

                new org.openrewrite.java.JavaIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        classType.set(classDecl.getType());
                        return super.visitClassDeclaration(classDecl, p);
                    }
                }.visit(cu, 0);

                assertThat(classType.get())
                    .as("ClassDeclaration should have a type")
                    .isNotNull();
                assertThat(classType.get())
                    .isInstanceOf(JavaType.Class.class);
                assertThat(((JavaType.Class) classType.get()).getFullyQualifiedName())
                    .isEqualTo("Foo");
            })
          )
        );
    }

    @Test
    void callsInChainHaveTheirOwnMethodType() {
        rewriteRun(
          scala(
            """
            class Foo {
              def f(m: Map[Int, Int], p: Int => Boolean, g: Int => Int) = m.view.filterKeys(p).mapValues(g)
              def h = Map(1 -> 2).mapValues(_ * 2)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(method(cu, "filterKeys").getMethodType().getName()).isEqualTo("filterKeys");
                assertThat(method(cu, "mapValues").getMethodType().getName()).isEqualTo("mapValues");
                J.MethodInvocation mapApply = find(cu, J.MethodInvocation.class,
                  m -> m.getSimpleName().equals("apply") && m.getSelect() instanceof J.Identifier i && i.getSimpleName().equals("Map"));
                assertThat(mapApply.getMethodType().getName()).isEqualTo("apply");
            })
          )
        );
    }

    @Test
    void companionApplyAndInfixCallsHaveMethodType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val xs = List(1)
              val pair = 1 -> 2
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation listApply = method(cu, "apply");
                assertThat(listApply.getMethodType()).isNotNull();
                assertThat(listApply.getMethodType().getName()).isEqualTo("apply");
                assertThat(method(cu, "->").getMethodType()).isNotNull();
                assertThat(method(cu, "->").getMethodType().getDeclaringType().getFullyQualifiedName())
                  .isEqualTo("scala.Predef");
            })
          )
        );
    }

    @Test
    void unqualifiedMethodCallIsNamedAfterTheMethod() {
        rewriteRun(
          scala(
            """
            class Foo {
              def greet(name: String): String = name
              def byBlock(f: => Unit): Unit = f
              def run = {
                greet("x")
                `greet`("y")
                byBlock { println("hi") }
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.MethodInvocation> invocations = findAll(cu, J.MethodInvocation.class, m -> true);
                assertThat(invocations)
                  .extracting(J.MethodInvocation::getSimpleName)
                  .containsExactly("greet", "greet", "byBlock", "println");
                assertThat(invocations).allSatisfy(m -> {
                    assertThat(m.getSelect()).isNull();
                    assertThat(m.getMethodType()).isNotNull();
                    assertThat(m.getMethodType().getName()).isEqualTo(m.getSimpleName());
                    assertThat(m.getName().getType()).isSameAs(m.getMethodType());
                });
            })
          )
        );
    }

    @Test
    void applySugarOnValueKeepsApplyName() {
        rewriteRun(
          scala(
            """
            class Foo {
              val f: Int => Int = _ + 1
              val arr = Array(1, 2)
              def run = {
                f(1)
                arr(0)
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                for (String callee : List.of("f", "arr")) {
                    J.MethodInvocation m = find(cu, J.MethodInvocation.class,
                      i -> i.getSelect() instanceof J.Identifier s && s.getSimpleName().equals(callee));
                    assertThat(m.getSimpleName()).isEqualTo("apply");
                    assertThat(m.getMethodType()).isNotNull();
                    assertThat(m.getMethodType().getName()).isEqualTo("apply");
                }
            })
          )
        );
    }

    @Test
    void namedVariableHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val xs = List(1)
              def f = {
                val n: Int = 1
                n
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                assertThat(TypeUtils.asFullyQualified(xs.getType()).getFullyQualifiedName())
                  .isEqualTo("scala.collection.immutable.List");
                assertThat(xs.getVariableType()).isNotNull();
                J.VariableDeclarations.NamedVariable n = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("n"));
                assertThat(n.getType()).isEqualTo(JavaType.Primitive.Int);
            })
          )
        );
    }

    @Test
    void memberSelectionHasType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val xs = List(1)
              val it = xs.toIterator
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.FieldAccess toIterator = find(cu, J.FieldAccess.class, f -> f.getSimpleName().equals("toIterator"));
                assertThat(TypeUtils.asFullyQualified(toIterator.getType()).getFullyQualifiedName())
                  .isEqualTo("scala.collection.Iterator");
                assertThat(toIterator.getName().getType()).isEqualTo(toIterator.getType());
                assertThat(toIterator.getName().getFieldType()).isNull();
            })
          )
        );
    }

    @Test
    void thisHasEnclosingClassType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val field = 1
              def g = this.field
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.FieldAccess thisField = find(cu, J.FieldAccess.class, f -> f.getSimpleName().equals("field"));
                assertThat(thisField.getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(TypeUtils.asFullyQualified(thisField.getTarget().getType()).getFullyQualifiedName())
                  .isEqualTo("Foo");
            })
          )
        );
    }

    @Test
    void binaryHasResultType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val m = Map(1 -> 2)
              def f(k: Int) = k > 0
              val m2 = m + (3 -> 4, 5 -> 6)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(find(cu, J.Binary.class, b -> b.getOperator() == J.Binary.Type.GreaterThan).getType())
                  .isEqualTo(JavaType.Primitive.Boolean);
                assertThat(TypeUtils.asFullyQualified(find(cu, J.Binary.class, b -> b.getOperator() == J.Binary.Type.Addition).getType())
                  .getFullyQualifiedName())
                  .isEqualTo("scala.collection.immutable.Map");
            })
          )
        );
    }

    @Test
    void lambdaHasFunctionTypeAndParameterIsOwnedByMethod() {
        rewriteRun(
          scala(
            """
            class Foo {
              def f(xs: List[Int], p: Int => Boolean) = xs.map(x => x + 1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Lambda lambda = find(cu, J.Lambda.class, l -> true);
                assertThat(TypeUtils.asFullyQualified(lambda.getType()).getFullyQualifiedName()).isEqualTo("scala.Function1");
                J.VariableDeclarations.NamedVariable p = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("p"));
                assertThat(p.getName().getType()).isNotInstanceOf(JavaType.Variable.class);
                assertThat(TypeUtils.asFullyQualified(p.getName().getType()).getFullyQualifiedName()).isEqualTo("scala.Function1");
                assertThat(p.getVariableType().getOwner()).isInstanceOf(JavaType.Method.class);
                assertThat(((JavaType.Method) p.getVariableType().getOwner()).getName()).isEqualTo("f");
            })
          )
        );
    }

    @Test
    void unresolvedReceiverHasNoMethodType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            class Foo {
              def r = m.retain((k, v) => true)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation retain = method(cu, "retain");
                assertThat(retain.getMethodType()).isNull();
                J.Identifier m = (J.Identifier) retain.getSelect();
                assertThat(m.getFieldType()).isNull();
            })
          )
        );
    }

    @Test
    void unresolvableWildcardImportDoesNotShadowOtherNames() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            import doobie.implicits._

            class Foo {
              val x = List(1, 2).to(Vector)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation to = method(cu, "to");
                assertThat(to.getMethodType()).isNotNull();
                assertThat(to.getMethodType().getDeclaringType().getFullyQualifiedName())
                  .isEqualTo("scala.collection.IterableOnceOps");
                J.VariableDeclarations.NamedVariable x = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("x"));
                assertThat(TypeUtils.asFullyQualified(x.getName().getType()).getFullyQualifiedName())
                  .isEqualTo("scala.collection.immutable.Vector");
            })
          )
        );
    }

    private static J.MethodInvocation method(J cu, String name) {
        return find(cu, J.MethodInvocation.class, m -> m.getSimpleName().equals(name));
    }

    private static <T extends J> T find(J cu, Class<T> type, Predicate<T> matching) {
        return findAll(cu, type, matching).getFirst();
    }

    private static <T extends J> List<T> findAll(J cu, Class<T> type, Predicate<T> matching) {
        List<T> found = new ArrayList<>();
        new ScalaIsoVisitor<List<T>>() {
            @Override
            public @Nullable J preVisit(J tree, List<T> acc) {
                if (type.isInstance(tree) && matching.test(type.cast(tree))) {
                    acc.add(type.cast(tree));
                }
                return tree;
            }
        }.visit(cu, found);
        assertThat(found).as("%s matching in %s", type.getSimpleName(), cu.printTrimmed()).isNotEmpty();
        return found;
    }
}
