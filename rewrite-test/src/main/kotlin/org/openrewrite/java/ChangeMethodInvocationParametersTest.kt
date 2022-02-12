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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * @author Alex Boyko
 */
interface ChangeMethodInvocationParametersTest : JavaRecipeTest {

    @Test
    fun testOneParameter(jp: JavaParser) {
        val cu = jp.parse(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        h.getBytes("utf8");
                    }
                }
            """.trimIndent()
        )

        val result = ChangeMethodInvocationParameters(
            "java.lang.String getBytes(java.lang.String)",
            listOf(ChangeMethodInvocationParameters.ParameterInfo.builder().template("StandardCharsets.UTF_8").oldParamIndex(1).imports(
                listOf("java.nio.charset.StandardCharsets")).build())
        )
            .run(cu)[0].after!!;

        assertThat(result.printAll()).isEqualTo(
            """
                import java.nio.charset.StandardCharsets;

                class Test {

                    void test() {
                        String h = "hello";
                        h.getBytes(StandardCharsets.UTF_8);
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testOneParameterWithCall(jp: JavaParser) {
        val cu = jp.parse(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        String a = "hello";
                        h.contentEquals(a);
                    }
                }
            """.trimIndent()
        )

        val result = ChangeMethodInvocationParameters(
            "java.lang.String contentEquals(java.lang.CharSequence)",
            listOf(ChangeMethodInvocationParameters.ParameterInfo.builder().template("new StringBuffer({})").oldParamIndex(1).imports(
                listOf("java.lang.StringBuffer")).build())
        )
            .run(cu)[0].after!!;

        assertThat(result.printAll()).isEqualTo(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        String a = "hello";
                        h.contentEquals(new StringBuffer(a));
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testOneParameterIntoMultiple(jp: JavaParser) {
        val cu = jp.parse(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        String find = "l";
                        h.lastIndexOf(find);
                    }
                }
            """.trimIndent()
        )

        val result = ChangeMethodInvocationParameters(
            "java.lang.String lastIndexOf(java.lang.String)",
            listOf(
                ChangeMethodInvocationParameters.ParameterInfo.builder().template("{} + \"l\"").oldParamIndex(1).imports(listOf("java.lang.StringBuffer")).build(),
                ChangeMethodInvocationParameters.ParameterInfo.builder().template("1").build()
            )
        )
            .run(cu)[0].after!!;

        assertThat(result.printAll()).isEqualTo(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        String find = "l";
                        h.lastIndexOf(find + "l", 1);
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testOneParameterToNoParams(jp: JavaParser) {
        val cu = jp.parse(
            """
                import java.nio.charset.StandardCharsets;

                class Test {

                    void test() {
                        String h = "hello";
                        byte[] b = h.getBytes(StandardCharsets.UTF_8);
                    }
                }
            """.trimIndent()
        )

        val result = ChangeMethodInvocationParameters(
            "java.lang.String getBytes(java.nio.charset.Charset)",
            listOf()
        )
            .run(cu)[0].after!!;

        assertThat(result.printAll()).isEqualTo(
            """
                class Test {

                    void test() {
                        String h = "hello";
                        byte[] b = h.getBytes();
                    }
                }
            """.trimIndent()
        )
    }

}
