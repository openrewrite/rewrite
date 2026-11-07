/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.ruby;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.tree.ParseError;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RubyParserTest {

    @Test
    void sourcePathIsRelativeToTheRoot() {
        Path root = Paths.get("/repo");
        SourceFile cu = parse(root.resolve("app/models/user.rb"), root, "puts 1\n", StandardCharsets.UTF_8);
        assertThat(cu.getSourcePath()).isEqualTo(Paths.get("app/models/user.rb"));
    }

    /**
     * Prism reports identifier names and literal values as bytes in the source's own encoding, so
     * decoding them as UTF-8 would desync the cursor on the first non-ASCII name.
     */
    @Test
    void nonUtf8Identifiers() {
        Charset windows1252 = Charset.forName("windows-1252");
        SourceFile cu = parse(Paths.get("probe.rb"), null, "café = 1\nputs café\n", windows1252);
        assertThat(cu).isInstanceOf(Rb.CompilationUnit.class);
        assertThat(cu.printAll()).isEqualTo("café = 1\nputs café\n");
        assertThat(cu.getCharset()).isEqualTo(windows1252);
    }

    @Test
    void nonUtf8LiteralValue() {
        SourceFile cu = parse(Paths.get("probe.rb"), null, "x = \"café\"\n", Charset.forName("windows-1252"));
        J.Literal literal = (J.Literal) ((J.Assignment) ((Rb.CompilationUnit) cu).getStatements().get(0))
                .getAssignment();
        assertThat(literal.getValue()).isEqualTo("café");
    }

    @Test
    void nestingTooDeepForPrismsStackFailsOnlyThatFile() {
        SourceFile tooDeep = parse("x = " + "-> a { ".repeat(60) + "a" + " }".repeat(60) + "\n");
        SourceFile after = parse(NEXT);

        assertThat(tooDeep).isInstanceOf(ParseError.class);
        assertThat(after).isInstanceOf(Rb.CompilationUnit.class);
    }

    /**
     * Seventy-five chained assignments recurse past Prism's stack, which nesting doesn't foresee.
     */
    @Test
    void stackOverrunFailsOnlyThatFile() {
        SourceFile overrun = parse(chainedAssignments(75));
        SourceFile after = parse(NEXT);

        assertThat(overrun).isInstanceOf(ParseError.class);
        assertThat(after).isInstanceOf(Rb.CompilationUnit.class);
    }

    /**
     * Sixty-five chained assignments run into the bottom of Prism's stack and still return a tree.
     */
    @Test
    void stackOverrunThatReturnsIsNotTrusted() {
        assertThat(parse(chainedAssignments(65))).isInstanceOf(ParseError.class);
        assertThat(parse(NEXT)).isInstanceOf(Rb.CompilationUnit.class);
    }

    @Test
    void nestingWithinPrismsStackParses() {
        String nested = "a { ".repeat(30) + "b" + " }".repeat(30) + "\n";
        SourceFile cu = parse(Paths.get("nested.rb"), null, nested, StandardCharsets.UTF_8);
        assertThat(cu).isInstanceOf(Rb.CompilationUnit.class);
        assertThat(cu.printAll()).isEqualTo(nested);
    }

    private static final String NEXT = "x = [1, 2].map { |a| a + 1 }\n";

    private static String chainedAssignments(int count) {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < count; i++) {
            source.append('a').append(i).append(" = ");
        }
        return source.append("1\n").toString();
    }

    private static SourceFile parse(String source) {
        return parse(Paths.get("file.rb"), null, source, StandardCharsets.UTF_8);
    }

    private static SourceFile parse(Path path, Path relativeTo, String source, Charset charset) {
        byte[] bytes = source.getBytes(charset);
        Parser.Input input = new Parser.Input(path, () -> new ByteArrayInputStream(bytes));
        return new RubyParser().parseInputs(List.of(input), relativeTo, new InMemoryExecutionContext())
                .findFirst().orElseThrow();
    }
}
