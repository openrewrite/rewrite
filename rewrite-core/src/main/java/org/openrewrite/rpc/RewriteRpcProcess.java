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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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

    private final Set<String> unsetEnvNames = new LinkedHashSet<>();

    @Setter
    private @Nullable Path stderrRedirect;

    @Nullable
    private Thread shutdownHook;

    @Nullable
    private Thread stderrDrainThread;

    @Nullable
    private Thread rssSamplerThread;

    @Nullable
    private IOException startupFailure;

    public RewriteRpcProcess(String... command) {
        this.command = command;
        this.setName("RewriteRpcProcess");
        this.setDaemon(false);
    }

    public Map<String, String> environment() {
        return environment;
    }

    /**
     * Strip these env vars from the spawned process's environment before
     * applying {@link #environment()}. Use when the subprocess bundles its
     * own runtime (e.g. a packaged Node) and inherited runtime-specific
     * vars from the parent — {@code NODE_OPTIONS}, {@code NODE_PATH},
     * {@code PYTHONHOME}, … — corrupt its startup.
     */
    public void unsetEnv(Collection<String> names) {
        this.unsetEnvNames.addAll(names);
    }

    public RewriteRpcProcess trace() {
        this.trace = true;
        return this;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            // Strip inherited vars BEFORE putAll so the caller's explicit
            // environment (which can re-set any of these names) wins.
            unsetEnvNames.forEach(pb.environment()::remove);
            pb.environment().putAll(environment);
            if (workingDirectory != null) {
                pb.directory(workingDirectory.toFile());
            }
            // Don't use ProcessBuilder.redirectError() — on Windows it leaks the
            // parent-side file handle after process termination, preventing deletion
            // of the log file.  Instead we drain stderr in a daemon thread.
            process = pb.start();
            stderrDrainThread = drainStderr(process, stderrRedirect);
        } catch (IOException e) {
            // Record the failure so start() can surface it instead of busy-waiting forever
            // on `process == null`. Throwing here would just kill this thread silently.
            this.startupFailure = e;
        }
    }

    private static Thread drainStderr(Process process, @Nullable Path stderrRedirect) {
        Thread thread = new Thread(() -> {
            byte[] buf = new byte[8192];
            try (InputStream stderr = process.getErrorStream()) {
                if (stderrRedirect != null) {
                    try (OutputStream out = Files.newOutputStream(stderrRedirect,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        int n;
                        while ((n = stderr.read(buf)) != -1) {
                            out.write(buf, 0, n);
                        }
                    }
                } else {
                    //noinspection StatementWithEmptyBody
                    while (stderr.read(buf) != -1) {
                        // discard
                    }
                }
            } catch (IOException ignored) {
            }
        }, "rpc-stderr-drain");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public @Nullable RuntimeException getLivenessCheck() {
        if (process == null) {
            return null;
        }

        if (!process.isAlive()) {
            int exitCode = process.exitValue();

            // Read any remaining stdout
            String stdOutput = "";
            try {
                InputStream inputStream = process.getInputStream();
                stdOutput = readFully(inputStream);
            } catch (UnsupportedOperationException e) {
                // Ignore errors reading final stdout
            }

            String message = "RPC process shut down early with exit code " + exitCode;
            if (!stdOutput.isEmpty()) {
                message += "\nStandard output:\n  " + stdOutput.replace("\n", "\n  ");
            }
            if (stderrRedirect != null) {
                message += "\nSee stderr log: " + stderrRedirect;
            }
            return new IllegalStateException(message.trim());
        }
        return null;
    }

    @Override
    public void start() {
        super.start();
        while (this.process == null) {
            if (!this.isAlive()) {
                if (startupFailure != null) {
                    throw new UncheckedIOException(
                            "Failed to start RPC process: " + String.join(" ", command),
                            startupFailure);
                }
                throw new IllegalStateException(
                        "RPC startup thread exited without starting process: " + String.join(" ", command));
            }
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Hold a reference so shutdown() can deregister the hook; otherwise each
        // RPC process leaks its full RewriteRpc graph via ApplicationShutdownHooks.
        shutdownHook = new Thread(() -> {
            try {
                shutdown();
            } catch (Throwable ignored) {
            }
        }, "rpc-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        startRssSampler();

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
        if (rssSamplerThread != null) {
            rssSamplerThread.interrupt();
            rssSamplerThread = null;
        }
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM is already shutting down (e.g. shutdown() invoked from the hook itself).
            }
            shutdownHook = null;
        }
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                int exitCode = process.exitValue();
                if (exitCode != 0 && exitCode != 1 && exitCode != 143) { // 143 = SIGTERM
                    throw new RuntimeException("Rewrite RPC process crashed with exit code: " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Wait for the drain thread to observe EOF and close its handle on
        // stderrRedirect. Without this, the parent-side file handle outlives
        // shutdown(); on Windows that breaks `@TempDir` test cleanup and any
        // reopen-the-same-path flow, because the file can't be deleted or
        // replaced while a handle is held without FILE_SHARE_DELETE. This is
        // the same hazard the drain-thread approach exists to avoid (see
        // run() above). Done outside the isAlive() guard so the join also
        // runs when the subprocess already exited on its own.
        if (stderrDrainThread != null) {
            try {
                stderrDrainThread.join(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stderrDrainThread = null;
        }
    }

    /**
     * Opt-in RSS profiling ({@code REWRITE_RPC_RSS_MS}): sample the subprocess RSS — which catches
     * native/off-heap growth the in-process counters miss — into {@code rpc-rss-<pid>.csv}.
     */
    private void startRssSampler() {
        String intervalEnv = System.getenv("REWRITE_RPC_RSS_MS");
        Path dir = workingDirectory;
        Process p = process;
        if (intervalEnv == null || dir == null || p == null) {
            return;
        }
        long intervalMs;
        try {
            intervalMs = Long.parseLong(intervalEnv.trim());
        } catch (NumberFormatException e) {
            return;
        }
        if (intervalMs <= 0) {
            return;
        }
        long pid;
        try {
            // Process.pid() is Java 9+, but this module compiles at Java 8 source level.
            pid = (long) Process.class.getMethod("pid").invoke(p);
        } catch (Exception e) {
            return;
        }
        Path rssCsv = dir.resolve("rpc-rss-" + pid + ".csv");
        Thread sampler = new Thread(() -> {
            try (OutputStream out = Files.newOutputStream(rssCsv,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                out.write("timestamp_ms,rss_kb\n".getBytes());
                out.flush();
                while (p.isAlive() && !Thread.currentThread().isInterrupted()) {
                    long rssKb = readRssKb(pid);
                    if (rssKb >= 0) {
                        out.write((System.currentTimeMillis() + "," + rssKb + "\n").getBytes());
                        out.flush();
                    }
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // Best-effort profiling; never disturb the run.
            }
        }, "rpc-rss-sampler");
        sampler.setDaemon(true);
        sampler.start();
        this.rssSamplerThread = sampler;
    }

    /** Resident set size of {@code pid} in KiB via {@code ps} (macOS + Linux), or -1 on failure. */
    private static long readRssKb(long pid) {
        try {
            Process ps = new ProcessBuilder("ps", "-o", "rss=", "-p", Long.toString(pid))
                    .redirectErrorStream(true).start();
            String out = readFully(ps.getInputStream()).trim();
            ps.waitFor(2, TimeUnit.SECONDS);
            if (out.isEmpty()) {
                return -1;
            }
            String[] tokens = out.split("\\s+");
            return Long.parseLong(tokens[tokens.length - 1]);
        } catch (Exception e) {
            return -1;
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
