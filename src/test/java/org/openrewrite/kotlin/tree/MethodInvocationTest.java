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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

class MethodInvocationTest implements RewriteTest {

    @Test
    void implicitFunctionCall() {
        rewriteRun(
          kotlin(
            """
            fun plugins ( input : ( ) -> String ) {
                println ( input ( ) )
            }
            """
          ),
          kotlin(
            """
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
            """
          ),
          kotlin(
            """
            class SpecScope  {
                val delegate : Spec = Spec ( )
                fun id ( id : String ) : Spec = delegate . id ( id )
            }
            public infix fun Spec . version ( version : String ) : Spec = version ( version )
            public inline val SpecScope . `java-library` : Spec get ( ) = id ( "org.gradle.java-library" )
            """
          ),
          kotlin(
            """
            class DSL  {
                fun plugins ( block : SpecScope . ( ) -> Unit ) {
                    block ( SpecScope ( ) )
                }
            }
            """
          ),
          kotlin(
            """
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
          kotlin("fun method ( arg : Any ) { }"),
          kotlin(
            """
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
            """
          ),
          kotlin(
            """
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
            """
          ),
          kotlin(
            """
            val t = Test ( )
            fun method ( ) {
                val a = t . method ( ) ?: null
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
                val l = listOf ( 1 , 2 , 3 )
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
            fun method ( arg : Any ) {
                val map = mapOf ( 1 to "one" , 2 to "two" , 3 to "three" )
            }
            """
          )
        );
    }

    @Test
    void multipleTypesOfMethodArguments() {
        rewriteRun(
          kotlin("fun methodA ( a : String , b : Int , c : Double ) { }"),
          kotlin(
            """
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
          kotlin("fun apply ( plugin : String ? = null) { }"),
          kotlin(
            """
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
          kotlin("fun < T : Number > methodA ( type : T ) { }"),
          kotlin(
            """
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
          kotlin("open class Test"),
          kotlin(
            """
            fun test ( a : Test ) { }
            
            fun method ( ) {
                test ( object : Test ( ) {
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
            """
          ),
          kotlin(
            """
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
            """
          ),
          kotlin(
            """
            fun Test < String > . test ( ) : Int {
                ensure ( false ) { "failure" }
                return 1
            }
            val x: Map<String, String> = emptyMap()
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/78")
    @Test
    @ExpectedToFail
    void infixTrailingLambdaDSL() {
        rewriteRun(
          kotlin(
            """
            class FreeSpec(private val init: FreeSpec.() -> Unit) {
              infix fun String.modify(block: () -> Unit) = TODO()
            }
            
            val spec = FreeSpec {
              "test" modify {
                println("Hello, world!")
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
            infix fun String.modify(block: () -> Unit) = TODO()
            
            val spec = "test" modify {
              println("Hello, world!")
            }
            """
          )
        );
    }
}
