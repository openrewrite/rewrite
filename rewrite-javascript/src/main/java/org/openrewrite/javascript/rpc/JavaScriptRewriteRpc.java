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
package org.openrewrite.javascript.rpc;

import io.moderne.jsonrpc.JsonRpc;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class JavaScriptRewriteRpc extends RewriteRpc {
    private static final RewriteRpcProcessManager<JavaScriptRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    JavaScriptRewriteRpc(JsonRpc jsonRpc, Environment marketplace) {
        super(jsonRpc, marketplace);
    }

    public static JavaScriptRewriteRpc getOrStart() {
        return MANAGER.getOrStart();
    }

    public static void setFactory(Builder builder) {
        MANAGER.setFactory(builder);
    }

    public static void shutdownCurrent() {
        MANAGER.shutdown();
    }

    public int installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }

    public int installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    public int installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }

    public static Builder builder() {
        return new Builder();
    }

    @RequiredArgsConstructor
    public static class Builder implements Supplier<JavaScriptRewriteRpc> {
        private Environment marketplace = Environment.builder().build();
        private Path npxPath = Paths.get("npx");
        private @Nullable Path log;
        private @Nullable Path recipeInstallDir;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean verboseLogging;
        private @Nullable Integer inspectBrk;

        public Builder marketplace(Environment marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder recipeInstallDir(@Nullable Path recipeInstallDir) {
            this.recipeInstallDir = recipeInstallDir;
            return this;
        }

        /**
         * Path to the `npx` executable, not just the directory it is installed in.
         *
         * @param npxPath The path to the `npx` executable.
         * @return This builder
         */
        public Builder npxPath(Path npxPath) {
            if (Files.notExists(npxPath) || Files.isDirectory(npxPath)) {
                throw new IllegalArgumentException("Invalid npx executable " + npxPath.toAbsolutePath().normalize());
            }
            this.npxPath = npxPath;
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

        /**
         * Enables info and debug level logging.
         *
         * @return This builder.
         */
        public Builder verboseLogging(boolean verboseLogging) {
            this.verboseLogging = verboseLogging;
            return this;
        }

        public Builder verboseLogging() {
            return verboseLogging(true);
        }

        /**
         * Set the port for the Node.js inspector to listen on. When this is set, you can use
         * an "Attach to Node.js/Chrome" run configuration in IDEA to debug the JavaScript Rewrite RPC process.
         * The Rewrite RPC process will block waiting for this connection.
         *
         * @param inspectBrk The port for the Node.js inspector to listen on.
         * @return This builder
         */
        public Builder inspectBrk(@Nullable Integer inspectBrk) {
            this.inspectBrk = inspectBrk;
            return this;
        }

        public Builder inspectBrk() {
            return inspectBrk(9229);
        }

        @Override
        public JavaScriptRewriteRpc get() {
            Stream<@Nullable String> cmd;

            if (inspectBrk != null) {
                // Find the server.js file - check local development path first, then installed package
                Path serverJs = Paths.get("rewrite/dist/rpc/server.js");
                if (!Files.exists(serverJs)) {
                    serverJs = Paths.get("node_modules/@openrewrite/rewrite/dist/rpc/server.js");
                }

                // We have to use node directly here because npx spawns a child node process. The
                // IDE's debug configuration would connect to the npx process rather than the spawned
                // node process and breakpoints don't get hit.
                cmd = Stream.of(
                        "node",
                        "--enable-source-maps",
                        "--inspect-brk=" + inspectBrk,
                        serverJs.toAbsolutePath().normalize().toString(),
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        verboseLogging ? "--verbose" : null,
                        recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize().toString()
                );
            } else {
                String version = StringUtils.readFully(getClass().getResourceAsStream("/META-INF/version.txt"));
                cmd = Stream.of(
                        npxPath.toString(),
                        // For SNAPSHOT versions, assume npm link has been run and don't use --package
                        version.endsWith("-SNAPSHOT") ? null : "--package=@openrewrite/rewrite@" + version,
                        "rewrite-rpc",
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        verboseLogging ? "--verbose" : null,
                        recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize().toString()
                );
            }

            RewriteRpcProcess process = new RewriteRpcProcess(cmd.filter(Objects::nonNull).toArray(String[]::new));
//            process.trace();
            process.environment().put("NODE_OPTIONS", "--enable-source-maps");
            process.start();

            return (JavaScriptRewriteRpc) new JavaScriptRewriteRpc(process.getRpcClient(), marketplace)
                    .livenessCheck(process::getLivenessCheck)
                    .timeout(timeout);
        }
    }
}
