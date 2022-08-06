package org.openrewrite.java.controlflow

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName", "UnusedAssignment", "UnnecessaryLocalVariable", "ConstantConditions")
interface ControlFlowDotFileViewerTest : RewriteTest  {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ControlFlowVisualization(true))
        spec.expectedCyclesThatMakeChanges(1).cycles(1)
    }

    @Test
    fun `control flow stress test`() = rewriteRun(
        java(
            """
            public class Test {
                public static void main(String[] args) {
                    int i = 0;
                    while (i < 10) {
                        i++;
                        for(String arg : args) {
                            if ("fiz".equals(arg)) {
                                System.out.println("fiz");
                            }
                            System.out.println(arg);
                            if (i == 5) {
                                break;
                            }
                            if ("buz".equals(arg)) {
                                System.out.println("buz");
                                continue;
                            }
                            int j = 0;
                            do {
                                System.out.println("buz");
                            } while (j++ < 10);
                        }
                    }
                }
            }
            """,
            """
            public class Test {
                /*~~(digraph main {
                rankdir = TB;
                0 [shape=circle, label="Start"];
                1 [shape=box, label="    {\l        System.out.println(\"buz\");\l    } while (j++ < 10);\l}"];
                2 [shape=diamond, label="Arrays.asList(args).iterator().hasNext()"];
                3 [shape=box, label="System.out.println(arg);\lif (i == 5)\l                   "];
                4 [shape=diamond, label="\"buz\".equals(arg)"];
                5 [shape=diamond, label="\"fiz\".equals(arg)"];
                6 [shape=diamond, label="i < 10"];
                7 [shape=diamond, label="i == 5"];
                8 [shape=box, label="if (\"buz\".equals(arg))\l                   "];
                9 [shape=box, label="int j = 0;\ldo\l                   "];
                10 [shape=diamond, label="j++ < 10"];
                11 [shape=box, label="{\l    System.out.println(\"buz\");\l    continue;\l}"];
                12 [shape=box, label="{\l    System.out.println(\"fiz\");\l}"];
                13 [shape=box, label="{\l    break;\l}"];
                14 [shape=box, label="{\l    i++;\l    for(             args)\l     "];
                15 [shape=box, label="{\l    if (\"fiz\".equals(arg))\l                       "];
                16 [shape=box, label="{\l    int i = 0;\l    while (i < 10)\l     "];
                17 [shape=circle, label="End"];
                0 -> 16;
                1 -> 10;
                2 -> 15 [label="True", color="green" fontcolor="green"];
                2 -> 6 [label="False", color="red" fontcolor="red"];
                3 -> 7;
                4 -> 11 [label="True", color="green" fontcolor="green"];
                4 -> 9 [label="False", color="red" fontcolor="red"];
                5 -> 12 [label="True", color="green" fontcolor="green"];
                5 -> 3 [label="False", color="red" fontcolor="red"];
                6 -> 14 [label="True", color="green" fontcolor="green"];
                6 -> 17 [label="False", color="red" fontcolor="red"];
                7 -> 13 [label="True", color="green" fontcolor="green"];
                7 -> 8 [label="False", color="red" fontcolor="red"];
                8 -> 4;
                9 -> 1;
                10 -> 1 [label="True", color="green" fontcolor="green"];
                10 -> 2 [label="False", color="red" fontcolor="red"];
                11 -> 10;
                12 -> 3;
                13 -> 6;
                14 -> 2;
                15 -> 5;
                16 -> 6;
            })~~>*/public static void main(String[] args) /*~~(BB: 10 CN: 6 EX: 1 | 1L)~~>*/{
                    int i = 0;
                    while (/*~~(1C (<))~~>*/i < 10) /*~~(2L)~~>*/{
                        i++;
                        for(String arg : args) /*~~(3L)~~>*/{
                            if (/*~~(2C)~~>*/"fiz".equals(arg)) /*~~(4L)~~>*/{
                                System.out.println("fiz");
                            }
                            /*~~(5L)~~>*/System.out.println(arg);
                            if (/*~~(3C (==))~~>*/i == 5) /*~~(6L)~~>*/{
                                break;
                            }
                            /*~~(7L)~~>*/if (/*~~(4C)~~>*/"buz".equals(arg)) /*~~(8L)~~>*/{
                                System.out.println("buz");
                                continue;
                            }
                            int j = /*~~(9L)~~>*/0;
                            do /*~~(10L)~~>*/{
                                System.out.println("buz");
                            } while (/*~~(5C (<))~~>*/j++ < 10);
                        }
                    }
                }
            }
            """
        )
    )
}
