/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.openrewrite.ruby.Assertions.ruby;

public class CharacterLiteralToStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CharacterLiteralToString());
    }

    @Test
    void convertToString() {
        rewriteRun(
          ruby(
            "?A",
            "'A'"
          )
        );
    }

    /**
     * An escape has to survive the conversion, and single quotes do not interpret escapes, so the
     * result is double-quoted.
     */
    @ParameterizedTest
    @MethodSource("escapesAndQuotes")
    void escapesAndQuotes(String before, String after) {
        rewriteRun(
          ruby(before, after)
        );
    }

    static Stream<Arguments> escapesAndQuotes() {
        return Stream.of(
          arguments("?\\n", "\"\\n\""),
          arguments("?\\t", "\"\\t\""),
          arguments("?\\r", "\"\\r\""),
          arguments("?\\e", "\"\\e\""),
          arguments("?\\0", "\"\\0\""),
          arguments("?\\\\", "\"\\\\\""),
          arguments("?'", "\"'\""),
          arguments("?\\s", "' '"),
          arguments("?\"", "'\"'"),
          arguments("?#", "'#'"),
          arguments("?é", "'é'")
        );
    }

    /**
     * Only a literal written in the {@code ?A} form is a character literal; a bare {@code ?} inside
     * an interpolated string and a {@code %w} element are ordinary text that happens to start with
     * one.
     */
    @ParameterizedTest
    @ValueSource(strings = {
      "url = \"#{path}?#{query}\"",
      "x = %w[?a ?b]",
      "x = %i[?a]",
      "x = \"?\"",
      "x = a ? b : c"
    })
    void notACharacterLiteral(String source) {
        rewriteRun(
          ruby(source)
        );
    }
}
