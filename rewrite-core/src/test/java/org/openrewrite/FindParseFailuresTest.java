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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;
import org.openrewrite.table.ParseFailures;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class FindParseFailuresTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindParseFailures(5, null, null));
    }

    @Test
    void findParseFailures() {
        ParseExceptionResult per = ParseExceptionResult.build(PlainTextParser.class, new RuntimeException("boom"), null);
        rewriteRun(
          spec -> spec.dataTable(ParseFailures.Row.class, rows -> {
              assertThat(rows).hasSize(1);
              ParseFailures.Row row = rows.get(0);
              assertThat(row.getParser()).isEqualTo("PlainTextParser");
              assertThat(row.getSourcePath()).isEqualTo("file.txt");
              assertThat(row.getExceptionType()).isEqualTo("RuntimeException");
              assertThat(row.getSnippet()).isEqualTo("hello");
          }),
          text(
            "hello world!",
            "~~(%s)~~>hello world!".formatted(per.getMessage()),
            spec -> spec
              .mapBeforeRecipe(pt -> pt
                .withSnippets(List.of(new PlainText.Snippet(
                  Tree.randomId(),
                  new Markers(Tree.randomId(), List.of(per)),
                  pt.getText())))
                .withText("")
              )
          )
        );
    }
}
