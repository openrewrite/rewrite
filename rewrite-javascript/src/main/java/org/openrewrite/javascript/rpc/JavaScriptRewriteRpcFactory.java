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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.javascript.internal.rpc.JavaScriptRewriteRpcProcess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * A factory for creating instances of {@link JavaScriptRewriteRpc} on demand.
 */
public interface JavaScriptRewriteRpcFactory {
    JavaScriptRewriteRpcFactory DEFAULT = JavaScriptRewriteRpcFactory
            .builder(Environment.builder().build())
            .build();

    JavaScriptRewriteRpc create();

    @RequiredArgsConstructor
    class Builder {
        private final Environment environment;
        private Path npxPath = Paths.get("npx");
        private @Nullable Path log;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean trace = false;
        private boolean verboseLogging = true;
        private @Nullable Integer inspectBrk;

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

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder trace(boolean trace) {
            this.trace = trace;
            return this;
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

        public JavaScriptRewriteRpcFactory build() {
            // npx --node-options="--enable-source-maps=4000 --inspect-brk=9229" @openrewrite/rewrite@8.60.2 rewrite-js
            StringJoiner nodeOptions = new StringJoiner(" ", "--node-options=\"", "\"");
            nodeOptions.add("--enable-source-maps");
            if (inspectBrk != null) {
                nodeOptions.add("--inspect-brk=" + inspectBrk);
            }

            String pkg = "--package=@openrewrite/rewrite@" + StringUtils.readFully(getClass().getResourceAsStream("/META-INF/version.txt"));
            String[] cmd = Stream.of(
                    npxPath.toString(), nodeOptions.toString(), pkg, "rewrite-rpc",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    verboseLogging ? "--verbose" : null
            ).filter(Objects::nonNull).toArray(String[]::new);

            System.out.println(String.join(" ", cmd));

            return () -> {
                JavaScriptRewriteRpcProcess process = new JavaScriptRewriteRpcProcess(trace, cmd);
                process.start();
                return new JavaScriptRewriteRpc(process, environment, timeout);
            };
        }
    }

    static Builder builder(Environment environment) {
        return new Builder(environment);
    }
}
