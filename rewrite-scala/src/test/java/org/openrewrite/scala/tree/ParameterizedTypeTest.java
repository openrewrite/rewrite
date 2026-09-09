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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class ParameterizedTypeTest implements RewriteTest {

    @Test
    void simpleParameterizedType() {
        rewriteRun(
          scala(
            """
            object Test {
              val list: List[String] = List("a", "b", "c")
            }
            """
          )
        );
    }

    @Test
    void arrayTypeAnnotationIsParameterizedType() {
        rewriteRun(
          scala(
            """
            object Test {
              val args: Array[String] = Array.empty[String]
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<TypeTree> typeExpression = new AtomicReference<>();

                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                        J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().get(0);
                        if ("args".equals(variable.getSimpleName())) {
                            typeExpression.set(multiVariable.getTypeExpression());
                        }
                        return super.visitVariableDeclarations(multiVariable, p);
                    }
                }.visit(cu, 0);

                assertThat(typeExpression.get())
                  .as("Array[String] should parse as a parameterized type annotation")
                  .isInstanceOf(J.ParameterizedType.class);

                J.ParameterizedType arrayType = (J.ParameterizedType) typeExpression.get();
                assertThat(arrayType.getClazz()).isInstanceOf(J.Identifier.class);
                assertThat(((J.Identifier) arrayType.getClazz()).getSimpleName()).isEqualTo("Array");
                assertThat(arrayType.getTypeParameters()).hasSize(1);
                assertThat(arrayType.getTypeParameters().get(0)).isInstanceOf(J.Identifier.class);
                assertThat(((J.Identifier) arrayType.getTypeParameters().get(0)).getSimpleName()).isEqualTo("String");
                assertThat(arrayType.getType()).isInstanceOf(JavaType.Parameterized.class);
                JavaType.Parameterized attributedType = (JavaType.Parameterized) arrayType.getType();
                assertThat(attributedType.getType().getFullyQualifiedName()).isEqualTo("scala.Array");
                assertThat(attributedType.getTypeParameters()).hasSize(1);
                assertThat(attributedType.getTypeParameters().get(0))
                  .isInstanceOf(JavaType.FullyQualified.class);
                assertThat(((JavaType.FullyQualified) attributedType.getTypeParameters().get(0)).getFullyQualifiedName())
                  .isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void multipleTypeParameters() {
        rewriteRun(
          scala(
            """
            object Test {
              val map: Map[String, Int] = Map("one" -> 1, "two" -> 2)
            }
            """
          )
        );
    }

    @Test
    void nestedParameterizedTypes() {
        rewriteRun(
          scala(
            """
            object Test {
              val nested: List[Option[String]] = List(Some("a"), None, Some("b"))
            }
            """
          )
        );
    }

    @Test
    void parameterizedTypeInMethodSignature() {
        rewriteRun(
          scala(
            """
            object Test {
              def getList(): List[Int] = List(1, 2, 3)
              
              def processMap(m: Map[String, Any]): Unit = {
                println(m)
              }
            }
            """
          )
        );
    }

    @Test
    void parameterizedTypeInNew() {
        rewriteRun(
          scala(
            """
            object Test {
              val list = new ArrayList[String]()
              val map = new HashMap[Int, String]()
            }
            """
          )
        );
    }

    @Test
    void wildcardType() {
        rewriteRun(
          scala(
            """
            object Test {
              def process(list: List[_]): Unit = {
                println(list.size)
              }
            }
            """
          )
        );
    }

    @Test
    void boundedTypeParameters() {
        rewriteRun(
          scala(
            """
            object Test {
              def sort[T <: Comparable[T]](list: List[T]): List[T] = {
                list.sorted
              }
            }
            """
          )
        );
    }

    @Test
    void boundedTypeParameterWithContextBound() {
        rewriteRun(
          scala(
            """
            trait Actor
            trait ClassTag[T]
            trait Lower
            trait Upper

            class ActorRefProvider[T <: Actor: ClassTag]
            class UpperBound[T <: Upper: ClassTag]
            class LowerBound[T >: Lower: ClassTag]
            class LowerAndUpperBound[T >: Lower <: Upper: ClassTag]
            class MultipleContextBounds[T <: Upper: ClassTag: Ordering]
            """
          )
        );
    }

    @Test
    void varianceAnnotations() {
        rewriteRun(
          scala(
            """
            object Test {
              class Container[+T](value: T)
              class MutableContainer[-T]
              
              val container: Container[String] = new Container("test")
            }
            """
          )
        );
    }

    @Test
    void typeProjection() {
        rewriteRun(
          scala(
            """
            object Test {
              trait Outer {
                type Inner
              }
              
              def process(x: Outer#Inner): Unit = {}
            }
            """
          )
        );
    }

    @Test
    void higherKindedTypes() {
        rewriteRun(
          scala(
            """
            object Test {
              def transform[F[_], A, B](fa: F[A])(f: A => B): F[B] = ???
            }
            """
          )
        );
    }

    @Test
    void higherKindedTypeParamNotCrammedIntoIdentifier() {
        // visitTypeParameter — `F[_]` must be modeled as J.ParameterizedType, not crammed into the name
        assertNoTypeArgInIdentifier(
          """
          trait Functor[F[_]] {
            val x: Int = 1
          }
          """
        );
    }

    @Test
    void multipleHigherKindedParamsNotCrammedIntoIdentifier() {
        assertNoTypeArgInIdentifier(
          """
          trait Iso[F[_], G[_]] {
            val x: Int = 1
          }
          """
        );
    }

    private void assertNoTypeArgInIdentifier(String source) {
        rewriteRun(
          scala(
            source,
            spec -> spec.afterRecipe(cu -> {
                java.util.List<String> identifierNames = new java.util.ArrayList<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        identifierNames.add(identifier.getSimpleName());
                        return super.visitIdentifier(identifier, p);
                    }
                }.visit(cu, 0);
                assertThat(identifierNames)
                  .as("higher-kinded type-param brackets should not be crammed into an identifier name")
                  .allSatisfy(name -> assertThat(name).doesNotContain("[", "]", "_"));
            })
          )
        );
    }

    @Test
    void significantCharactersInComments() {
        // visitAppliedTypeTree — `[` in block comment between type constructor and type args
        rewriteRun(
          scala(
            """
              val xs: List /* [ */ [Int] = List(1)
              """
          )
        );
    }
}
