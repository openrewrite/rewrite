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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.expectMethodType;
import static org.openrewrite.golang.Assertions.go;

/**
 * Regression test for method-type attribution when the RPC binary is built
 * with {@code -trimpath} (as the Moderne CLI ships it). {@code -trimpath}
 * strips the binary's baked-in GOROOT, so the parser's go/types importer
 * can't resolve the stdlib and every {@code J.MethodInvocation} loses its
 * {@code JavaType.Method}. {@link GoRewriteRpc} recovers GOROOT from the host
 * toolchain and passes it to the subprocess; this verifies attribution
 * survives.
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class GoRootTrimpathAttributionIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() throws Exception {
        Path trimpathBinary = tempDir.resolve("rewrite-go-rpc-trimpath");
        Process build = new ProcessBuilder("go", "build", "-trimpath",
                "-o", trimpathBinary.toAbsolutePath().toString(), "./cmd/rpc")
                .directory(new File(".").getAbsoluteFile())
                .redirectErrorStream(true)
                .start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(build.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                out.append(line).append('\n');
            }
        }
        assertThat(build.waitFor(150, TimeUnit.SECONDS)).as("go build -trimpath finished").isTrue();
        assertThat(build.exitValue()).as("go build -trimpath succeeded:\n" + out).isZero();

        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(trimpathBinary)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
                .allowNonWhitespaceInWhitespace(true)
                .build());
    }

    @Test
    void methodTypesResolveDespiteTrimpath() {
        rewriteRun(
                go(
                        """
                                package main

                                import "net/http"

                                func fetch() {
                                \thttp.Get("http://example.com/api")
                                }

                                func doIt(c *http.Client, req *http.Request) {
                                \tc.Do(req)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            expectMethodType(cu, "Get", "net/http");
                            expectMethodType(cu, "Do", "net/http.Client");
                        })
                )
        );
    }
}
