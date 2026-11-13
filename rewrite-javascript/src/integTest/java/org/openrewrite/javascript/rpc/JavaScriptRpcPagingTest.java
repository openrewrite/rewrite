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
package org.openrewrite.javascript.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.JavaScriptIsoVisitor;
import org.openrewrite.javascript.JavaScriptParser;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaScriptRpcPagingTest {

    private static final String SOURCE = source();

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder().recipeInstallDir(tempDir));
        // pages small enough that the source spans hundreds of them
        JavaScriptRewriteRpc.getOrStart().batchSize(50);
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
        JavaScriptRewriteRpc.resetFactory();
    }

    @Test
    void aTreeSpanningManyPagesTravelsBothWays() {
        SourceFile parsed = parse();
        JavaScriptRewriteRpc rpc = JavaScriptRewriteRpc.getOrStart();
        rpc.reset();

        assertThat(rpc.print(parsed)).isEqualTo(SOURCE);
    }

    @Test
    void aTransferThePeerCannotReadLeavesTheConnectionUsable() {
        SourceFile parsed = parse();
        // midway through the tree, so the peer has already asked for the page after the one it fails on
        SourceFile unreadable = (SourceFile) new JavaScriptIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                return "m10".equals(identifier.getSimpleName()) ?
                        identifier.withMarkers(identifier.getMarkers().add(new UnknownToThePeer(Tree.randomId()))) :
                        identifier;
            }
        }.visitNonNull(parsed, 0);
        JavaScriptRewriteRpc rpc = JavaScriptRewriteRpc.getOrStart();
        rpc.reset();

        assertThatThrownBy(() -> rpc.print(unreadable)).hasMessageContaining("No RPC codec registered");
        assertThat(rpc.print(parsed)).isEqualTo(SOURCE);
    }

    private static SourceFile parse() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        return JavaScriptParser.builder().build().parse(ctx, SOURCE).findFirst().orElseThrow();
    }

    private static String source() {
        StringBuilder src = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            src.append("function m").append(i).append("(a: number, b: number): number {\n")
                    .append("    const x = a + b;\n")
                    .append("    if (x > b) { return x; } else { return b; }\n")
                    .append("}\n");
        }
        return src.toString();
    }

    // The peer has no codec for this, so it cannot read past it
    static class UnknownToThePeer implements Marker, RpcCodec<UnknownToThePeer> {
        private final UUID id;

        UnknownToThePeer(UUID id) {
            this.id = id;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        @SuppressWarnings("unchecked")
        public UnknownToThePeer withId(UUID id) {
            return new UnknownToThePeer(id);
        }

        @Override
        public void rpcSend(UnknownToThePeer after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public UnknownToThePeer rpcReceive(UnknownToThePeer before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }
}
