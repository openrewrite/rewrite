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
package org.openrewrite.maven.rpc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.MessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import org.jspecify.annotations.Nullable;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.marketplace.RecipeMarketplaceReader;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.marketplace.MavenRecipeBundleResolver;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;
import org.openrewrite.rpc.RewriteRpc;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A standalone RPC server application that communicates via stdin/stdout.
 * <p>
 * This application is designed to be spawned as a subprocess by Python, JavaScript,
 * or any other language that needs to use Java-based OpenRewrite recipes.
 * <p>
 * Usage:
 * <pre>
 *   java -cp &lt;classpath&gt; org.openrewrite.maven.rpc.JavaRewriteRpc [options]
 * </pre>
 * <p>
 * Options:
 * <ul>
 *   <li>--marketplace=&lt;path&gt; - Path to marketplace CSV file (required)</li>
 *   <li>--log-file=&lt;path&gt; - Log file for debugging</li>
 *   <li>--trace - Enable RPC message tracing</li>
 * </ul>
 * <p>
 * The marketplace CSV must contain columns for recipe metadata and bundle information.
 * Required columns: name, ecosystem, packageName, version
 * <p>
 * Example marketplace.csv:
 * <pre>
 * name,displayName,ecosystem,packageName,version
 * org.openrewrite.java.ChangeType,Change type,maven,org.openrewrite:rewrite-java,8.73.0
 * </pre>
 */
public class JavaRewriteRpc {

    public static void main(String[] args) {
        @Nullable Path marketplaceCsv = null;
        @Nullable Path logFile = null;
        boolean trace = false;

        // Parse command line arguments
        for (String arg : args) {
            if (arg.startsWith("--marketplace=")) {
                marketplaceCsv = Paths.get(arg.substring("--marketplace=".length()));
            } else if (arg.startsWith("--log-file=")) {
                logFile = Paths.get(arg.substring("--log-file=".length()));
            } else if (arg.equals("--trace")) {
                trace = true;
            }
        }

        if (marketplaceCsv == null) {
            System.err.println("Error: --marketplace=<path> is required");
            System.err.println("Usage: java org.openrewrite.maven.rpc.JavaRewriteRpc --marketplace=<csv> [--log-file=<path>] [--trace]");
            System.exit(1);
        }

        if (!Files.exists(marketplaceCsv)) {
            System.err.println("Error: Marketplace CSV file not found: " + marketplaceCsv);
            System.exit(1);
        }

        // Redirect stderr to log file if specified (to avoid interfering with RPC on stdout)
        PrintStream logStream = null;
        if (logFile != null) {
            try {
                logStream = new PrintStream(Files.newOutputStream(logFile,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND));
                System.setErr(logStream);
            } catch (IOException e) {
                System.err.println("Failed to open log file: " + e.getMessage());
            }
        }

        try {
            run(marketplaceCsv, trace, logStream);
        } catch (Exception e) {
            System.err.println("RPC server error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (logStream != null) {
                logStream.close();
            }
        }
    }

    private static void run(Path marketplaceCsv, boolean trace, @Nullable PrintStream logStream) {
        // Load the recipe marketplace from CSV
        RecipeMarketplaceReader reader = new RecipeMarketplaceReader();
        RecipeMarketplace marketplace = reader.fromCsv(marketplaceCsv);

        if (logStream != null) {
            logStream.println("Loaded marketplace with " + marketplace.getAllRecipes().size() + " recipes from " + marketplaceCsv);
            logStream.flush();
        }

        // Set up execution context
        PrintStream errorHandler = logStream != null ? logStream : System.err;
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
            errorHandler.println("Error: " + t.getMessage());
            t.printStackTrace(errorHandler);
        });

        // Configure HTTP and Maven settings
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        // Create artifact cache in user's .rewrite directory
        Path cacheDir;
        try {
            cacheDir = Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "artifacts");
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Maven artifact cache directory", e);
        }
        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(cacheDir);

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
                artifactCache,
                null, // No custom Maven settings
                new HttpUrlConnectionSender(),
                t -> errorHandler.println("Download error: " + t.getMessage())
        );

        // Set up resolvers
        List<RecipeBundleResolver> resolvers = new ArrayList<>();
        resolvers.add(new MavenRecipeBundleResolver(ctx, downloader, RecipeClassLoader::new));

        if (logStream != null) {
            logStream.println("Configured Maven recipe bundle resolver");
            logStream.flush();
        }

        // Set up JSON-RPC communication on stdin/stdout
        SimpleModule module = new SimpleModule();
        module.addSerializer(Path.class, new PathSerializer());
        module.addDeserializer(Path.class, new PathDeserializer());

        JsonMessageFormatter formatter = new JsonMessageFormatter(module, new ParameterNamesModule());
        MessageHandler handler = new HeaderDelimitedMessageHandler(formatter, System.in, System.out);

        if (trace) {
            handler = new TraceMessageHandler("server", handler);
        }

        JsonRpc jsonRpc = new JsonRpc(handler);

        // Create the RPC server with the marketplace and resolvers
        RewriteRpc server = new RewriteRpc(jsonRpc, marketplace, resolvers);

        if (logStream != null) {
            server.log(logStream);
            logStream.println("RPC server started");
            logStream.flush();
        }

        // The JsonRpc.bind() call in RewriteRpc constructor starts the message handler
        // that reads from stdin in a background thread.

        // Keep the main thread alive while the RPC handler processes messages.
        // Use Object.wait() to block indefinitely - the process will be terminated
        // externally when the parent process closes stdin or kills this process.
        Object lock = new Object();
        synchronized (lock) {
            try {
                // Wait indefinitely - this keeps the JVM alive
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (logStream != null) {
                    logStream.println("Server interrupted");
                    logStream.flush();
                }
            }
        }

        server.shutdown();
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
