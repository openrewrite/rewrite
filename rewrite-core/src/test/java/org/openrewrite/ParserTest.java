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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingExecutionContextView;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class ParserTest implements RewriteTest {

    @Test
    void overrideCharset() {
        rewriteRun(
                spec -> spec.executionContext(ParsingExecutionContextView
                        .view(new InMemoryExecutionContext())
                        .setCharset(ISO_8859_1)),
                text(
                        "À1",
                        spec -> spec.beforeRecipe(txt ->
                                assertThat(txt.getCharset()).isEqualTo(ISO_8859_1))
                )
        );
    }

    @Test
    void canPrintParseError() {
        ParseError pe = ParseError.build(new PlainTextParser(),
          Parser.Input.fromString("bad file"),
          null,
          new InMemoryExecutionContext(),
          new RuntimeException("bad file!!"));

        assertThat(pe.printAll()).isEqualTo("bad file");
    }
}
