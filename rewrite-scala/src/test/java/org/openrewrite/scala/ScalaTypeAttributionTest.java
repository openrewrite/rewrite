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
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.scala.marker.EtaExpansion;
import org.openrewrite.scala.marker.FunctionApplication;
import org.openrewrite.scala.marker.ImplicitArguments;
import org.openrewrite.scala.marker.ImplicitConversion;
import org.openrewrite.scala.marker.ImplicitImport;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
                  .isEqualTo("scala.Predef$");
            })
          )
        );
    }

    @Test
    void companionApplyHasInstantiatedMethodType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val s = Set(1, 2)
              val xs = List("a")
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.MethodInvocation> applies = findAll(cu, J.MethodInvocation.class, m -> m.getSimpleName().equals("apply"));
                JavaType.Method setApply = applies.get(0).getMethodType();
                JavaType.Method listApply = applies.get(1).getMethodType();
                assertThat(TypeUtils.toString(setApply.getReturnType())).isEqualTo("scala.collection.immutable.Set<int>");
                assertThat(setApply.getParameterTypes()).singleElement()
                  .satisfies(elems -> assertThat(((JavaType.Parameterized) elems).getTypeParameters()).containsExactly(JavaType.Primitive.Int));
                assertThat(TypeUtils.toString(listApply.getReturnType())).isEqualTo("scala.collection.immutable.List<java.lang.String>");
                assertThat(setApply.getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.collection.IterableFactory");
            })
          )
        );
    }

    @Test
    void parameterlessPolymorphicFactoryHasInstantiatedReturnType() {
        rewriteRun(
          scala(
            """
            package p
            import scala.collection.mutable.ListBuffer
            class D {
              val lb = ListBuffer.empty[String]
              val sq = Seq.empty[String]
              val st = Set.empty[String]
              val mb = Map.newBuilder[String, Any]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.MethodInvocation> empties =
                  findAll(cu, J.MethodInvocation.class, m -> m.getSimpleName().equals("empty"));
                JavaType.Method listBufferEmpty = empties.get(0).getMethodType();
                JavaType.Method seqEmpty = empties.get(1).getMethodType();
                JavaType.Method setEmpty = empties.get(2).getMethodType();
                JavaType.Method mapNewBuilder = method(cu, "newBuilder").getMethodType();
                assertThat(TypeUtils.toString(listBufferEmpty.getReturnType()))
                  .isEqualTo("scala.collection.mutable.ListBuffer<java.lang.String>");
                assertThat(TypeUtils.toString(seqEmpty.getReturnType()))
                  .isEqualTo("scala.collection.immutable.Seq<java.lang.String>");
                assertThat(TypeUtils.toString(setEmpty.getReturnType()))
                  .isEqualTo("scala.collection.immutable.Set<java.lang.String>");
                assertThat(TypeUtils.toString(mapNewBuilder.getReturnType()))
                  .isEqualTo("scala.collection.mutable.Builder<scala.Tuple2<java.lang.String, java.lang.Object>, " +
                             "scala.collection.immutable.Map<java.lang.String, java.lang.Object>>");
                assertThat(FindMissingTypes.findMissingTypes(cu, false))
                  .extracting(r -> r.getMessage() + ": " + r.getPrintedTree())
                  .isEmpty();
            })
          )
        );
    }

    @Test
    void inheritedFactoryOfHigherKindedCompanionHasInstantiatedReturnType() {
        rewriteRun(
          scala(
            """
            package p
            abstract class GenericCompanion[+CC[X]] {
              def empty[A]: CC[A] = ???
            }
            abstract class GenMapFactory[CC[A, B]] {
              def newBuilder[A, B]: Iterator[(A, CC[A, B])] = ???
            }
            class Box[A]
            object Box extends GenericCompanion[Box]
            class Pairs[A, B]
            object Pairs extends GenMapFactory[Pairs]
            class D {
              val b = Box.empty[String]
              val p = Pairs.newBuilder[String, Int]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation boxEmpty = method(cu, "empty");
                J.MethodInvocation pairsNewBuilder = method(cu, "newBuilder");
                JavaType.Method emptyType = boxEmpty.getMethodType();
                JavaType.Method newBuilderType = pairsNewBuilder.getMethodType();
                assertThat(emptyType.getDeclaringType().getFullyQualifiedName()).isEqualTo("p.GenericCompanion");
                assertThat(TypeUtils.toString(emptyType.getReturnType())).isEqualTo("p.Box<java.lang.String>");
                assertThat(TypeUtils.toString(newBuilderType.getReturnType()))
                  .isEqualTo("scala.collection.Iterator<scala.Tuple2<java.lang.String, p.Pairs<java.lang.String, int>>>");
                assertThat(TypeUtils.isWellFormedType(emptyType)).isTrue();
                assertThat(TypeUtils.isWellFormedType(newBuilderType)).isTrue();
            })
          )
        );
    }

    @Test
    void declaredResultOfHigherKindedTypeParameterIsWellFormed() {
        rewriteRun(
          scala(
            """
            trait Factory[CC[_]] {
              def empty[A]: CC[A]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration empty = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("empty"));
                JavaType returnType = empty.getMethodType().getReturnType();
                assertThat(returnType).isInstanceOf(JavaType.GenericTypeVariable.class);
                assertThat(((JavaType.GenericTypeVariable) returnType).getName()).isEqualTo("CC");
                assertThat(((JavaType.GenericTypeVariable) returnType).getBounds()).isEmpty();
                assertThat(TypeUtils.isWellFormedType(empty.getMethodType())).isTrue();
            })
          )
        );
    }

    @Test
    void higherKindedTypeParameterIsBoundedByItsApplication() {
        rewriteRun(
          // the parser leaves the bound of a higher-kinded type parameter in its whitespace
          spec -> spec.typeValidationOptions(TypeValidation.builder().allowNonWhitespaceInWhitespace(true).build()),
          scala(
            """
            trait IterableFactory[CC[X] <: Iterable[X]] {
              def from[A](it: Iterable[A]): CC[A]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration from = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("from"));
                JavaType returnType = from.getMethodType().getReturnType();
                assertThat(returnType).isInstanceOf(JavaType.GenericTypeVariable.class);
                assertThat(((JavaType.GenericTypeVariable) returnType).getBounds())
                  .singleElement()
                  .satisfies(bound -> assertThat(TypeUtils.toString(bound)).isEqualTo("scala.collection.Iterable<X>"));
                assertThat(TypeUtils.isWellFormedType(from.getMethodType())).isTrue();
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

    @Test
    void defaultParserHasScalaLibraryOnClasspath() {
        rewriteRun(
          spec -> spec.parser(ScalaParser.builder()),
          scala(
            """
            class Foo {
              val xs = List(1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation listApply = method(cu, "apply");
                assertThat(listApply.getMethodType()).isNotNull();
                assertThat(listApply.getMethodType().getDeclaringType().getFullyQualifiedName())
                  .startsWith("scala.collection");
            })
          )
        );
    }

    @Test
    void scalaLibraryIsOnClasspathWhenOnlyNonScalaJarsAreGiven() {
        rewriteRun(
          spec -> spec.parser(ScalaParser.builder().classpath(JavaParser.dependenciesFromClasspath("assertj-core"))),
          scala(
            """
            import org.assertj.core.api.Assertions

            class Foo {
              val xs = List(1)
              val a = Assertions.assertThat(1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation listApply = method(cu, "apply");
                assertThat(listApply.getMethodType()).isNotNull();
                assertThat(listApply.getMethodType().getDeclaringType().getFullyQualifiedName())
                  .startsWith("scala.collection");
                J.MethodInvocation assertThatCall = method(cu, "assertThat");
                assertThat(assertThatCall.getMethodType()).isNotNull();
                assertThat(assertThatCall.getMethodType().getDeclaringType().getFullyQualifiedName())
                  .isEqualTo("org.assertj.core.api.Assertions");
            })
          )
        );
    }

    @Test
    void implicitlyConvertedExpressionKeepsItsOwnTypeAndIsMarked() {
        rewriteRun(
          scala(
            """
            import scala.language.implicitConversions

            class Foo {
              implicit def intToStr(i: Int): String = i.toString
              val n = 1
              val s: String = n
              val k = "12".toInt
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier n = find(cu, J.Identifier.class, i -> i.getSimpleName().equals("n") && i.getMarkers().findFirst(ImplicitConversion.class).isPresent());
                assertThat(n.getType()).isEqualTo(JavaType.Primitive.Int);
                ImplicitConversion intToStr = n.getMarkers().findFirst(ImplicitConversion.class).orElseThrow();
                assertThat(intToStr.getConversion().getName()).isEqualTo("intToStr");
                assertThat(TypeUtils.isString(intToStr.getType())).isTrue();

                J.Literal twelve = find(cu, J.Literal.class, l -> "12".equals(l.getValue()));
                assertThat(TypeUtils.isString(twelve.getType())).isTrue();
                ImplicitConversion augmentString = twelve.getMarkers().findFirst(ImplicitConversion.class).orElseThrow();
                assertThat(augmentString.getConversion().getName()).isEqualTo("augmentString");
                assertThat(TypeUtils.asFullyQualified(augmentString.getType()).getFullyQualifiedName())
                  .isEqualTo("scala.collection.StringOps");
            })
          )
        );
    }

    @Test
    void importsAreAttributedWithTheImportedDefinition() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            import scala.concurrent.ExecutionContext.Implicits.global
            import scala.concurrent.Future
            import scala.math.max
            import akka.actor.ActorSystem
            import scala.jdk.CollectionConverters._
            import Defaults.v
            import Defaults.iv
            import Defaults.IO
            import Defaults.{helper => h, iv => i2}

            object Defaults {
              def helper: Int = 1
              val v: Int = 2
              implicit val iv: String = "s"
              implicit object IO
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Import global = importOf(cu, "global");
                assertThat(global.getMarkers().findFirst(ImplicitImport.class)).isPresent();
                JavaType.Method globalMethod = (JavaType.Method) global.getQualid().getType();
                assertThat(globalMethod.getName()).isEqualTo("global");
                assertThat(globalMethod.getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.concurrent.ExecutionContext$Implicits$");

                J.Import future = importOf(cu, "Future");
                assertThat(TypeUtils.asFullyQualified(future.getQualid().getType()).getFullyQualifiedName()).isEqualTo("scala.concurrent.Future");
                assertThat(future.getQualid().getName().getFieldType().getName()).isEqualTo("Future");
                assertThat(future.getMarkers().findFirst(ImplicitImport.class)).isEmpty();

                JavaType.Method max = (JavaType.Method) importOf(cu, "max").getQualid().getType();
                assertThat(max.getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.math.package$");

                assertThat(importOf(cu, "ActorSystem").getQualid().getType()).isInstanceOf(JavaType.Unknown.class);
                assertThat(importOf(cu, "_").getQualid().getType()).isNull();

                J.Import v = importOf(cu, "v");
                JavaType.Variable vVariable = (JavaType.Variable) v.getQualid().getType();
                assertThat(vVariable.getName()).isEqualTo("v");
                assertThat(TypeUtils.asFullyQualified(vVariable.getOwner()).getFullyQualifiedName()).isEqualTo("Defaults$");
                assertThat(v.getQualid().getName().getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(v.getQualid().getName().getFieldType()).isEqualTo(vVariable);
                assertThat(v.getMarkers().findFirst(ImplicitImport.class)).isEmpty();

                J.Import iv = importOf(cu, "iv");
                assertThat(iv.getQualid().getType()).isInstanceOf(JavaType.Variable.class);
                assertThat(iv.getMarkers().findFirst(ImplicitImport.class)).isPresent();

                J.Import io = importOf(cu, "IO");
                assertThat(TypeUtils.asFullyQualified(io.getQualid().getType()).getFullyQualifiedName()).isEqualTo("Defaults$IO$");
                assertThat(io.getQualid().getName().getFieldType().getName()).isEqualTo("IO");
                assertThat(io.getMarkers().findFirst(ImplicitImport.class)).isPresent();

                S.ImportSelector helper = find(cu, S.ImportSelector.class, sel -> sel.getName() != null && sel.getName().getSimpleName().equals("helper"));
                assertThat(((JavaType.Method) helper.getName().getType()).getName()).isEqualTo("helper");
                assertThat(helper.getAlias().getType()).isEqualTo(helper.getName().getType());
                assertThat(helper.getMarkers().findFirst(ImplicitImport.class)).isEmpty();
                S.ImportSelector i2 = find(cu, S.ImportSelector.class, sel -> sel.getName() != null && sel.getName().getSimpleName().equals("iv"));
                assertThat(i2.getName().getFieldType().getName()).isEqualTo("iv");
                assertThat(i2.getMarkers().findFirst(ImplicitImport.class)).isPresent();
            })
          )
        );
    }

    @Test
    void implicitArgumentsPassedByTheCompilerAreMarked() {
        rewriteRun(
          scala(
            """
            import scala.concurrent.ExecutionContext
            import scala.concurrent.ExecutionContext.Implicits.global
            import scala.concurrent.Future

            class Foo {
              val f = Future { 1 }
              val explicit = Future(2)(ExecutionContext.global)
              val sorted = List((1, "a")).sorted
              def g = for { a <- f } yield a
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                ImplicitArguments future = application(cu, "Future").getMarkers().findFirst(ImplicitArguments.class).orElseThrow();
                assertThat(future.getArguments()).singleElement().satisfies(arg -> {
                    JavaType.Method global = (JavaType.Method) arg;
                    assertThat(global.getName()).isEqualTo("global");
                    assertThat(global.getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.concurrent.ExecutionContext$Implicits$");
                });

                J.MethodInvocation explicit = find(cu, J.MethodInvocation.class, m -> m.getArguments().getFirst() instanceof J.Literal l && Integer.valueOf(2).equals(l.getValue()));
                assertThat(explicit.getMarkers().findFirst(ImplicitArguments.class)).isEmpty();

                J.FieldAccess sorted = find(cu, J.FieldAccess.class, fa -> fa.getSimpleName().equals("sorted"));
                assertThat(sorted.getMarkers().findFirst(ImplicitArguments.class).orElseThrow().getArguments())
                  .extracting(arg -> arg instanceof JavaType.Method m ? m.getName() : ((JavaType.Variable) arg).getName())
                  .containsExactly("Tuple2", "Int", "String");

                S.For forYield = find(cu, S.For.class, l -> true);
                assertThat(forYield.getMarkers().findFirst(ImplicitArguments.class).orElseThrow().getArguments())
                  .singleElement().extracting(arg -> ((JavaType.Method) arg).getName()).isEqualTo("global");
            })
          )
        );
    }

    @Test
    void importedGivenPassedToUsingClauseIsMarked() {
        rewriteRun(
          scala(
            """
            object Defaults {
              given name: String = "x"
            }

            class Foo {
              def greet(using s: String): String = s
              def implied = {
                import Defaults.name
                greet
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier greet = find(cu, J.Identifier.class, i -> i.getSimpleName().equals("greet") && i.getMarkers().findFirst(ImplicitArguments.class).isPresent());
                assertThat(greet.getMarkers().findFirst(ImplicitArguments.class).orElseThrow().getArguments()).singleElement().satisfies(arg -> {
                    JavaType.Variable name = (JavaType.Variable) arg;
                    assertThat(name.getName()).isEqualTo("name");
                    assertThat(TypeUtils.asFullyQualified(name.getOwner()).getFullyQualifiedName()).isEqualTo("Defaults$");
                });
                assertThat(importOf(cu, "name").getMarkers().findFirst(ImplicitImport.class)).isPresent();
            })
          )
        );
    }

    @Test
    void conversionGivenAsInstanceIsMarked() {
        rewriteRun(
          scala(
            """
            class Foo {
              given Conversion[Double, String] = _.toString
              val s: String = 1.5
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Literal literal = find(cu, J.Literal.class, l -> Double.valueOf(1.5).equals(l.getValue()));
                assertThat(literal.getType()).isEqualTo(JavaType.Primitive.Double);
                ImplicitConversion conversion = literal.getMarkers().findFirst(ImplicitConversion.class).orElseThrow();
                assertThat(conversion.getConversion().getName()).isEqualTo("apply");
                assertThat(conversion.getConversion().getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.Conversion");
                assertThat(TypeUtils.isString(conversion.getType())).isTrue();
            })
          )
        );
    }

    @Test
    void explicitCallOfConversionMethodIsNotMarked() {
        rewriteRun(
          scala(
            """
            class Foo {
              implicit def intToStr(i: Int): String = i.toString
              val s = intToStr(5)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Literal five = find(cu, J.Literal.class, l -> Integer.valueOf(5).equals(l.getValue()));
                assertThat(five.getMarkers().findFirst(ImplicitConversion.class)).isEmpty();
                assertThat(application(cu, "intToStr").getMarkers().findFirst(ImplicitConversion.class)).isEmpty();
            })
          )
        );
    }

    @Test
    void etaExpandedMethodReferenceIsMarked() {
        rewriteRun(
          scala(
            """
            class Foo {
              def inc(i: Int): Int = i + 1
              val xs = List(1).map(inc)
              val ys = List(1).map(this.inc)
              val zs = List(1).map(x => inc(x))
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier inc = find(cu, J.Identifier.class, i -> i.getSimpleName().equals("inc") && i.getMarkers().findFirst(EtaExpansion.class).isPresent());
                assertThat(inc.getType()).isEqualTo(JavaType.Primitive.Int);
                EtaExpansion eta = inc.getMarkers().findFirst(EtaExpansion.class).orElseThrow();
                assertThat(eta.getMethod().getName()).isEqualTo("inc");
                JavaType.Parameterized function = TypeUtils.asParameterized(eta.getType());
                assertThat(function.getFullyQualifiedName()).isEqualTo("scala.Function1");
                assertThat(function.getTypeParameters()).containsExactly(JavaType.Primitive.Int, JavaType.Primitive.Int);

                J.FieldAccess thisInc = find(cu, J.FieldAccess.class, f -> f.getSimpleName().equals("inc"));
                assertThat(thisInc.getMarkers().findFirst(EtaExpansion.class)).isPresent();

                J.MethodInvocation call = application(cu, "inc");
                assertThat(call.getMarkers().findFirst(EtaExpansion.class)).isEmpty();
                assertThat(call.getName().getMarkers().findFirst(EtaExpansion.class)).isEmpty();
            })
          )
        );
    }

    @Test
    void etaExpandedInitializerOfInferredValIsMarked() {
        rewriteRun(
          scala(
            """
            class Foo {
              def inc(i: Int): Int = i + 1
              val f = inc
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier inc = findAll(cu, J.Identifier.class, i -> i.getSimpleName().equals("inc")).getLast();
                assertThat(inc.getType()).isEqualTo(JavaType.Primitive.Int);
                EtaExpansion eta = inc.getMarkers().findFirst(EtaExpansion.class).orElseThrow();
                assertThat(eta.getMethod().getName()).isEqualTo("inc");
                assertThat(TypeUtils.asParameterized(eta.getType()).getFullyQualifiedName()).isEqualTo("scala.Function1");
            })
          )
        );
    }

    @Test
    void explicitEtaExpansionHasFunctionType() {
        rewriteRun(
          scala(
            """
            class Foo {
              def inc(i: Int): Int = i + 1
              val f = inc _
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MemberReference reference = find(cu, J.MemberReference.class, r -> true);
                assertThat(TypeUtils.asFullyQualified(reference.getType()).getFullyQualifiedName()).isEqualTo("scala.Function1");
            })
          )
        );
    }

    @Test
    void illTypedCallsHaveNoMethodType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            class Person(name: String)

            class Foo {
              def g(s: String): Int = 1
              val wrongArgument = g(true)
              val wrongArity = g("a", "b")
              val wrongConstructorArgument = new Person(1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation wrongArgument = find(cu, J.MethodInvocation.class,
                  m -> isApplicationOf(m, "g") && m.getArguments().size() == 1);
                assertThat(wrongArgument.getMethodType()).isNull();
                assertThat(wrongArgument.getType()).isNull();
                J.MethodInvocation wrongArity = find(cu, J.MethodInvocation.class,
                  m -> isApplicationOf(m, "g") && m.getArguments().size() == 2);
                assertThat(wrongArity.getMethodType()).isNull();
                J.NewClass newPerson = find(cu, J.NewClass.class, n -> true);
                assertThat(newPerson.getConstructorType()).isNull();
            })
          )
        );
    }

    @Test
    void callsWithNonConformingTypeArgumentsHaveNoMethodType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            class Foo {
              def num[T <: Number](t: T): T = t
              val xs = List(1, 2)
              val wrongKind = xs.to[Vector]
              val outOfBounds = num[String]("a")
              val conforming = num[Integer](1)
              val inferred = List.empty[Int]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation wrongKind = method(cu, "to");
                List<J.MethodInvocation> ofNum = findAll(cu, J.MethodInvocation.class, m -> m.getSimpleName().equals("num"));
                assertThat(wrongKind.getMethodType()).isNull();
                assertThat(wrongKind.getType()).isNull();
                assertThat(ofNum).hasSize(2);
                assertThat(ofNum.getFirst().getMethodType()).isNull();
                assertThat(ofNum.getLast().getMethodType()).isNotNull()
                  .satisfies(m -> assertThat(m.getName()).isEqualTo("num"));
                assertThat(method(cu, "empty").getMethodType()).isNotNull()
                  .satisfies(m -> assertThat(m.getName()).isEqualTo("empty"));
            })
          )
        );
    }

    @Test
    void illTypedCallHasSameShapeAsWellTypedCall() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            class Foo {
              def g(s: String): Int = 1
              def k: Int => Int = i => i
              val h: Int => Int = i => i
              val wellTyped = g("a")
              val wrongArgument = g(true)
              val wrongArity = g("a", "b")
              val functionValue = h(true)
              val parameterlessMethod = k(true)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.MethodInvocation> calls = new org.openrewrite.java.JavaIsoVisitor<List<J.MethodInvocation>>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<J.MethodInvocation> found) {
                        found.add(method);
                        return super.visitMethodInvocation(method, found);
                    }
                }.reduce(cu, new ArrayList<>());

                List<J.MethodInvocation> ofG = calls.stream().filter(m -> m.getSimpleName().equals("g")).toList();

                assertThat(ofG).hasSize(3).allSatisfy(m -> {
                    assertThat(m.getSelect()).isNull();
                    assertThat(m.getMarkers().findFirst(FunctionApplication.class)).isEmpty();
                });
                assertThat(calls).filteredOn(m -> m.getSimpleName().equals("apply"))
                  .extracting(m -> ((J.Identifier) m.getSelect()).getSimpleName())
                  .containsExactlyInAnyOrder("h", "k");
            })
          )
        );
    }

    @Test
    void callWithUnresolvedArgumentKeepsMethodType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none()),
          scala(
            """
            class Foo {
              def g(s: String): Int = 1
              val x = g(missing.Thing.value)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation g = application(cu, "g");
                assertThat(g.getMethodType()).isNotNull();
                assertThat(g.getMethodType().getName()).isEqualTo("g");
            })
          )
        );
    }

    @Test
    void referenceToOverloadedMethodHasTypeOfResolvedAlternative() {
        rewriteRun(
          scala(
            """
            import scala.math.max

            class Foo {
              def f = {
                println("hello")
                max(1, 2)
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation println = application(cu, "println");
                assertThat(println.getMethodType().getName()).isEqualTo("println");
                assertThat(println.getType()).isEqualTo(JavaType.Primitive.Void);
                J.MethodInvocation max = application(cu, "max");
                assertThat(max.getMethodType().getParameterTypes()).containsExactly(JavaType.Primitive.Int, JavaType.Primitive.Int);
                assertThat(max.getType()).isEqualTo(JavaType.Primitive.Int);
            })
          )
        );
    }

    @Test
    void callInForComprehensionHasItsOwnMethodType() {
        rewriteRun(
          scala(
            """
            class Foo {
              def f = for (x <- Some(1)) println(x)
              def g = for (i <- 0 to 10) yield i
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation some = application(cu, "Some");
                assertThat(some.getMethodType().getName()).isEqualTo("apply");
                assertThat(some.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.Some$");
                assertThat(new MethodMatcher("scala.Some$ apply(..)").matches(some)).isTrue();
                assertThat(new MethodMatcher("scala.Some apply(..)").matches(some)).isFalse();
                assertThat(method(cu, "to").getMethodType().getName()).isEqualTo("to");
            })
          )
        );
    }

    @Test
    void lambdaParametersWithoutTypeHaveTheirInferredType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val xs = List(1, 2).map(u => u + 1)
              val f: (Int, String) => Int = (a, b) => a + b.length
              val ys = List("a").map { s => s.length }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(lambdaParameter(cu, "u").getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(lambdaParameter(cu, "a").getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(TypeUtils.isString(lambdaParameter(cu, "b").getType())).isTrue();
                assertThat(TypeUtils.isString(lambdaParameter(cu, "s").getType())).isTrue();
                for (String name : List.of("u", "a", "b", "s")) {
                    J.Identifier param = lambdaParameter(cu, name);
                    assertThat(param.getFieldType()).isNotNull();
                    assertThat(param.getFieldType().getName()).isEqualTo(name);
                    assertThat(param.getFieldType().getType()).isEqualTo(param.getType());
                }
            })
          )
        );
    }

    @Test
    void wildcardLambdaParameterHasTypeButNoVariable() {
        rewriteRun(
          scala(
            """
            class Foo {
              val f: (Int, String) => Int = (a, _) => a
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier wildcard = lambdaParameter(cu, "_");
                assertThat(TypeUtils.isString(wildcard.getType())).isTrue();
                assertThat(wildcard.getFieldType()).isNull();
            })
          )
        );
    }

    @Test
    void typedLambdaParameterHasVariableType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val xs = List(1, 2).map((u: Int) => u + 1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable u = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("u"));
                assertThat(u.getVariableType()).isNotNull();
                assertThat(u.getVariableType().getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(u.getName().getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(u.getName().getFieldType()).isSameAs(u.getVariableType());
            })
          )
        );
    }

    @Test
    void boundNameHasTypeOfBoundValue() {
        rewriteRun(
          scala(
            """
            case class Store(key: String)
            class Foo {
              def f(x: Any) = x match {
                case s @ Store(k) => s.key
                case _ => ""
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier s = find(cu, S.Binding.class, b -> b.getName().getSimpleName().equals("s")).getName();
                assertThat(TypeUtils.asFullyQualified(s.getType()).getFullyQualifiedName()).isEqualTo("Store");
                assertThat(s.getFieldType()).isNotNull();
                assertThat(s.getFieldType().getName()).isEqualTo("s");
                assertThat(s.getFieldType().getType()).isEqualTo(s.getType());
            })
          )
        );
    }

    @Test
    void namedArgumentNameHasTypeOfParameter() {
        rewriteRun(
          scala(
            """
            case class R(a: Int, b: String)
            class Foo {
              def m(x: Int, y: Int = 2): Int = x + y
              val r = R(b = "x", a = 1)
              val v = m(1, y = 3)
              def send(path: String, method: String = "GET")(body: Int => Unit): Unit = ()
              send("/", method = "HEAD") { x => () }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.Identifier b = namedArgument(cu, "b");
                assertThat(TypeUtils.isString(b.getType())).isTrue();
                assertThat(b.getFieldType().getName()).isEqualTo("b");
                J.Identifier a = namedArgument(cu, "a");
                assertThat(a.getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(a.getFieldType().getName()).isEqualTo("a");
                J.Identifier y = namedArgument(cu, "y");
                assertThat(y.getType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(y.getFieldType().getName()).isEqualTo("y");
                assertThat(y.getFieldType().getOwner()).isInstanceOf(JavaType.Method.class);
                assertThat(((JavaType.Method) y.getFieldType().getOwner()).getName()).isEqualTo("m");
                J.Identifier method = namedArgument(cu, "method");
                assertThat(TypeUtils.isString(method.getType())).isTrue();
                assertThat(method.getFieldType().getName()).isEqualTo("method");
            })
          )
        );
    }

    @Test
    void interpolatorHasMethodType() {
        rewriteRun(
          scala(
            """
            class Foo {
              val name = "x"
              val n = 1
              val a = s"hi $name"
              val b = raw"a\\n$name"
              val c = f"$n%d"
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                for (String interpolator : List.of("s", "raw", "f")) {
                    J.Identifier id = find(cu, S.InterpolatedString.class, i -> i.getInterpolator().getSimpleName().equals(interpolator)).getInterpolator();
                    assertThat(id.getType()).as(interpolator).isInstanceOf(JavaType.Method.class);
                    JavaType.Method method = (JavaType.Method) id.getType();
                    assertThat(method.getName()).isEqualTo(interpolator);
                    assertThat(method.getDeclaringType().getFullyQualifiedName()).isEqualTo("scala.StringContext");
                }
            })
          )
        );
    }

    @Test
    void libraryClassHasItsSupertypes() {
        rewriteRun(
          scala(
            """
            class LibraryClassHolder {
              val xs: java.util.ArrayList[String] = new java.util.ArrayList[String]()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                JavaType.FullyQualified arrayList = TypeUtils.asFullyQualified(xs.getType());
                assertThat(arrayList.getFullyQualifiedName()).isEqualTo("java.util.ArrayList");
                assertThat(arrayList.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Class);
                assertThat(arrayList.getSupertype().getFullyQualifiedName()).isEqualTo("java.util.AbstractList");
                assertThat(TypeUtils.isAssignableTo("java.util.Collection", arrayList)).isTrue();
                assertThat(TypeUtils.isAssignableTo("java.lang.Iterable", arrayList)).isTrue();
            })
          )
        );
    }

    @Test
    void libraryInterfaceHasInterfaceKind() {
        rewriteRun(
          scala(
            """
            class LibraryInterfaceHolder {
              val xs: java.util.List[String] = new java.util.ArrayList[String]()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                JavaType.FullyQualified list = TypeUtils.asFullyQualified(xs.getType());
                assertThat(list.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
                assertThat(TypeUtils.isAssignableTo("java.util.Collection", list)).isTrue();
            })
          )
        );
    }

    @Test
    void sourceClassExtendingLibraryClassIsAssignableToLibrarySupertypes() {
        rewriteRun(
          scala(
            """
            class Names extends java.util.ArrayList[String]
            """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration names = find(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Names"));
                assertThat(TypeUtils.isAssignableTo("java.util.List", names.getType())).isTrue();
            })
          )
        );
    }

    @Test
    void classTypeParameterInSupertypeIsTypeVariable() {
        rewriteRun(
          scala(
            """
            class Box[T] extends java.util.ArrayList[T]
            """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration box = find(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Box"));
                JavaType.Parameterized supertype = (JavaType.Parameterized) box.getType().getSupertype();
                assertThat(supertype.getTypeParameters()).singleElement()
                  .isInstanceOfSatisfying(JavaType.GenericTypeVariable.class, t -> assertThat(t.getName()).isEqualTo("T"));
            })
          )
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void methodTypeParameterRepeatedInItsBound() {
        rewriteRun(
          scala(
            """
            trait Pair[A, B]
            object Pairs {
              def same[T <: Pair[T, T]](t: T): T = t
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration same = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("same"));
                assertBoundIsPairOfItself(same.getMethodType().getParameterTypes().getFirst(), "T");
            })
          )
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void classTypeParameterRepeatedInItsBound() {
        rewriteRun(
          scala(
            """
            trait Pair[A, B]
            class Node[T <: Pair[T, T]](val t: T)
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable t = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("t"));
                assertBoundIsPairOfItself(t.getType(), "T");
            })
          )
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void typeParametersBoundingEachOther() {
        rewriteRun(
          scala(
            """
            trait Pair[A, B]
            object Pairs {
              def swap[T <: Pair[U, U], U <: Pair[T, T]](t: T, u: U): T = t
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration swap = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("swap"));
                List<JavaType> parameterTypes = swap.getMethodType().getParameterTypes();
                assertBoundIsPairOf(parameterTypes.get(0), "T", "U");
                assertBoundIsPairOf(parameterTypes.get(1), "U", "T");
            })
          )
        );
    }

    @Test
    void repeatedParameterOfScalaMethodIsSeq() {
        rewriteRun(
          scala(
            """
            object Foo {
              def f(x: Int, more: String*): Int = more.size
              val n = f(1, "a", "b")
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration f = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("f"));
                J.VariableDeclarations.NamedVariable more = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("more"));
                J.Identifier moreRef = find(cu, J.Identifier.class, i -> i.getSimpleName().equals("more") && i != more.getName());
                S.RepeatedType repeated = find(cu, S.RepeatedType.class, r -> true);
                JavaType.Method declared = f.getMethodType();
                JavaType.Method invoked = method(cu, "f").getMethodType();
                assertThat(declared.getParameterTypes()).hasSize(2);
                assertThat(TypeUtils.toString(declared.getParameterTypes().get(1))).isEqualTo("scala.collection.immutable.Seq<java.lang.String>");
                assertThat(declared.hasFlags(Flag.Varargs)).isTrue();
                assertThat(invoked).isSameAs(declared);
                assertThat(more.getVariableType().getType()).isSameAs(declared.getParameterTypes().get(1));
                assertThat(more.getVariableType().hasFlags(Flag.Varargs)).isTrue();
                assertThat(moreRef.getType()).isSameAs(declared.getParameterTypes().get(1));
                assertThat(repeated.getType()).isSameAs(declared.getParameterTypes().get(1));
            })
          )
        );
    }

    @Test
    void repeatedParameterOfScalaLibraryMethodIsSeq() {
        rewriteRun(
          scala(
            """
            object Foo {
              val xs = List(1, 2)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Method apply = method(cu, "apply").getMethodType();
                assertThat(apply.getParameterTypes()).singleElement()
                  .satisfies(p -> assertThat(TypeUtils.toString(p)).isEqualTo("scala.collection.immutable.Seq<int>"));
                assertThat(apply.hasFlags(Flag.Varargs)).isTrue();
            })
          )
        );
    }

    @Test
    void repeatedParameterOfJavaMethodIsArray() {
        rewriteRun(
          scala(
            """
            object Foo {
              val s = String.format("%s-%s", "a", "b")
              val xs = java.util.Arrays.asList("a", "b")
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Method format = method(cu, "format").getMethodType();
                JavaType.Method asList = method(cu, "asList").getMethodType();
                assertThat(format.getParameterTypes()).hasSize(2);
                assertThat(format.getParameterTypes().get(1)).isInstanceOfSatisfying(JavaType.Array.class,
                  a -> assertThat(TypeUtils.isOfClassType(a.getElemType(), "java.lang.Object")).isTrue());
                assertThat(format.hasFlags(Flag.Varargs)).isTrue();
                assertThat(asList.getParameterTypes()).singleElement().isInstanceOf(JavaType.Array.class);
                assertThat(asList.hasFlags(Flag.Varargs)).isTrue();
            })
          )
        );
    }

    @Test
    void methodWithoutRepeatedParameterIsNotVarargs() {
        rewriteRun(
          scala(
            """
            object Foo {
              def f(xs: Seq[String]): Int = xs.size
              val s = String.valueOf(1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Method f = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("f")).getMethodType();
                JavaType.Method valueOf = method(cu, "valueOf").getMethodType();
                assertThat(f.hasFlags(Flag.Varargs)).isFalse();
                assertThat(valueOf.hasFlags(Flag.Varargs)).isFalse();
            })
          )
        );
    }

    private static void assertBoundIsPairOfItself(JavaType type, String name) {
        assertBoundIsPairOf(type, name, name);
    }

    private static void assertBoundIsPairOf(JavaType type, String name, String argumentName) {
        assertThat(type).isInstanceOfSatisfying(JavaType.GenericTypeVariable.class, t -> {
            assertThat(t.getName()).isEqualTo(name);
            assertThat(t.getBounds()).singleElement().isInstanceOfSatisfying(JavaType.Parameterized.class, bound -> {
                assertThat(bound.getFullyQualifiedName()).isEqualTo("Pair");
                assertThat(bound.getTypeParameters()).hasSize(2).allSatisfy(argument ->
                  assertThat(argument).isInstanceOfSatisfying(JavaType.GenericTypeVariable.class,
                    a -> assertThat(a.getName()).isEqualTo(argumentName)));
            });
        });
    }

    @Test
    void stringTypeIsIndependentOfParseOrder() {
        // given
        ScalaParser parser = ScalaParser.builder().build();
        String declaresStringType = """
          class DeclaresStringType {
            def size(s: String): Int = s.length
          }
          """;
        String usesStringLiteral = """
          class UsesStringLiteral {
            val s = "b"
          }
          """;

        // when
        List<SourceFile> parsed = parser.parse(new InMemoryExecutionContext(), declaresStringType, usesStringLiteral).toList();

        // then
        J.VariableDeclarations.NamedVariable s = find((J) parsed.get(1), J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("s"));
        assertThat(TypeUtils.isAssignableTo("java.lang.CharSequence", s.getType())).isTrue();
        assertThat(TypeUtils.isAssignableTo("java.lang.Comparable", s.getType())).isTrue();
    }

    @Test
    void scalaParserDoesNotLeaveShallowClassesInCacheSharedWithJavaParser() {
        // given
        JavaTypeCache typeCache = new JavaTypeCache();
        ExecutionContext ctx = new InMemoryExecutionContext();
        ScalaParser.builder().typeCache(typeCache).build().parse(ctx, """
          class ScalaArrayListHolder {
            val xs: java.util.ArrayList[String] = new java.util.ArrayList[String]()
          }
          """).toList();

        // when
        J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion().typeCache(typeCache).build().parse(ctx, """
          class JavaArrayListHolder {
              java.util.ArrayList<String> xs;
          }
          """).findFirst().orElseThrow();

        // then
        J.VariableDeclarations xs = (J.VariableDeclarations) cu.getClasses().getFirst().getBody().getStatements().getFirst();
        assertThat(TypeUtils.isAssignableTo("java.util.List", xs.getType())).isTrue();
    }

    @Test
    void scalaLibraryClassFirstSeenViaNamedImportHasItsSupertypes() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable.ArrayBuffer
            class ImportedBufferHolder {
              val xs: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                JavaType.FullyQualified arrayBuffer = TypeUtils.asFullyQualified(xs.getType());
                assertThat(arrayBuffer.getFullyQualifiedName()).isEqualTo("scala.collection.mutable.ArrayBuffer");
                assertThat(arrayBuffer.getSupertype().getFullyQualifiedName()).isEqualTo("scala.collection.mutable.AbstractBuffer");
                assertThat(TypeUtils.isAssignableTo("scala.collection.mutable.Buffer", arrayBuffer)).isTrue();
                assertThat(TypeUtils.isAssignableTo("scala.collection.Seq", arrayBuffer)).isTrue();
            })
          )
        );
    }

    @Test
    void scalaLibraryClassFirstSeenViaPackageImportHasItsSupertypes() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable
            class QualifiedBufferHolder {
              val xs: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer.empty[Int]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                JavaType.FullyQualified arrayBuffer = TypeUtils.asFullyQualified(xs.getType());
                assertThat(arrayBuffer.getFullyQualifiedName()).isEqualTo("scala.collection.mutable.ArrayBuffer");
                assertThat(TypeUtils.isAssignableTo("scala.collection.mutable.Buffer", arrayBuffer)).isTrue();
                assertThat(TypeUtils.isAssignableTo("scala.collection.Seq", arrayBuffer)).isTrue();
            })
          )
        );
    }

    @Test
    void sourceClassWithCompanionObjectDeclaredFirstHasItsSupertypes() {
        rewriteRun(
          scala(
            """
            object Names {
              def empty: Names = new Names
            }
            class Names extends java.util.ArrayList[String]
            """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration names = findAll(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Names")).getLast();
                assertThat(names.getType().getSupertype().getFullyQualifiedName()).isEqualTo("java.util.ArrayList");
                assertThat(TypeUtils.isAssignableTo("java.util.List", names.getType())).isTrue();
            })
          )
        );
    }

    @Test
    void companionObjectDeclaredFirstHasItsOwnSupertypesAndMethods() {
        rewriteRun(
          scala(
            """
            object Box extends Runnable {
              def run(): Unit = ()
            }
            class Box extends java.util.ArrayList[String]
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.ClassDeclaration> boxes = findAll(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Box"));
                JavaType.FullyQualified boxObject = boxes.getFirst().getType();
                JavaType.FullyQualified boxClass = boxes.getLast().getType();
                assertThat(boxObject.getFullyQualifiedName()).isEqualTo("Box$");
                assertThat(TypeUtils.isAssignableTo("java.lang.Runnable", boxObject)).isTrue();
                assertThat(boxObject.getMethods()).extracting(JavaType.Method::getName).contains("run");
                assertThat(boxClass.getFullyQualifiedName()).isEqualTo("Box");
                assertThat(TypeUtils.isAssignableTo("java.lang.Runnable", boxClass)).isFalse();
                assertThat(TypeUtils.isAssignableTo("java.util.List", boxClass)).isTrue();
            })
          )
        );
    }

    @Test
    void objectCompanionOfTraitIsAFinalClass() {
        rewriteRun(
          scala(
            """
            trait Codec
            object Codec {
              def apply(): Codec = new Codec {}
            }
            class CodecHolder {
              val c = Codec()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodInvocation apply = application(cu, "Codec");
                JavaType.FullyQualified codecObject = apply.getMethodType().getDeclaringType();
                assertThat(codecObject.getFullyQualifiedName()).isEqualTo("Codec$");
                assertThat(codecObject.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Class);
                assertThat(codecObject.hasFlags(Flag.Final)).isTrue();
                JavaType.FullyQualified codecTrait = find(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Codec") && c.getKind() == J.ClassDeclaration.Kind.Type.Interface).getType();
                assertThat(codecTrait.getFullyQualifiedName()).isEqualTo("Codec");
                assertThat(codecTrait.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
            })
          )
        );
    }

    @Test
    void scalaLibraryClassImportedInEarlierFileKeepsSupertypesInLaterFile() {
        // given
        JavaTypeCache typeCache = new JavaTypeCache();
        ScalaParser parser = ScalaParser.builder().typeCache(typeCache).build();
        String importsBuffer = """
          import scala.collection.mutable.ArrayBuffer
          class ImportsBuffer
          """;
        String usesBuffer = """
          class UsesBuffer {
            val xs: scala.collection.mutable.ArrayBuffer[Int] = scala.collection.mutable.ArrayBuffer.empty[Int]
          }
          """;

        // when
        List<SourceFile> parsed = parser.parse(new InMemoryExecutionContext(), importsBuffer, usesBuffer).toList();

        // then
        J.VariableDeclarations.NamedVariable xs = find((J) parsed.get(1), J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
        assertThat(TypeUtils.isAssignableTo("scala.collection.mutable.Buffer", xs.getType())).isTrue();
        assertThat(TypeUtils.isAssignableTo("scala.collection.Seq", xs.getType())).isTrue();
    }

    @Test
    void membersOfScalaLibraryClassesAreNotPopulated() {
        rewriteRun(
          scala(
            """
            class Names {
              val xs: List[String] = Nil
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable xs = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("xs"));
                JavaType.FullyQualified list = TypeUtils.asFullyQualified(xs.getType());
                assertThat(list.getFullyQualifiedName()).isEqualTo("scala.collection.immutable.List");
                assertThat(list.getMethods()).isEmpty();
                J.ClassDeclaration names = find(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Names"));
                assertThat(names.getType().getMembers()).extracting(JavaType.Variable::getName).contains("xs");
            })
          )
        );
    }

    @Test
    void arrayTypesAreJavaArrays() {
        rewriteRun(
          scala(
            """
            class Foo {
              val names: Array[String] = "a,b".split(",")
              val nums = Array(1, 2)
              val grid: Array[Array[Int]] = Array(Array(1))
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable names = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("names"));
                J.VariableDeclarations.NamedVariable nums = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("nums"));
                J.VariableDeclarations.NamedVariable grid = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("grid"));
                JavaType.Array namesType = TypeUtils.asArray(names.getType());
                JavaType.Array splitReturnType = TypeUtils.asArray(method(cu, "split").getMethodType().getReturnType());
                JavaType.Array numsType = TypeUtils.asArray(nums.getType());
                JavaType.Array gridType = TypeUtils.asArray(grid.getType());
                assertThat(namesType).isNotNull();
                assertThat(TypeUtils.isOfClassType(namesType.getElemType(), "java.lang.String")).isTrue();
                assertThat(splitReturnType).isNotNull();
                assertThat(TypeUtils.isOfClassType(splitReturnType.getElemType(), "java.lang.String")).isTrue();
                assertThat(numsType).isNotNull();
                assertThat(numsType.getElemType()).isEqualTo(JavaType.Primitive.Int);
                assertThat(gridType).isNotNull();
                assertThat(TypeUtils.asArray(gridType.getElemType())).isNotNull();
                assertThat(TypeUtils.asArray(gridType.getElemType()).getElemType()).isEqualTo(JavaType.Primitive.Int);
            })
          )
        );
    }

    @Test
    void javaMethodWithArrayParameterMatchesJavaSignature() {
        rewriteRun(
          scala(
            """
            class Foo {
              val nums: Array[Int] = Array(3, 1)
              def render = java.util.Arrays.toString(nums)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                MethodMatcher matcher = new MethodMatcher("java.util.Arrays toString(int[])");
                J.MethodInvocation toString = method(cu, "toString");
                assertThat(matcher.matches(toString)).isTrue();
            })
          )
        );
    }

    @Test
    void inferredUnionTypesAreMultiCatch() {
        rewriteRun(
          scala(
            """
            package p
            object U {
              val a = Array("1", true)
              val m = Map("id" -> "x", "f" -> Map("a" -> 1))
              def r(xs: List[Any]) = xs.map { case s: String => s; case i: Int => i }
              def n(xs: List[String]) = xs.foreach { case null => 1; case v => v }
              def k(b: Boolean) = if (b) "s" else 1
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.VariableDeclarations.NamedVariable> variables = findAll(cu, J.VariableDeclarations.NamedVariable.class, v -> true);
                List<J.MethodInvocation> invocations = findAll(cu, J.MethodInvocation.class, m -> true);
                List<J.MethodDeclaration> declarations = findAll(cu, J.MethodDeclaration.class, m -> true);
                JavaType.Array aType = TypeUtils.asArray(find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("a")).getType());
                JavaType.Parameterized mType = TypeUtils.asParameterized(find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("m")).getType());
                JavaType.Method kType = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("k")).getMethodType();
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).extracting(FindMissingTypes.MissingTypeResult::getPrintedTree).isEmpty();
                assertThat(variables).allSatisfy(v -> {
                    assertThat(TypeUtils.isWellFormedType(v.getType())).as(v.getSimpleName()).isTrue();
                    assertThat(TypeUtils.isWellFormedType(v.getName().getType())).as(v.getSimpleName()).isTrue();
                });
                assertThat(invocations).allSatisfy(m -> assertThat(TypeUtils.isWellFormedType(m.getMethodType())).as(m.getSimpleName()).isTrue());
                assertThat(declarations).allSatisfy(m -> assertThat(TypeUtils.isWellFormedType(m.getMethodType())).as(m.getSimpleName()).isTrue());
                assertThat(aType).isNotNull();
                assertThat(unionMembers(aType.getElemType())).containsExactly("boolean", "java.lang.String");
                assertThat(mType).isNotNull();
                assertThat(unionMembers(mType.getTypeParameters().get(1)))
                  .containsExactly("java.lang.String", "scala.collection.immutable.Map<java.lang.String, int>");
                assertThat(unionMembers(kType.getReturnType())).containsExactly("int", "java.lang.String");
            })
          )
        );
    }

    @Test
    void annotationNamesAreTyped() {
        rewriteRun(
          scala(
            """
            package p
            import scala.annotation.tailrec
            class B {
              @transient val x = 1
              @volatile var y = 2
              @tailrec final def loop(i: Int): Int = if (i <= 0) 0 else loop(i - 1)
              @throws[Exception] def t(): Unit = ()
              @throws(classOf[Exception]) def c(): Unit = ()
              @deprecated("x", "1") def d(): Unit = ()
              @`deprecated` def q(): Unit = ()
              @scala.annotation.nowarn def n(): Unit = ()
              @Deprecated def j(): Unit = ()
              @SuppressWarnings(Array("unused")) def w(): Unit = ()
              def u(o: Any): Int = (o: @unchecked) match { case i: Int => i }
            }
            @SerialVersionUID(1L) class C(@transient val z: Int)
            @deprecated object O
            class D[@specialized T]
            """,
            spec -> spec.afterRecipe(cu -> {
                List<String> annotationTypes = findAll(cu, J.Annotation.class, a -> true).stream()
                  .map(a -> TypeUtils.asFullyQualified(a.getType()))
                  .map(t -> t == null ? null : t.getFullyQualifiedName())
                  .toList();
                assertThat(annotationTypes).containsExactly(
                  "scala.transient",
                  "scala.volatile",
                  "scala.annotation.tailrec",
                  "scala.throws",
                  "scala.throws",
                  "scala.deprecated",
                  "scala.deprecated",
                  "scala.annotation.nowarn",
                  "java.lang.Deprecated",
                  "java.lang.SuppressWarnings",
                  "scala.unchecked",
                  "scala.SerialVersionUID",
                  "scala.transient",
                  "scala.deprecated",
                  "scala.specialized"
                );
                assertThat(FindMissingTypes.findMissingTypes(cu, false))
                  .extracting(FindMissingTypes.MissingTypeResult::getPrintedTree)
                  .isEmpty();
            })
          )
        );
    }

    @Test
    void javaStaticMethodIsSharedWithJavaParserThroughTypeCache() {
        // given
        JavaTypeCache typeCache = new JavaTypeCache();
        ExecutionContext ctx = new InMemoryExecutionContext();
        J scalaCu = (J) ScalaParser.builder().typeCache(typeCache).build().parse(ctx, """
          class ScalaStatics {
            val n = Integer.parseInt("1")
            val s = String.format("%s", "a")
          }
          """).findFirst().orElseThrow();

        // when
        J.CompilationUnit javaCu = (J.CompilationUnit) JavaParser.fromJavaVersion().typeCache(typeCache).build().parse(ctx, """
          class JavaStatics {
              int n = Integer.parseInt("1");
              String s = String.format("%s", "a");
          }
          """).findFirst().orElseThrow();

        // then
        List<Statement> fields = javaCu.getClasses().getFirst().getBody().getStatements();
        J.MethodInvocation javaParseInt = (J.MethodInvocation) ((J.VariableDeclarations) fields.get(0)).getVariables().getFirst().getInitializer();
        J.MethodInvocation javaFormat = (J.MethodInvocation) ((J.VariableDeclarations) fields.get(1)).getVariables().getFirst().getInitializer();
        assertThat(javaParseInt.getMethodType()).isNotNull().isSameAs(method(scalaCu, "parseInt").getMethodType());
        assertThat(javaFormat.getMethodType()).isNotNull().isSameAs(method(scalaCu, "format").getMethodType());
    }

    @Test
    void javaStaticOwnerIsTheJavaClassWithItsSupertypes() {
        rewriteRun(
          scala(
            """
            class IntegerStatics {
              val n = Integer.parseInt("1")
              val m = java.util.List.of(1)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.FullyQualified integer = method(cu, "parseInt").getMethodType().getDeclaringType();
                assertThat(integer.getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                assertThat(integer.getSupertype().getFullyQualifiedName()).isEqualTo("java.lang.Number");
                JavaType.FullyQualified list = method(cu, "of").getMethodType().getDeclaringType();
                assertThat(list.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
            })
          )
        );
    }

    @Test
    void classNameEndingInDollarKeepsItsDollar() {
        rewriteRun(
          scala(
            """
            class Dollar$
            object Companion
            class DollarHolder {
              val d: Dollar$ = new Dollar$
              val c = Companion
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration dollarClass = find(cu, J.ClassDeclaration.class, c -> c.getSimpleName().equals("Dollar$"));
                J.VariableDeclarations.NamedVariable d = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("d"));
                J.VariableDeclarations.NamedVariable c = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("c"));
                assertThat(TypeUtils.asFullyQualified(dollarClass.getType()).getFullyQualifiedName()).isEqualTo("Dollar$");
                assertThat(TypeUtils.asFullyQualified(d.getType()).getFullyQualifiedName()).isEqualTo("Dollar$");
                assertThat(TypeUtils.asFullyQualified(c.getType()).getFullyQualifiedName()).isEqualTo("Companion$");
            })
          )
        );
    }

    @Test
    void writtenUnionTypeIsMultiCatch() {
        rewriteRun(
          scala(
            """
            object U {
              val u: String | Int = 1
              val w: Int | String = 1
              val lit: 1 | 2 = 1
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType uType = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("u")).getType();
                JavaType wType = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("w")).getType();
                JavaType litType = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("lit")).getType();
                List<S.UnionType> unionTrees = findAll(cu, S.UnionType.class, t -> true);
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).extracting(FindMissingTypes.MissingTypeResult::getPrintedTree).isEmpty();
                assertThat(unionMembers(uType)).containsExactly("int", "java.lang.String");
                assertThat(wType).isSameAs(uType);
                assertThat(unionTrees.getFirst().getType()).isSameAs(uType);
                assertThat(litType).isEqualTo(JavaType.Primitive.Int);
            })
          )
        );
    }

    @Test
    void nullTypeIsPrimitiveNull() {
        rewriteRun(
          scala(
            """
            object U {
              val n: Null = null
              val s: String | Null = null
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType nType = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("n")).getType();
                JavaType sType = find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("s")).getType();
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).extracting(FindMissingTypes.MissingTypeResult::getPrintedTree).isEmpty();
                assertThat(nType).isSameAs(JavaType.Primitive.Null);
                assertThat(sType).isInstanceOf(JavaType.MultiCatch.class);
                assertThat(((JavaType.MultiCatch) sType).getThrowableTypes())
                  .satisfiesExactly(
                    t -> assertThat(TypeUtils.isOfClassType(t, "java.lang.String")).isTrue(),
                    t -> assertThat(t).isSameAs(JavaType.Primitive.Null));
            })
          )
        );
    }

    @Test
    void bindingOfExceptionAlternativesIsMultiCatch() {
        rewriteRun(
          scala(
            """
            object U {
              def c(): Int = try 1 catch { case e1 @ (_: IllegalArgumentException | _: IllegalStateException) => e1.getMessage().length }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.Identifier> e1 = findAll(cu, J.Identifier.class, i -> i.getSimpleName().equals("e1"));
                JavaType.Variable fieldType = e1.getFirst().getFieldType();
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).extracting(FindMissingTypes.MissingTypeResult::getPrintedTree).isEmpty();
                assertThat(e1).hasSize(2).allSatisfy(i -> assertThat(unionMembers(i.getType()))
                  .containsExactly("java.lang.IllegalArgumentException", "java.lang.IllegalStateException"));
                assertThat(fieldType).isNotNull();
                assertThat(fieldType.getType()).isSameAs(e1.getFirst().getType());
                assertThat(method(cu, "getMessage").getMethodType().getDeclaringType().getFullyQualifiedName())
                  .isEqualTo("java.lang.Throwable");
            })
          )
        );
    }

    @Test
    void unionReachedAgainThroughItsMemberIsOneInstance() {
        rewriteRun(
          scala(
            """
            object Holder {
              def first(): Node | Null = null
            }
            class Node {
              def next: Node | Null = null
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration first = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("first"));
                J.MethodDeclaration next = find(cu, J.MethodDeclaration.class, m -> m.getSimpleName().equals("next"));
                JavaType firstReturnType = first.getMethodType().getReturnType();
                JavaType nextReturnType = next.getMethodType().getReturnType();
                assertThat(unionMembers(firstReturnType)).containsExactly("Node", "null");
                assertThat(nextReturnType).isSameAs(firstReturnType);
            })
          )
        );
    }

    @Test
    void nestedJavaClassHasBinaryName() {
        rewriteRun(
          scala(
            """
            class EntryHost {
              val entry: java.util.Map.Entry[String, Int] = null
              val key = entry.getKey()
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType entry = variable(cu, "entry").getType();
                J.MethodInvocation getKey = method(cu, "getKey");
                assertThat(TypeUtils.asFullyQualified(entry).getFullyQualifiedName()).isEqualTo("java.util.Map$Entry");
                assertThat(TypeUtils.isOfClassType(entry, "java.util.Map$Entry")).isTrue();
                assertThat(getKey.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.util.Map$Entry");
                assertThat(new MethodMatcher("java.util.Map$Entry getKey()").matches(getKey)).isTrue();
            })
          )
        );
    }

    @Test
    void findAnnotationsMatchesScalaAnnotations() {
        rewriteRun(
          spec -> spec.recipes(
            new FindAnnotations("@scala.transient", false),
            new FindAnnotations("@scala.annotation.tailrec", false)
          ),
          scala(
            """
            import scala.annotation.tailrec
            class B {
              @transient val x = 1
              @tailrec final def loop(i: Int): Int = if (i <= 0) 0 else loop(i - 1)
              @volatile var y = 2
            }
            """,
            """
            import scala.annotation.tailrec
            class B {
              /*~~>*/@transient val x = 1
              /*~~>*/@tailrec final def loop(i: Int): Int = if (i <= 0) 0 else loop(i - 1)
              @volatile var y = 2
            }
            """
          )
        );
    }

    private static List<String> unionMembers(@Nullable JavaType type) {
        assertThat(type).isInstanceOf(JavaType.MultiCatch.class);
        return ((JavaType.MultiCatch) type).getThrowableTypes().stream().map(JavaType::toString).toList();
    }

    @Test
    void nestedScalaClassHasBinaryName() {
        rewriteRun(
          scala(
            """
            class Outer {
              class Inner
              object Nested {
                class Deep
              }
            }
            object Holder {
              class Member
            }
            class NestedHost(o: Outer) {
              val inner: o.Inner = null
              val nested = o.Nested
              val deep: o.Nested.Deep = null
              val member: Holder.Member = null
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                JavaType inner = variable(cu, "inner").getType();
                JavaType nested = variable(cu, "nested").getType();
                JavaType deep = variable(cu, "deep").getType();
                JavaType member = variable(cu, "member").getType();
                assertThat(TypeUtils.asFullyQualified(inner).getFullyQualifiedName()).isEqualTo("Outer$Inner");
                assertThat(TypeUtils.asFullyQualified(nested).getFullyQualifiedName()).isEqualTo("Outer$Nested$");
                assertThat(TypeUtils.asFullyQualified(deep).getFullyQualifiedName()).isEqualTo("Outer$Nested$Deep");
                assertThat(TypeUtils.asFullyQualified(member).getFullyQualifiedName()).isEqualTo("Holder$Member");
            })
          )
        );
    }

    @Test
    void localVariablesOfOverloadsAreDistinct() {
        rewriteRun(
          scala(
            """
            class Overloads {
              def m(a: Int): Unit = { val x = a }
              def m(a: String): Unit = { val x = a }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                List<J.VariableDeclarations.NamedVariable> params = findAll(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("a"));
                List<J.VariableDeclarations.NamedVariable> locals = findAll(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("x"));
                assertThat(params).extracting(v -> TypeUtils.toString(v.getVariableType().getType())).containsExactly("int", "java.lang.String");
                assertThat(locals).extracting(v -> TypeUtils.toString(v.getVariableType().getType())).containsExactly("int", "java.lang.String");
                assertThat(locals.get(0).getVariableType().getOwner()).isNotSameAs(locals.get(1).getVariableType().getOwner());
            })
          )
        );
    }

    @Test
    void nestedJavaClassMethodsAreSharedWithJavaParserThroughTypeCache() {
        // given
        JavaTypeCache typeCache = new JavaTypeCache();
        ExecutionContext ctx = new InMemoryExecutionContext();
        J scalaCu = (J) ScalaParser.builder().typeCache(typeCache).build().parse(ctx, """
          class ScalaNested {
            val c = java.util.Map.Entry.comparingByKey[String, Integer]()
            def k(e: java.util.Map.Entry[String, Integer]) = e.getKey()
          }
          """).findFirst().orElseThrow();

        // when
        J.CompilationUnit javaCu = (J.CompilationUnit) JavaParser.fromJavaVersion().typeCache(typeCache).build().parse(ctx, """
          class JavaNested {
              Object c = java.util.Map.Entry.<String, Integer>comparingByKey();
              Object k(java.util.Map.Entry<String, Integer> e) { return e.getKey(); }
          }
          """).findFirst().orElseThrow();

        // then
        assertThat(javaMethod(javaCu, "comparingByKey").getMethodType()).isNotNull().isSameAs(method(scalaCu, "comparingByKey").getMethodType());
        assertThat(javaMethod(javaCu, "getKey").getMethodType()).isNotNull().isSameAs(method(scalaCu, "getKey").getMethodType());
    }

    @Test
    void sameNamedLocalsOfDifferentTypesInOneMethod() {
        rewriteRun(
          scala(
            """
            class SameNamedLocals {
              def m(): Unit = {
                { val x = 1 }
                { val x = "s" }
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(findAll(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals("x")))
                  .extracting(v -> TypeUtils.toString(v.getVariableType().getType()))
                  .containsExactly("int", "java.lang.String");
            })
          )
        );
    }

    @Test
    void sameNamedCaseBindersOfDifferentTypes() {
        rewriteRun(
          scala(
            """
            class SameNamedBinders {
              def m(o: Any): String = o match {
                case v: Int => v.toString
                case v: String => v
              }
            }
            """,
            spec -> spec.afterRecipe(cu ->
                assertThat(findAll(cu, J.Identifier.class, i -> i.getSimpleName().equals("v")))
                  .extracting(i -> TypeUtils.toString(i.getFieldType().getType()))
                  .containsExactly("int", "int", "java.lang.String", "java.lang.String"))
          )
        );
    }

    private static J.Identifier lambdaParameter(J cu, String name) {
        return findAll(cu, J.Lambda.class, l -> true).stream()
          .flatMap(l -> l.getParameters().getParameters().stream())
          .filter(p -> p instanceof J.Identifier i && i.getSimpleName().equals(name))
          .map(J.Identifier.class::cast)
          .findFirst()
          .orElseThrow();
    }

    private static J.Identifier namedArgument(J cu, String name) {
        return (J.Identifier) find(cu, J.Assignment.class,
          a -> a.getVariable() instanceof J.Identifier i && i.getSimpleName().equals(name)).getVariable();
    }

    private static J.MethodInvocation application(J cu, String function) {
        return find(cu, J.MethodInvocation.class, m -> isApplicationOf(m, function));
    }

    private static boolean isApplicationOf(J.MethodInvocation m, String function) {
        return m.getSelect() == null ?
          m.getSimpleName().equals(function) :
          m.getSimpleName().equals("apply") && m.getSelect() instanceof J.Identifier i && i.getSimpleName().equals(function);
    }

    private static J.Import importOf(J cu, String name) {
        return find(cu, J.Import.class, i -> i.getQualid().getSimpleName().equals(name));
    }

    private static J.VariableDeclarations.NamedVariable variable(J cu, String name) {
        return find(cu, J.VariableDeclarations.NamedVariable.class, v -> v.getSimpleName().equals(name));
    }

    private static J.MethodInvocation method(J cu, String name) {
        return find(cu, J.MethodInvocation.class, m -> m.getSimpleName().equals(name));
    }

    private static J.MethodInvocation javaMethod(J.CompilationUnit cu, String name) {
        return new JavaIsoVisitor<List<J.MethodInvocation>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, List<J.MethodInvocation> acc) {
                if (m.getSimpleName().equals(name)) {
                    acc.add(m);
                }
                return super.visitMethodInvocation(m, acc);
            }
        }.reduce(cu, new ArrayList<>()).getFirst();
    }

    private static <T extends J> T find(J cu, Class<T> type, Predicate<T> matching) {
        return findAll(cu, type, matching).getFirst();
    }

    private static <T extends J> List<T> findAll(J cu, Class<T> type, Predicate<T> matching) {
        List<T> found = new ScalaIsoVisitor<List<T>>() {
            @Override
            public @Nullable J preVisit(J tree, List<T> acc) {
                if (type.isInstance(tree) && matching.test(type.cast(tree))) {
                    acc.add(type.cast(tree));
                }
                return tree;
            }
        }.reduce(cu, new ArrayList<>());
        assertThat(found).as("%s matching in %s", type.getSimpleName(), cu.printTrimmed()).isNotEmpty();
        return found;
    }
}
