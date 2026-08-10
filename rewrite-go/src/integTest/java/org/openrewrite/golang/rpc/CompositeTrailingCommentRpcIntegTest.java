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
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.marker.TrailingComma;
import org.openrewrite.golang.tree.Go;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A composite literal that ends with a trailing comma carries a {@link TrailingComma} marker whose
 * {@code after} space is the gap between that comma and the closing brace, holding any trailing line
 * comment. The marker's RPC codec carries both its {@code before} and {@code after} spaces with their
 * comments. {@code reset()} clears both caches so {@code Print} deserializes the whole tree from the
 * wire rather than reusing cached nodes.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CompositeTrailingCommentRpcIntegTest {

    @TempDir
    Path tempDir;

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

    private void roundTrip(String source) {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();
        assertThat(cu).isInstanceOf(Go.CompilationUnit.class);
        rpc.reset();
        assertThat(rpc.print(cu)).isEqualTo(source);
    }

    @Test
    void newlineBeforeClosingBraceNoComment() {
        roundTrip("""
                package main

                var x = &T{
                \tA: 1,
                }
                """);
    }

    @Test
    void commentAfterTrailingComma() {
        roundTrip("""
                package main

                var x = &T{
                \tA: 1, // trailing comment
                }
                """);
    }

    @Test
    void commentBeforeTrailingComma() {
        roundTrip("""
                package main

                var x = []int{
                \t1 /* before comma */,
                }
                """);
    }
}
