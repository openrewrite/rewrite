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
package org.openrewrite.java.controlflow

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName", "UnusedAssignment", "UnnecessaryLocalVariable", "ConstantConditions")
interface ControlFlowDotFileViewerTest : RewriteTest  {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ControlFlowVisualization(true, false))
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
                edge [fontname=Arial];
                0 [shape=circle, label="Start", fontname="Arial"];
                1 [shape=box, label="    {\l        System.out.println(\"buz\");\l    } while (j++ < 10);\l}\l", fontname="Courier"];
                2 [shape=diamond, label="Arrays.asList(args).iterator().hasNext()", fontname="Courier"];
                3 [shape=box, label="System.out.println(arg);\lif (i == 5)\l                   \l", fontname="Courier"];
                4 [shape=diamond, label="\"buz\".equals(arg)", fontname="Courier"];
                5 [shape=diamond, label="\"fiz\".equals(arg)", fontname="Courier"];
                6 [shape=diamond, label="i < 10", fontname="Courier"];
                7 [shape=diamond, label="i == 5", fontname="Courier"];
                8 [shape=box, label="if (\"buz\".equals(arg))\l                   \l", fontname="Courier"];
                9 [shape=box, label="int j = 0;\ldo\l                   \l", fontname="Courier"];
                10 [shape=diamond, label="j++ < 10", fontname="Courier"];
                11 [shape=box, label="{\l    System.out.println(\"buz\");\l    continue;\l}\l", fontname="Courier"];
                12 [shape=box, label="{\l    System.out.println(\"fiz\");\l}\l", fontname="Courier"];
                13 [shape=box, label="{\l    break;\l}\l", fontname="Courier"];
                14 [shape=box, label="{\l    i++;\l    for(String arg : args)\l     \l", fontname="Courier"];
                15 [shape=box, label="{\l    if (\"fiz\".equals(arg))\l                       \l", fontname="Courier"];
                16 [shape=box, label="{\l    int i = 0;\l    while (i < 10)\l     \l", fontname="Courier"];
                17 [shape=circle, label="End", fontname="Arial"];
                0 -> 16;
                1 -> 10;
                2 -> 15 [label="True", color="green3" fontcolor="green3"];
                2 -> 6 [label="False", color="red" fontcolor="red"];
                3 -> 7;
                4 -> 11 [label="True", color="green3" fontcolor="green3"];
                4 -> 9 [label="False", color="red" fontcolor="red"];
                5 -> 12 [label="True", color="green3" fontcolor="green3"];
                5 -> 3 [label="False", color="red" fontcolor="red"];
                6 -> 14 [label="True", color="green3" fontcolor="green3"];
                6 -> 17 [label="False", color="red" fontcolor="red"];
                7 -> 13 [label="True", color="green3" fontcolor="green3"];
                7 -> 8 [label="False", color="red" fontcolor="red"];
                8 -> 4;
                9 -> 1;
                10 -> 1 [label="True", color="green3" fontcolor="green3"];
                10 -> 2 [label="False", color="red" fontcolor="red"];
                11 -> 2;
                12 -> 3;
                13 -> 6;
                14 -> 2;
                15 -> 5;
                16 -> 6;
                {rank="src";0};
                {rank="sink";17};
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
