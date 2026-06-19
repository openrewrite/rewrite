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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.scala.tree.S;
import org.openrewrite.tree.ParseError;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ScalaParser#parseInputs(Iterable, java.nio.file.Path, org.openrewrite.ExecutionContext)}
 * must never throw: a failure to parse one input yields a {@link ParseError} for that input
 * only, and a failure of the batch as a whole degrades to per-file parsing.
 */
class ScalaParserErrorHandlingTest {

    private static final String GOOD_SOURCE = "object Good { val x = 1 }";

    @Test
    void unparseableInputProducesParseErrorAndOtherInputsStillParse() {
        // Expression nesting deep enough to overflow the stack of dotty's
        // recursive descent parser, making this single input "throw" to parse.
        StringBuilder bad = new StringBuilder("object Bad { val x = ");
        int depth = 20_000;
        for (int i = 0; i < depth; i++) {
            bad.append('(');
        }
        bad.append('1');
        for (int i = 0; i < depth; i++) {
            bad.append(')');
        }
        bad.append(" }");

        ScalaParser parser = ScalaParser.builder().build();
        List<SourceFile> parsed = parser.parse(GOOD_SOURCE, bad.toString()).collect(Collectors.toList());

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        assertThat(parsed.get(1)).isInstanceOf(ParseError.class);
    }

    @Test
    void batchFailureDegradesToPerFileParsing() {
        // A source whose first read fails (e.g. a transient I/O error) makes batch
        // compilation fail as a whole; parseInputs must degrade to per-file parsing
        // instead of throwing.
        Supplier<InputStream> flakySource = new Supplier<InputStream>() {
            private boolean first = true;

            @Override
            public InputStream get() {
                if (first) {
                    first = false;
                    throw new IllegalStateException("simulated transient read failure");
                }
                return new ByteArrayInputStream("object Flaky { val x = 2 }".getBytes(StandardCharsets.UTF_8));
            }
        };

        List<Parser.Input> inputs = Arrays.asList(
          Parser.Input.fromString(Paths.get("Good.scala"), GOOD_SOURCE),
          new Parser.Input(Paths.get("Flaky.scala"), flakySource)
        );

        ScalaParser parser = ScalaParser.builder().build();
        List<SourceFile> parsed = parser.parseInputs(inputs, null, new InMemoryExecutionContext())
          .collect(Collectors.toList());

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        assertThat(parsed.get(1)).isInstanceOf(S.CompilationUnit.class);
    }

    @Test
    void withSelfTypeParsesWhenClasspathLacksScalaStdlib() {
        // A non-empty classpath without the Scala stdlib overrides dotty's default
        // classpath lookup, so `Run` construction cannot resolve the `scala` package
        // (the situation a fat-jar application is in even with an empty classpath).
        // Parsing an `A with B` type must still produce a real tree, not throw
        // MissingCoreLibraryException.
        ScalaParser parser = ScalaParser.builder()
          .classpath(JavaParser.dependenciesFromClasspath("assertj-core"))
          .build();

        List<SourceFile> parsed = parser.parse("class Service { self: Logging with Config => }")
          .collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
    }

    @Test
    void withSelfTypeParsesWithRuntimeClasspath() {
        // Precondition for withSelfTypeParsesWhenClasspathLacksScalaStdlib: the
        // self-type syntax itself is supported when the stdlib is resolvable.
        ScalaParser parser = ScalaParser.builder().classpath(JavaParser.runtimeClasspath()).build();

        List<SourceFile> parsed = parser.parse("class Service { self: Logging with Config => }")
          .collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
    }
}
