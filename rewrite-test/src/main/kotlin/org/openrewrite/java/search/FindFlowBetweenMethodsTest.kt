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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("FunctionName")
interface FindFlowBetweenMethodsTest : JavaRecipeTest {

    @Test
    fun `taint flow between subject only`(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFlowBetweenMethods(
            "java.util.LinkedList <constructor>()",
            true,
            "java.util.LinkedList remove()",
            true,
            "Select",
            "Taint"),
        before = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    LinkedList<Integer> l = new LinkedList<>();
                    l.add(5);
                    System.out.println(l);
                    l.remove();
                }
            }
            """,
        after = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    LinkedList<Integer> l = /*~~>*/new LinkedList<>();
                    /*~~>*/l.add(5);
                    System.out.println(/*~~>*/l);
                    /*~~>*/l.remove();
                }
            }
            """
    )


    @Test
    fun `taint flow between arguments only`(jp: JavaParser) = assertChanged(
            jp,
            recipe = FindFlowBetweenMethods(
                "java.lang.Integer parseInt(String)",
                true,
                "java.util.LinkedList remove(..)",
                true,
                "Arguments",
                "Taint"),
            before = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = Integer.parseInt("10");
                    LinkedList<Integer> l = new LinkedList<>();
                    l.add(x);
                    l.remove(x);
                }
            }
            """,
            after = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = /*~~>*/Integer.parseInt("10");
                    LinkedList<Integer> l = new LinkedList<>();
                    l.add(/*~~>*/x);
                    l.remove(/*~~>*/x);
                }
            }
            """
    )

    @Test
    fun `taint flow through multiple subjects, Integer source and sink methods specified` (jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFlowBetweenMethods(
            "java.lang.Integer parseInt(String)",
            true,
            "java.lang.Integer equals(..)",
            true,
            "Both",
            "Taint"),
        before = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = Integer.parseInt("10");
                    LinkedList<Integer> l = new LinkedList<>();
                    l.add(x);
                    System.out.println(l);
                    x.equals(10);
                }
            }
            """,
        after = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = /*~~>*/Integer.parseInt("10");
                    LinkedList<Integer> l = new LinkedList<>();
                    l.add(x);
                    System.out.println(l);
                    /*~~>*/x.equals(10);
                }
            }
            """
    )

    @Test
    fun `no taint flow through arguments` (jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindFlowBetweenMethods(
            "java.lang.Integer parseInt(String)",
            true,
            "java.lang.Integer equals(..)",
            true,
            "Arguments",
            "Taint"),
        before = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = Integer.parseInt("10");
                    x.equals(10);
                }
            }
            """
    )




    @Test
    fun `taint flow between arguments and subject`(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFlowBetweenMethods(
            "java.util.LinkedList <constructor>()",
            true,
            "java.util.LinkedList remove()",
            true,
            "Both",
            "Taint"),
        before = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = Integer.parseInt("10");
                    LinkedList<Integer> l = new LinkedList<>();
                    System.out.println(x);
                    System.out.println(l);
                    l.remove();
                }
            }
            """,
        after = """
            import java.util.LinkedList;
            class Test {
                void test() {
                    Integer x = Integer.parseInt("10");
                    LinkedList<Integer> l = /*~~>*/new LinkedList<>();
                    System.out.println(x);
                    System.out.println(/*~~>*/l);
                    /*~~>*/l.remove();
                }
            }
            """
    )
}
