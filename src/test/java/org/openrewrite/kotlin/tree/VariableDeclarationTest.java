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

class VariableDeclarationTest implements RewriteTest {

    @Test
    void singleVariableDeclaration() {
        rewriteRun(
          kotlin("val a = 1")
        );
    }

    @Test
    void addition() {
        rewriteRun(
          kotlin("val a = 1 + 1")
        );
    }

    @Test
    void singleVariableDeclarationWithTypeConstraint() {
        rewriteRun(
          kotlin("val a : Int = 1")
        );
    }

    @Test
    void anonymousObject() {
        rewriteRun(
          kotlin("open class Test"),
          kotlin("val o : Test = object : Test ( ) { }")
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
          kotlin("class Spec"),
          kotlin("inline val Spec . `java-base` : String get ( ) = \"  \"")
        );
    }

    @Test
    void getter() {
        rewriteRun(
          kotlin("class Spec"),
          kotlin(
            """
            val isEmpty : Boolean
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
    void propertyAccessor() {
        rewriteRun(
          kotlin(
            """
            class Test {
                val value = 10
            }
            val a = Test ( )
            val b = "${a.value}"
            
            val after = 0
            """
          )
        );
    }

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
            val b = "${a.testValue.innerValue}"
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
            val map = mapOf ( 1 to "one" , 2 to "two" , 3 to "three" )
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
            package org.foo
            class Test<T>
            """
          ),
          kotlin(
            """
            import org.foo.Test
            val a : Test < * > = null
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

    @Test
    void typeAlias() {
        rewriteRun(
          kotlin(
            """
            class Test
            """
          ),
          kotlin(
            """
            typealias TestAlias = Test
            val a : TestAlias = Test ( )
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/58")
    @Test
    void destructuringVariableDecleration() {
        rewriteRun(
          kotlin(
            """
            fun example ( ) {
              val ( a , b , c ) = Triple ( 1 , 2 , 3 )
            }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/76")
    @Test
    @ExpectedToFail
    void propertyDelegation() {
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
    void unresolvedNameInLightSource() {
        rewriteRun(
          kotlin(
            """
            val t = SomeInput.Test
            """
          )
        );
    }
}
