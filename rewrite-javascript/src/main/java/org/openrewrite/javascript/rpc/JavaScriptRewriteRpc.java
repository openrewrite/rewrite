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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.MessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class JavaScriptRewriteRpc extends RewriteRpc {
    private final JavaScriptRewriteRpcProcess process;
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModules(new ParameterNamesModule(), new JavaTimeModule())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setVisibility(VisibilityChecker.Std.defaultInstance()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

    private JavaScriptRewriteRpc(JavaScriptRewriteRpcProcess process, Environment marketplace) {
        super(process.getRpcClient(), marketplace);
        this.process = process;
    }

    public static JavaScriptRewriteRpc start(Environment marketplace, String... command) {
        JavaScriptRewriteRpcProcess process = new JavaScriptRewriteRpcProcess(command);
        process.start();
        return new JavaScriptRewriteRpc(process, marketplace);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        process.interrupt();
        try {
            process.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private static class JavaScriptRewriteRpcProcess extends Thread {
        private final String[] command;

        @Nullable
        private Process process;

        @Getter
        private JsonRpc rpcClient;

        public JavaScriptRewriteRpcProcess(String... command) {
            this.command = command;
            this.setDaemon(false);
        }

        @Override
        public void run() {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                process = pb.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public synchronized void start() {
            super.start();
            while (this.process == null) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            MessageHandler handler = new HeaderDelimitedMessageHandler(
                    new JsonMessageFormatter(mapper),
                    this.process.getInputStream(),
                    this.process.getOutputStream());

            // FIXME provide an option to make tracing optional
//            handler = new TraceMessageHandler("client", handler);
            this.rpcClient = new JsonRpc(handler);
        }
    }
}
