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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class AnnotationTest implements RewriteTest {
    
    @Test
    void simpleAnnotation() {
        rewriteRun(
            scala(
                """
                @deprecated
                def oldMethod(): Unit = {}
                """
            )
        );
    }
    
    @Test
    void annotationWithStringArgument() {
        rewriteRun(
            scala(
                """
                @deprecated("Use newMethod instead")
                def oldMethod(): Unit = {}
                """
            )
        );
    }
    
    @Test
    void annotationWithNamedArguments() {
        rewriteRun(
            scala(
                """
                @deprecated(message = "Use newMethod", since = "2.0")
                def oldMethod(): Unit = {}
                """
            )
        );
    }
    
    @Test
    void multipleAnnotations() {
        rewriteRun(
            scala(
                """
                @deprecated("Old method")
                @throws[Exception]
                def riskyMethod(): Unit = {}
                """
            )
        );
    }
    
    @Test
    void annotationOnClass() {
        rewriteRun(
            scala(
                """
                @deprecated
                class OldClass {
                }
                """
            )
        );
    }
    
    @Test
    void annotationOnVariable() {
        rewriteRun(
            scala(
                """
                class Test {
                  @volatile var flag = false
                  @transient val data = "test"
                }
                """
            )
        );
    }
    
    @Test
    void annotationWithClassArgument() {
        rewriteRun(
            scala(
                """
                @throws[IllegalArgumentException]("Invalid argument")
                def validate(x: Int): Unit = {}
                """
            )
        );
    }
    
    @Test
    void annotationWithArrayArgumentMultiline() {
        rewriteRun(
            scala(
                """
                @SuppressWarnings(
                  Array(
                    "a", "b"
                  )
                )
                val x = 1
                """
            )
        );
    }

    @Test
    void annotationOnParameter() {
        rewriteRun(
            scala(
                """
                def process(@unchecked value: Any): Unit = {}
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeLazyVal() {
        rewriteRun(
            scala(
                """
                class Test {
                  @JsonIgnore
                  lazy val schema: String = "x"
                }
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeFinalClass() {
        rewriteRun(
            scala(
                """
                @SerialVersionUID(1L)
                final class Box(val x: Int)
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeOverrideDef() {
        rewriteRun(
            scala(
                """
                class Test {
                  @Override
                  override def toString: String = "x"
                }
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforePrivateVal() {
        rewriteRun(
            scala(
                """
                class Test {
                  @JsonIgnore
                  private val secret: String = "x"
                }
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeSealedTrait() {
        rewriteRun(
            scala(
                """
                @SerialVersionUID(1L)
                sealed trait Status
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeObject() {
        rewriteRun(
            scala(
                """
                @deprecated
                object Marker
                """
            )
        );
    }

    @Test
    void annotationOnSameLineBeforeObject() {
        rewriteRun(
            scala(
                """
                @deprecated object Marker
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeCaseObject() {
        rewriteRun(
            scala(
                """
                @SerialVersionUID(1L)
                case object Marker
                """
            )
        );
    }

    @Test
    void multipleAnnotationsBeforeLazyVal() {
        rewriteRun(
            scala(
                """
                class Test {
                  @JsonIgnore
                  @transient
                  lazy val schema: String = "x"
                }
                """
            )
        );
    }

    @Test
    void annotationOnTypeArgument() {
        rewriteRun(
            scala(
                """
                class Box[A]
                trait Test {
                  def f: Box[Int @deprecated]
                }
                """
            )
        );
    }

    @Test
    void annotationOnReturnType() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f: String @deprecated = "x"
                }
                """
            )
        );
    }

    @Test
    void annotationOnMethodParameterType() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(x: Int @deprecated): Unit = ()
                }
                """
            )
        );
    }

    @Test
    void qualifiedAnnotationName() {
        rewriteRun(
            scala(
                """
                @scala.annotation.implicitNotFound("msg")
                trait Foo[T]
                """
            )
        );
    }

    @Test
    void annotationOnOwnLineBeforeImplicitVal() {
        rewriteRun(
            scala(
                """
                class Test {
                  @JsonIgnore
                  implicit val schema: String = "x"
                }
                """
            )
        );
    }
}
