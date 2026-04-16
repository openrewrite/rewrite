/*
 * Copyright 2025 the original author or authors.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * RPC client that communicates with a Go process for parsing and printing Go source code.
 */
@Getter
public class GoRewriteRpc extends RewriteRpc {

    private static final RewriteRpcProcessManager<GoRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    private final String command;
    private final RewriteRpcProcess process;

    GoRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers, String command) {
        super(process.getRpcClient(), marketplace, resolvers);
        this.command = command;
        this.process = process;
    }

    public static @Nullable GoRewriteRpc get() {
        return MANAGER.get();
    }

    public static GoRewriteRpc getOrStart() {
        return MANAGER.getOrStart();
    }

    public static void setFactory(Builder builder) {
        MANAGER.setFactory(builder);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        process.shutdown();
    }

    public static void shutdownCurrent() {
        MANAGER.shutdown();
    }

    /**
     * Install recipes from a local file path (e.g., a local Go module).
     *
     * @param recipes Path to the local package directory
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes.getAbsoluteFile().toPath()),
                InstallRecipesResponse.class
        );
    }

    /**
     * Install recipes from a package name.
     *
     * @param packageName The package name to install (e.g., a Git repository URL)
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    /**
     * Install recipes from a package name with a specific version.
     *
     * @param packageName The package name to install (e.g., a Git repository URL)
     * @param version     Optional version specification
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements Supplier<GoRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private List<RecipeBundleResolver> resolvers = Collections.emptyList();
        private @Nullable Path goBinaryPath;
        private Duration timeout = Duration.ofSeconds(60);
        private @Nullable Path log;
        private boolean traceRpcMessages;

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder resolvers(List<RecipeBundleResolver> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        public Builder goBinaryPath(Path path) {
            this.goBinaryPath = path;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder log(@Nullable Path log) {
            this.log = log;
            return this;
        }

        public Builder traceRpcMessages() {
            this.traceRpcMessages = true;
            return this;
        }

        @Override
        public GoRewriteRpc get() {
            String binaryPath;
            if (goBinaryPath != null) {
                binaryPath = goBinaryPath.toString();
            } else {
                // Check for a custom binary with installed recipes
                java.nio.file.Path customBin = java.nio.file.Paths.get(
                        System.getProperty("user.home"), ".rewrite", "go-recipes", "rewrite-go-rpc");
                if (java.nio.file.Files.isExecutable(customBin)) {
                    binaryPath = customBin.toString();
                } else {
                    binaryPath = "rewrite-go-rpc";
                }
            }

            Stream<@Nullable String> cmd = Stream.of(
                    binaryPath,
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );

            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);
            process.start();

            try {
                return (GoRewriteRpc) new GoRewriteRpc(process, marketplace, resolvers, String.join(" ", cmdArr))
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
