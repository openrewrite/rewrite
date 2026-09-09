/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Go server prints every parsed compilation unit and compares it against the
 * file on disk, reporting a mismatch as a {@link ParseError}. These cover the wire
 * from the {@link ExecutionContext} option through to that comparison.
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class PrintIdempotencyIntegTest {

    /**
     * Constructs whose whitespace, line endings or comment placement are the most
     * likely to be dropped between the Go AST and the LST.
     */
    private static final Map<String, String> TRICKY_SOURCES = new LinkedHashMap<>();

    static {
        TRICKY_SOURCES.put("crlf.go", "package p\r\n\r\nfunc Crlf() int {\r\n\tx := 1\r\n\treturn x\r\n}\r\n");
        TRICKY_SOURCES.put("bom.go", "\uFEFFpackage p\n\nfunc Bom() {}\n");
        TRICKY_SOURCES.put("nofinalnewline.go", "package p\n\nfunc NoFinalNewline() {}");
        TRICKY_SOURCES.put("comments.go", "package p\n\n// a\n/* b */\n// c\nfunc Comments(a /* inline */ int) {}\n");
        TRICKY_SOURCES.put("mixedindent.go", "package p\n\nfunc MixedIndent() {\n  x := 1\n\t_ = x\n}\n");
        TRICKY_SOURCES.put("trailingspace.go", "package p   \n\nfunc TrailingSpace() {}   \n");
        TRICKY_SOURCES.put("rawstring.go", "package p\n\nvar Raw = `a\\nb\n\tc`\n");
        TRICKY_SOURCES.put("emptydecls.go", "package p\n\nimport ()\n\nvar ()\n\ntype ()\n");
        TRICKY_SOURCES.put("semicolons.go", "package p\n\nfunc Semis() { a := 1; b := 2; _, _ = a, b }\n");
        TRICKY_SOURCES.put("linedirective.go", "package p\n\n//line gen.go:10\nfunc LineDirective() {}\n");
    }

    @TempDir
    Path tempDir;

    @TempDir
    Path projectDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void trickySourcesSurviveTheCheckWhenParsedAsAProject() throws IOException {
        write(projectDir.resolve("go.mod"), "module example.com/p\n\ngo 1.22\n");
        for (Map.Entry<String, String> source : TRICKY_SOURCES.entrySet()) {
            write(projectDir.resolve(source.getKey()), source.getValue());
        }

        List<SourceFile> sources = GoRewriteRpc.getOrStart()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(toList());

        assertNoParseErrors(sources);
        assertThat(sources).filteredOn(s -> s instanceof Go.CompilationUnit).hasSize(TRICKY_SOURCES.size());
    }

    @Test
    void trickySourcesSurviveTheCheckWhenParsedAsFiles() throws IOException {
        List<Parser.Input> inputs = TRICKY_SOURCES.entrySet().stream()
                .map(source -> {
                    Path path = projectDir.resolve(source.getKey());
                    try {
                        write(path, source.getValue());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return Parser.Input.fromFile(path);
                })
                .collect(toList());

        List<SourceFile> sources = GolangParser.builder().build()
                .parseInputs(inputs, projectDir, new InMemoryExecutionContext())
                .collect(toList());

        assertNoParseErrors(sources);
    }

    @Test
    void unparseableFileIsReportedRatherThanDropped() throws IOException {
        write(projectDir.resolve("go.mod"), "module example.com/p\n\ngo 1.22\n");
        write(projectDir.resolve("broken.go"), "package p\n\nfunc Broken( {\n");

        List<SourceFile> sources = GoRewriteRpc.getOrStart()
                .parseProject(projectDir, new InMemoryExecutionContext())
                .collect(toList());

        assertThat(sources)
                .filteredOn(s -> s instanceof ParseError)
                .extracting(s -> s.getSourcePath().toString())
                .containsExactly("broken.go");
    }

    private static void assertNoParseErrors(List<SourceFile> sources) {
        List<ParseError> parseErrors = sources.stream()
                .filter(s -> s instanceof ParseError)
                .map(s -> (ParseError) s)
                .collect(toList());
        assertThat(parseErrors)
                .as("expected zero parse errors; got:\n%s", parseErrors.stream()
                        .map(pe -> pe.getSourcePath() + ": " + pe.getMarkers()
                                .findFirst(ParseExceptionResult.class)
                                .map(ParseExceptionResult::getMessage)
                                .orElse("(no message)"))
                        .collect(joining("\n")))
                .isEmpty();
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
