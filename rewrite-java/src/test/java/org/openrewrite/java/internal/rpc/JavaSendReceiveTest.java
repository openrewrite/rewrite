/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.internal.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaSendReceiveTest implements RewriteTest {
    RewriteRpc server;
    RewriteRpc client;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        Environment env = Environment.builder().build();

        server = new RewriteRpc(new JsonRpc(new TraceMessageHandler("server", new HeaderDelimitedMessageHandler(serverIn, serverOut))), env)
          .batchSize(1);
        client = new RewriteRpc(new JsonRpc(new TraceMessageHandler("client", new HeaderDelimitedMessageHandler(clientIn, clientOut))), env)
          .batchSize(1);
    }

    @AfterEach
    void after() {
        server.shutdown();
        client.shutdown();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                Tree t = server.visit((SourceFile) tree, ChangeValue.class.getName(), 0);
                stopAfterPreVisit();
                return requireNonNull(t);
            }
        }));
    }

    @Disabled("Disabled until we've cleaned up the enum serialization")
    @DocumentExample
    @Test
    void sendReceiveIdempotence() {
        rewriteRun(
          java(
            """
              class Foo {
                  String s = "value";
              }
              """,
            """
              class Foo {
                  String s = "changed";
              }
              """
          )
        );
    }

    static class ChangeValue extends JavaVisitor<Integer> {
        @Override
        public J visitLiteral(J.Literal literal, Integer p) {
            if ("value".equals(literal.getValue())) {
                return literal.withValue("changed").withValueSource("\"changed\"");
            }
            return literal;
        }
    }
}
