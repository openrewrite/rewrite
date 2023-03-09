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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

class MethodDeclarationTest implements RewriteTest {

    @Test
    void methodDeclaration() {
        rewriteRun(
          kotlin("fun method ( ) { }")
        );
    }

    @Test
    void parameters() {
        rewriteRun(
          kotlin("fun method ( i : Int ) { }")
        );
    }

    @Test
    void functionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : (  ) -> String ) { }")
        );
    }

    @Test
    void typedFunctionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : ( Int , Int ) -> Boolean ) { }")
        );
    }

    @Test
    void functionTypeWithReceiver() {
        rewriteRun(
          kotlin("fun method ( arg : String . ( ) -> String ) { }")
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          kotlin("fun method ( ) : Boolean = true")
        );
    }

    @Test
    void returnType() {
        rewriteRun(
          kotlin(
            """
            fun method ( ) : Boolean {
                return true
            }
            """
          )
        );
    }

    @Test
    void methodDeclarationDeclaringType() {
        rewriteRun(
          kotlin(
            """
            class A {
                fun method ( ) {
                }
            }
            """
          )
        );
    }

    @Test
    void infix() {
        rewriteRun(
          kotlin(
           """
            class Spec {
                fun version ( version : String) : Spec {
                    return this
                }
            }
            """
          ),
          kotlin(
            """
            class A {
              fun method ( ) {
              }
            }
            """
          ),
          kotlin("infix fun Spec . version ( version : String ) : Spec = version ( version )")
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("fun `some quoted id` ( ) { }")
        );
    }

    @Test
    void defaults() {
        rewriteRun(
          kotlin("fun apply ( plugin : String ? = null ) { }")
        );
    }

    @Test
    void reifiedGeneric() {
        rewriteRun(
          kotlin("inline fun < reified T > method ( value : T ) { }")
        );
    }

    @Test
    void genericTypeParameters() {
        rewriteRun(
          kotlin("fun <T : Number > method ( type : T ) { }")
        );
    }

    @Test
    void receiverType() {
        rewriteRun(
          kotlin("class Test"),
          kotlin("fun Test . method ( ) { }")
        );
    }

    @Test
    void methodInvocationOnReceiverType() {
        rewriteRun(
          kotlin(
            """
            class Test {
                fun build ( s : ( ) -> String ) {
                }
            }
            """
          ),
          kotlin(
            """
            fun Test . method ( ) = build {
                "42"
            }
            """
          )
        );
    }

    @Test
    void nullableReturnType() {
        rewriteRun(
          kotlin(
            """
            fun method() : Array<Int> ? {
            }
            """
          )
        );
    }

    @Test
    void typeParameterAndTypeReceiver() {
        rewriteRun(
          kotlin(
            """
            fun <T: Any> Array<Int>.method(t: T) = Unit
            """
          )
        );
    }
}
