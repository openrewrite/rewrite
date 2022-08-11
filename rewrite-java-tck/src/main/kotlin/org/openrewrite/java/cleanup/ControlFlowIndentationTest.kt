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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface ControlFlowIndentationTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = ControlFlowIndentation()

    @Test
    fun removesIndentationFromStatementAfterIf(jp: JavaParser) = assertChanged(
        before = """
            class A {
                {
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
        after = """
            class A {
                {
                    if(true)
                        foo();
                    foo();
                }
                
                static void foo() { 
                // There's no control flow in this method body, so its indentation should remain untouched
                            int a = 0;
                        }
            }
        """
    )

    @Test
    fun addsIndentationToStatementInIf(jp: JavaParser) = assertChanged(
        before = """
            class A {
                {
                    if(true)
                    foo();
                    foo();
                }
                
                static void foo(){}
            }
        """,
        after = """
            class A {
                {
                    if(true)
                        foo();
                    foo();
                }
                
                static void foo(){}
            }
        """
    )

    @Test
    fun leavesIndentationAloneWhenBlocksAreExplicit(jp: JavaParser) = assertUnchanged(
        before = """
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
        """
    )

    @Test
    fun removesIndentationFromStatementAfterIfElse(jp: JavaParser) = assertChanged(
        before = """
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
        after = """
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
        """
    )

    @Test
    fun elseIf(jp: JavaParser) = assertChanged(
        before = """
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
        after = """
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
        """
    )

    @Test
    fun removesIndentationFromStatementAfterLoop(jp: JavaParser) = assertChanged(
        before = """
            class A {
                {
                    while(false)
                        foo();
                        foo();
                }
                
                static void foo(){}
            }
        """,
        after = """
            class A {
                {
                    while(false)
                        foo();
                    foo();
                }
                
                static void foo(){}
            }
        """
    )
}
