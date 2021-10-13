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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.style.GeneralFormatStyle

interface NormalizeLineBreaksTest: JavaRecipeTest {
    companion object {
        const val windows = "" +
                "class Test {\r\n" +
                "    // some comment\r\n" +
                "    public void test() {\r\n" +
                "        System.out.println();\r\n" +
                "    }\r\n" +
                "}"

        const val linux = "" +
                "class Test {\n" +
                "    // some comment\n" +
                "    public void test() {\n" +
                "        System.out.println();\n" +
                "    }\n" +
                "}"

        const val windowsJavadoc = "" +
                "/**\r\n" +
                " *\r\n" +
                " */\r\n" +
                "class Test {\r\n" +
                "}"

        const val linuxJavadoc = "" +
                "/**\n" +
                " *\n" +
                " */\n" +
                "class Test {\n" +
                "}"
    }

    @Test
    fun trimKeepCRLF() {
        assertThat("\n  test\r\n  test".replace('\r', '⏎').trimIndent()
            .replace('⏎', '\r')).isEqualTo("test\r\ntest")
    }

    @Test
    fun windowsToLinux(jp: JavaParser) = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(false)) },
        before = windows,
        after = linux
    )

    @Test
    fun linuxToWindows(jp: JavaParser) = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(true)) },
        before = linux,
        after = windows
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun doNotChangeWindowsJavadoc(jp: JavaParser) = assertUnchanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(true)) },
        before = windowsJavadoc
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun doNotChangeLinuxJavadoc(jp: JavaParser) = assertUnchanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(false)) },
        before = linuxJavadoc
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun windowsToLinuxJavadoc(jp: JavaParser) = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(false)) },
        before = windowsJavadoc,
        after = linuxJavadoc
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun linuxToWindowsJavadoc(jp: JavaParser) = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(true)) },
        before = linuxJavadoc,
        after = windowsJavadoc
    )

    @Issue("https://github.com/openrewrite/rewrite-docs/issues/67")
    @Test
    fun preservesExistingWindowsEndingsByDefault(jp: JavaParser) = assertUnchanged(
        recipe = NormalizeLineBreaks(),
        before = windows
    )

    @Issue("https://github.com/openrewrite/rewrite-docs/issues/67")
    @Test
    fun preservesExistingLinuxEndingsByDefault(jp: JavaParser) = assertUnchanged(
        recipe = NormalizeLineBreaks(),
        before = linux
    )
}
