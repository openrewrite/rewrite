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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the visitor dispatch gap on {@code *tree.Else}.
 * Without the dispatcher entry, {@code JavaSender.VisitElse} never runs —
 * the synthetic Else wrapper that maps Go's {@code if/else} onto Java's
 * {@code J.If.Else} was sent through preVisit only, leaving the receive
 * queue desynchronized for the whole parse and surfacing as
 * {@code ClassCastException: Space cannot be cast to JRightPadded} or
 * {@code Expected CHANGE with positions in receiveList}.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ParseProjectIfElseTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path projectDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void ifElseInProjectFile() throws Exception {
        write(projectDir.resolve("go.mod"), """
                module example.com/foo

                go 1.22
                """);
        write(projectDir.resolve("main.go"), """
                package main

                func abs(x int) int {
                \tif x < 0 {
                \t\treturn -x
                \t} else {
                \t\treturn x
                \t}
                }
                """);

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        List<SourceFile> sources = rpc.parseProject(projectDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        List<ParseError> parseErrors = sources.stream()
                .filter(s -> s instanceof ParseError)
                .map(s -> (ParseError) s)
                .collect(Collectors.toList());

        assertThat(parseErrors)
                .as("expected zero parse errors; got: %s",
                        parseErrors.stream()
                                .map(pe -> pe.getSourcePath() + ": " + pe.getMarkers().findFirst(ParseExceptionResult.class)
                                        .map(ParseExceptionResult::getMessage).orElse("(no message)"))
                                .collect(Collectors.joining("\n")))
                .isEmpty();
    }

    private static void write(Path path, String content) throws java.io.IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
