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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.marker.Implicit;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"UnusedReceiverParameter", "PropertyName", "RemoveCurlyBracesFromTemplate", "UnnecessaryStringEscape", "RedundantGetter", "ConstantConditionIf", "RedundantSetter", "RedundantSemicolon"})
class VariableDeclarationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "\n",
      "\r\n"
    })
    void singleVariableDeclaration(String newLine) {
        rewriteRun(
          kotlin("%sval a = 1%s".formatted(newLine, newLine))
        );
    }

    @Test
    void addition() {
        rewriteRun(
          kotlin("val a = 1 + 1")
        );
    }

    @Test
    void deSugar() {
        rewriteRun(
          kotlin(
                """
            val a = if (2 !in 1 .. 10) "X" else "Y"
            """
          )
        );
    }

    @Test
    void yikes() {
        rewriteRun(
          kotlin("val b =  !   (    (     1 .  plus   (    2     ) +  2   )    !in      1 ..  3   )    .     not( )")
        );
    }

    @Test
    void singleVariableDeclarationWithTypeConstraint() {
        rewriteRun(
          kotlin("val a = ArrayList<String>()")
        );
    }

    @Test
    void anonymousObject() {
        rewriteRun(
          kotlin(
            """
              open class Test
              val o  : Test   =  object   :     Test  (   )     {    }
              """
          )
        );
    }

    @Test
    void ifExpression() {
        rewriteRun(
          kotlin(
            """
              val latest = if ( true ) {
                  "latest.release"
              } else {
                  "latest.integration"
              }
              """
          )
        );
    }

    @Test
    void inline() {
        rewriteRun(
          kotlin(
            """
              class Spec
              inline val  Spec   .    `java-base`     : String  get   (    )   =  "  "
              """
          )
        );
    }

    @Test
    void getter() {
        rewriteRun(
          kotlin(
            """
              val isEmpty   : Boolean
                  get ( ) : Boolean = 1 == 1
              """
          )
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("val `quoted-id` = true")
        );
    }

    @Test
    void stringTemplate() {
        rewriteRun(
          kotlin(
            """
              val a = "Hello"
              val b = "World"
              val c = "${a} ${b}!"
              
              val after = 0
              """
          )
        );
    }

    @Test
    void stringTemplateNoBraces() {
        rewriteRun(
          kotlin(
            """
              val a = "Hello"
              val b = "World"
              val c = "$a $b!"
              
              val after = 0
              """
          )
        );
    }

    @Test
    void whitespaceAfter() {
        rewriteRun(
          kotlin(
            """
              val a = "Hello"
              val b = " $a "
              val c = " ${a} "
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/172")
    @Test
    void propertyAccessor() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val value = 10
              }
              val a = Test ( )
              val b = " ${   a . value   }"
              
              val after = 0
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/172")
    @Test
    void multipleFieldAccess() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val testValue = Inner ( )
                  class Inner {
                      val innerValue = 10
                  }
              }
              
              val a = Test ( )
              val b = "${   a . testValue . innerValue   }"
              """
          )
        );
    }

    @Test
    void tripleQuotedString() {
        rewriteRun(
          kotlin(
            """
              val template = \"\"\"
                Hello world!
              \"\"\"
              """
          )
        );
    }

    @Test
    void mapOf() {
        rewriteRun(
          kotlin(
            """
              val map =   mapOf ( 1 to "one" , 2 to "two" , 3 to "three" )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/13")
    @Test
    void wildcard() {
        rewriteRun(
          kotlin(
            """
              class Test<T>
              val a : Test  <   *    >?     = null
              """
          )
        );
    }

    @Test
    void ifElseExpression() {
        rewriteRun(
          kotlin(
            """
              fun method ( condition : Boolean ) : Unit = if ( condition ) Unit else Unit
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/58")
    @Test
    void destructuringVariableDeclaration() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun example  (   )    {
                val   (    a     , b  ,   c    )     = Triple  (   1    ,     "Two" ,  3   )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/76")
    @Test
    void delegationByLazy() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val value by lazy { 10 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/264")
    @Test
    void delegationByLazyWithType() {
        rewriteRun(
          kotlin(
            """
              class User {
                  val value  :   Int    by     lazy {  10   }
              }
              """
          )
        );
    }

    @Test
    void delegatedProperty() {
        rewriteRun(
          kotlin(
            """
              import kotlin.reflect.KProperty

              class IntDelegate {
                  private var storedValue: Int = 0

                  operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                      println("Getting value of property ${property.name}")
                      return storedValue
                  }

                  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                      println("Setting value of property ${property.name}")
                      storedValue = value
                  }
              }

              class Example {
                  var value: Int by IntDelegate()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/277")
    @Test
    void provideDelegateBinaryType() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath("clikt")),
          kotlin(
            //language=none, disabled since Kotlin inspection does not detect the classpath
            """
              import com.github.ajalt.clikt.core.CliktCommand
              import com.github.ajalt.clikt.parameters.arguments.argument
              import com.github.ajalt.clikt.parameters.arguments.multiple
              import com.github.ajalt.clikt.parameters.types.file

              class CodeGenCli : CliktCommand("Generate Java sources for SCHEMA file(s)") {
                  private val schemas by argument().file(mustExist = true).multiple()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/277")
    @Test
    void provideDelegateExtension() {
        rewriteRun(
          kotlin(
            """
              operator fun String.provideDelegate(thisRef: T,
                     prop :  kotlin   .    reflect     . KProperty  <*>   ): kotlin.properties.ReadOnlyProperty<T, Any> {
                  return null!!
              }
              class T {
                  val s by ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/82")
    @Test
    void genericIntersectionType() {
        rewriteRun(
          kotlin(
            """
              val first : String = "1"
              val second : Int = 2
              
              val l = listOf ( "foo" to first , "bar" to second )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/84")
    @Test
    void unresolvedNameFirSource() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlin(
            //language=none, disabled due to invalid code
            """
              val t = SomeInput . Test
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/84")
    @Test
    void varargArgumentExpression() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              class StringValue {
                  val value : String = ""
              }
              fun method ( input : Any ) {
                  val split = (  input   as    StringValue     ) .  value   .    split ( "-" ) .  toTypedArray   ( )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/93")
    @Test
    void parameterizedReceiver() {
        rewriteRun(
          kotlin(
            """
              class SomeParameterized<T>
              val SomeParameterized < Int > . receivedMember : Int
                  get ( ) = 42
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/93")
    @Test
    void abstractReceiver() {
        rewriteRun(
          kotlin(
            """
              class SomeParameterized<T>
              abstract class Test {
                  abstract val SomeParameterized <  Int > . receivedMember : Int
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/93")
    @SuppressWarnings("RedundantSetter")
    @Test
    void setter() {
        rewriteRun(
          kotlin(
            """
              var s : String = ""
                  set  (   value    )     {
                      field  =   value
                  }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/93")
    @Test
    void getterBeforeSetter() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var stringRepresentation : String = ""
                      get ( ) = field
                      set ( value ) {
                          field = value
                      }
              }
              """
          )
        );
    }

    @Test
    void getterSetterWithTrailingSemiColon() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var stringRepresentation : String = ""
                      get ( ) = field   ;
                      set ( value ) {
                          field = value
                      } ;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/93")
    @Test
    void setterBeforeGetter() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var stringRepresentation : String = ""
                      set ( value ) {
                          field = value
                      }
                      get ( ) = field
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/560")
    @Test
    void accessorAfterTrailingSemiColon() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var n: Int = 0  ;   protected set
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/135")
    @Test
    void checkNonNull() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun foo() {
                  val l: List<String?> = listOf ( "x" )
                  val a = l [ 0 ] !!
              }
              """
          )
        );
    }

    @Test
    void hasFinalModifier() {
        rewriteRun(
          kotlin(
            """
              val l = 42
              """,
                spec -> spec.afterRecipe(cu -> {
                    for (Statement statement : cu.getStatements()) {
                        if (statement instanceof J.VariableDeclarations) {
                            J.Modifier.hasModifier(((J.VariableDeclarations) statement).getModifiers(), J.Modifier.Type.Final);
                            assertThat(J.Modifier.hasModifier(((J.VariableDeclarations) statement).getModifiers(), J.Modifier.Type.Final)).isTrue();
                        }
                    }
                }))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/207")
    @Test
    void preserveTrailingSemicolon() {
        rewriteRun(
          kotlin(
            """
              val a  =   1   /*C1*/  ; /*C2*/
              val    b   =  2 ;
              """
          )
        );
    }

    @Test
    void anonymousObjectWithoutSupertype() {
        rewriteRun(
          kotlin(
            """
              val x: Any = object  {}   .    javaClass
              """
          )
        );
    }

    @Test
    void spaceBetweenEqualsInDestructuringDeclaration() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun getUserInfo(): Pair<String, String> {
                  return Pair("Leo", "Messi")
              }

              fun main() {
                  val (firstName, lastName)   =     getUserInfo()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/286")
    @Test
    void unusedUnderScoreVariableInDestructuringDeclaration() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun getUserInfo(): Pair<String, String> {
                  return Pair("Leo", "Messi")
              }

              fun main() {
                  val (_, lastName) = getUserInfo()
              }
              """
          )
        );
    }

    @Test
    void typeExpressionPresent() {
        rewriteRun(
          kotlin(
            """
              val i1 = 1
              var i2 = 3
              val i3: Int = 2
              """,
            spec -> spec.afterRecipe(cu ->
                assertThat(cu.getStatements()).satisfiesExactly(
                    i1 -> assertThat(((J.VariableDeclarations) i1).getTypeExpression()).satisfies(
                        type -> assertThat(type).isNull()
                    ),
                    i2 -> assertThat(((J.VariableDeclarations) i2).getTypeExpression()).satisfies(
                        type -> assertThat(type).isNull()
                    ),
                    i3 -> assertThat(((J.VariableDeclarations) i3).getTypeExpression()).satisfies(
                        type -> {
                            assertThat(type).isInstanceOf(J.Identifier.class);
                            assertThat(((J.Identifier) type).getSimpleName()).isEqualTo("Int");
                            assertThat(type.getMarkers().findFirst(Implicit.class)).isEmpty();
                            assertThat(type.getType()).isInstanceOf(JavaType.Class.class);
                            assertThat(((JavaType.Class) Objects.requireNonNull(type.getType())).getFullyQualifiedName()).isEqualTo("kotlin.Int");
                        }
                    )
                )
            )
          )
        );
    }

    @Test
    void typealias() {
        rewriteRun(
          kotlin(
            """
              class Other
              typealias  OldAlias   =    Other
              """
          )
        );
    }

    @Test
    void typealiasLambda() {
        rewriteRun(
          kotlin(
            """
              /*C1*/ typealias   Operation =  (Int , Int )   ->    Int
              """
          )
        );
    }

    @Test
    void typedFunctionCallInitializer() {
        rewriteRun(
          kotlin(
            """
              val x = emptySet  <   String    > (  )
              """
          )
        );
    }

    @Test
    void trailingSemicolon() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var t /*C0*/ : /*C1*/ Int = 1 /*C2*/  ; /*C3*/
                  fun method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/416")
    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun example ( ) {
                val (
                  a ,
                  b   ,
                ) = Pair ( 1, 2 )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/404")
    @Test
    void destructuringWithTypes() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_VARIABLE")
              fun example ( ) {
                val ( a   :  Int   , b :  String  ?   ) = Pair ( 1 , "Two" )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/455")
    @Test
    void starTypeProjection() {
        rewriteRun(
          kotlin(
            """
              val f = Enum<*>::name
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/446")
    @Test
    void trailingCommaOnParameterized() {
        rewriteRun(
          kotlin(
            """
             val m: Map<
                Int ,
                String ,
              > = mapOf()
             """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/447")
    @Test
    void extensionProperty() {
        rewriteRun(
          kotlin(
            """
              class A {
                  var internalProperty = "x"
              }

              var A .  property   : String
                  get() = ""
                  set(value) {
                      internalProperty = value
                  }
              """
          )
        );
    }

    @Test
    void typeReferencePrefix_1_VariableDeclarations() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var t /*C1*/ : Int = 1 /*C2*/  ; /*C3*/
                  fun method() {}
              }
              """
          )
        );
    }

    @Test
    void typeReferencePrefix_2_MethodParameter() {
        rewriteRun(
          kotlin(
            """
              fun method ( input /*C0*/ : Any = 1 /*C2*/ , x : Int ) {
              }
              """
          )
        );
    }

    @Test
    void typeReferencePrefix_3_MethodReturnType() {
        rewriteRun(
          kotlin(
            """
              fun method () /*C3*/ :  Int {
                 return 1
              }
              """
          )
        );
    }
}
