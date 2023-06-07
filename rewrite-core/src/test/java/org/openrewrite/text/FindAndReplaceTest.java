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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class FindAndReplaceTest implements RewriteTest {

    @DocumentExample
    @Test
    void nonTxtExtension() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """,
            spec -> spec.path("test.yml")
          )
        );
    }

    @Test
    void defaultNonRegex() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """
          )
        );
    }

    @Test
    void regexReplace() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", true)),
          text(
            """
              This is text.
              """,
            """
              GGGGGGGGGGGGG
              """
          )
        );
    }

    @Test
    void captureGroups() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("This is ([^.]+).", "I like $1.", true)),
          text(
            """
              This is text.
              """,
            """
              I like text.
              """
          )
        );
    }
}
