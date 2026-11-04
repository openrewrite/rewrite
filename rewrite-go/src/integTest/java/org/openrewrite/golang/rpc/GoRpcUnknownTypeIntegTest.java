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
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code JavaType.Unknown} is a {@link JavaType.FullyQualified} in Java, so it
 * reaches the peer inside a class list — a supertype's interfaces, a type's
 * annotations — wherever attribution gave up, and must keep its place there.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoRpcUnknownTypeIntegTest {

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

    private static final String SOURCE =
      "package main\n" +
      "\n" +
      "func f(x int) int {\n" +
      "\treturn x\n" +
      "}\n";

    private static final String RENAMED =
      "package main\n" +
      "\n" +
      "func f(flag int) int {\n" +
      "\treturn flag\n" +
      "}\n";

    private void assertRoundTrips(JavaType identifierType) {
        SourceFile cu = GolangParser.builder().build().parse(SOURCE).findFirst().orElseThrow();
        SourceFile mutated = (SourceFile) new GolangVisitor<ExecutionContext>() {
            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                return "x".equals(identifier.getSimpleName()) ?
                  identifier.withType(identifierType) : identifier;
            }
        }.visitNonNull(cu, new InMemoryExecutionContext());

        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        rpc.reset();
        // A Go-native recipe sends the visited tree back, the leg that reads
        // the list the receive side built.
        Tree visited = rpc.prepareRecipe("org.openrewrite.golang.test.RenameXToFlag")
          .getVisitor().visit(mutated, new InMemoryExecutionContext());
        assertThat(visited).isNotNull();
        assertThat(rpc.print((SourceFile) visited)).isEqualTo(RENAMED);
    }

    @Test
    void unknownAmongInterfaces() {
        JavaType.Class type = (JavaType.Class) JavaType.ShallowClass.build("example.Foo");
        assertRoundTrips(type.withInterfaces(singletonList(
          (JavaType.FullyQualified) JavaType.Unknown.getInstance())));
    }

    @Test
    void unknownAmongAnnotations() {
        JavaType.Class type = (JavaType.Class) JavaType.ShallowClass.build("example.Foo");
        assertRoundTrips(type.withAnnotations(singletonList(
          (JavaType.FullyQualified) JavaType.Unknown.getInstance())));
    }
}
