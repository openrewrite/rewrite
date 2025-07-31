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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantSuppression", "RedundantNullableReturnType", "RedundantVisibilityModifier", "UnusedReceiverParameter", "SortModifiers", "TrailingComma", "RedundantGetter", "RedundantSetter"})
class AnnotationTest implements RewriteTest {

    @Test
    void fileScope() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath()),
          kotlin(
            """
              @file : Suppress  (   "DEPRECATION_ERROR" )

              class A
              """
          )
        );
    }

    @Test
    void multipleFileScope() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.FILE)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno
              """
          ),
          kotlin(
            //language=none
            """
              @file : Anno
              @file : Anno
              @file : Anno
              """
          )
        );
    }

    @Test
    void annotationOnEnumEntry() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath()),
          kotlin(
            """
              annotation class Anno
              enum class EnumTypeA {
                  @Anno
                  FOO
              }
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
                J.VariableDeclarations v = (J.VariableDeclarations) ((J.ClassDeclaration) cu.getStatements().get(2)).getBody().getStatements().getFirst();
                assertThat(v.getLeadingAnnotations()).hasSize(2);
            })
          )
        );
    }

    @Test
    void arrayArgument() {
        rewriteRun(
          kotlin(
            """
              @Target (  AnnotationTarget . PROPERTY   )
              @Retention  ( AnnotationRetention . SOURCE )
              annotation class Anno ( val values : Array <  String > )

              @Anno( values =  [   "a"    ,     "b" ,  "c"   ]    )
              val a = 42
              """
          )
        );
    }

    @Test
    void fullyQualifiedAnnotation() {
        //noinspection RemoveRedundantQualifierName
        rewriteRun(
          kotlin(
            """
              @kotlin.Deprecated("")
              class A
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno ( val values :   Array < String > )
              @Anno( values = [ "a" , "b" ,  /* trailing comma */ ] )
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
          kotlin(
            """
              annotation class Anno
              class TestA {
                  @get : Anno
                  @set : Anno
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
          kotlin(
            """
              annotation class Anno
              class TestA {
                  @get : Anno
                  public
                  @set : Anno
                  var name: String = ""
              }
              """
          ),
          kotlin(
            """
              annotation class Anno
              class TestB {
                  public
                  @get  : Anno
                  @set :  Anno
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
          kotlin(
            """
              annotation class Anno
              class Example(
                  @get : Anno
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
          kotlin(
            """
              annotation class Anno
              class Example ( /**/  /**/ @get : Anno /**/ /**/ @set : Anno /**/ /**/ var foo: String , @get : Anno val bar: String )
              """
          )
        );
    }

    @Test
    void annotationOnExplicitGetter() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              class Test {
                  public var stringRepresentation : String = ""
                      @Anno
                      // comment
                      get ( ) = field

                      @Anno
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
          kotlin(
            """
              annotation class Anno
              class Example  (   @param    :     Anno val  quux   :     String )
              """
          )
        );
    }

    @Test
    void fieldAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              class Example ( @field : Anno val foo : String )
              """
          )
        );
    }

    @Test
    void receiverAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              fun @receiver  :   Anno    String     . myExtension  (   )    {      }
              """
          )
        );
    }

    @Test
    void setParamAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno

              class Example {
                  @setparam : Anno
                  @set: Anno
                  var name: String = ""
              }
              """
          )
        );
    }

    @Test
    void destructuringVariableDeclaration() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              annotation class Anno

              fun example ( ) {
                val (  @Anno   a , @Anno b , @Anno c ) = Triple ( 1 , 2 , 3 )
              }
              """
          )
        );
    }

    @Test
    void annotationsInManyLocations() {
        rewriteRun(
          kotlin(
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

              @Suppress("RedundantSetter","RedundantSuppression")
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
              @Ann typealias Other =   @Ann  String
              """
          )
        );
    }

    @Test
    void lastAnnotations() {
        rewriteRun(
          kotlin(
            """
              annotation class A
              annotation class B
              annotation class C
              annotation class LAST

              @A open  @B   internal    @C @LAST  class Foo
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration last = (J.ClassDeclaration) cu.getStatements().get(cu.getStatements().size() - 1);
                List<J.Annotation> annotationList = last.getPadding().getKind().getAnnotations();
                assertThat(annotationList).hasSize(2);
                assertThat(annotationList.getFirst().getSimpleName()).isEqualTo("C");
                assertThat(annotationList.get(1).getSimpleName()).isEqualTo("LAST");
            })
          )
        );
    }

    @Test
    void lambdaExpression() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.EXPRESSION)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno

              fun method ( ) {
                  val list = listOf ( 1 , 2 , 3 )
                  list  .  filterIndexed { index  ,   _    -> @Anno  index   %    2 == 0 }
              }
              """
            )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/267")
    @Test
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

              @Suppress( )
              @Ann
              class A
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "@A @B object C",
      "@A internal @B object C"
    })
    void objectDeclaration(String input) {
        rewriteRun(
          kotlin(
            """
              annotation class A
              annotation class B

              %s
              """.formatted(input)
          )
        );
    }

    @Test
    void annotationEntryTrailingComma() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno ( val a: Int = 1, val b: Int = 2, val c: Int = 3 )
              @Anno( a = 42, b = 42, c = 42 , // Trailing comma HERE
              )
              class Test
              """
          )
        );
    }

    @Test
    void commentBeforeGetter() {
        rewriteRun(
          kotlin(
            """
              public class Movie(
                  title: () -> String? = { throw IllegalStateException("Field `title` was not requested") }
              ) {
                  private val _title: () -> String? = title

                  /**
                   * The original, non localized title with some specials characters : %!({[*$,.:;.
                   */
                  @get:JvmName("getTitle")
                  public val title: String?
                      get() = _title.invoke()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/377")
    @Test
    void annotatedTypeParameter() {
        rewriteRun(
          kotlin(
            """
              val releaseDates: List< /*C0*/ @Suppress  /*C1*/ String> = emptyList()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/398")
    @Test
    void annotatedFunction() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.TYPE)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno

              class Foo(
                private val option:  @Anno   () -> Unit
              )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/408")
    @ParameterizedTest
    @ValueSource(strings = {
      "String",
      "Map<*, *>"
    })
    void annotatedTypeParameter(String input) {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.TYPE)
              annotation class Ann
              class Test(
                val map: Map< @Ann  %s   ,    @Ann %s  > = emptyMap()
              )
              """.formatted(input, input)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/397")
    @Test
    void fieldUseSiteWithMultipleAnnotations() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.FIELD)
              @Retention(AnnotationRetention.SOURCE)
              annotation class A1

              @Target(AnnotationTarget.FIELD)
              @Retention(AnnotationRetention.SOURCE)
              annotation class A2 (val name: String)

              class Test {
                  @field :  [   A1    A2 (  "numberfield "   )    ]
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/565")
    @Test
    void useSiteMultiAnnotationAfterAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              class Example(
                  @Deprecated("")
                  @get : [Anno]
                  val bar : String
              )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/397")
    @Test
    void fieldUseSiteWithSingleAnnotationInBracket() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.FIELD)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno
              class A {
                  @field: [ Anno ]
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Test
    void fieldUseSiteWithSingleAnnotationImplicitBracket() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.FIELD)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno
              class A {
                  @field :  Anno
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/453")
    @Test
    void arrayOfCallWithInAnnotation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlin(
            """
              @Target(AnnotationTarget.FUNCTION)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Ann(
                  val test: Test
              )
              annotation class Test ( val arg: Array<String> )
              @Ann(test = Test(
                  arg = arrayOf("")
              ))
              fun use() {
              }
              """
          )
        );
    }

    @Test
    void annotatedIntersectionType() {
        rewriteRun(
          kotlin(
            """
              import java.util.*

              @Target(AnnotationTarget.TYPE)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno

              @Suppress("UNUSED_PARAMETER")
              fun < T : Any ? > test( n : Optional < @Anno  T   &   @Anno Any > = Optional.empty < T > ( ) ) { }
              """
          )
        );
    }

    @Test
    void allUseSiteCases() {
        rewriteRun(
          kotlin(
            """
              @file : Suppress ( "UNUSED_VARIABLE" )
              class C

              annotation class Ann1
              annotation class Ann2
              // case 0, Non use-site, regular annotation
              @Ann1
              val x1 = 40

              // case 1, use-site, implicit bracket
              @field : Ann1
              val x2 = 41

              // case 2, use-site, explicit bracket
              @field : [ Ann1 ]
              val x3 = 42

              // case 3, use-site, multi annotations with explicit bracket
              @field : [Ann1 Ann2]
              val x4 = 43

              // case 4, use-site without target
              @[ Ann1 Ann2]
              val x5 = 44
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/552")
    @Test
    void trailingAnnotationOnSecondaryConstructor() {
        rewriteRun(
          kotlin(
            """
              annotation class A1
              class Test ( val answer : Int ) {
                  private @A1 constructor  (   )    : this  (   42    )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/552")
    @Test
    void trailingAnnotationOnPropertyAccessor() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann
              var s : String = ""
                  internal @Ann set  (   value    )     {
                      field  =   value
                  }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/552")
    @Test
    void trailingAnnotationOnTypeReference() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              class Example  (   public    @Anno val  quux   :     String )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/552")
    @Test
    void trailingAnnotationNamedFunction() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              internal @Anno fun method() {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/557")
    @Test
    void annotatedTypeInFunctionTypeParens() {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.TYPE)
              @Retention(AnnotationRetention.SOURCE)
              annotation class Anno
              fun method ( ) {
                  val lambda : suspend (   @Anno Int ) -> Int = { number : Int -> number * number }
              }
              """
          )
        );
    }
}
