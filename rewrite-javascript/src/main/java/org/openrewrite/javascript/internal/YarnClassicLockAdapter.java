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
package org.openrewrite.javascript.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converts a yarn 1.x {@code yarn.lock} (custom non-YAML format) into the
 * npm {@code package-lock.json} v3 shape consumed by {@link LockFileParser}.
 * <p>
 * yarn classic blocks look like:
 * <pre>
 *   "lodash@^4.17.21", "lodash@^4.17.0":
 *     version "4.17.21"
 *     resolved "..."
 *     dependencies:
 *       foo "^2.0.0"
 * </pre>
 * Each block is parsed into one entry. The first occurrence of a given name
 * is emitted as a top-level npm v3 path ({@code node_modules/<name>}); later
 * occurrences (multi-version cases) get a synthetic nested path so they're
 * preserved in {@code all} but excluded from {@code topLevel}.
 */
public final class YarnClassicLockAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private YarnClassicLockAdapter() {}

    public static String toNpmV3(String yarnLockContent) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("lockfileVersion", 3);
        ObjectNode packages = root.putObject("packages");
        packages.putObject(""); // root placeholder

        Set<String> seen = new HashSet<>();
        int[] dupCounter = {0};
        Block current = null;
        boolean inDeps = false;

        for (String rawLine : yarnLockContent.split("\n")) {
            String line = stripCarriageReturn(rawLine);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = countLeadingSpaces(line);
            if (indent == 0 && trimmed.endsWith(":")) {
                if (current != null) {
                    emit(current, packages, seen, dupCounter);
                }
                current = parseBlockHeader(trimmed);
                inDeps = false;
            } else if (current != null && indent == 2) {
                if (trimmed.equals("dependencies:")) {
                    inDeps = true;
                } else {
                    inDeps = false;
                    KeyValue kv = parseFieldLine(trimmed);
                    if ("version".equals(kv.key)) {
                        current.version = kv.value;
                    }
                }
            } else if (current != null && indent == 4 && inDeps) {
                KeyValue kv = parseFieldLine(trimmed);
                current.deps.put(kv.key, kv.value);
            }
        }
        if (current != null) {
            emit(current, packages, seen, dupCounter);
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize npm v3: " + e.getMessage(), e);
        }
    }

    private static void emit(Block block, ObjectNode packages, Set<String> seen, int[] dupCounter) {
        if (block.name == null || block.version == null) {
            return; // malformed block, skip
        }
        String path = seen.add(block.name)
                ? "node_modules/" + block.name
                : "node_modules/__dup_" + (dupCounter[0]++) + "__/node_modules/" + block.name;
        ObjectNode entry = packages.putObject(path);
        entry.put("version", block.version);
        if (!block.deps.isEmpty()) {
            ObjectNode depsNode = entry.putObject("dependencies");
            block.deps.forEach(depsNode::put);
        }
    }

    private static Block parseBlockHeader(String headerLine) {
        // strip trailing ':'
        String keys = headerLine.substring(0, headerLine.length() - 1);
        // first key only (multi-key blocks share the same name); split on first comma
        int comma = keys.indexOf(',');
        String firstKey = (comma >= 0 ? keys.substring(0, comma) : keys).trim();
        firstKey = unquote(firstKey);
        // extract package name from "name@constraint" — handle scoped names starting with '@'
        int atIdx = firstKey.startsWith("@") ? firstKey.indexOf('@', 1) : firstKey.indexOf('@');
        String name = atIdx > 0 ? firstKey.substring(0, atIdx) : firstKey;
        return new Block(name);
    }

    private static KeyValue parseFieldLine(String line) {
        // Possible shapes:
        //   key "value"       (most common)
        //   "key" "value"     (scoped names in dependencies sub-block)
        //   key value         (rare, unquoted)
        int firstSpace = line.indexOf(' ');
        if (firstSpace < 0) {
            return new KeyValue(unquote(line), "");
        }
        String key = unquote(line.substring(0, firstSpace).trim());
        String value = unquote(line.substring(firstSpace + 1).trim());
        return new KeyValue(key, value);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static int countLeadingSpaces(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static String stripCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    private static final class Block {
        final String name;
        String version;
        final Map<String, String> deps = new LinkedHashMap<>();

        Block(String name) {
            this.name = name;
        }
    }

    private static final class KeyValue {
        final String key;
        final String value;

        KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
