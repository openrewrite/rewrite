package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.Expression
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
}
