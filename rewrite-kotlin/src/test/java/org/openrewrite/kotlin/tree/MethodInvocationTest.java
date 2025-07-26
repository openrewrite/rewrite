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
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.marker.IndexedAccess;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantVisibilityModifier", "PropertyName", "RedundantNullableReturnType", "UnusedReceiverParameter", "ConstantConditionIf", "MoveLambdaOutsideParentheses"})
class MethodInvocationTest implements RewriteTest {

    @Test
    void implicitFunctionCall() {
        rewriteRun(
          kotlin(
            """
              fun plugins ( input : ( ) -> String ) {
                  println ( input ( ) )
              }
              fun main ( ) {
                  plugins {
                      "test"
                  }
              }
              """
          )
        );
    }

    @Test
    void unqualifiedImportedCall() {
        rewriteRun(
          kotlin(
            //language=none
            """
              package pkg
              
              import pkg.Callee.calleeMethod  /* C1 */
              import pkg.Callee.CALLEE_FIELD  /* C2 */
              
              class Caller {
                  fun method(): Any = calleeMethod()
                  fun method2(): Any = CALLEE_FIELD
              }
              
              /*42*/ object Callee {
                  const val CALLEE_FIELD = ""
                  fun calleeMethod(): Unit = Unit
              }
              """
          )
        );
    }

    @Test
    void buildGradle() {
        rewriteRun(
          kotlin(
            """
              class Spec {
                  var id = ""
                  fun id ( arg : String) : Spec {
                      return this
                  }
                  fun version ( version : String) : Spec {
                      return this
                  }
              }

              class SpecScope  {
                  val delegate : Spec = Spec ( )
                  fun id ( id : String ) : Spec = delegate . id ( id )
              }
              infix fun Spec . version ( version : String ) : Spec = version ( version )
              public inline val SpecScope . `java-library` : Spec get ( ) = id ( "org.gradle.java-library" )

              class DSL  {
                  fun plugins ( block : SpecScope . ( ) -> Unit ) {
                      block ( SpecScope ( ) )
                  }
              }

              fun method ( ) {
                  DSL ( ) .
                  
                  plugins {
                      `java-library`
                  
                      id ( "nebula.release") version "16.0.0"
                  
                      id ( "nebula.maven-manifest" ) version "18.4.0"
                      id ( "nebula.maven-nebula-publish" ) version "18.4.0"
                      id ( "nebula.maven-resolved-dependencies" ) version "18.4.0"
                  
                      id ( "nebula.contacts" ) version "6.0.0"
                      id ( "nebula.info" ) version "11.3.3"
                  
                      id ( "nebula.javadoc-jar" ) version "18.4.0"
                      id ( "nebula.source-jar" ) version "18.4.0"
                  }
              }
              """
          )
        );
    }

    @Test
    void methodWithLambda() {
        rewriteRun(
          kotlin(
            """
              fun method ( arg : Any ) { }
              fun callMethodWithLambda ( ) {
                  method {
                  }
              }
              """
          )
        );
    }

    @Test
    void nullSafeDereference() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method ( ) {
                  }
              }
              fun method ( test : Test ? ) {
                  val a = test ?. method ( )
              }
              """
          )
        );
    }

    @Test
    void elvisOperator() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method () : String ? {
                      return ""
                  }
              }
              val t = Test ( )
              fun method ( ) {
                  val a = t  .   method ( ) ?: null
              }
              """
          )
        );
    }

    @Test
    void listOf() {
        rewriteRun(
          kotlin(
            """
              fun method ( arg : Any ) {
                  val l  =   listOf    (     1 ,  2   ,    3     )
              }
              """
          )
        );
    }

    @Test
    void mapOf() {
        rewriteRun(
          kotlin(
            """
              val map = mapOf ( 1 to "one" , 2 to "two" , 3 to "three" )
              """
          )
        );
    }

    @Test
    void multipleTypesOfMethodArguments() {
        rewriteRun(
          kotlin(
            """
              fun methodA ( a : String , b : Int , c : Double ) { }
              fun methodB ( ) {
                  methodA ( "a" , 1 , 2.0 )
              }
              """
          )
        );
    }

    @Test
    void parameterAssignment() {
        rewriteRun(
          kotlin(
            """
              fun apply ( plugin : String ? = null) { }
              fun method ( ) {
                  apply ( plugin = "something" )
              }
              """
          )
        );
    }

    @Test
    void typeParameters() {
        rewriteRun(
          kotlin(
            """
              fun < T : Number > methodA ( type : T ) { }
              fun methodB ( ) {
                  methodA < Int > ( 10 )
              }
              """
          )
        );
    }

    @Test
    void anonymousObject() {
        rewriteRun(
          kotlin(
            """
              open class Test
              
              fun test ( a : Test ) { }
              
              fun method ( ) {
                  test ( object :  Test   ( ) {
                  } )
              }
              """
          )
        );
    }

    @Test
    void lambdaArgument() {
        rewriteRun(
          kotlin(
            """
              interface Test < in R > {
                  public fun < B > shift ( r : R ) : B
                  public fun ensure ( condition : Boolean , shift : ( ) -> R ) : Unit =
                      if ( condition ) Unit else shift ( shift ( ) )
              }
              fun Test < String > . test ( ) : Int {
                  ensure ( false , { "failure" } )
                  return 1
              }
              """
          )
        );
    }

    @Test
    void trailingLambdaArgument() {
        rewriteRun(
          kotlin(
            """
              interface Test < in R > {
                  public fun < B > shift ( r : R ) : B
                  public fun ensure ( condition : Boolean , shift : ( ) -> R ) : Unit =
                      if ( condition ) Unit else shift ( shift ( ) )
              }
              fun Test < String > . test ( ) : Int {
                  ensure ( false ) { "failure" }
                  return 1
              }
              val x: Map < String , String > = emptyMap ( )
              """
          )
        );
    }

    @Test
    void trailingLambdaArgumentWithParentheses() {
        rewriteRun(
          kotlin(
            """
              fun String.modify(block: () -> Unit) = this
              
              val spec = "test".modify( /*42*/  ) {
                  println("Hello, world!")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/413")
    @Test
    void trailingLambdaArgumentWithParentheses1() {
        rewriteRun(
          kotlin(
            """
              @Suppress("UNUSED_PARAMETER")
              fun String.modify(block: ( Any? ) -> Any? ) = this
              
              @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
              val spec = "test".modify {
                  (  i   ) ->
              }
              
              operator fun Any?.component1(): Any {
                  return ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/78")
    @Test
    void infixTrailingLambdaDSL() {
        rewriteRun(
          kotlin(
            """
              class FreeSpec ( private val initializer : FreeSpec . ( ) -> Unit ) {
                infix fun String . modify ( block : ( ) -> Unit ) : Nothing = TODO ( )
              }
              
              val spec = FreeSpec {
                "test" modify {
                  println ( "Hello, world!" )
                }
              }
              """
          )
        );
    }

    @Test
    void infixTrailingLambda() {
        rewriteRun(
          kotlin(
            """
              infix fun String.modify(block: () -> Unit) = this
              
              val spec = "test"  modify   {
                  println("Hello, world!")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/65")
    @Test
    void trailingVarargParameter() {
        rewriteRun(
          kotlin(
            """
              fun asList (n : Int, vararg ns : Int) : List < Int > {
                  val result = ArrayList < Int > ( )
                  for ( t in ns ) // ns is an Array
                      result . add ( t )
                  return result
              }
              
              val list = asList ( 1 , 2 , 3 , 4 )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/65")
    @Test
    void varargParameter() {
        rewriteRun(
          kotlin(
            """
              fun asList ( vararg ns : Int ) : List < Int > {
                  val result = ArrayList < Int > ( )
                  for ( t in ns ) // ns is an Array
                      result . add ( t )
                  return result
              }
              
              val list = asList ( 1 , 2 , 3 , 4 )
              """
          )
        );
    }

    @Test
    void fullyQualifiedInvocation() {
        rewriteRun(
          kotlin(
            """
              package some.org
              fun fooBar ( ) { }
              """
          ),
          kotlin(
            """
              val x = some . org . fooBar ( )
              """
          )
        );
    }

    @Test
    void unresolvedMethodInvocationName() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          kotlin(
            """
              val x = some .  qualified   . fooBar ( )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/92")
    @SuppressWarnings("RedundantSuspendModifier")
    @Test
    void receiverWithModifier() {
        rewriteRun(
          kotlin(
            """
              class SomeReceiver
              suspend inline fun SomeReceiver  .   method(
                crossinline body  :   suspend     SomeReceiver .  () -> Unit
              ) {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/83")
    @Test
    void reifiedClassReference() {
        rewriteRun(
          kotlin(
            """
              inline fun <reified TClass> default(arg: String) {
                  val v = TClass::class.qualifiedName
              }
              """
          )
        );
    }

    @Test
    void errorNameRefOnSelect() {
        rewriteRun(
          kotlin(
            """
              fun test() {
                "foo".toString()
              }
              """
          )
        );
    }

    @Test
    void errorNameRefOnSelectWithReference() {
        rewriteRun(
          kotlin(
            """
              fun test(bar: String) {
                "foo $bar".toInt()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/131")
    @Test
    void spreadArgumentMethodInvocation() {
        rewriteRun(
          kotlin(
            """
              fun format ( vararg params : String ) { }
              fun test ( ) {
                format (  *   arrayOf ( "foo" , "bar" ) )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/131")
    @Test
    void spreadArgumentProperty() {
        rewriteRun(
          kotlin(
            """
              fun format ( first: String, vararg params : String ) { }
              fun test ( ) {
                val x = arrayOf ( "foo" , "bar" )
                format ( "" , * x )
              }
              """
          )
        );
    }

    @Test
    void conditionalArgument() {
        rewriteRun(
          kotlin(
            """
              fun method ( s : String ) { }
              val x = method ( if ( true ) "foo" else "bar" )
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              fun method ( s : String ) { }
              val x = method ( "foo", )
              val y = method ( if ( true ) "foo" else "bar" /*c1*/ , /*c2*/ )
              """
          )
        );
    }

    @Test
    void trailingCommaMultipleArguments() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun foo(a : Int, b : Int) = a + b
                  fun bar(): Int =
                      foo(1, 1,  ) + foo(
                          a = 1,
                          b = 1,
                      )
              }
              """
          )
        );
    }

    @Test
    void nullSafeOnMethodTarget() {
        rewriteRun(
          kotlin(
            """
              val l = "x".length?.let { it + 1 }
              """
          )
        );
    }

    @Test
    void trailingCommaAndTrailingLambda() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun foo(a : Int, b : (Int) -> Int) = a + b(a)
                  fun bar(): Int =
                      foo(1  ,   ) { i -> i } + foo(
                          a = 1,
                      ) { i -> i }
              }
              """
          )
        );
    }

    @Test
    void parameterAndTrailingLambda() {
        rewriteRun(
          kotlin(
            """
              fun f(x: Int, y: (Int) -> Int)  = y(x)

              fun test() {
                  print(f(1) { 2 })
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/100")
    @Test
    void anonymousLambdaInSuperConstructorCall() {
        rewriteRun(
          kotlin(
            """
              abstract class Test(arg: () -> Unit) {
                  /*23*/ init {
                      arg()
                  }
              }
              class ExtensionTest : Test({
                  println("hello")
              })
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/270")
    @Test
    void extensionFunctionCall() {
        rewriteRun(
          kotlin(
            """
              val block: Collection<Any>.() -> Unit = {}
              val r = listOf("descriptor").block()

              val block2: Collection<Any>.(String, () -> Unit) -> Unit = {_, _ -> }
              val r2 = listOf("descriptor").block2("x")  {   }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/233")
    @Test
    void indexedAccess() {
        rewriteRun(
          kotlin(
            """
              val arr = IntArray(1)
              val a0 =  arr   [    0     ]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/233")
    @Test
    void customIndexedAccess() {
        rewriteRun(
          kotlin(
            """
              class Surface {
                  operator fun get(x: Int, y: Int) = 2 * x + 4 * y - 10
              }
              val surface = Surface()
              val z = surface[4, 2]
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(((J.VariableDeclarations) cu.getStatements().get(cu.getStatements().size() - 1))).satisfies(
                    z ->
                        assertThat(((J.MethodInvocation) z.getVariables().getFirst().getInitializer())).satisfies(
                            get -> {
                                assertThat(get.getMarkers().findFirst(IndexedAccess.class)).isPresent();
                                assertThat(((J.Identifier) get.getSelect()).getSimpleName()).isEqualTo("surface");
                                assertThat(get.getName().getSimpleName()).isEqualTo("<get>");
                                assertThat(get.getArguments()).hasSize(2);
                            }
                        )
                );
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/297")
    @Test
    void spaceAfterLambdaParameter() {
        rewriteRun(
          kotlin(
            """
                val l = listOf("")
                val v = Pair(
                    l?.map { true },
                    "foo"
                  )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/308")
    @Test
    void trailingLambdaAfterNullSafe() {
        rewriteRun(
          kotlin(
            """
              val x = "x"
                   ?.associateTo(mutableMapOf()) { p ->
                       p to listOfNotNull(p.uppercase())
                   }
              """
          )
        );
    }

    @Test
    void safeQualifiedExpression() {
        rewriteRun(
          kotlin(
            """
              data class Node(val name : String) {
                  fun findTypeDefinition(document: Any): Any? {
                      return null
                  }
              }

              fun method() {
                  val node = Node("x") // END
                  node .  findTypeDefinition(1)   ?.let    { definition ->
                      println("Found definition: $definition")
                  }
              }
              """
          )
        );
    }

    @Test
    void namedParameters() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun foo(s: String) = s
                  fun bar(ss: Array<String>) {
                      foo(
                          s = ss[0]
                      )
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/417")
    @Test
    void namedArgumentWithSpreadOperator() {
        rewriteRun(
          kotlin(
            """
              fun format(format: String, vararg params: String) { }
              fun test() {
                  format("f", params = /*C0*/ *   arrayOf( "foo" , "bar"))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/448")
    @Test
    void indexAccessAsSelectOfAMethodCall() {
        rewriteRun(
          kotlin(
            """
              var l1: List<() -> Unit> = emptyList()
              var l2: List<(Int) -> Unit> = emptyList()

              fun test() {
                  l1 [ 0  ]   (    )
                  val x =  l2 [  1 ]   (    42 )
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/443")
    @Test
    void methodInvocationAsSelect() {
        rewriteRun(
          kotlin(
            """
              @Suppress("UNUSED_PARAMETER")
              enum class Type {
                  A, B, C;
                  fun getType(type: Type): Type {
                      return A
                  }
              }
              fun Type.type(): Type {
                  Type.values().forEach { type -> {
                      val someType = getType(type)(this)
                  }}
                  return Type.A
              }
              @Suppress("UNUSED_PARAMETER")
              operator fun Type.invoke(type: Type): Any {
                  return Type.A
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/449")
    @Test
    void thisAsSelect() {
        rewriteRun(
          kotlin(
            """
              inline fun < reified T : Enum < T >, V> ( ( T ) -> V ). find ( value : V ): T ? {
                  return enumValues < T > ( ) . firstOrNull { this ( it )  == value }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/450")
    @Test
    void unaryAsSelect() {
        rewriteRun(
          kotlin(
            """
              class Test(foo: () -> Unit) {
                private var foo: (() -> Unit)? = foo
                fun method() {
                  @Suppress("UNUSED_VARIABLE")
                  val bar = foo!!()
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/451")
    @Test
    void parenthesesAsSelect() {
        rewriteRun(
          kotlin(
            """
              open class A(
                val foo : ( ( Any ) -> A) -> A
              )
              class B : A ( foo = { x -> ( :: A ) ( x ) } ) {
                @Suppress("UNUSED_PARAMETER")
                fun mRef(a: Any) {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/452")
    @Test
    void newClassAsSelect() {
        rewriteRun(
          kotlin(
            """
              class A
              val a = A ( ) ( )

              operator fun A.invoke() : A {
                  return A()
              }
              """
          )
        );
    }

    @Test
    void stringLiteralAsSelect() {
        rewriteRun(
          kotlin(
            """
              class A

              operator fun String.invoke() : A {
                  return A()
              }

              val a = "42" ( )
              """
          )
        );
    }

    @Test
    void stringLiteralAsSelectWithLambda() {
        rewriteRun(
          kotlin(
            """
              operator fun String.invoke(action: () -> Unit)   {
                  action()
              }

              val a = "42"

              val b = "X" {
                  // do something
              }

              val c = "${a}" {
                  // do something
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/556")
    @Test
    void innerClassInvocation() {
        rewriteRun(
          kotlin(
            """
              class Pony2 {
                  inner class Builder

                  companion object {
                      fun newBuilder() = Pony2().Builder()
                  }
              }
              """
          )
        );
    }
}
