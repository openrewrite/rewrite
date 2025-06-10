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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class JavaScriptRewriteRpc extends RewriteRpc {

    private final @Nullable JavaScriptRewriteRpcProcess process;
    private final @Nullable Closeable closeable;

    private JavaScriptRewriteRpc(JavaScriptRewriteRpcProcess process, Environment marketplace) {
        super(requireNonNull(process.getRpcClient()), marketplace);
        this.process = process;
        this.closeable = null;
    }

    private JavaScriptRewriteRpc(JsonRpc rpc, Closeable closeable, Environment marketplace) {
        super(rpc, marketplace);
        this.process = null;
        this.closeable = closeable;
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

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private static volatile @Nullable Path defaultInstallationDirectory;

        private Environment marketplace = Environment.builder().build();
        private Path nodePath = Paths.get("node");
        private @Nullable Path installationDirectory;
        private int port;
        private boolean trace;
        private @Nullable Path logFile;
        private @Nullable Duration timeout;

        private static Path getDefaultInstallationDirectory() {
            if (defaultInstallationDirectory == null) {
                synchronized (Builder.class) {
                    if (defaultInstallationDirectory == null) {
                        defaultInstallationDirectory = initializeDefaultInstallationDirectory();
                    }
                }
            }
            return requireNonNull(defaultInstallationDirectory);
        }

        private static Path initializeDefaultInstallationDirectory() {
            try {
                Path tempDir = Files.createTempDirectory("javascript-rewrite-rpc");

                // Add shutdown hook to clean up temporary directory
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try (Stream<Path> stream = Files.walk(tempDir)) {
                        //noinspection ResultOfMethodCallIgnored
                        stream.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException ignore) {
                    }
                }));

                // Create package.json with required dependencies
                //language=json
                String packageJsonContent = "{\n" +
                                            "  \"name\": \"javascript-rewrite-rpc-temp\",\n" +
                                            "  \"version\": \"1.0.0\",\n" +
                                            "  \"private\": true,\n" +
                                            "  \"dependencies\": {\n" +
                                            "    \"@openrewrite/rewrite\": \"next\",\n" +
                                            "    \"typescript\": \"latest\"\n" +
                                            "  }\n" +
                                            "}\n";

                Path packageJsonPath = tempDir.resolve("package.json");
                Files.write(packageJsonPath, packageJsonContent.getBytes(StandardCharsets.UTF_8));

                Path errorFile = Files.createTempFile("npm-install-error", ".log");

                try {
                    // Run npm install to download dependencies
                    ProcessBuilder npmInstall = new ProcessBuilder("npm", "install")
                            .directory(tempDir.toFile())
                            .redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")))
                            .redirectError(ProcessBuilder.Redirect.to(errorFile.toFile()));

                    Process process = npmInstall.start();
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        // Read error content from the temporary file
                        String errorContent;
                        try {
                            errorContent = new String(Files.readAllBytes(errorFile), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            errorContent = "Unable to read error output: " + e.getMessage();
                        }

                        throw new RuntimeException("Failed to install npm dependencies in temporary directory. Exit code: " +
                                                   exitCode + ". Error output: " + errorContent);
                    }
                } finally {
                    try {
                        Files.deleteIfExists(errorFile);
                    } catch (IOException ignore) {
                    }
                }

                return tempDir.resolve("node_modules/@openrewrite/rewrite/dist");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to initialize default installation directory", e);
            }
        }

        public Builder marketplace(Environment marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder nodePath(Path nodePath) {
            this.nodePath = nodePath;
            return this;
        }

        public Builder installationDirectory(Path installationDirectory) {
            this.installationDirectory = installationDirectory;
            return this;
        }

        public Builder socket(int port) {
            this.port = port;
            return this;
        }

        public Builder trace(boolean trace) {
            this.trace = trace;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logFile(Path logFile) {
            this.logFile = logFile;
            return this;
        }

        public JavaScriptRewriteRpc build() {
            JavaScriptRewriteRpc rewriteRpc;
            if (port != 0) {
                Socket socket;
                JsonRpc rpc;
                try {
                    socket = new Socket("127.0.0.1", port);
                    rpc = createRpcClient(socket.getInputStream(), socket.getOutputStream(), trace);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                rewriteRpc = new JavaScriptRewriteRpc(rpc, socket, marketplace);
            } else {
                // Use default installation directory if none provided (lazy-loaded)
                Path effectiveInstallationDirectory = installationDirectory != null
                        ? installationDirectory
                        : getDefaultInstallationDirectory();

                List<String> command = new ArrayList<>(Arrays.asList(
                        nodePath.toString(),
                        "--stack-size=4000",
                        "--enable-source-maps",
                        // Uncomment this to debug the server
                        //  "--inspect-brk",
                        effectiveInstallationDirectory.resolve("src/rpc/server.js").toString()
                ));
                if (logFile != null) {
                    command.add("--log-file");
                    command.add(logFile.toString());
                }

                JavaScriptRewriteRpcProcess process = new JavaScriptRewriteRpcProcess(trace, command.toArray(new String[0]));
                process.start();
                rewriteRpc = new JavaScriptRewriteRpc(process, marketplace);
            }

            if (timeout != null) {
                rewriteRpc.timeout(timeout);
            }
            return rewriteRpc;
        }
    }

    private static class JavaScriptRewriteRpcProcess extends Thread {
        private final boolean trace;
        private final String[] command;

        @Nullable
        private Process process;

        @Getter
        private @Nullable JsonRpc rpcClient;

        public JavaScriptRewriteRpcProcess(boolean trace, String... command) {
            this.trace = trace;
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

            this.rpcClient = createRpcClient(this.process.getInputStream(), this.process.getOutputStream(), trace);
        }
    }

    private static JsonRpc createRpcClient(InputStream inputStream, OutputStream outputStream, boolean trace) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Path.class, new PathSerializer());
        module.addDeserializer(Path.class, new PathDeserializer());
        JsonMessageFormatter formatter = new JsonMessageFormatter(module);
        MessageHandler handler = new HeaderDelimitedMessageHandler(formatter, inputStream, outputStream);

        if (trace)
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
