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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.*;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;
import static org.openrewrite.test.SourceSpecs.text;

/**
 * Tests parallel execution of RewriteRpc recipe runs. Each test method gets its
 * own RPC client/server pair (matching the ThreadLocal-per-thread model used in
 * real RPC-based parsers like JavaScript and Python). JUnit runs the test methods
 * concurrently, mirroring CI parallel test execution.
 * <p>
 * With the in-process piped stream setup, all tests pass since there is no shared
 * filesystem (npx cache). When run against real subprocess-based RPC (e.g.,
 * JavaScript), concurrent npx invocations can race on the shared
 * {@code ~/.npm/_npx/} cache directory, causing TAR_ENTRY_ERROR or
 * "rewrite-rpc: not found" errors.
 */
@Execution(ExecutionMode.CONCURRENT)
class ParallelRewriteRpcTest implements RewriteTest {

    private final Environment env = Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.text")
            .build();

    private RecipeMarketplace marketplace;
    private RewriteRpc client;
    private RewriteRpc server;

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

    @Test
    void changeTextA() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "changed-A")))
                        .validateRecipeSerialization(false),
                text("original", "changed-A")
        );
    }

    @Test
    void changeTextB() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "changed-B")))
                        .validateRecipeSerialization(false),
                text("hello", "changed-B")
        );
    }

    @Test
    void changeTextC() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "changed-C")))
                        .validateRecipeSerialization(false),
                text("world", "changed-C")
        );
    }

    @Test
    void changeTextD() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "changed-D")))
                        .validateRecipeSerialization(false),
                text("foo", "changed-D")
        );
    }

    @Test
    void scanningRecipeThatGenerates() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.CreateTextFile",
                                Map.of("fileContents", "generated", "relativeFileName", "gen.txt")))
                        .validateRecipeSerialization(false),
                text(null, "generated", spec -> spec.path("gen.txt"))
        );
    }

    @Test
    void changeTextWithMultipleSources() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "multi")))
                        .validateRecipeSerialization(false),
                text("one", "multi"),
                text("two", "multi"),
                text("three", "multi")
        );
    }

    @Test
    void recipeWithRecipeList() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.rpc.RewriteRpcTest$RecipeWithRecipeList", Map.of()))
                        .validateRecipeSerialization(false),
                text("hi", "hello")
        );
    }

    @Test
    void changeTextE() {
        rewriteRun(
                spec -> spec
                        .recipe(client.prepareRecipe("org.openrewrite.text.ChangeText",
                                Map.of("toText", "changed-E")))
                        .validateRecipeSerialization(false),
                text("bar", "changed-E")
        );
    }

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
                public org.openrewrite.config.RecipeDescriptor describe(RecipeListing listing) {
                    return env.activateRecipes(listing.getName()).getDescriptor();
                }

                @Override
                public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
                    return new RecipeLoader(null).load(listing.getName(), options);
                }
            };
        }
    }
}
