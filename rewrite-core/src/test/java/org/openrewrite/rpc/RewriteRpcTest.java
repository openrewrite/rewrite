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

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.*;
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class RewriteRpcTest implements RewriteTest {
    Environment env = Environment.builder()
      .scanRuntimeClasspath("org.openrewrite.text")
      .build();

    RecipeMarketplace marketplace;
    RewriteRpc client;
    RewriteRpc server;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        marketplace = env.toMarketplace(runtimeClasspath());

        JsonMessageFormatter clientFormatter = new JsonMessageFormatter(new ParameterNamesModule());
        JsonMessageFormatter serverFormatter = new JsonMessageFormatter(new ParameterNamesModule());

        client = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(clientFormatter, clientIn, clientOut)), marketplace)
          .batchSize(1);

        server = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(serverFormatter, serverIn, serverOut)), marketplace, List.of(new TestRecipeBundleResolver()))
          .batchSize(1);
    }

    @AfterEach
    void after() {
        client.shutdown();
        server.shutdown();
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

    @Disabled("Print requires bidirectional RPC (GetObject callback) which deadlocks in the in-process test setup. " +
              "Works correctly when calling to a real subprocess (e.g., Java to Python/JS).")
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
    void getMarketplace() {
        assertThat(client.getMarketplace(new RecipeBundle("runtime",
          "", null, null, null)).getAllRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        Recipe recipe = client.prepareRecipe("org.openrewrite.text.Find",
          Map.of("find", "hello"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Find text");
    }

    @Test
    void prepareRecipeFallsBackToRecipeLoaderWhenResolverMissing() {
        // Install a listing for a recipe that exists on the classpath, but with a
        // "maven" ecosystem bundle. The server only has a "runtime" resolver, so
        // listing.prepare() will fail. The fix falls through to RecipeLoader which
        // finds the class directly.
        RecipeListing mavenListing = new RecipeListing(
          null,
          "org.openrewrite.text.ChangeText",
          "Change text",
          "Change the contents of text files.",
          null,
          List.of(new OptionDescriptor("toText", "java.lang.String",
            "To text", "The text to change to.", null, null, true, null)),
          List.of(),
          1,
          new RecipeBundle("maven", "org.openrewrite:rewrite-core", null, "1.0.0", null)
        );
        marketplace.install(mavenListing, List.of());

        Recipe recipe = client.prepareRecipe("org.openrewrite.text.ChangeText",
          Map.of("toText", "hello"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Change text");
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

        Cursor clientC2 = server.getCursor(client.getCursorIds(c2), null);
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

    /**
     * A trivial resolver for testing that returns the existing marketplace without
     * requiring any actual dependency resolution.
     */
    class TestRecipeBundleResolver implements RecipeBundleResolver {
        @Override
        public String getEcosystem() {
            return "runtime";
        }

        @Override
        public RecipeBundleReader resolve(RecipeBundle bundle) {
            return new RecipeBundleReader() {
                @Override
                public RecipeBundle getBundle() {
                    return bundle;
                }

                @Override
                public RecipeMarketplace read() {
                    return marketplace;
                }

                @Override
                public RecipeDescriptor describe(RecipeListing listing) {
                    return env.activateRecipes(listing.getName()).getDescriptor();
                }

                @Override
                public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
                    // Use RecipeLoader to instantiate directly, avoiding recursive marketplace lookup
                    return new RecipeLoader(null).load(listing.getName(), options);
                }
            };
        }
    }
}
