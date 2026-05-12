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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.ParseWarning;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.scala.tree.S;
import org.openrewrite.tree.ParseError;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ScalaParserDiagnosticsTest {

    // Snippet whose match case is unreachable (the wildcard subsumes the literal),
    // which Dotty reports as a `Match case Unreachable Warning` during type-checking.
    private static final String SNIPPET_WITH_WARNING =
            "object Diag {\n" +
            "  def x(i: Int): Int = i match {\n" +
            "    case _ => 0\n" +
            "    case 1 => 1\n" +
            "  }\n" +
            "}\n";

    @Test
    void diagnosticsDoNotLeakToStderr() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));

            ScalaParser parser = ScalaParser.builder().classpath(JavaParser.runtimeClasspath()).build();
            List<SourceFile> parsed = parser.parse(SNIPPET_WITH_WARNING).collect(Collectors.toList());

            assertThat(parsed).hasSize(1);
            assertThat(parsed.get(0)).isNotInstanceOf(ParseError.class);
        } finally {
            System.setErr(originalErr);
        }

        assertThat(captured.toString(StandardCharsets.UTF_8))
                .as("Dotty diagnostics must not leak to stderr from ScalaParser")
                .isEmpty();
    }

    @Test
    void warningsAreSurfacedAsMarkers() {
        ScalaParser parser = ScalaParser.builder().classpath(JavaParser.runtimeClasspath()).build();
        List<SourceFile> parsed = parser.parse(SNIPPET_WITH_WARNING).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        SourceFile cu = parsed.get(0);
        assertThat(cu).isInstanceOf(S.CompilationUnit.class);

        List<ParseWarning> warnings = cu.getMarkers().findAll(ParseWarning.class);
        assertThat(warnings)
                .as("Dotty diagnostics should be surfaced on the LST as ParseWarning markers")
                .isNotEmpty();
        assertThat(warnings).anyMatch(w -> w.getMessage().toLowerCase().contains("unreachable"));
    }
}
