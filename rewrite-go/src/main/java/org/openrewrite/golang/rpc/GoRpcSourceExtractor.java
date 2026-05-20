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
package org.openrewrite.golang.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts the Go RPC server source tree bundled inside the rewrite-go JAR
 * (under {@code META-INF/rewrite-go/src/}) to a caller-provided directory.
 * <p>
 * The bundled tree contains the {@code cmd/rpc} entry point, the complete
 * {@code pkg/} subtree (every submodule, not only those reachable from
 * {@code ./cmd/rpc}), {@code go.mod}/{@code go.sum}, and vendored
 * third-party dependencies, so consumers can run {@code go build ./cmd/rpc}
 * against the extracted directory without network access — and can also
 * compile additional entry points that reference arbitrary {@code pkg/}
 * subpackages without us shipping an allowlist.
 * <p>
 * Typical usage from the Moderne CLI:
 * <pre>{@code
 * Path src = Files.createTempDirectory("rewrite-go-rpc-src");
 * GoRpcSourceExtractor.extractTo(src);
 * // ... run `go build -o rewrite-go-rpc ./cmd/rpc` in src ...
 * }</pre>
 */
public final class GoRpcSourceExtractor {

    static final String RESOURCE_ROOT = "META-INF/rewrite-go/src";

    private GoRpcSourceExtractor() {
    }

    /**
     * Extract the bundled Go RPC source tree to {@code target}, creating it
     * (and intermediate directories) if necessary. Existing files at the
     * destination are overwritten.
     *
     * @param target directory to write into; must be writable
     * @throws IOException if the bundled tree cannot be located or written
     */
    public static void extractTo(Path target) throws IOException {
        Files.createDirectories(target);

        URL marker = GoRpcSourceExtractor.class.getClassLoader().getResource(RESOURCE_ROOT + "/go.mod");
        if (marker == null) {
            throw new IOException("Go RPC source bundle not found on classpath at /" + RESOURCE_ROOT);
        }

        String protocol = marker.getProtocol();
        if ("jar".equals(protocol)) {
            extractFromJar(marker, target);
        } else if ("file".equals(protocol)) {
            extractFromFilesystem(marker, target);
        } else {
            // Fall back to NIO FileSystems (works for jar: at minimum, but already handled above).
            extractViaFileSystems(marker, target);
        }
    }

    private static void extractFromJar(URL marker, Path target) throws IOException {
        // jar:file:/path/to/rewrite-go.jar!/META-INF/rewrite-go/src/go.mod
        String spec = marker.getPath();
        int bang = spec.indexOf("!/");
        if (bang < 0) {
            throw new IOException("Unrecognized jar URL: " + marker);
        }
        URI jarUri = URI.create(spec.substring(0, bang));
        Path jarPath = Paths.get(jarUri);
        String prefix = RESOURCE_ROOT + "/";

        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || name.equals(prefix)) {
                    continue;
                }
                String relative = name.substring(prefix.length());
                Path out = target.resolve(relative).normalize();
                if (!out.startsWith(target)) {
                    throw new IOException("Refusing to write outside target: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static void extractFromFilesystem(URL marker, Path target) throws IOException {
        Path markerPath = Paths.get(uriOf(marker));
        // markerPath ends in .../META-INF/rewrite-go/src/go.mod; the source root is its parent.
        Path root = markerPath.getParent();
        if (root == null) {
            throw new IOException("Cannot derive source root from: " + marker);
        }
        copyTree(root, target);
    }

    private static void extractViaFileSystems(URL marker, Path target) throws IOException {
        URI markerUri = uriOf(marker);
        try (FileSystem fs = FileSystems.newFileSystem(markerUri, Collections.<String, Object>emptyMap())) {
            Path markerPath = fs.provider().getPath(markerUri);
            Path root = markerPath.getParent();
            if (root == null) {
                throw new IOException("Cannot derive source root from: " + marker);
            }
            copyTree(root, target);
        }
    }

    private static void copyTree(Path root, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.forEach(src -> {
                try {
                    Path rel = root.relativize(src);
                    // Cross-FS relativize can yield a Path from a non-default provider;
                    // route through string to land on the target's filesystem.
                    Path dest = target.resolve(rel.toString());
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private static URI uriOf(URL url) throws IOException {
        try {
            return url.toURI();
        } catch (Exception e) {
            throw new IOException("Invalid URL: " + url, e);
        }
    }
}
