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
package org.openrewrite.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class RewriteRpcTest implements RewriteTest {
    Environment env = Environment.builder()
      .scanRuntimeClasspath("org.openrewrite.text")
      .build();

    RewriteRpc server;
    RewriteRpc client;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        JsonRpc serverJsonRpc = new JsonRpc(new TraceMessageHandler("server",
          new HeaderDelimitedMessageHandler(serverIn, serverOut)));
        server = new RewriteRpc(serverJsonRpc, env).batchSize(1).timeout(Duration.ofMinutes(10));

        JsonRpc clientJsonRpc = new JsonRpc(new TraceMessageHandler("client",
          new HeaderDelimitedMessageHandler(clientIn, clientOut)));
        client = new RewriteRpc(clientJsonRpc, env).batchSize(1).timeout(Duration.ofMinutes(10));
    }

    @AfterEach
    void after() {
        server.shutdown();
        client.shutdown();
    }

    @Test
    void sendReceiveExecutionContext() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage("key", "value");

        client.localObjects.put("123", ctx);
        InMemoryExecutionContext received = server.getObject("123");
        assertThat(received.<String>getMessage("key")).isEqualTo("value");
    }

    @DocumentExample
    @Test
    void sendReceiveIdempotence() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @SneakyThrows
              @Override
              public Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                  Tree t = server.visit((SourceFile) tree, ChangeText.class.getName(), 0);
                  stopAfterPreVisit();
                  return requireNonNull(t);
              }
          })),
          text(
            "Hello Jon!",
            "Hello World!"
          )
        );
    }

    @Test
    void print() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text ->
              assertThat(server.print(text)).isEqualTo("Hello Jon!"))
          )
        );
    }

    @Test
    void getRecipes() {
        assertThat(server.getRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        Recipe recipe = server.prepareRecipe("org.openrewrite.text.Find",
          Map.of("find", "hello"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Find text");
    }

    @Test
    void runRecipe() {
        CountDownLatch latch = new CountDownLatch(1);
        rewriteRun(
          spec -> spec
            .recipe(server.prepareRecipe("org.openrewrite.text.Find",
              Map.of("find", "hello")))
            .validateRecipeSerialization(false)
            .dataTable(TextMatches.Row.class, rows -> {
                assertThat(rows).contains(new TextMatches.Row(
                  "hello.txt", "~~>Hello Jon!"));
                latch.countDown();
            }),
          text(
            "Hello Jon!",
            "~~>Hello Jon!",
            spec -> spec.path("hello.txt")
          )
        );

        assertThat(latch.getCount()).isEqualTo(0);
    }

    @Test
    void runScanningRecipeThatGenerates() {
        rewriteRun(
          spec -> spec
            .recipe(server.prepareRecipe("org.openrewrite.text.CreateTextFile",
              Map.of("fileContents", "hello", "relativeFileName", "hello.txt")))
            .validateRecipeSerialization(false),
          text(
            null,
            "hello",
            spec -> spec.path("hello.txt")
          )
        );
    }

    @Test
    void runRecipeWithRecipeList() {
        rewriteRun(
          spec -> spec
            .recipe(server.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$RecipeWithRecipeList", Map.of()))
            .validateRecipeSerialization(false),
          text(
            "hi",
            "hello"
          )
        );
    }

    @Test
    void getCursor() {
        Cursor parent = new Cursor(null, Cursor.ROOT_VALUE);
        Cursor c1 = new Cursor(parent, 0);
        Cursor c2 = new Cursor(c1, 1);

        Cursor clientC2 = client.getCursor(server.getCursorIds(c2));
        assertThat(clientC2.<Integer>getValue()).isEqualTo(1);
        assertThat(clientC2.getParentOrThrow().<Integer>getValue()).isEqualTo(0);
        assertThat(clientC2.getParentOrThrow(2).<String>getValue()).isEqualTo(Cursor.ROOT_VALUE);
    }

    static class ChangeText extends PlainTextVisitor<Integer> {
        @Override
        public PlainText visitText(PlainText text, Integer p) {
            return text.withText("Hello World!");
        }
    }

    @SuppressWarnings("unused")
    static class RecipeWithRecipeList extends Recipe {
        @Override
        public String getDisplayName() {
            return "A recipe that has a recipe list";
        }

        @Override
        public String getDescription() {
            return "To verify that it is possible for a recipe list to be called over RPC.";
        }

        @Override
        public void buildRecipeList(RecipeList recipes) {
            recipes.recipe(new org.openrewrite.text.ChangeText("hello"));
        }
    }
}
