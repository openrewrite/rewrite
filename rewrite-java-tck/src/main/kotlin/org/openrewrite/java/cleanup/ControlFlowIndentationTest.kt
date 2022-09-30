/*
 * Copyright 2021 the original author or authors.
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
@file:Suppress("SuspiciousIndentAfterControlStatement", "ClassInitializerMayBeStatic", "ConstantConditions",
    "IfStatementWithIdenticalBranches", "DuplicateCondition"
)

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface ControlFlowIndentationTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ControlFlowIndentation())
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2277")
    @Test
    fun removesIndentationFromStatementAroundIf() = rewriteRun(
        java("""
            class A {
                {
                        foo(); // This should be left alone because it does not come after control flow
                    if(true)
                    foo();
                        foo();
                }
                
                static void foo() { 
                // There's no control flow in this method body, so its indentation should remain untouched
                            int a = 0;
                        }
            }
        """,
        """
            class A {
                {
                        foo(); // This should be left alone because it does not come after control flow
                    if(true)
                        foo();
                    foo();
                }
                
                static void foo() { 
                // There's no control flow in this method body, so its indentation should remain untouched
                            int a = 0;
                        }
            }
        """)
    )

    @Test
    fun leavesIndentationAloneWhenBlocksAreExplicit() = rewriteRun(
        java("""
            class A {
                {
                    if(true) {
                            foo();
                    } else if(true) {
                            foo();
                    } else {
                            foo();
                    }
                            foo();
                }
                
                static void foo(){}
            }
        """)
    )

    @Test
    fun removesIndentationFromStatementAfterIfElse() = rewriteRun(
        java("""
            class A {
                {
                    if(true) {
                        foo();
                    } else
                        foo();
                        foo();
                }
                
                static void foo(){}
            }
        """,
        """
            class A {
                {
                    if(true) {
                        foo();
                    } else
                        foo();
                    foo();
                }
                
                static void foo(){}
            }
        """)
    )

    @Test
    fun elseIf(jp: JavaParser) = rewriteRun(
        java("""
            class A {
                {
                    if(true){
                        foo();
                    } else if(false)
                    foo();
                    else {
                        foo();
                    }
                }
                static void foo(){}
            }
        """,
        """
            class A {
                {
                    if(true){
                        foo();
                    } else if(false)
                        foo();
                    else {
                        foo();
                    }
                }
                static void foo(){}
            }
        """)
    )

    @Test
    fun removesIndentationFromStatementAfterLoop() = rewriteRun(
        java("""
            class A {
                {
                    while(false)
                        foo();
                        foo();
                }
                
                static void foo(){}
            }
        """,
        """
            class A {
                {
                    while(false)
                        foo();
                    foo();
                }
                
                static void foo(){}
            }
        """)
    )
}
