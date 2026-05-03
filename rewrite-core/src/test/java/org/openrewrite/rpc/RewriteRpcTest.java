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
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.Markers;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.*;
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        var serverOut = new PipedOutputStream();
        var clientOut = new PipedOutputStream();
        var serverIn = new PipedInputStream(clientOut);
        var clientIn = new PipedInputStream(serverOut);

        marketplace = env.toMarketplace(runtimeClasspath());

        var clientFormatter = new JsonMessageFormatter(new ParameterNamesModule());
        var serverFormatter = new JsonMessageFormatter(new ParameterNamesModule());

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

    /**
     * Verifies that getObject() uses remoteObjects (last synced state) as the
     * diff baseline, not localObjects. When the local side modifies a tree
     * (e.g., via a local recipe) before calling getObject(), the localObjects
     * entry diverges from what the remote used as its baseline. Using
     * localObjects would cause NO_CHANGE fields to retain the local
     * modification rather than the synced value.
     * <p>
     * The bug only manifests for object fields (identity-compared via ==)
     * where the sender emits NO_CHANGE. Here, the server changes only text
     * (a value field), so Markers — an object field preserved by reference
     * in withText() — is sent as NO_CHANGE. If the client locally created
     * a different Markers object, the wrong baseline would retain it.
     */
    @Test
    void getObjectUsesRemoteObjectsAsBaseline() {
        PlainText original = PlainText.builder()
          .sourcePath(Path.of("test.txt"))
          .text("Hello")
          .build();

        String id = original.getId().toString();
        String sourceFileType = PlainText.class.getName();

        // Server has the tree; client fetches it → both synced
        server.localObjects.put(id, original);
        PlainText synced = client.getObject(id, sourceFileType);
        UUID syncedMarkersId = synced.getMarkers().getId();

        // Server modifies text only — Markers reference stays the same
        // (original.withText() preserves the Markers by reference via @With),
        // so the handler sends NO_CHANGE for the Markers field.
        // Using original (not synced) ensures server's before == original.getMarkers()
        // and after == original.withText(...).getMarkers() are the same reference.
        server.localObjects.put(id, original.withText("Hello World"));

        // Client locally modifies Markers (simulating a local recipe step),
        // creating a new Markers object with a different ID
        Markers localMarkers = synced.getMarkers().withId(Tree.randomId());
        client.localObjects.put(id, synced.withMarkers(localMarkers));

        // Fetch: server sends Markers=NO_CHANGE, text=CHANGE("Hello World").
        // The receiver should apply NO_CHANGE against remoteObjects (synced
        // markers), not localObjects (local markers).
        PlainText result = client.getObject(id, sourceFileType);
        assertThat(result.getText()).isEqualTo("Hello World");
        assertThat(result.getMarkers().getId())
          .describedAs("Markers should come from remoteObjects baseline, not localObjects")
          .isEqualTo(syncedMarkersId);
    }

    /**
     * Verifies that when getObject() fails mid-serialization on the sender side,
     * the sender removes the stale entry from remoteObjects. This ensures that
     * a subsequent getObject() for the same ID sends a full ADD (not a CHANGE
     * delta against a partially-sent, stale baseline).
     * <p>
     * Without the fix, the sender would keep the stale remoteObjects entry and
     * attempt a CHANGE diff on retry, causing cascading desync errors.
     */
    @Test
    void sendFailureCleansUpRemoteObjects() {
        PlainText original = PlainText.builder()
          .sourcePath(Path.of("test.txt"))
          .text("Hello")
          .build();

        String id = original.getId().toString();
        String sourceFileType = PlainText.class.getName();

        // Step 1: successful sync — both sides establish remoteObjects baseline
        server.localObjects.put(id, original);
        PlainText synced = client.getObject(id, sourceFileType);
        assertThat(synced.getText()).isEqualTo("Hello");
        assertThat(server.remoteObjects).containsKey(id);

        // Step 2: replace with a PlainText that has null sourcePath, causing
        // NPE in PlainTextRpcCodec.rpcSend() at d.getSourcePath().toString()
        PlainText badTree = PlainText.builder()
          .id(original.getId())
          .text("Bad")
          .build(); // no sourcePath → null → NPE during send
        server.localObjects.put(id, badTree);

        // Step 3: client.getObject() should fail because the sender NPEs mid-serialization
        try {
            client.getObject(id, sourceFileType);
        } catch (Exception expected) {
            // Expected — sender failed and emitted premature END_OF_OBJECT
        }

        // Step 4: verify the sender cleaned up its stale remoteObjects entry
        // and rolled back any refs assigned during the failed exchange
        assertThat(server.remoteObjects)
          .describedAs("Sender should remove stale remoteObjects entry after send failure")
          .doesNotContainKey(id);
        int refsAfterFailure = server.localRefs.size();
        assertThat(refsAfterFailure)
          .describedAs("Sender should roll back localRefs assigned during failed exchange")
          .isEqualTo(0);

        // Step 5: put back a valid tree and retry — should succeed via full ADD
        PlainText fixed = original.withText("Fixed");
        server.localObjects.put(id, fixed);
        PlainText result = client.getObject(id, sourceFileType);
        assertThat(result.getText()).isEqualTo("Fixed");
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

    @Disabled("Disabled until https://github.com/openrewrite/rewrite/pull/5260 is complete")
    @Test
    void runRecipe() {
        var latch = new CountDownLatch(1);
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

    /**
     * When a composite recipe has consecutive sub-recipes that are all RpcRecipes
     * bound to the same RewriteRpc instance, the scheduler batches them into a
     * single BatchVisit RPC call. The remote runs all visitors in sequence and
     * the host fetches the final result at the end. This test verifies that
     * two consecutive same-RPC ChangeText recipes produce the correct final output.
     */
    @Test
    void consecutiveSameRpcRecipesAreBatchedAndProduceCorrectResult() {
        Recipe r1 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "step1"));
        Recipe r2 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "step2"));

        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(r1, r2)))
            .validateRecipeSerialization(false)
            // Consecutive ChangeText recipes aren't idempotent (r1 re-applies in cycle 2).
            // We only need 1 cycle to verify deferral correctness.
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "hello",
            "step2"
          )
        );
    }

    /**
     * Verifies three consecutive same-RPC recipes. The scheduler should batch
     * all three into one BatchVisit and fetch only once at the end.
     */
    @Test
    void threeConsecutiveSameRpcRecipes() {
        Recipe r1 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "A"));
        Recipe r2 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "B"));
        Recipe r3 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "C"));

        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(r1, r2, r3)))
            .validateRecipeSerialization(false)
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "hello",
            "C"
          )
        );
    }

    /**
     * When a batch contains recipes where only some modify the tree,
     * only those recipes should appear in the RecipesThatMadeChanges marker.
     * Previously, all recipes in the batch were attributed regardless of
     * whether they actually modified the tree.
     */
    @Test
    void batchOnlyAttributesRecipesThatActuallyMadeChanges() {
        Recipe r1 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "A"));
        Recipe r2 = client.prepareRecipe("org.openrewrite.text.Find", Map.of("find", "NOMATCH_PATTERN"));
        Recipe r3 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "B"));

        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(r1, r2, r3)))
            .validateRecipeSerialization(false)
            .cycles(1).expectedCyclesThatMakeChanges(1),
          text(
            "hello",
            "B",
            spec -> spec.afterRecipe(result -> {
                RecipesThatMadeChanges marker = result.getMarkers()
                  .findFirst(RecipesThatMadeChanges.class)
                  .orElseThrow(() -> new AssertionError("Expected RecipesThatMadeChanges marker"));
                List<String> recipeNames = marker.getRecipes().stream()
                  .map(stack -> stack.get(stack.size() - 1).getName())
                  .toList();
                assertThat(recipeNames)
                  .describedAs("Only recipes that modified the tree should be attributed")
                  .contains("org.openrewrite.text.ChangeText")
                  .doesNotContain("org.openrewrite.text.Find");
            })
          )
        );
    }

    /**
     * When a batch of same-RPC recipes is followed by a non-RPC recipe,
     * the scheduler should flush the batch at the boundary and
     * then run the non-RPC recipe on the fetched result.
     */
    @Test
    void sameRpcBatchFollowedByNonRpcRecipe() {
        Recipe rpc1 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "from-rpc"));
        Recipe local = new org.openrewrite.text.ChangeText("from-local");

        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(rpc1, local)))
            .validateRecipeSerialization(false)
            .expectedCyclesThatMakeChanges(2),
          text(
            "hello",
            "from-local"
          )
        );
    }

    /**
     * A single RPC recipe (no consecutive same-RPC peer) should behave
     * identically to the non-batched path — no batching, immediate getObject.
     */
    @Test
    void singleRpcRecipeNoBatch() {
        Recipe r = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "only"));

        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(r)))
            .validateRecipeSerialization(false),
          text(
            "hello",
            "only"
          )
        );
    }

    /**
     * When an RPC batch visit throws an exception (e.g. a recipe visitor fails on the
     * remote side), the error must be visible: the source file should carry a Markup.Error
     * marker and the error should be recorded in the SourcesFileErrors data table.
     */
    @Test
    void batchVisitExceptionProducesErrorMarker() {
        // given
        // Two consecutive same-RPC recipes are required for the batch path to kick in
        Recipe r1 = client.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$ThrowingRpcRecipe", Map.of());
        Recipe r2 = client.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$ThrowingRpcRecipe", Map.of());

        var errors = new java.util.ArrayList<Throwable>();
        var ctx = new InMemoryExecutionContext(errors::add);
        var source = PlainText.builder().text("hello").sourcePath(Path.of("test.txt")).build();

        // when
        RecipeRun run = new RecipeScheduler().scheduleRun(
          new CompositeRecipe(List.of(r1, r2)),
          new InMemoryLargeSourceSet(List.of(source)), ctx, 1, 1);

        // then
        assertThat(errors).isNotEmpty();

        List<Result> results = run.getChangeset().getAllResults();
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getAfter().getMarkers().findFirst(Markup.Error.class))
          .describedAs("Source should carry Markup.Error so the failure is visible")
          .isPresent();
    }

    /**
     * Source files whose type the remote does not advertise via GetLanguages must
     * not be added to a BatchVisit. Without this gate the remote's receiver crashes
     * on the unknown type and the resulting exception is attached to the file as a
     * Markup.Error marker, falsely reporting the file as modified.
     */
    @Test
    void batchSkipsSourceFilesNotInRemoteLanguages() {
        // given
        // Two consecutive same-RPC recipes are required for the batch path to kick in
        Recipe r1 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "step1"));
        Recipe r2 = client.prepareRecipe("org.openrewrite.text.ChangeText", Map.of("toText", "step2"));

        // Quark is not in the hardcoded GetLanguages list, so the batch path must skip it.
        var quark = new org.openrewrite.quark.Quark(UUID.randomUUID(), Path.of("unknown.bin"), Markers.EMPTY, null, null);

        var ctx = new InMemoryExecutionContext();
        RecipeRun run = new RecipeScheduler().scheduleRun(
          new CompositeRecipe(List.of(r1, r2)),
          new InMemoryLargeSourceSet(List.of(quark)), ctx, 1, 1);

        // then
        assertThat(run.getChangeset().getAllResults())
          .describedAs("Quark of an unsupported language type should pass through untouched")
          .allSatisfy(result -> assertThat(result.getAfter().getMarkers().findFirst(Markup.Error.class))
            .describedAs("No Markup.Error should be attached for an unsupported source type")
            .isEmpty());
    }

    @Test
    void getCursor() {
        var parent = new Cursor(null, Cursor.ROOT_VALUE);
        var c1 = new Cursor(parent, 0);
        var c2 = new Cursor(c1, 1);

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
    public static class ThrowingRpcRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Throwing RPC recipe";
        }

        @Override
        public String getDescription() {
            return "Test recipe that throws during visit.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    throw new RuntimeException("boom from RPC");
                }
            };
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
