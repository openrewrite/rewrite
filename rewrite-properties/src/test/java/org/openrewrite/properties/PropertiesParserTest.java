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
package org.openrewrite.properties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class PropertiesParserTest implements RewriteTest {

    @Test
    void noEndOfFile() {
        rewriteRun(
          properties(
            "key=value",
            spec -> spec.beforeRecipe(p -> assertThat(p.getEof()).isEmpty())
          )
        );
    }

    @Test
    void endOfFile() {
        rewriteRun(
          properties(
            "key=value\n",
            spec -> spec
              .noTrim()
              .beforeRecipe(p -> assertThat(p.getEof()).isEqualTo("\n"))
          )
        );
    }

    @Test
    void extraneousCharactersAtEndOfFile() {
        rewriteRun(
          properties(
            "key=value\nasdf\n",
            spec -> spec
              .noTrim()
              .beforeRecipe(p -> assertThat(p.getEof()).isEqualTo("\nasdf\n"))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2499")
    @ParameterizedTest
    @ValueSource(strings = {"#", "!"})
    void commentThenEntry(String commentStyle) {
        rewriteRun(
          properties(
            """
              %s this is a comment
              key=value
              """.formatted(commentStyle),
            spec -> spec.beforeRecipe(p -> {
                assertThat(p.getContent().get(0))
                  .isInstanceOf(Properties.Comment.class)
                  .matches(comment -> ((Properties.Comment) comment).getMessage().equals(" this is a comment"));
                assertThat(p.getContent().get(1))
                  .isInstanceOf(Properties.Entry.class)
                  .matches(e -> e.getPrefix().equals("\n"));
            })
          )
        );
    }

    @Test
    void multipleEntries() {
        rewriteRun(
          properties(
            """
              key=value1
              key2 = value2
              """,
            containsValues("value1", "value2")
          )
        );
    }

    @SuppressWarnings({"WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty"})
    @Issue("https://github.com/openrewrite/rewrite/issues/2471")
    @Test
    @Disabled
    void escapedEndOfLine() {
        rewriteRun(
          properties(
            """
              key=val\\
                        ue1
              ke\\
                  y2 = value2
              """,
            containsValues("value1", "value2")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2411")
    @Test
    void commentsWithMultipleDelimiters() {
        rewriteRun(
          properties(
            """
              ########################
              #
              ########################
                          
              key1=value1
                          
              !!!!!!!!!!!!!!!!!!!!!!!
              !
              !!!!!!!!!!!!!!!!!!!!!!!
                          
              key2=value2
              """,
            containsValues("value1", "value2")
          )
        );
    }

    @SuppressWarnings("WrongPropertyKeyValueDelimiter")
    @Issue("https://github.com/openrewrite/rewrite/issues/2501")
    @Test
    void delimitedByWhitespace() {
        rewriteRun(
          properties(
            """
              key1         value1
              key2:value2
              """,
            containsValues("value1", "value2")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2501")
    @Test
    void escapedEntryDelimiters() {
        rewriteRun(
          properties(
            """
              ke\\=y=value1
              key\\:2=value2
              key3=val\\=ue3
              key4=val\\:ue4
              """,
            containsValues("value1", "value2", "val\\=ue3", "val\\:ue4")
          )
        );
    }

    private static Consumer<SourceSpec<Properties.File>> containsValues(String... valueAssertions) {
        return spec -> spec.beforeRecipe(props -> {
            List<String> values = TreeVisitor.collect(new PropertiesVisitor<>() {
                @Override
                public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                    return SearchResult.found(entry);
                }
            }, props, new ArrayList<>(), Properties.Entry.class, e -> e.getValue().getText());

            assertThat(values).containsExactly(valueAssertions);
        });
    }
}
