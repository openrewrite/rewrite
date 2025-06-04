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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.MessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaScriptRewriteRpc extends RewriteRpc {

    private final @Nullable JavaScriptRewriteRpcProcess process;
    private final @Nullable Closeable closeable;

    private JavaScriptRewriteRpc(JavaScriptRewriteRpcProcess process, Environment marketplace) {
        super(process.getRpcClient(), marketplace);
        this.process = process;
        this.closeable = null;
    }

    private JavaScriptRewriteRpc(JsonRpc rpc, Closeable closeable, Environment marketplace) {
        super(rpc, marketplace);
        this.process = null;
        this.closeable = closeable;
    }

    public static JavaScriptRewriteRpc start(Environment marketplace, String... command) {
        JavaScriptRewriteRpcProcess process = new JavaScriptRewriteRpcProcess(command);
        process.start();
        return new JavaScriptRewriteRpc(process, marketplace);
    }

    public static JavaScriptRewriteRpc connect(Environment marketplace, int port) {
        Socket socket;
        JsonRpc rpc;
        try {
            socket = new Socket("127.0.0.1", port);
            rpc = createRpcClient(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new JavaScriptRewriteRpc(rpc, socket, marketplace);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (process != null) {
            process.interrupt();
            try {
                process.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
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

            this.rpcClient = createRpcClient(this.process.getInputStream(), this.process.getOutputStream());
        }
    }

    private static JsonRpc createRpcClient(InputStream inputStream, OutputStream outputStream) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Path.class, new PathSerializer());
        module.addDeserializer(Path.class, new PathDeserializer());
        JsonMessageFormatter formatter = new JsonMessageFormatter(module);
        MessageHandler handler = new HeaderDelimitedMessageHandler(formatter, inputStream, outputStream);

        // FIXME provide an option to make tracing optional
        handler = new TraceMessageHandler("client", handler);
        return new JsonRpc(handler);
    }

    private static class PathSerializer extends JsonSerializer<Path> {
        @Override
        public void serialize(Path path, JsonGenerator g, SerializerProvider serializerProvider) throws IOException {
            g.writeString(path.toString());
        }
    }

    private static class PathDeserializer extends JsonDeserializer<Path> {
        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String pathString = p.getValueAsString();
            return Paths.get(pathString);
        }
    }
}
