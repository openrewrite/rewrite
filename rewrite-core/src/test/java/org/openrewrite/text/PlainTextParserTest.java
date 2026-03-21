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

import java.util.stream.Stream;
import org.assertj.core.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;

import java.nio.file.Path;
import java.util.List;
import org.openrewrite.Parser.Input;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextParserTest {

    @Test
    void plainTextMask() {
        PlainTextParser parser = PlainTextParser.builder()
          .plainTextMasks(Path.of("."), List.of("**/*.png"))
          .build();

        List<Parser.Input> inputs = List.of(Parser.Input.fromString(Path.of("test.png"), "test"));
        assertThat(inputs)
          .allMatch(input -> parser.accept(input.getPath()));

        assertThat(parser
          .parseInputs(inputs, null, new InMemoryExecutionContext())
          .findFirst()
        ).containsInstanceOf(PlainText.class);
    }

    @ParameterizedTest
    @MethodSource
    void testUtf8WithAndWithoutBom(String text, boolean hasBom) {
        PlainTextParser parser = PlainTextParser.builder().build();
        SourceFile parsed = parser.parse(text).findFirst().orElseThrow();

        assertThat(parsed).isInstanceOf(PlainText.class);

        assertThat(parsed.isCharsetBomMarked()).isEqualTo(hasBom);

        SourceFile checked = parser.requirePrintEqualsInput(
          parsed, Input.fromString(text), null, new InMemoryExecutionContext()
        );

        assertThat(checked).isNotInstanceOf(ParseError.class);
        assertThat(checked).isSameAs(parsed);
    }

    static Stream<Arguments> testUtf8WithAndWithoutBom() {
        return Stream.of(
          Arguments.of("""
              <?xml version="1.0" encoding="UTF-8"?><a />
              """, false),
          Arguments.of("""
              \uFEFF<?xml version="1.0" encoding="UTF-8"?><a />
              """, true)
        );
    }
}
