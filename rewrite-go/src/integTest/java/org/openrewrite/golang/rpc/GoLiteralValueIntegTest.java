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
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A recipe reads a literal's value, never its source text, so a Go numeric constant has to arrive here as a Java
 * value of the kind and the width the source wrote.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoLiteralValueIntegTest {

    @TempDir
    Path tempDir;

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
    void numericConstantsKeepTheirValueType() {
        List<Object> values = literalValues(
                """
                        package main

                        const (
                        	one       = 1
                        	maxInt64  = 9223372036854775807
                        	wider     = 300000000000000000000
                        	hugeFloat = 3e20
                        	fraction  = 1.5
                        )
                        """);

        assertThat(values).containsExactly(
                1,
                9223372036854775807L,
                new BigInteger("300000000000000000000"),
                3e20,
                1.5);
    }

    @Test
    void stringAndCharacterConstantsAreUnaffected() {
        List<Object> values = literalValues(
                """
                        package main

                        const (
                        	greeting = "hi"
                        	letter   = 'a'
                        )
                        """);

        assertThat(values).containsExactly("hi", (int) 'a');
    }

    private List<Object> literalValues(String source) {
        SourceFile cu = GolangParser.builder().build().parse(source).findFirst().orElseThrow();
        assertThat(cu).as("parse must yield a Go.CompilationUnit, not a ParseError: %s", cu)
                .isInstanceOf(org.openrewrite.golang.tree.Go.CompilationUnit.class);

        List<Object> values = new ArrayList<>();
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                values.add(literal.getValue());
                return super.visitLiteral(literal, ctx);
            }
        }.visit(cu, new InMemoryExecutionContext());
        return values;
    }
}
