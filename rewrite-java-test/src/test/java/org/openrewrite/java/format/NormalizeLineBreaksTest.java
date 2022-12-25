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
package org.openrewrite.java.format;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("TextBlockMigration")
class NormalizeLineBreaksTest implements RewriteTest {

    private static Consumer<RecipeSpec> normalizeLineBreaks(boolean useCRLF) {
        return spec -> spec.recipe(toRecipe(() -> new NormalizeLineBreaksVisitor<>(
          new GeneralFormatStyle(useCRLF))));
    }

    @Language("java")
    String windows = "" +
                     "class Test {\r\n" +
                     "    // some comment\r\n" +
                     "    public void test() {\r\n" +
                     "        System.out.println();\r\n" +
                     "    }\r\n" +
                     "}";

    @Language("java")
    String linux = "" +
                   "class Test {\n" +
                   "    // some comment\n" +
                   "    public void test() {\n" +
                   "        System.out.println();\n" +
                   "    }\n" +
                   "}";

    @Language("java")
    String windowsJavadoc = "" +
                            "/**\r\n" +
                            " *\r\n" +
                            " */\r\n" +
                            "class Test {\r\n" +
                            "}";

    @Language("java")
    String linuxJavadoc = "" +
                          "/**\n" +
                          " *\n" +
                          " */\n" +
                          "class Test {\n" +
                          "}";

    @Test
    void trimKeepCRLF() {
        assertThat(StringUtils.trimIndent("\n  test\r\n  test".replace('\r', '⏎'))
          .replace('⏎', '\r')).isEqualTo("test\r\ntest");
    }

    @Test
    void windowsToLinux() {
        rewriteRun(
          normalizeLineBreaks(false),
          java(windows, linux)
        );
    }

    @Test
    void linuxToWindows() {
        rewriteRun(
          normalizeLineBreaks(true),
          java(linux, windows)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void doNotChangeWindowsJavadoc() {
        rewriteRun(
          normalizeLineBreaks(true),
          java(windowsJavadoc)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void doNotChangeLinuxJavadoc() {
        rewriteRun(
          normalizeLineBreaks(false),
          java(linuxJavadoc)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void windowsToLinuxJavadoc() {
        rewriteRun(
          normalizeLineBreaks(false),
          java(windowsJavadoc, linuxJavadoc)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void linuxToWindowsJavadoc() {
        rewriteRun(
          normalizeLineBreaks(true),
          java(linuxJavadoc, windowsJavadoc)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-docs/issues/67")
    @Test
    void preservesExistingWindowsEndingsByDefault() {
        rewriteRun(
          normalizeLineBreaks(true),
          java(windows)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-docs/issues/67")
    @Test
    void preservesExistingLinuxEndingsByDefault() {
        rewriteRun(
          normalizeLineBreaks(false),
          java(linux)
        );
    }
}
