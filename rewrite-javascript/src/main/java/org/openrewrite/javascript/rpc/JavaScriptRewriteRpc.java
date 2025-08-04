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
import org.openrewrite.internal.ManagedThreadLocal;
import org.openrewrite.rpc.RewriteRpc;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;

public class JavaScriptRewriteRpc extends RewriteRpc {

    private final CloseableSupplier<JsonRpc> supplier;

    private JavaScriptRewriteRpc(CloseableSupplier<JsonRpc> supplier, Environment marketplace, Duration timeout) {
        super(supplier.get(), marketplace, timeout);
        this.supplier = supplier;
    }

    private static final ManagedThreadLocal<JavaScriptRewriteRpc> THREAD_LOCAL = new ManagedThreadLocal<>();

    public static ManagedThreadLocal<JavaScriptRewriteRpc> current() {
        return THREAD_LOCAL;
    }

    @Override
    public void close() {
        super.close();
        supplier.close();
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

    public static Builder bundledInstallation(Environment marketplace) {
        return builder(marketplace);
    }

    public static Builder bundledInstallation(Environment marketplace, Path installationDirectory) {
        return builder(marketplace).installationDirectory(extractBundledInstallation(installationDirectory));
    }

    public static Builder builder(Environment marketplace) {
        return new Builder(marketplace);
    }

    public static class Builder extends RewriteRpc.Builder<Builder> {
        private static volatile @Nullable Path bundledInstallationDirectory;

        private Path nodePath = Paths.get("node");
        private @Nullable Path installationDirectory;
        private int inspectPort;
        private int port;
        private boolean trace;
        private @Nullable Path logFile;

        private Builder(Environment marketplace) {
            super(marketplace);
        }

        private static Path getBundledInstallationDirectory() {
            if (bundledInstallationDirectory == null) {
                synchronized (Builder.class) {
                    if (bundledInstallationDirectory == null) {
                        bundledInstallationDirectory = initializeBundledInstallationDirectory();
                    }
                }
            }
            return requireNonNull(bundledInstallationDirectory);
        }

        private static Path initializeBundledInstallationDirectory() {
            try {
                Path tempDir = Files.createTempDirectory("javascript-rewrite-rpc");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try (Stream<Path> stream = Files.walk(tempDir)) {
                        stream.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException ignore) {
                    }
                }));

                return extractBundledInstallation(tempDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public Builder nodePath(Path nodePath) {
            this.nodePath = nodePath;
            return this;
        }

        public Builder installationDirectory(Path installationDirectory) {
            this.installationDirectory = installationDirectory;
            return this;
        }

        public Builder inspectAndBreak() {
            return inspectAndBreak(9229);
        }

        public Builder inspectAndBreak(int port) {
            this.inspectPort = port;
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

        public Builder logFile(Path logFile) {
            this.logFile = logFile;
            return this;
        }

        @Override
        public JavaScriptRewriteRpc build() {
            JavaScriptRewriteRpc rewriteRpc;
            if (port != 0) {
                rewriteRpc = new JavaScriptRewriteRpc(new CloseableSupplier<JsonRpc>() {
                    @SuppressWarnings("NotNullFieldNotInitialized")
                    private Socket socket;

                    @Override
                    public JsonRpc get() {
                        try {
                            socket = new Socket("127.0.0.1", port);
                            return createRpcClient(socket.getInputStream(), socket.getOutputStream(), trace);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }, marketplace, timeout);
            } else {
                // Use default installation directory if none provided (lazy-loaded)
                Path effectiveInstallationDirectory = installationDirectory != null ?
                        installationDirectory :
                        getBundledInstallationDirectory();

                List<String> command = new ArrayList<>(Arrays.asList(
                        nodePath.toString(),
                        "--stack-size=4000",
                        "--enable-source-maps"
                ));
                if (inspectPort != 0) {
                    command.add("--inspect-brk=" + inspectPort);
                }
                command.add(effectiveInstallationDirectory.resolve("src/rpc/server.js").toString());

                if (logFile != null) {
                    command.add("--log-file=" + logFile);
                }

                rewriteRpc = new JavaScriptRewriteRpc(new CloseableSupplier<JsonRpc>() {
                    private @Nullable JavaScriptRewriteRpcProcess process;

                    @Override
                    public JsonRpc get() {
                        process = new JavaScriptRewriteRpcProcess(logFile, trace, command.toArray(new String[0]));
                        process.start();
                        return requireNonNull(process.rpcClient);
                    }

                    @Override
                    public void close() {
                        if (process != null) {
                            process.interrupt();
                            try {
                                process.join();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }, marketplace, timeout);
            }

            return rewriteRpc;
        }
    }

    /**
     * Extracts the bundled JavaScript installation to the specified directory.
     * This method can be used to extract the bundled installation independently
     * of creating a JavaScriptRewriteRpc instance.
     * <p>
     * If a package.json file already exists in the extraction directory, it will
     * be compared with the bundled package.json. If they differ, the entire
     * extraction directory will be cleaned before extracting the bundled installation.
     *
     * @param extractionDirectory The directory where the bundled installation should be extracted
     * @return The path to the extracted installation directory (specifically the dist folder)
     * @throws UncheckedIOException if extraction fails
     */
    private static Path extractBundledInstallation(Path extractionDirectory) {
        try {
            Files.createDirectories(extractionDirectory);

            // Check if we need to clean the directory by comparing package.json files
            boolean needsCleanup = shouldCleanExtractionDirectory(extractionDirectory);

            if (needsCleanup) {
                // Delete everything in the extraction directory
                if (Files.exists(extractionDirectory)) {
                    try (Stream<Path> stream = Files.walk(extractionDirectory)) {
                        stream.sorted(Comparator.reverseOrder())
                                .filter(path -> !path.equals(extractionDirectory)) // Don't delete the directory itself
                                .map(Path::toFile)
                                .forEach(File::delete);
                    }
                }
            }

            InputStream packageStream = JavaScriptRewriteRpc.class.getResourceAsStream("/production-package.zip");
            if (packageStream == null) {
                throw new IllegalStateException("production-package.zip not found in resources");
            }

            try (ZipInputStream zipIn = new ZipInputStream(packageStream)) {
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Path outputPath = extractionDirectory.resolve(entry.getName());
                        Files.createDirectories(outputPath.getParent());
                        Files.copy(zipIn, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zipIn.closeEntry();
                }
            }

            return extractionDirectory.resolve("node_modules/@openrewrite/rewrite/dist");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean shouldCleanExtractionDirectory(Path extractionDirectory) throws IOException {
        Path existingPackageJson = extractionDirectory.resolve("package.json");
        if (!Files.exists(existingPackageJson)) {
            return false; // No existing package.json, no need to clean
        }

        String bundledPackageJsonContent = getBundledPackageJsonContent();
        String existingPackageJsonContent = new String(Files.readAllBytes(existingPackageJson), StandardCharsets.UTF_8);
        return !bundledPackageJsonContent.equals(existingPackageJsonContent);
    }

    private static String getBundledPackageJsonContent() throws IOException {
        InputStream packageStream = JavaScriptRewriteRpc.class.getResourceAsStream("/production-package.zip");
        if (packageStream == null) {
            throw new IllegalStateException("package.json not found in resources");
        }

        try (ZipInputStream zipIn = new ZipInputStream(packageStream)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if ("package.json".equals(entry.getName())) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int byteCount;
                    byte[] data = new byte[4096];
                    while ((byteCount = zipIn.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, byteCount);
                    }
                    return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                }
                zipIn.closeEntry();
            }
        }

        throw new IllegalStateException("package.json not found in resources");
    }

    private interface CloseableSupplier<T> extends Supplier<T>, AutoCloseable {
        @Override
        void close();
    }

    private static class JavaScriptRewriteRpcProcess extends Thread {
        private final @Nullable Path logFile;
        private final boolean trace;
        private final String[] command;

        @Nullable
        private Process process;

        @Getter
        private @Nullable JsonRpc rpcClient;

        public JavaScriptRewriteRpcProcess(@Nullable Path logFile, boolean trace, String... command) {
            this.logFile = logFile;
            this.trace = trace;
            this.command = command;
            this.setDaemon(false);
        }

        @Override
        public void run() {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectError(logFile != null ? ProcessBuilder.Redirect.appendTo(logFile.toFile()) :
                        ProcessBuilder.Redirect.INHERIT);
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
