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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("ALL")
class ClassDeclarationTest implements RewriteTest {

    @Test
    void whitespaceInImport() {
        rewriteRun(
          kotlin(
            """
              import java . util . Collections as cs
              import java . io . *

              class A
              """
          )
        );
    }

    @Test
    void annotatedConstructor() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL") @Deprecated("",ReplaceWith("Any()")) constructor() {
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedSupertype() {
        rewriteRun(
          kotlin(
            """
              interface I1 {}
              class Test :    @Suppress  Any() ,I1  {
              }
              """
          )
        );
    }

    @Test
    void annotatedDelegatingConstructor() {
        rewriteRun(
          kotlin(
            """
              class Test(i: Int) {
                  @Suppress("ALL") // comment
                  internal constructor() : this(0)
              }
              """
          )
        );
    }

    @Test
    void multipleClassDeclarationsInOneCompilationUnit() {
        rewriteRun(
          kotlin(
            """
              package some.other.name
              class A { }
              class B { }
              """
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          kotlin(
            """
              class A
              class B
              """
          )
        );
    }

    @Test
    void classImplements() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B
              class C : A , B
              class D : B , A
              """
          )
        );
    }

    @Test
    void classExtends() {
        rewriteRun(
          kotlin(
            """
              open class A
              class B : A ( )
              """
          )
        );
    }

    @Test
    void extendsAndImplementsInMixedOrder() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B
              open class C

              class D : A , C ( ) , B
              """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          kotlin(
            """
              interface  C   {
                  class Inner {
                  }
              }
              """,
                spec -> spec.afterRecipe(cu -> {
                    assertThat(cu.getStatements().stream()
                            .anyMatch(it -> it instanceof J.ClassDeclaration &&
                                    ((J.ClassDeclaration) it).getKind() == J.ClassDeclaration.Kind.Type.Interface)).isTrue();
                })
          )
        );
    }

    @Test
    void modifierOrdering() {
        rewriteRun(
          kotlin("public /* comment */ abstract open class A")
        );
    }

    @Test
    void annotationClass() {
        rewriteRun(
          kotlin("annotation class A",
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements().stream()
                  .anyMatch(it -> it instanceof J.ClassDeclaration &&
                    ((J.ClassDeclaration) it).getKind() == J.ClassDeclaration.Kind.Type.Annotation)).isTrue();
            }))
          );
    }

    @Test
    void enumClass() {
        rewriteRun(
          kotlin("enum  class A",
            spec -> spec.afterRecipe(cu -> {
              assertThat(cu.getStatements().stream()
                .anyMatch(it -> it instanceof J.ClassDeclaration &&
                  ((J.ClassDeclaration) it).getKind() == J.ClassDeclaration.Kind.Type.Enum)).isTrue();
          }))
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          kotlin(
            """
              public  @Deprecated ( "message 0" )   abstract    @Suppress("") class Test

              @Deprecated ( "message 1" )
              @Suppress ( "" )
              class A

              @Suppress ( "unused" , "unchecked" )
              @Deprecated ( "message 2" )
              class B
              """
          )
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("class `Quoted id here`")
        );
    }

    @Test
    void typeArguments() {
        rewriteRun(
          kotlin("open class B < T > { }")
        );
    }

    @Test
    void singleBoundedTypeParameters() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B

              class KotlinTypeGoat < T : A , S : B>
              """
          )
        );
    }

    @Test
    void primaryConstructor() {
        rewriteRun(
          kotlin("class Test  (   val    answer : Int )")
        );
    }

    @Test
    void primaryConstructorWithAnySupertype() {
        rewriteRun(
          kotlin("class Test  :   Any    (     )")
        );
    }

    @Test
    void primaryConstructorWithParameterizedSupertype() {
        rewriteRun(
          kotlin("class Test : java.util.ArrayList<String>()")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/74")
    @Test
    void secondaryConstructor() {
        rewriteRun(
          kotlin(
            """
              class Test ( val answer : Int ) {
                  /*c1*/ constructor  /*c2*/ (   )    : this  (   42    )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/94")
    @Test
    void explicitInlineConstructor() {
        rewriteRun(
          kotlin("class Test  internal   constructor    ( )")
        );
    }

    @Test
    void implicitConstructorWithSuperType() {
        rewriteRun(
          kotlin(
            """
              open class Other
              class Test constructor ( val answer : Int ) : Other ( ) { }
              """
          )
        );
    }

    @Test
    void singleLineCommentBeforeModifier() {
        rewriteRun(
          kotlin(
            """
              @Deprecated ( "" )
              // Some comment
              open class A
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/160")
    @Test
    void multipleBounds() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B
              interface C
              interface D

              class KotlinTypeGoat<T, S>  where   S : A, T : D, S : B, T : C
              """
          )
        );
    }

    @Test
    void object() {
        rewriteRun(
          kotlin(" object Test")
        );
    }

    @Test
    void objectWithMultipleSuperTypes() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B
              object Foo  :   A    ,      B { }
              """
          )
        );
    }

    @Test
    void suspendFunctionTypeAsSuperType() {
        rewriteRun(
          kotlin(
            """
              abstract class  Test   :    suspend (  )   ->    String
              """
          )
        );
    }

    @Test
    void explicitDelegation() {
        rewriteRun(
          kotlin(
            """
              class Test(c : Collection<String>): Collection<String> by c
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/190")
    @Test
    void companionObject() {
        rewriteRun(
          kotlin("object Companion"),
          kotlin(
            """
              class TestA {
                  companion object
              }
              """
          ),
          kotlin(
            """
              class TestB {
                  companion object Foo
              }
              """
          ),
          kotlin(
            """
              class TestC {
                  companion object Companion
              }
              """
          )
        );
    }

    @Test
    void variance() {
        rewriteRun(
          kotlin("interface A  <   in    R     >"),
          kotlin("interface B < out R >")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/72")
    @Test
    void sealedClassWithPropertiesAndDataClass() {
        rewriteRun(
          kotlin(
            """
              sealed class InvalidField {
                  abstract val field : String
              }
              data class InvalidEmail ( val errors : List < String > ) : InvalidField ( ) {
                  override val field : String = "email"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/72")
    @Test
    void sealedInterfaceWithPropertiesAndDataClass() {
        rewriteRun(
          kotlin(
            """
              sealed interface InvalidField {
                  val field : String
              }
              data class InvalidEmail ( val errors : List < String > ) : InvalidField {
                  override val field : String = "email"
              }
              """
          )
        );
    }

    @Test
    void sealedInterfaceWithPropertiesAndObject() {
        rewriteRun(
          kotlin(
            """
              sealed interface InvalidField {
                  val field : String
              }
              object InvalidEmail  :   InvalidField    {
                  override val field : String = "email"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/68")
    @Test
    void init() {
        rewriteRun(
          kotlin(
                """
            class Test {
                init   {
                    println ( "Hello, world!" )
                }
            }
            """
          )
        );
    }

    @Test
    void valueClass() {
        rewriteRun(
          kotlin("@JvmInline value class Wrapper ( val int : Int )")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/66")
    @Test
    void typeParameterReference() {
        rewriteRun(
          kotlin(
            """
              abstract class BaseSubProjectionNode < T , R > (
                  val parent : T,
                  val root : R
              ) {

                  constructor ( parent : T , root : R , id : Int ) : this ( parent , root )

                  fun parent ( ) : T {
                      return parent
                  }

                  fun root ( ) : R {
                      return root
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/99")
    @Test
    void implicitThis() {
        rewriteRun(
          kotlin(
            """
              abstract class Test ( arg : Test  .   ( )  ->   Unit    =  { } ) {
                  init {
                      arg ( )
                  }
              }
              """
          )
        );
    }

    @Test
    void mixedAnnotationsAndModifiers() {
        rewriteRun(
          kotlin(
            """
              @Repeatable
              annotation class A ( val s : String )

              open @A ( "1" ) public @A ( "2" ) class TestA
              @A ( "1" ) open @A ( "2" ) public class TestB
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              class Test(val attr: String,)
              """
          )
        );
    }

    @Test
    void hasFinalModifier() {
        rewriteRun(
          kotlin("class A",
            spec -> spec.afterRecipe(cu -> {
                for (Statement statement : cu.getStatements()) {
                    if (statement instanceof J.ClassDeclaration) {
                        J.Modifier.hasModifier(((J.ClassDeclaration) statement).getModifiers(), J.Modifier.Type.Final);
                        assertThat(J.Modifier.hasModifier(((J.ClassDeclaration) statement).getModifiers(), J.Modifier.Type.Final)).isTrue();
                    }
                }
            }))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/270")
    @Test
    void onlySecondaryConstructors() {
        rewriteRun(
          kotlin(
            """
              class SerializationException : IllegalArgumentException {
                  constructor(message: String?, cause: Throwable?) : super(message, cause)
              }
              """,
              spec -> spec.afterRecipe(cu -> {
                  assertThat(cu.getStatements()).satisfiesExactly(stmt -> {
                      J.ClassDeclaration clazz = (J.ClassDeclaration) stmt;
                      assertThat(clazz.getBody().getStatements()).satisfiesExactly(decl -> {
                          K.Constructor constructor = (K.Constructor) decl;
                          assertThat(constructor.getMethodDeclaration().getParameters()).satisfiesExactly(
                              message -> assertThat(message).isInstanceOf(J.VariableDeclarations.class),
                              cause -> assertThat(cause).isInstanceOf(J.VariableDeclarations.class)
                          );
                          assertThat(constructor.getMethodDeclaration().getBody()).isNull();
                          assertThat(constructor.getInvocation().getArguments()).satisfiesExactly(
                              message -> assertThat(message).isInstanceOf(J.Identifier.class),
                              cause -> assertThat(cause).isInstanceOf(J.Identifier.class)
                          );
                      });
                  });
              })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/270")
    @Test
    void secondaryConstructorWithBody() {
        rewriteRun(
          kotlin(
            """
              class SerializationException : IllegalArgumentException {
                  constructor(message: String?, cause: Throwable?) : super(message, cause) {
                      println("foo")
                  }
              }
              """
          )
        );
    }

    @Test
    void localClass() {
        rewriteRun(
          kotlin(
            """
              fun foo() {
                  class Inner
              }
              """
          )
        );
    }

    @Test
    void coneProjection() {
        rewriteRun(
          kotlin(
            """
              val map = mapOf(Pair("one", 1)) as? Map<*, *>
              val s = map.orEmpty().entries.joinToString { (key, value) -> "$key: $value" }
              """
          )
        );
    }

    @Test
    void outerClassTypeParameters() {
        rewriteRun(
          kotlin(
            """
              class Test<K, V> {
                  abstract inner class LinkedTreeMapIterator<T> : MutableIterator<T> {
                      var lastReturned: Map.Entry<K, V>? = null
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/301")
    @Test
    void qualifiedSuperType() {
        rewriteRun(
          kotlin(
            """
              abstract class LinkedHashTreeMap<K, V> : AbstractMutableMap<K, V>() {
                  abstract inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, V>>()
              }
              """
          )
        );
    }

    @Test
    void constructorWithModifier() {
        rewriteRun(
          kotlin(
            """
              import java.lang.RuntimeException

              public class MyException : RuntimeException {
                  public constructor() : super()
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "abstract",
      "internal",
      "private",
      "protected",
      "public",
      "sealed",
    })
    void modifiers(String input) {
        rewriteRun(
          kotlin(
            "%s class Foo".formatted(input)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/396")
    @Test
    void explicitExtendsAny() {
        rewriteRun(
          kotlin("class Foo< out  T   :     Any >  () {}"
          )
        );
    }

    @Test
    void variantWithAnnotaton() {
        rewriteRun(
          kotlin(
            """
            @Target(AnnotationTarget.TYPE_PARAMETER)
            annotation class Anno
            class Foo  <   @Anno     out     T :  Any    >  () {}
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/403")
    @Test
    void trailingCommaInTypeArgument() {
        rewriteRun(
          kotlin("open class B <  T   , > { }")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/435")
    @ParameterizedTest
    @ValueSource(strings = {
      "class Test {};",
      "class Test ();",
      "class Test {} /*C0*/;   ",
      "class Test {} /*C0*/;   class Next"
    })
    void trailingSemiColon(String input) {
        rewriteRun(
          kotlin(
            """
              %s
              """.formatted(input)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/437")
    @Test
    void enumClassWithParametersWithTrailingComma() {
        rewriteRun(
          kotlin(
            """
              enum class Code {
                  YES ,
              }
              enum class Test ( val arg: Code , ) {
                  FOO ( Code.YES , ) {
                      // Body is required to reproduce issue
                  }
              }
              """
          )
        );
    }

}
