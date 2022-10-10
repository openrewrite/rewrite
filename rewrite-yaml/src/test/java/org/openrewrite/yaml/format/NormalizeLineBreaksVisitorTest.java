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
package org.openrewrite.yaml.format;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.yaml.Assertions.yaml;

class NormalizeLineBreaksVisitorTest implements RewriteTest {

    @Language("yml")
    final String windows = "" +
                           "root:\r\n" +
                           "  - a: 0\r\n" +
                           "    b: 0";

    @Language("yml")
    final String linux = "" +
                         "root:\n" +
                         "  - a: 0\n" +
                         "    b: 0";

    @Language("yml")
    final String mixedLinux = "" +
                              "root:\n" +
                              "  - a: 0\n" +
                              "  - b: 0\r\n" +
                              "  - c: 0";

    @Language("yml")
    final String formattedLinux = "" +
                                  "root:\n" +
                                  "  - a: 0\n" +
                                  "  - b: 0\n" +
                                  "  - c: 0";

    @Language("yml")
    final String mixedWindows = "" +
                                "root:\r\n" +
                                "  - a: 0\n" +
                                "  - b: 0\r\n" +
                                "  - c: 0";

    @Language("yml")
    final String formattedWindows = "" +
                                    "root:\r\n" +
                                    "  - a: 0\r\n" +
                                    "  - b: 0\r\n" +
                                    "  - c: 0";

    @Test
    void windowsToLinux() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new NormalizeLineBreaksVisitor<>(new GeneralFormatStyle(false), null))),
          yaml(windows, linux)
        );
    }

    @Test
    void linuxToWindows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new NormalizeLineBreaksVisitor<>(new GeneralFormatStyle(true), null))),
          yaml(linux, windows)
        );
    }

    @Test
    void autoDetectLinux() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AutoFormatVisitor<>(null))),
          yaml(mixedLinux, formattedLinux)
        );
    }

    @Test
    void autoDetectWindows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AutoFormatVisitor<>(null))),
          yaml(mixedWindows, formattedWindows)
        );
    }
}
