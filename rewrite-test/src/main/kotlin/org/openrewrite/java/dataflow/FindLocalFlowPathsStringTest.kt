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
package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface FindLocalFlowPathsStringTest: RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalFlowSpec<J.Literal, J.MethodInvocation>() {
                override fun isSource(expr: J.Literal, cursor: Cursor) = expr.value == "42"
                override fun isSink(expr: J.MethodInvocation, cursor: Cursor) = true
            })
        })
    }

    @Test
    fun transitiveAssignment() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n;
                        /*~~>*/System.out.println(/*~~>*/o);
                        String p = o;
                    }
                }
            """
        )
    )

    @Test
    @Disabled("This isn't finding the search results for `String o = n + '/';` for some reason")
    fun `taint flow is not data flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
                """
        )
    )

    @Test
    @Disabled("MISSING: I don't know what's wrong here, but I'm getting weird results here")
    fun `taint flow is not data flow but it is tracked to call site`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n.toString() + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n.toString() + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
                """
        )
    )
}
