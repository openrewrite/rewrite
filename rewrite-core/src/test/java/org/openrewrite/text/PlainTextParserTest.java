/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.text.Assertions.plainText;

class PlainTextParserTest {

    @Test
    void plainTextMask() {
        PlainTextParser parser = PlainTextParser.builder()
          .plainTextMasks(Paths.get("."), List.of("**/*.png"))
          .build();

        List<Parser.Input> inputs = List.of(Parser.Input.fromString(Paths.get("test.png"), "test"));
        assertThat(inputs)
          .allMatch(input -> parser.accept(input.getPath()));

        assertThat(parser
          .parseInputs(inputs, null, new InMemoryExecutionContext())
          .findFirst()
        ).containsInstanceOf(PlainText.class);
    }

    @Nested
    class NpmTest implements RewriteTest {
        @Test
        void packageJson() {
            rewriteRun(
              plainText(
                """
                {
                  "name": "hello-world",
                  "version": "1.0.0",
                  "description": "A simple hello world package",
                  "main": "index.js",
                  "scripts": {
                    "test": "echo \\"Error: no test specified\\" && exit 1"
                  },
                  "keywords": ["hello", "world"],
                  "author": "Tony Wroten",
                  "license": "MIT"
                }
                """,
                spec -> spec
                  .path("package.json")
                  .afterRecipe(plainText -> {
                      assertThat(plainText.getMarkers().getMarkers()).isNotEmpty();
                  })
              )
            );
        }
    }
}
