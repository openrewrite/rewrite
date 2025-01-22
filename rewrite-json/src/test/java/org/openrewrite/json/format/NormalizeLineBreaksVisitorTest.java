/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.json.format;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NormalizeLineBreaksVisitorTest implements RewriteTest {

    @Language("json")
    final String windows =
        "{\r\n" +
        "  \"name\": \"John\",\r\n" +
        "  \"age\": 30,\r\n" +
        "  \"car\": null}";

    @Language("json")
    final String linux =
        "{\n" +
        "  \"name\": \"John\",\n" +
        "  \"age\": 30,\n" +
        "  \"car\": null}";

    @Language("json")
    final String mixedLinux =
        "{\n" +
        "  \"name\": \"John\",\n" +
        "  \"age\": 30,\r\n" +
        "  \"car\": null}";

    @Language("json")
    final String mixedWindows =
      "{\r\n" +
      "  \"name\": \"John\",\r\n" +
      "  \"age\": 30,\n" +
      "  \"car\": null}";

    private TreeVisitor<?, ExecutionContext> LINUX_VISITOR = new NormalizeLineBreaksVisitor<>(new GeneralFormatStyle(false), null);
    private TreeVisitor<?, ExecutionContext> WINDOWS_VISITOR = new NormalizeLineBreaksVisitor<>(new GeneralFormatStyle(true), null);

    @Test
    void windowsToLinux() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> LINUX_VISITOR)),
          json(windows, linux)
        );
    }

    @Test
    void linuxToWindows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> WINDOWS_VISITOR)),
          json(linux, windows)
        );
    }

    @Test
    void fixLinux() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> LINUX_VISITOR)),
          json(mixedLinux, linux)
        );
    }

    @Test
    void fixtWindows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> WINDOWS_VISITOR)),
          json(mixedWindows, windows)
        );
    }
}
