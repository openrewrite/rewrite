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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RubyParserTest {

    @Test
    void sourcePathIsRelativeToTheRoot() {
        Path root = Paths.get("/repo");
        SourceFile cu = parse(root.resolve("app/models/user.rb"), root, "puts 1\n", Charset.forName("UTF-8"));
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

    private static SourceFile parse(Path path, Path relativeTo, String source, Charset charset) {
        byte[] bytes = source.getBytes(charset);
        Parser.Input input = new Parser.Input(path, () -> new ByteArrayInputStream(bytes));
        return new RubyParser().parseInputs(List.of(input), relativeTo, new InMemoryExecutionContext())
                .findFirst().orElseThrow();
    }
}
