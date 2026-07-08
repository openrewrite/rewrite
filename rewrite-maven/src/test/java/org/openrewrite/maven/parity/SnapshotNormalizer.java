/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.parity;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normalization rules for snapshot values plus the known-nondeterminism mask registry
 * ({@code parity/masks.txt}). Normalization preserves everything after a masked root so two
 * distinct repositories never collapse into the same token.
 */
public class SnapshotNormalizer {
    private static final Pattern LOCALHOST_PORT = Pattern.compile("localhost:\\d+");
    private static final Pattern DATED_SNAPSHOT = Pattern.compile("\\d{8}\\.\\d{6}-\\d+");
    private static final Pattern URL_AUTHORITY = Pattern.compile("(https?://)[^/\\s]+");

    private final List<String> rootReplacements = new ArrayList<>();

    public SnapshotNormalizer(Path... roots) {
        for (Path root : roots) {
            String uri = root.toUri().toString();
            rootReplacements.add(uri.endsWith("/") ? uri : uri + "/");
            rootReplacements.add(root.toAbsolutePath() + "/");
        }
    }

    public @Nullable String normalize(@Nullable String s) {
        if (s == null) {
            return null;
        }
        for (String root : rootReplacements) {
            s = s.replace(root, "<path>/");
        }
        s = LOCALHOST_PORT.matcher(s).replaceAll("<local>");
        s = DATED_SNAPSHOT.matcher(s).replaceAll("<ts>");
        return s;
    }

    public @Nullable String normalizeMessage(@Nullable String message) {
        if (message == null) {
            return null;
        }
        int newline = message.indexOf('\n');
        String firstLine = newline < 0 ? message : message.substring(0, newline);
        firstLine = URL_AUTHORITY.matcher(firstLine).replaceAll("$1<authority>");
        return normalize(firstLine);
    }

    @Value
    public static class Mask {
        String ledgerId;
        String jsonPathPrefix;
    }

    /**
     * Loads {@code parity/masks.txt} from the classpath: one {@code ledgerId jsonPathPrefix} pair
     * per line, {@code #} comments allowed. A mask without a ledger id fails the build.
     */
    public static List<Mask> loadMasks() {
        List<Mask> masks = new ArrayList<>();
        try (InputStream is = SnapshotNormalizer.class.getResourceAsStream("/parity/masks.txt")) {
            if (is == null) {
                return masks;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    throw new IllegalStateException("Every mask requires a ledger id and a JSON path prefix: " + line);
                }
                masks.add(new Mask(parts[0], parts[1]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return masks;
    }
}
