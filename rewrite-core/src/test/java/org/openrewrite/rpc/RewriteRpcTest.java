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
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    RewriteRpc client;
    RewriteRpc server;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        client = RewriteRpc.from(new JsonRpc(new HeaderDelimitedMessageHandler(clientIn, clientOut)), env)
          .timeout(Duration.ofMinutes(10))
          .build()
          .batchSize(1);

        server = RewriteRpc.from(new JsonRpc(new HeaderDelimitedMessageHandler(serverIn, serverOut)), env)
          .timeout(Duration.ofMinutes(10))
          .build()
          .batchSize(1);
    }

    @AfterEach
    void after() {
        client.close();
        server.close();
    }

    @DocumentExample
    @Test
    void sendReceiveIdempotence() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              @SneakyThrows
              public Tree preVisit(Tree tree, ExecutionContext ctx) {
                  Tree t = client.visit((SourceFile) tree, ChangeText.class.getName(), 0);
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
              assertThat(client.print(text)).isEqualTo("Hello Jon!"))
          )
        );
    }

    @Test
    void getRecipes() {
        assertThat(client.getRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        Recipe recipe = client.prepareRecipe("org.openrewrite.text.Find",
          Map.of("find", "hello"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Find text");
    }

    @Disabled("Disabled until https://github.com/openrewrite/rewrite/pull/5260 is complete")
    @Test
    void runRecipe() {
        CountDownLatch latch = new CountDownLatch(1);
        rewriteRun(
          spec -> spec
            .recipe(client.prepareRecipe("org.openrewrite.text.Find",
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
            .recipe(client.prepareRecipe("org.openrewrite.text.CreateTextFile",
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
            .recipe(client.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$RecipeWithRecipeList", Map.of()))
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

        Cursor clientC2 = server.getCursor(client.getCursorIds(c2));
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
