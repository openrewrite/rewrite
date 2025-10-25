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
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.internal.StringUtils.readFully;

/**
 * A client for spawning and communicating with a subprocess that implements Rewrite RPC.
 */
public class RewriteRpcProcess extends Thread {
    private final String[] command;

    @Setter
    private boolean trace;

    @Setter
    private @Nullable Path workingDirectory;

    @Nullable
    private Process process;

    @SuppressWarnings("NotNullFieldNotInitialized")
    @Getter
    private JsonRpc rpcClient;

    private final Map<String, String> environment = new LinkedHashMap<>();

    public RewriteRpcProcess(String... command) {
        this.command = command;
        this.setName("JavaScriptRewriteRpcProcess");
        this.setDaemon(false);
    }

    public Map<String, String> environment() {
        return environment;
    }

    public RewriteRpcProcess trace() {
        this.trace = true;
        return this;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().putAll(environment);
            if (workingDirectory != null) {
                pb.directory(workingDirectory.toFile());
            }
            process = pb.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public @Nullable RuntimeException getLivenessCheck() {
        if (process != null && !process.isAlive()) {
            int exitCode = process.exitValue();
            String errorOutput = "", stdOutput = "";

            // Read any remaining output from the process
            try (InputStream errorStream = process.getErrorStream();
                 InputStream inputStream = process.getInputStream()) {
                errorOutput = readFully(errorStream);
                stdOutput = readFully(inputStream);
            } catch (IOException | UnsupportedOperationException e) {
                // Ignore errors reading final output
            }

            String message = "JavaScript RPC process shut down early with exit code " + exitCode;
            if (!stdOutput.isEmpty()) {
                message += "\nStandard output:\n  " + stdOutput.replace("\n", "\n  ");
            }
            if (!errorOutput.isEmpty()) {
                message += "\nError output:\n  " + errorOutput.replace("\n", "\n  ");
            }
            return new IllegalStateException(message.trim());
        }
        return null;
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

        SimpleModule module = new SimpleModule();
        module.addSerializer(Path.class, new PathSerializer());
        module.addDeserializer(Path.class, new PathDeserializer());
        JsonMessageFormatter formatter = new JsonMessageFormatter(module);
        MessageHandler handler = new HeaderDelimitedMessageHandler(formatter,
                process.getInputStream(), process.getOutputStream());
        if (trace) {
            handler = new TraceMessageHandler("client", handler);
        }
        this.rpcClient = new JsonRpc(handler);
    }

    public void shutdown() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                int exitCode = process.exitValue();
                if (exitCode != 0 && exitCode != 143) { // 143 = SIGTERM
                    throw new RuntimeException("JavaScript Rewrite RPC process crashed with exit code: " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
