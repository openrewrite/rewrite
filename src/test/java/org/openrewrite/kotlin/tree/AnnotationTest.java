/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantSuppression", "RedundantNullableReturnType", "RedundantVisibilityModifier", "UnusedReceiverParameter", "SortModifiers"})
class AnnotationTest implements RewriteTest {

    @Language("kotlin")
    private static final String ANNOTATION =
            """
              @Target(
                  AnnotationTarget.CLASS,
                  AnnotationTarget.ANNOTATION_CLASS,
                  AnnotationTarget.TYPE_PARAMETER,
                  AnnotationTarget.PROPERTY,
                  AnnotationTarget.FIELD,
                  AnnotationTarget.LOCAL_VARIABLE,
                  AnnotationTarget.VALUE_PARAMETER,
                  AnnotationTarget.CONSTRUCTOR,
                  AnnotationTarget.FUNCTION,
                  AnnotationTarget.PROPERTY_GETTER,
                  AnnotationTarget.PROPERTY_SETTER,
                  AnnotationTarget.TYPE,
                  AnnotationTarget.EXPRESSION,
                  AnnotationTarget.FILE,
                  AnnotationTarget.TYPEALIAS
              )
              @Retention(AnnotationRetention.SOURCE)
              annotation class Ann
              """;

    @Test
    void fileScope() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath()),
          kotlin(
            """
              @file : Suppress ( "DEPRECATION_ERROR" )

              class A
              """
          )
        );
    }

    @Test
    void multipleFileScope() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              @file : Ann
              @file : Ann
              @file : Ann
              """
          )
        );
    }

    @Test
    void annotationWithDefaultArgument() {
        rewriteRun(
          kotlin(
            """
              @SuppressWarnings ( "ConstantConditions" , "unchecked" )
              class A
              """
          )
        );
    }

    @Test
    void leadingAnnotations() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              annotation class Anno2
              class Test {
                  @Anno
                  @Anno2
                  val id: String = "1"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations v = (J.VariableDeclarations) ((J.ClassDeclaration) cu.getStatements().get(2)).getBody().getStatements().get(0);
                assertThat(v.getLeadingAnnotations().size()).isEqualTo(2);
            })
          )
        );
    }

    @Test
    void arrayArgument() {
        rewriteRun(
          kotlin(
            """
              @Target ( AnnotationTarget . LOCAL_VARIABLE )
              @Retention ( AnnotationRetention . SOURCE )
              annotation class Test ( val values : Array < String > )
              """
          ),
          kotlin(
            """
              @Test( values = [ "a" , "b" , "c" ] )
              val a = 42
              """
          )
        );
    }

    @Test
    void fullyQualifiedAnnotation() {
        rewriteRun(
          kotlin(
            """
              @java.lang.Deprecated
              class A {
              }
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              annotation class Test ( val values : Array < String > )
              @Test( values = [ "a" , "b" , /* trailing comma */ ] )
              val a = 42
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/80")
    @Test
    void jvmNameAnnotation() {
        rewriteRun(
          kotlin(
            """
              import kotlin.jvm.JvmName
              @get : JvmName ( "getCount" )
              val count : Int ?
                  get ( ) = 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTargetAnnotationOnly() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class TestA {
                  @get : Ann
                  @set : Ann
                  var name : String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class TestA {
                  @get : Ann
                  public
                  @set : Ann
                  var name: String = ""
              }
              """
          ),
          kotlin(
            """
              class TestB {
                  public
                  @get : Ann
                  @set : Ann
                  var name : String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void constructorParameterWithAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example(
                  @get : Ann
                  val bar : String
                  )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void getUseSiteOnConstructorParams() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example ( /**/  /**/ @get : Ann /**/ /**/ @set : Ann /**/ /**/ var foo: String , @get : Ann val bar: String )
              """
          )
        );
    }

    @Test
    void annotationOnExplicitGetter() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Test {
                  public var stringRepresentation : String = ""
                      @Ann
                      // comment
                      get ( ) = field

                      @Ann
                      set ( value ) {
                          field = value
                      }
              }
              """
          )
        );
    }

    @Test
    void paramAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example ( @param : Ann val quux : String )
              """
          )
        );
    }

    @Test
    void fieldAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example ( @field : Ann val foo : String )
              """
          )
        );
    }

    @Test
    void receiverAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun @receiver : Ann String . myExtension ( ) { }
              """
          )
        );
    }

    @Test
    void setParamAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example {
                  @setparam : Ann
                  var name: String = ""
              }
              """
          )
        );
    }

    @Test
    void destructuringVariableDeclaration() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun example ( ) {
                val ( @Ann a , @Ann b , @Ann c ) = Triple ( 1 , 2 , 3 )
              }
              """
          )
        );
    }

    @Test
    void annotationsInManyLocations() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              @Ann
              open class Test < @Ann in Number > ( @Ann val s : String ) {
                  @Ann var n : Int = 42
                      @Ann get ( ) = 42
                      @Ann set ( @Ann value ) {
                          field = value
                      }
              
                  @Ann inline fun < @Ann reified T > m ( @Ann s : @Ann String ) : String {
                      @Ann return (@Ann s)
                  }
              }
              @Ann typealias Other = @Ann String
              """
          )
        );
    }

    @Test
    void lambdaExpression() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun method ( ) {
                  val list = listOf ( 1 , 2 , 3 )
                  list . filterIndexed { index , _ -> @Ann index % 2 == 0 }
              }
              """
            )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/267")
    void expressionAnnotationInsideLambda() {
        rewriteRun(
          kotlin(
            """
              val s = java.util.function.Supplier<String> {
                  @Suppress("UNCHECKED_CAST")
                  requireNotNull("x")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/284")
    @Test
    void annotationWithEmptyArguments() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann

              @Suppress()
              @Ann
              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/286")
    @Test
    void catchUnderscore() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  try {
                  } catch (_: InterruptedException) {
                  }
              }
              """
          )
        );
    }
}
