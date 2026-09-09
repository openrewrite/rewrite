/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.BeforeEach;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared harness for the per-package-manager lock regeneration tests: a stub {@link HttpSender} backed by
 * {@link #routes}, an offline {@link ExecutionContext}, resource loading, and the disabled live re-record
 * helpers each subclass drives from its own {@code recordGoldens*} test.
 */
abstract class LockRegenTestSupport {

    protected static final String REG = "https://registry.npmjs.org/";

    protected ExecutionContext ctx;
    protected final Map<String, String> routes = new HashMap<>();
    /** Routes whose body is binary (e.g. a package tarball, served verbatim without UTF-8 mangling). */
    protected final Map<String, byte[]> binaryRoutes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        binaryRoutes.clear();
        HttpSender http = request -> {
            String url = request.getUrl().toString();
            byte[] binary = binaryRoutes.get(url);
            if (binary != null) {
                return new HttpSender.Response(200, new ByteArrayInputStream(binary), () -> {
                });
            }
            String body = routes.get(url);
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    protected void assertNpmReproduces(String pkgResource, String lockResource, String lockfileVersion) throws Exception {
        Path tmp = Files.createTempDirectory("npm-regen-record");
        try {
            npmInstallInto(tmp, pkgResource, lockResource, lockfileVersion);
        } finally {
            deleteRecursively(tmp);
        }
    }

    /**
     * Verify a two-phase incremental golden pair: {@code pkg-before} installed from scratch must reproduce the
     * {@code before} lock, then the edited {@code pkg} installed in the same directory (the before lock present)
     * must reproduce {@code after} — the incremental truth the engine's resolve-and-patch is held to.
     */
    protected void assertNpmReproducesIncremental(String dir, String before, String after,
                                                  String lockfileVersion) throws Exception {
        Path tmp = Files.createTempDirectory("npm-regen-record");
        try {
            npmInstallInto(tmp, dir + "/pkg-before", dir + "/" + before, lockfileVersion);
            npmInstallInto(tmp, dir + "/pkg", dir + "/" + after, lockfileVersion);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private void npmInstallInto(Path tmp, String pkgResource, String lockResource,
                                String lockfileVersion) throws Exception {
        Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("npm", "install", "--package-lock-only",
                "--lockfile-version", lockfileVersion, "--no-audit", "--no-fund")
                .directory(tmp.toFile())
                .redirectOutput(tmp.resolve("npm.log").toFile())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("npm install timed out for " + pkgResource);
        }
        String generated = new String(Files.readAllBytes(tmp.resolve("package-lock.json")), StandardCharsets.UTF_8);
        assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
    }

    private static void deleteRecursively(Path tmp) throws IOException {
        try (Stream<Path> walk = Files.walk(tmp)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    protected void assertPnpmReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-regen-record");
        try {
            pnpmInstallInto(tmp, pkgResource, lockResource);
        } finally {
            deleteRecursively(tmp);
        }
    }

    /** Verify a two-phase incremental golden pair with real pnpm (see {@link #assertNpmReproducesIncremental}). */
    protected void assertPnpmReproducesIncremental(String dir) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-regen-record");
        try {
            pnpmInstallInto(tmp, dir + "/pkg-before", dir + "/before");
            pnpmInstallInto(tmp, dir + "/pkg", dir + "/after");
        } finally {
            deleteRecursively(tmp);
        }
    }

    private void pnpmInstallInto(Path tmp, String pkgResource, String lockResource) throws Exception {
        Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("pnpm", "install", "--lockfile-only", "--no-color")
                .directory(tmp.toFile())
                .redirectOutput(tmp.resolve("pnpm.log").toFile())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("pnpm install timed out for " + pkgResource);
        }
        String generated = new String(Files.readAllBytes(tmp.resolve("pnpm-lock.yaml")), StandardCharsets.UTF_8);
        assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
    }

    protected void assertBunReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("bun-regen-record");
        try {
            bunInstallInto(tmp, pkgResource, lockResource);
        } finally {
            deleteRecursively(tmp);
        }
    }

    /**
     * Verify a two-phase incremental golden pair: {@code pkg-before} installed from scratch must reproduce the
     * {@code before} lock, then the edited {@code pkg} installed in the same directory (the before lock present)
     * must reproduce {@code after} — the incremental truth the engine's resolve-and-patch is held to.
     */
    protected void assertBunReproducesIncremental(String dir, String before, String after) throws Exception {
        Path tmp = Files.createTempDirectory("bun-regen-record");
        try {
            bunInstallInto(tmp, dir + "/pkg-before", dir + "/" + before);
            bunInstallInto(tmp, dir + "/pkg", dir + "/" + after);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private void bunInstallInto(Path tmp, String pkgResource, String lockResource) throws Exception {
        Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("bun", "install", "--lockfile-only", "--no-progress")
                .directory(tmp.toFile())
                .redirectOutput(tmp.resolve("bun.log").toFile())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("bun install timed out for " + pkgResource);
        }
        String generated = new String(Files.readAllBytes(tmp.resolve("bun.lock")), StandardCharsets.UTF_8);
        assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
    }

    protected void assertYarnReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("yarn-regen-record");
        try {
            yarnInstallInto(tmp, pkgResource, lockResource);
        } finally {
            deleteRecursively(tmp);
        }
    }

    /**
     * Verify a two-phase incremental golden pair: {@code pkg-before} installed from scratch must reproduce the
     * {@code before} lock, then the edited {@code pkg} installed in the same directory (the before lock and
     * node_modules present) must reproduce {@code after} — the incremental truth the engine is held to.
     */
    protected void assertYarnReproducesIncremental(String dir, String before, String after) throws Exception {
        Path tmp = Files.createTempDirectory("yarn-regen-record");
        try {
            yarnInstallInto(tmp, dir + "/pkg-before", dir + "/" + before);
            yarnInstallInto(tmp, dir + "/pkg", dir + "/" + after);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private void yarnInstallInto(Path tmp, String pkgResource, String lockResource) throws Exception {
        Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
        ProcessBuilder pb = new ProcessBuilder("yarn", "install", "--ignore-scripts", "--non-interactive", "--no-progress");
        pb.directory(tmp.toFile());
        pb.environment().put("YARN_CACHE_FOLDER", tmp.resolve(".cache").toString());
        pb.redirectOutput(tmp.resolve("yarn.log").toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("yarn install timed out for " + pkgResource);
        }
        String generated = new String(Files.readAllBytes(tmp.resolve("yarn.lock")), StandardCharsets.UTF_8);
        assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
    }

    protected static String resource(String path) {
        return new String(bytesResource(path), StandardCharsets.UTF_8);
    }

    protected static byte[] bytesResource(String path) {
        try (InputStream in = LockRegenTestSupport.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
