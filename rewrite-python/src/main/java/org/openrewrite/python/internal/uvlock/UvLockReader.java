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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses uv.lock text into the {@link UvLock} model. uv.lock is machine-emitted TOML with a
 * fixed shape, so this is a purpose-built parser over that shape rather than a full TOML LST;
 * anything outside the cataloged format throws {@link UvLockFormatException} so that a
 * re-emission can never silently drop data.
 */
public final class UvLockReader {

    private UvLockReader() {
    }

    public static UvLock parse(String content) {
        String[] lines = content.split("\n", -1);
        int[] pos = {0};

        Map<String, Object> header = readKeyValues(lines, pos);
        Integer version = null;
        Integer revision = null;
        String requiresPython = null;
        List<String> resolutionMarkers = null;
        for (Map.Entry<String, Object> e : header.entrySet()) {
            String key = e.getKey();
            if ("version".equals(key)) {
                version = intValue(key, e.getValue());
            } else if ("revision".equals(key)) {
                revision = intValue(key, e.getValue());
            } else if ("requires-python".equals(key)) {
                requiresPython = stringValue(key, e.getValue());
            } else if ("resolution-markers".equals(key)) {
                resolutionMarkers = stringList(key, e.getValue());
            } else {
                throw new UvLockFormatException("Unrecognized top-level key: " + key);
            }
        }
        if (version == null) {
            throw new UvLockFormatException("uv.lock is missing the version key");
        }
        if (version != 1) {
            throw new UvLockFormatException("Unrecognized uv.lock version: " + version);
        }
        if (revision != null && (revision < 1 || revision > 3)) {
            throw new UvLockFormatException("Unrecognized uv.lock revision: " + revision);
        }

        UvLockOptions options = null;
        UvLockManifest manifest = null;
        List<RawPackage> rawPackages = new ArrayList<>();

        while (true) {
            String headerLine = nextTableHeader(lines, pos);
            if (headerLine == null) {
                break;
            }
            if ("[[package]]".equals(headerLine)) {
                RawPackage raw = new RawPackage();
                raw.keys = readKeyValues(lines, pos);
                rawPackages.add(raw);
            } else if ("[options]".equals(headerLine)) {
                if (options != null || !rawPackages.isEmpty()) {
                    throw new UvLockFormatException("Unexpected [options] table placement");
                }
                options = toOptions(readKeyValues(lines, pos));
            } else if ("[manifest]".equals(headerLine)) {
                if (manifest != null || !rawPackages.isEmpty()) {
                    throw new UvLockFormatException("Unexpected [manifest] table placement");
                }
                manifest = toManifest(readKeyValues(lines, pos));
            } else if ("[package.optional-dependencies]".equals(headerLine)) {
                lastPackage(rawPackages, headerLine).optionalDependencies = readKeyValues(lines, pos);
            } else if ("[package.dev-dependencies]".equals(headerLine)) {
                lastPackage(rawPackages, headerLine).devDependencies = readKeyValues(lines, pos);
            } else if ("[package.metadata]".equals(headerLine)) {
                lastPackage(rawPackages, headerLine).metadata = readKeyValues(lines, pos);
            } else if ("[package.metadata.requires-dev]".equals(headerLine)) {
                lastPackage(rawPackages, headerLine).requiresDev = readKeyValues(lines, pos);
            } else {
                throw new UvLockFormatException("Unrecognized table: " + headerLine);
            }
        }

        List<UvLockPackage> packages = new ArrayList<>(rawPackages.size());
        for (RawPackage raw : rawPackages) {
            packages.add(toPackage(raw));
        }
        return new UvLock(version, revision, requiresPython, resolutionMarkers, options, manifest, packages);
    }

    private static final class RawPackage {
        Map<String, Object> keys = new LinkedHashMap<>();
        @Nullable Map<String, Object> optionalDependencies;
        @Nullable Map<String, Object> devDependencies;
        @Nullable Map<String, Object> metadata;
        @Nullable Map<String, Object> requiresDev;
    }

    private static RawPackage lastPackage(List<RawPackage> rawPackages, String headerLine) {
        if (rawPackages.isEmpty()) {
            throw new UvLockFormatException(headerLine + " table before any [[package]]");
        }
        return rawPackages.get(rawPackages.size() - 1);
    }

    /**
     * Advance past blank lines to the next table header, or null at EOF.
     */
    private static @Nullable String nextTableHeader(String[] lines, int[] pos) {
        while (pos[0] < lines.length) {
            String line = lines[pos[0]];
            if (line.trim().isEmpty()) {
                pos[0]++;
                continue;
            }
            if (line.startsWith("[[") || (line.startsWith("[") && line.endsWith("]"))) {
                pos[0]++;
                return line.trim();
            }
            throw new UvLockFormatException("Expected a table header but found: " + line);
        }
        return null;
    }

    /**
     * Read `key = value` lines until the next table header or EOF, joining
     * continuation lines of multiline arrays.
     */
    private static Map<String, Object> readKeyValues(String[] lines, int[] pos) {
        Map<String, Object> result = new LinkedHashMap<>();
        while (pos[0] < lines.length) {
            String line = lines[pos[0]];
            if (line.trim().isEmpty()) {
                // A blank line only ever precedes another table header or EOF
                break;
            }
            if (line.startsWith("[")) {
                break;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new UvLockFormatException("Expected 'key = value' but found: " + line);
            }
            String key = line.substring(0, eq).trim();
            if (key.isEmpty() || key.startsWith("\"")) {
                throw new UvLockFormatException("Unsupported key syntax: " + line);
            }
            StringBuilder valueText = new StringBuilder(line.substring(eq + 1));
            pos[0]++;
            while (!isComplete(valueText)) {
                if (pos[0] >= lines.length) {
                    throw new UvLockFormatException("Unterminated value for key: " + key);
                }
                valueText.append('\n').append(lines[pos[0]]);
                pos[0]++;
            }
            if (result.put(key, parseCompleteValue(key, valueText.toString())) != null) {
                throw new UvLockFormatException("Duplicate key: " + key);
            }
        }
        return result;
    }

    /**
     * Whether brackets/braces are balanced outside of strings, i.e. the value ends on this line.
     */
    private static boolean isComplete(CharSequence s) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') {
                    throw new UvLockFormatException("Escape sequences are not supported in uv.lock strings");
                }
                if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                depth--;
            }
        }
        return depth <= 0 && !inString;
    }

    private static Object parseCompleteValue(String key, String text) {
        int[] p = {0};
        Object value = parseValue(text, p);
        skipWhitespace(text, p);
        if (p[0] != text.length()) {
            throw new UvLockFormatException("Trailing content after value for key " + key + ": " + text.substring(p[0]));
        }
        return value;
    }

    private static Object parseValue(String s, int[] p) {
        skipWhitespace(s, p);
        if (p[0] >= s.length()) {
            throw new UvLockFormatException("Unexpected end of value");
        }
        char c = s.charAt(p[0]);
        if (c == '"') {
            return parseString(s, p);
        }
        if (c == '[') {
            return parseArray(s, p);
        }
        if (c == '{') {
            return parseInlineTable(s, p);
        }
        if (c >= '0' && c <= '9') {
            return parseInteger(s, p);
        }
        throw new UvLockFormatException("Unsupported value syntax at: " + s.substring(p[0]));
    }

    private static String parseString(String s, int[] p) {
        p[0]++; // opening quote
        int start = p[0];
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]);
            if (c == '\\') {
                throw new UvLockFormatException("Escape sequences are not supported in uv.lock strings");
            }
            if (c == '"') {
                String value = s.substring(start, p[0]);
                p[0]++;
                return value;
            }
            p[0]++;
        }
        throw new UvLockFormatException("Unterminated string");
    }

    private static Long parseInteger(String s, int[] p) {
        int start = p[0];
        while (p[0] < s.length() && s.charAt(p[0]) >= '0' && s.charAt(p[0]) <= '9') {
            p[0]++;
        }
        return Long.parseLong(s.substring(start, p[0]));
    }

    private static List<Object> parseArray(String s, int[] p) {
        p[0]++; // '['
        List<Object> values = new ArrayList<>();
        while (true) {
            skipWhitespace(s, p);
            if (p[0] >= s.length()) {
                throw new UvLockFormatException("Unterminated array");
            }
            if (s.charAt(p[0]) == ']') {
                p[0]++;
                return values;
            }
            values.add(parseValue(s, p));
            skipWhitespace(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ',') {
                p[0]++;
            } else if (p[0] >= s.length() || s.charAt(p[0]) != ']') {
                throw new UvLockFormatException("Expected ',' or ']' in array");
            }
        }
    }

    private static Map<String, Object> parseInlineTable(String s, int[] p) {
        p[0]++; // '{'
        Map<String, Object> table = new LinkedHashMap<>();
        skipWhitespace(s, p);
        if (p[0] < s.length() && s.charAt(p[0]) == '}') {
            p[0]++;
            return table;
        }
        while (true) {
            skipWhitespace(s, p);
            int keyStart = p[0];
            while (p[0] < s.length() && isBareKeyChar(s.charAt(p[0]))) {
                p[0]++;
            }
            String key = s.substring(keyStart, p[0]);
            if (key.isEmpty()) {
                throw new UvLockFormatException("Expected a bare key in inline table");
            }
            skipWhitespace(s, p);
            if (p[0] >= s.length() || s.charAt(p[0]) != '=') {
                throw new UvLockFormatException("Expected '=' after inline table key " + key);
            }
            p[0]++;
            if (table.put(key, parseValue(s, p)) != null) {
                throw new UvLockFormatException("Duplicate inline table key: " + key);
            }
            skipWhitespace(s, p);
            if (p[0] >= s.length()) {
                throw new UvLockFormatException("Unterminated inline table");
            }
            char c = s.charAt(p[0]);
            if (c == '}') {
                p[0]++;
                return table;
            }
            if (c != ',') {
                throw new UvLockFormatException("Expected ',' or '}' in inline table");
            }
            p[0]++;
        }
    }

    private static boolean isBareKeyChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
    }

    private static void skipWhitespace(String s, int[] p) {
        while (p[0] < s.length() && (s.charAt(p[0]) == ' ' || s.charAt(p[0]) == '\t' || s.charAt(p[0]) == '\n')) {
            p[0]++;
        }
    }

    // ---- conversion of generic structures to the typed model ----

    private static UvLockOptions toOptions(Map<String, Object> map) {
        String excludeNewer = null;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if ("exclude-newer".equals(e.getKey())) {
                excludeNewer = stringValue(e.getKey(), e.getValue());
            } else {
                throw new UvLockFormatException("Unrecognized [options] key: " + e.getKey());
            }
        }
        return new UvLockOptions(excludeNewer);
    }

    private static UvLockManifest toManifest(Map<String, Object> map) {
        List<String> members = null;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if ("members".equals(e.getKey())) {
                members = stringList(e.getKey(), e.getValue());
            } else {
                throw new UvLockFormatException("Unrecognized [manifest] key: " + e.getKey());
            }
        }
        if (members == null) {
            throw new UvLockFormatException("[manifest] table is missing members");
        }
        return new UvLockManifest(members);
    }

    private static UvLockPackage toPackage(RawPackage raw) {
        String name = null;
        String version = null;
        UvLockSource source = null;
        List<String> resolutionMarkers = null;
        List<UvLockDependency> dependencies = null;
        UvLockArtifact sdist = null;
        List<UvLockArtifact> wheels = null;
        for (Map.Entry<String, Object> e : raw.keys.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if ("name".equals(key)) {
                name = stringValue(key, v);
            } else if ("version".equals(key)) {
                version = stringValue(key, v);
            } else if ("source".equals(key)) {
                source = toSource(v);
            } else if ("resolution-markers".equals(key)) {
                resolutionMarkers = stringList(key, v);
            } else if ("dependencies".equals(key)) {
                dependencies = toDependencies(key, v);
            } else if ("sdist".equals(key)) {
                sdist = toArtifact(tableValue(key, v));
            } else if ("wheels".equals(key)) {
                wheels = new ArrayList<>();
                for (Object item : listValue(key, v)) {
                    wheels.add(toArtifact(tableValue(key, item)));
                }
            } else {
                throw new UvLockFormatException("Unrecognized package key: " + key);
            }
        }
        if (name == null || version == null || source == null) {
            throw new UvLockFormatException("Package entry is missing name, version or source");
        }

        Map<String, List<UvLockDependency>> optionalDependencies = toDependencyGroups(raw.optionalDependencies);
        Map<String, List<UvLockDependency>> devDependencies = toDependencyGroups(raw.devDependencies);
        UvLockMetadata metadata = toMetadata(raw);

        return new UvLockPackage(name, version, source, resolutionMarkers, dependencies, sdist, wheels,
                optionalDependencies, devDependencies, metadata);
    }

    private static @Nullable Map<String, List<UvLockDependency>> toDependencyGroups(@Nullable Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            groups.put(e.getKey(), toDependencies(e.getKey(), e.getValue()));
        }
        return groups;
    }

    private static @Nullable UvLockMetadata toMetadata(RawPackage raw) {
        if (raw.metadata == null && raw.requiresDev == null) {
            return null;
        }
        List<UvLockRequirement> requiresDist = null;
        List<String> providesExtras = null;
        if (raw.metadata != null) {
            for (Map.Entry<String, Object> e : raw.metadata.entrySet()) {
                String key = e.getKey();
                if ("requires-dist".equals(key)) {
                    requiresDist = toRequirements(key, e.getValue());
                } else if ("provides-extras".equals(key)) {
                    providesExtras = stringList(key, e.getValue());
                } else {
                    throw new UvLockFormatException("Unrecognized [package.metadata] key: " + key);
                }
            }
        }
        Map<String, List<UvLockRequirement>> requiresDev = null;
        if (raw.requiresDev != null) {
            requiresDev = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.requiresDev.entrySet()) {
                requiresDev.put(e.getKey(), toRequirements(e.getKey(), e.getValue()));
            }
        }
        return new UvLockMetadata(requiresDist, providesExtras, requiresDev);
    }

    private static UvLockSource toSource(Object v) {
        Map<String, Object> table = tableValue("source", v);
        if (table.size() != 1) {
            throw new UvLockFormatException("Expected exactly one key in source table, found: " + table.keySet());
        }
        Map.Entry<String, Object> e = table.entrySet().iterator().next();
        return new UvLockSource(UvLockSource.Type.fromKey(e.getKey()), stringValue(e.getKey(), e.getValue()));
    }

    private static List<UvLockDependency> toDependencies(String key, Object v) {
        List<UvLockDependency> dependencies = new ArrayList<>();
        for (Object item : listValue(key, v)) {
            dependencies.add(toDependency(tableValue(key, item)));
        }
        return dependencies;
    }

    private static UvLockDependency toDependency(Map<String, Object> table) {
        String name = null;
        String version = null;
        UvLockSource source = null;
        List<String> extra = null;
        String marker = null;
        for (Map.Entry<String, Object> e : table.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if ("name".equals(key)) {
                name = stringValue(key, v);
            } else if ("version".equals(key)) {
                version = stringValue(key, v);
            } else if ("source".equals(key)) {
                source = toSource(v);
            } else if ("extra".equals(key)) {
                extra = stringList(key, v);
            } else if ("marker".equals(key)) {
                marker = stringValue(key, v);
            } else {
                throw new UvLockFormatException("Unrecognized dependency key: " + key);
            }
        }
        if (name == null) {
            throw new UvLockFormatException("Dependency entry is missing name");
        }
        return new UvLockDependency(name, version, source, extra, marker);
    }

    private static UvLockArtifact toArtifact(Map<String, Object> table) {
        String url = null;
        String path = null;
        String hash = null;
        Long size = null;
        String uploadTime = null;
        for (Map.Entry<String, Object> e : table.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if ("url".equals(key)) {
                url = stringValue(key, v);
            } else if ("path".equals(key)) {
                path = stringValue(key, v);
            } else if ("hash".equals(key)) {
                hash = stringValue(key, v);
            } else if ("size".equals(key)) {
                size = longValue(key, v);
            } else if ("upload-time".equals(key)) {
                uploadTime = stringValue(key, v);
            } else {
                throw new UvLockFormatException("Unrecognized artifact key: " + key);
            }
        }
        if (url == null && path == null) {
            throw new UvLockFormatException("Artifact entry has neither url nor path");
        }
        return new UvLockArtifact(url, path, hash, size, uploadTime);
    }

    private static List<UvLockRequirement> toRequirements(String key, Object v) {
        List<UvLockRequirement> requirements = new ArrayList<>();
        for (Object item : listValue(key, v)) {
            requirements.add(toRequirement(tableValue(key, item)));
        }
        return requirements;
    }

    private static UvLockRequirement toRequirement(Map<String, Object> table) {
        String name = null;
        List<String> extras = null;
        String editable = null;
        String marker = null;
        String specifier = null;
        String index = null;
        for (Map.Entry<String, Object> e : table.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if ("name".equals(key)) {
                name = stringValue(key, v);
            } else if ("extras".equals(key)) {
                extras = stringList(key, v);
            } else if ("editable".equals(key)) {
                editable = stringValue(key, v);
            } else if ("marker".equals(key)) {
                marker = stringValue(key, v);
            } else if ("specifier".equals(key)) {
                specifier = stringValue(key, v);
            } else if ("index".equals(key)) {
                index = stringValue(key, v);
            } else {
                throw new UvLockFormatException("Unrecognized requirement key: " + key);
            }
        }
        if (name == null) {
            throw new UvLockFormatException("Requirement entry is missing name");
        }
        return new UvLockRequirement(name, extras, editable, marker, specifier, index);
    }

    // ---- typed accessors over the generic parse ----

    private static String stringValue(String key, Object v) {
        if (!(v instanceof String)) {
            throw new UvLockFormatException("Expected a string for key: " + key);
        }
        return (String) v;
    }

    private static long longValue(String key, Object v) {
        if (!(v instanceof Long)) {
            throw new UvLockFormatException("Expected an integer for key: " + key);
        }
        return (Long) v;
    }

    private static int intValue(String key, Object v) {
        return (int) longValue(key, v);
    }

    private static List<Object> listValue(String key, Object v) {
        if (!(v instanceof List)) {
            throw new UvLockFormatException("Expected an array for key: " + key);
        }
        //noinspection unchecked
        return (List<Object>) v;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> tableValue(String key, Object v) {
        if (!(v instanceof Map)) {
            throw new UvLockFormatException("Expected an inline table for key: " + key);
        }
        return (Map<String, Object>) v;
    }

    private static List<String> stringList(String key, Object v) {
        List<String> strings = new ArrayList<>();
        for (Object item : listValue(key, v)) {
            strings.add(stringValue(key, item));
        }
        return strings;
    }
}
