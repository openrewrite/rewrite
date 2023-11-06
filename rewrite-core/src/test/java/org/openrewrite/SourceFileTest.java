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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.text.PlainText;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFileTest {

    @Test
    void isPrintEqualForDefaultCharsets() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Parser.Input input = Parser.Input.fromString("äö");
        SourceFile sourceFile = PlainText.builder()
          .text("äö")
          .build();

        assertThat(sourceFile.printEqualsInput(input, ctx))
          .isTrue();
    }

    @Test
    void isPrintEqualForExplicitCharsets() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Parser.Input input = Parser.Input.fromString("äö", StandardCharsets.ISO_8859_1);
        SourceFile sourceFile = PlainText.builder()
          .text("äö")
          .charsetName("ISO-8859-1")
          .build();

        assertThat(sourceFile.printEqualsInput(input, ctx))
          .isTrue();
    }

    @Test
    void isNotPrintEqualForDifferentCharsets() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Parser.Input input = Parser.Input.fromString("äö");
        SourceFile sourceFile = PlainText.builder()
          .text("äö")
          .charsetName("ISO-8859-1")
          .build();

        assertThat(sourceFile.printEqualsInput(input, ctx))
          .isFalse();
    }

}
