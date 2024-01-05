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

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Test
    void printIdempotentDiffPrint() {
        Parser parser = new PlainTextParser();
        ExecutionContext ctx = new InMemoryExecutionContext();

        String before = """
          line 1
          line 2
          """;
        String after = """
          line 1
          DIFF HERE
          line 2
          """;
        Path path = Paths.get("1.txt");
        String expectedDiff = """
          --- a/1.txt
          +++ b/1.txt
          @@ -1,3 +1,2 @@\s
           line 1
          -DIFF HERE
           line 2
          """;

        Parser.Input input1 = new Parser.Input(path, () -> new ByteArrayInputStream(before.getBytes()));
        Parser.Input input2 = new Parser.Input(path, () -> new ByteArrayInputStream(after.getBytes()));
        SourceFile s1 = parser.parse(input1.getSource(ctx).readFully()).toList().get(0);
        SourceFile out = parser.requirePrintEqualsInput(s1, input2, null, ctx);
        assertThat(out).isInstanceOf(ParseError.class);
        ParseExceptionResult parseExceptionResult = out.getMarkers().findFirst(ParseExceptionResult.class).get();
        int startIndex = parseExceptionResult.getMessage().indexOf(expectedDiff);
        int endIndex = startIndex + expectedDiff.length();
        assertThat(parseExceptionResult.getMessage().substring(startIndex, endIndex)).isEqualTo(expectedDiff);
    }
}
