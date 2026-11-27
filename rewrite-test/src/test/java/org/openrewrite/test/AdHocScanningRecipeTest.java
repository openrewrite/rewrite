/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.util.List;

import static org.openrewrite.test.SourceSpecs.text;

class AdHocScanningRecipeTest implements RewriteTest {

    @DocumentExample
    @Test
    void editAndGenerateSources() {
        rewriteRun(
          spec -> spec.recipe(new AdHocScanningRecipe(null, null, null,
            () -> new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return "hello".equals(text.getText()) ? text.withText("goodbye") : text;
                }
            },
            () -> List.of(PlainText.builder().sourcePath(Path.of("generated.txt")).text("generated").build()),
            null, 1)),
          text("hello", "goodbye"),
          text(null, "generated", spec -> spec.path("generated.txt"))
        );
    }
}
