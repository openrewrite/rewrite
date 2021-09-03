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
package org.openrewrite.java.format

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.style.GeneralFormatStyle

interface NormalizeLineBreaksTest {

    @Test
    fun simpleFileToLinuxLineFeed(jp: JavaParser) {
        val style = GeneralFormatStyle(false)
        val before =
            "class Test {\r\n" +
                    "    // some comment\r\n" +
                    "    public void test() {\n" +
                    "        System.out.println();\n" +
                    "    }\r\n" +
                    "}\n"
        val expected =
            "class Test {\n" +
                    "    // some comment\n" +
                    "    public void test() {\n" +
                    "        System.out.println();\n" +
                    "    }\n" +
                    "}\n"
        val after = NormalizeLineBreaksVisitor<ExecutionContext>(style).visit(jp.parse(before)[0], InMemoryExecutionContext())!!.print()
        Assertions.assertThat(after.toCharArray()).isEqualTo(expected.toCharArray())
    }

    @Test
    fun simpleFileToWindowsLineEnd(jp: JavaParser) {
        val style = GeneralFormatStyle(true)
        val before =
            "class Test {\r\n" +
                    "    // some comment\r\n" +
                    "    public void test() {\n" +
                    "        System.out.println();\n" +
                    "    }\r\n" +
                    "}\n"
        val expected =
            "class Test {\r\n" +
                    "    // some comment\r\n" +
                    "    public void test() {\r\n" +
                    "        System.out.println();\r\n" +
                    "    }\r\n" +
                    "}\r\n"

        val after = NormalizeLineBreaksVisitor<ExecutionContext>(style).visit(jp.parse(before)[0], InMemoryExecutionContext())!!.print()
        Assertions.assertThat(after.toCharArray()).isEqualTo(expected.toCharArray())
    }
}
