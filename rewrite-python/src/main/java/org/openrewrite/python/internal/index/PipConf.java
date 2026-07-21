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
package org.openrewrite.python.internal.index;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The {@code index-url}/{@code extra-index-url} slice of pip's INI configuration.
 */
@Value
class PipConf {
    @Nullable
    String indexUrl;

    List<String> extraIndexUrls;

    static PipConf load(Environment env) {
        String indexUrl = null;
        List<String> extraIndexUrls = Collections.emptyList();
        for (Path file : configFiles(env)) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Map<String, String> values = parse(file);
            String value = values.get("index-url");
            if (value != null) {
                indexUrl = value;
            }
            value = values.get("extra-index-url");
            if (value != null) {
                extraIndexUrls = splitWhitespace(value);
            }
        }
        return new PipConf(indexUrl, extraIndexUrls);
    }

    /**
     * Global then user (legacy before primary) with {@code PIP_CONFIG_FILE} last;
     * later files override earlier ones per key.
     */
    static List<Path> configFiles(Environment env) {
        List<Path> files = new ArrayList<>();
        String os = env.osName().toLowerCase(Locale.ROOT);
        Path home = env.userHome();
        if (os.contains("win")) {
            String appData = env.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                files.add(Paths.get(appData).resolve("pip").resolve("pip.ini"));
            }
        } else if (os.contains("mac")) {
            files.add(home.resolve(".pip").resolve("pip.conf"));
            files.add(home.resolve("Library").resolve("Application Support").resolve("pip").resolve("pip.conf"));
        } else {
            files.add(Paths.get("/etc/pip.conf"));
            files.add(home.resolve(".pip").resolve("pip.conf"));
            String xdg = env.getenv("XDG_CONFIG_HOME");
            Path configHome = xdg != null && !xdg.isEmpty() ? Paths.get(xdg) : home.resolve(".config");
            files.add(configHome.resolve("pip").resolve("pip.conf"));
        }
        String explicit = env.getenv("PIP_CONFIG_FILE");
        if (explicit != null && !explicit.isEmpty()) {
            files.add(Paths.get(explicit));
        }
        return files;
    }

    private static Map<String, String> parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        String section = null;
        String lastKey = null;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                lastKey = null;
                continue;
            }
            char first = line.charAt(0);
            if (first == '#' || first == ';') {
                continue;
            }
            if (Character.isWhitespace(first)) {
                if (section != null && lastKey != null) {
                    Map<String, String> values = sections.get(section);
                    values.put(lastKey, values.get(lastKey) + "\n" + line.trim());
                }
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim().toLowerCase(Locale.ROOT);
                sections.computeIfAbsent(section, k -> new LinkedHashMap<>());
                lastKey = null;
                continue;
            }
            int separator = separatorIndex(trimmed);
            if (separator < 0 || section == null) {
                lastKey = null;
                continue;
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            sections.get(section).put(key, trimmed.substring(separator + 1).trim());
            lastKey = key;
        }
        Map<String, String> effective = new LinkedHashMap<>();
        Map<String, String> global = sections.get("global");
        if (global != null) {
            effective.putAll(global);
        }
        // [install] is command-scoped and overrides [global]
        Map<String, String> install = sections.get("install");
        if (install != null) {
            effective.putAll(install);
        }
        return effective;
    }

    private static int separatorIndex(String line) {
        int eq = line.indexOf('=');
        int colon = line.indexOf(':');
        if (eq < 0) {
            return colon;
        }
        return colon < 0 ? eq : Math.min(eq, colon);
    }

    static List<String> splitWhitespace(String value) {
        List<String> result = new ArrayList<>();
        for (String part : value.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
}
