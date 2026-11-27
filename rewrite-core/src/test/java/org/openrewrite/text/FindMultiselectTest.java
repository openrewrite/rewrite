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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import java.util.Set;

import static org.openrewrite.test.SourceSpecs.text;

class FindMultiselectTest implements RewriteTest {

    @DocumentExample
    @Test
    void findText() {
        rewriteRun(
          spec -> spec.recipe(new FindMultiselect("text", null, null, null)),
          text(
            """
              This is text.
              """,
            """
              This is ~~>text.
              """
          )
        );
    }

    @Test
    void findRegexIgnoringCaseByDefault() {
        rewriteRun(
          spec -> spec.recipe(new FindMultiselect("te[a-z]+", true, null, null)),
          text(
            """
              This is Text.
              """,
            """
              This is ~~>Text.
              """
          )
        );
    }

    @Test
    void honorCaseSensitiveOption() {
        rewriteRun(
          spec -> spec.recipe(new FindMultiselect("text", true, Set.of("Case-sensitive"), null)),
          text(
            """
              Text and text.
              """,
            """
              Text and ~~>text.
              """
          )
        );
    }

    @Test
    void restrictToFilePattern() {
        rewriteRun(
          spec -> spec.recipe(new FindMultiselect("text", null, null, "**/*.md")),
          text(
            "This is text.",
            "This is ~~>text.",
            spec -> spec.path("README.md")
          ),
          text(
            "This is text.",
            spec -> spec.path("notes.txt")
          )
        );
    }
}
