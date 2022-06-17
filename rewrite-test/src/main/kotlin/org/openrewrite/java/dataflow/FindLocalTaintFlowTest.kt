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

import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.J.MethodInvocation
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface FindLocalTaintFlowTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalTaintFlowSpec<MethodInvocation, Expression>() {
                override fun isSource(mi: MethodInvocation, cursor: Cursor) =
                    mi.name.simpleName == "source"

                override fun isSink(expr: Expression, cursor: Cursor) =
                    true

                override fun isSanitizer(expression: Expression, cursor: Cursor): Boolean =
                    when(expression) {
                        is J.Binary -> expression.operator == J.Binary.Type.Addition
                            && (expression.right as? J.Literal)?.value == "sanitizer"
                        else -> false
                    }
            })
        })
        spec.expectedCyclesThatMakeChanges(1).cycles(1)
    }

    @Test
    fun `taint tracking through string manipulations`() = rewriteRun(
        java(
            """
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = source();
                        String o = n.substring(0, 3);
                        String p = o.toUpperCase();
                        System.out.println(p);
                    }
                }
            """,
            """
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = /*~~>*/source();
                        String o = /*~~>*//*~~>*/n.substring(0, 3);
                        String p = /*~~>*//*~~>*/o.toUpperCase();
                        System.out.println(/*~~>*/p);
                    }
                }
            """
        )
    )

    @Test
    fun `taint tracking through file manipulations`() = rewriteRun(
        java(
            """
                import java.io.File;
                import java.nio.file.Path;
                class Test {
                    File source() { return null; }
                    void test() {
                        {
                            File n = source();
                            String o = n.getAbsolutePath();
                            String p = o.toUpperCase();
                            System.out.println(p);
                        }
                        // To Path Type
                        {
                            File n = source();
                            Path o = n.toPath();
                            File p = o.toFile();
                            System.out.println(p);
                        }
                    }
                }
            """,
            """
                import java.io.File;
                import java.nio.file.Path;
                class Test {
                    File source() { return null; }
                    void test() {
                        {
                            File n = /*~~>*/source();
                            String o = /*~~>*//*~~>*/n.getAbsolutePath();
                            String p = /*~~>*//*~~>*/o.toUpperCase();
                            System.out.println(/*~~>*/p);
                        }
                        // To Path Type
                        {
                            File n = /*~~>*/source();
                            Path o = /*~~>*//*~~>*/n.toPath();
                            File p = /*~~>*//*~~>*/o.toFile();
                            System.out.println(/*~~>*/p);
                        }
                    }
                }
            """
        )
    )

    @Test
    fun `taint tracking through file constructor`() = rewriteRun(
        java(
            """
                import java.io.File;
                import java.net.URI;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = source();
                        File o = new File(n);
                        URI p = o.toURI();
                        System.out.println(p);
                    }
                }
            """,
            """
                import java.io.File;
                import java.net.URI;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = /*~~>*/source();
                        File o = /*~~>*/new File(/*~~>*/n);
                        URI p = /*~~>*//*~~>*/o.toURI();
                        System.out.println(/*~~>*/p);
                    }
                }
            """
        )
    )

    @Test
    fun `taint tracking through String join`() = rewriteRun(
            java(
                """
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = source();
                        String o = String.join(", ", n);
                        String p = String.join(o, ", ");
                        String q = String.join(" ", "hello", p);
                        System.out.println(q);
                    }
                }
                """,
            """
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = /*~~>*/source();
                        String o = /*~~>*/String.join(", ", /*~~>*/n);
                        String p = /*~~>*/String.join(/*~~>*/o, ", ");
                        String q = /*~~>*/String.join(" ", "hello", /*~~>*/p);
                        System.out.println(/*~~>*/q);
                    }
                }
                """
            )
    )

    @Test
    fun `taint tracking through string appending`() = rewriteRun(
        java(
            """
                import java.io.File;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = source();
                        String o = "hello " + n ;
                        String p = o + " world";
                        String q = p + File.separatorChar;
                        String r = q + true;
                        System.out.println(r);
                    }
                }
                """,
            """
                import java.io.File;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = /*~~>*/source();
                        String o = /*~~>*/"hello " + /*~~>*/n ;
                        String p = /*~~>*//*~~>*/o + " world";
                        String q = /*~~>*//*~~>*/p + File.separatorChar;
                        String r = /*~~>*//*~~>*/q + true;
                        System.out.println(/*~~>*/r);
                    }
                }
                """
        )
    )

    @Test
    fun `taint stops at a sanitizer`() = rewriteRun(
        java(
            """
                import java.io.File;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = source();
                        String o = "hello " + n ;
                        String p = o + " world";
                        String q = p + "sanitizer";
                        String r = q + true;
                        System.out.println(r);
                    }
                }
                """,
            """
                import java.io.File;
                class Test {
                    String source() { return null; }
                    void test() {
                        String n = /*~~>*/source();
                        String o = /*~~>*/"hello " + /*~~>*/n ;
                        String p = /*~~>*//*~~>*/o + " world";
                        String q = /*~~>*/p + "sanitizer";
                        String r = q + true;
                        System.out.println(r);
                    }
                }
                """
        )
    )
}
