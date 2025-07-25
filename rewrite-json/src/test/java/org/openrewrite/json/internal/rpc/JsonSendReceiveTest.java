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
package org.openrewrite.json.internal.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;

import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JsonSendReceiveTest implements RewriteTest {
    RewriteRpc server;
    RewriteRpc client;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        Environment env = Environment.builder().build();

        server = RewriteRpc.from(new JsonRpc(new TraceMessageHandler("server",
          new HeaderDelimitedMessageHandler(serverIn, serverOut))), env)
          .timeout(Duration.ofSeconds(10))
          .build()
          .batchSize(1);

        client = RewriteRpc.from(new JsonRpc(new TraceMessageHandler("client",
          new HeaderDelimitedMessageHandler(clientIn, clientOut))), env)
          .timeout(Duration.ofSeconds(10))
          .build()
          .batchSize(1);
    }

    @AfterEach
    void after() {
        server.close();
        client.close();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                Tree t = server.visit((SourceFile) tree, ChangeValue.class.getName(), 0);
                stopAfterPreVisit();
                return t;
            }
        }));
    }

    @DocumentExample
    @Test
    void sendReceiveIdempotence() {
        rewriteRun(
          //language=json
          json(
            """
              {
                "key": "value",
                "array": [1, 2, 3]
              }
              """,
            """
              {
                "key": "changed",
                "array": [1, 2, 3]
              }
              """
          )
        );
    }

    static class ChangeValue extends JsonVisitor<Integer> {
        @Override
        public Json visitLiteral(Json.Literal literal, Integer p) {
            if (literal.getValue().equals("value")) {
                return literal.withValue("changed").withSource("\"changed\"");
            }
            return literal;
        }
    }
}
