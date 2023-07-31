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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.dir;
import static org.openrewrite.test.SourceSpecs.text;

class FindTest implements RewriteTest {

    @DocumentExample
    @Test
    void regex() {
        rewriteRun(
          spec -> spec.recipe(new Find("[T\\s]", true, true, null, null, null)),
          text(
            """
              This is\ttext.
              """,
            """
              ~~>This~~> is~~>\ttext.
              """
          )
        );
    }

    @Test
    void plainText() {
        rewriteRun(
          spec -> spec.recipe(new Find("\\s", null, null, null, null, null)),
          text(
            """
              This i\\s text.
              """,
            """
              This i~~>\\s text.
              """
          )
        );
    }

    @Test
    void caseInsensitive() {
        rewriteRun(
          spec -> spec.recipe(new Find("text", null, null, null, null, "**/foo/**;**/baz/**")),
          dir("foo",
            text(
              """
                TEXT
                """,
              """
                ~~>TEXT
                """
            )
          ),
          dir("bar",
            text("""
              TEXT
              """)
          ),
          dir("baz",
            text(
              """
                TEXT
                """,
              """
                ~~>TEXT
                """
            )
          )
        );
    }
}
