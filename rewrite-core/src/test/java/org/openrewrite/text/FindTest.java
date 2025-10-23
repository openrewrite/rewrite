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
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.dir;
import static org.openrewrite.test.SourceSpecs.text;

class FindTest implements RewriteTest {

    @DocumentExample
    @Test
    void regex() {
        rewriteRun(
          spec -> spec.recipe(new Find("[T\\s]", true, true, null, null, null, null, null)),
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
    void isFullMatch() {
        rewriteRun(
          spec -> spec.recipe(new Find("This is text.", true, true, null, null, null, null, null))
            .dataTable(TextMatches.Row.class, rows ->
              assertThat(rows)
                .hasSize(1)
                .allSatisfy(r -> assertThat(r.getMatch()).isEqualTo("~~>This is text."))),
          text(
            """
              This is text.
              """,
            """
              ~~>This is text.
              """
          )
        );
    }

    @Test
    void dataTable() {
        rewriteRun(
          spec -> spec.recipe(new Find("text", null, null, null, null, null, null, 50))
            .dataTable(TextMatches.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.getFirst().getMatch()).isEqualTo("This is ~~>text.");
            }),
          text(
            """
              This is a line above.
              This is text.
              This is a line below.
              """,
            """
              This is a line above.
              This is ~~>text.
              This is a line below.
              """
          )
        );
    }

    @Test
    void plainText() {
        rewriteRun(
          spec -> spec.recipe(new Find("\\s", null, null, null, null, null, null, 50)),
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
          spec -> spec.recipe(new Find("text", null, null, null, null, "**/foo/**;**/baz/**", null, 50)),
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

    @Test
    void regexBasicMultiLine() {
        rewriteRun(
          spec -> spec.recipe(new Find("[T\\s]", true, true, true, null, null, null, 50)),
          text(
            """
              This is\ttext.
              This is\ttext.
              """,
            """
              ~~>This~~> is~~>\ttext.~~>
              ~~>This~~> is~~>\ttext.
              """
          )
        );
    }

    @Test
    void regexWithoutMultilineAndDotall() {
        rewriteRun(
          spec -> spec.recipe(new Find("^This.*below\\.$", true, true, false, false, null, null, 50)),
          text(
            """
              This is text.
              This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """
          )
        );
    }

    @Test
    void regexMatchingWhitespaceWithoutMultilineWithDotall() {
        rewriteRun(
          spec -> spec.recipe(new Find("One.Two$", true, true, false, true, null, null, 50)),
          //language=csv
          text( // the `.` above matches the space character on the same line
            """
              Zero
              One Two
              Three
              """
          )
        );
    }

    @Test
    void regexWithoutMultilineAndWithDotAll() {
        rewriteRun(
          spec -> spec.recipe(new Find("^This.*below\\.$", true, true, false, true, null, null, 50)),
          text(
            """
              This is text.
              This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """,
            """
              ~~>This is text.
              This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """
          )
        );
    }

    @Test
    void regexWithMultilineAndWithoutDotall() {
        rewriteRun(
          spec -> spec.recipe(new Find("^This.*below\\.$", true, true, true, false, null, null, 50)),
          text(
            """
              This is text.
              This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """,
            """
              This is text.
              ~~>This is a line below.
              This is a line above.
              This is text.
              ~~>This is a line below.
              """
          )
        );
    }

    @Test
    void regexWithBothMultilineAndDotAll() {
        rewriteRun(
          spec -> spec.recipe(new Find("^This.*below\\.$", true, true, true, true, null, null, 50)),
          text(
            """
              The first line.
              This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """,
            """
              The first line.
              ~~>This is a line below.
              This is a line above.
              This is text.
              This is a line below.
              """
          )
        );
    }

    @Test
    void description() {
        rewriteRun(
          spec -> spec.recipe(new Find("text", null, null, null, null, null, true, 50)),
          text(
            """
              This is text.
              """,
            """
              This is ~~(text)~~>text.
              """
          )
        );
    }

    @Test
    void longLine() {
        rewriteRun(
          spec -> spec.recipe(new Find("very", null, null, null, null, null, null, 50))
            .dataTable(TextMatches.Row.class, rows -> {
                assertThat(rows).hasSize(18);
                assertThat(rows).satisfiesExactly(
                  r -> assertThat(r.getMatch()).isEqualTo("This is a ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("...is is a very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("...a very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, ..."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very l..."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very long li..."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("..., very, very, very, very, very, very, very, very, ~~>very long line.")
                );
            }),
          text(
            """
              This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line.
              """,
            """
              This is a ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very long line.
              """
          )
        );
    }

    @Test
    void justMatch() {
        rewriteRun(
          spec -> spec.recipe(new Find("very", null, null, null, null, null, null, null))
            .dataTable(TextMatches.Row.class, rows -> {
                assertThat(rows).hasSize(18);
                assertThat(rows).allSatisfy(
                  r -> assertThat(r.getMatch()).isEqualTo("...~~>very...")
                );
            }),
          text(
            """
              This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line.
              """,
            """
              This is a ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very long line.
              """
          )
        );
    }

    @Test
    void noTruncate() {
        rewriteRun(
          spec -> spec.recipe(new Find("very", null, null, null, null, null, null, -1))
            .dataTable(TextMatches.Row.class, rows -> {
                assertThat(rows).hasSize(18);
                assertThat(rows).satisfiesExactly(
                  r -> assertThat(r.getMatch()).isEqualTo("This is a ~~>very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, ~~>very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, ~~>very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, ~~>very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, ~~>very, very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, very, ~~>very, very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, ~~>very, very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, ~~>very, very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, ~~>very, very long line."),
                  r -> assertThat(r.getMatch()).isEqualTo("This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, ~~>very long line.")
                );
            }),
          text(
            """
              This is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very long line.
              """,
            """
              This is a ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very, ~~>very long line.
              """
          )
        );
    }

    @Test
    void sequentialFinds() {
        rewriteRun(
          spec -> spec.recipes(
            new Find("foo", null, null, null, null, null, null, null),
            new Find("bar", null, null, null, null, null, null, null)
          ),
          text(
            """
              This contains foo and bar.
              """,
            """
              This contains ~~>foo and ~~>bar.
              """
          )
        );
    }
}
