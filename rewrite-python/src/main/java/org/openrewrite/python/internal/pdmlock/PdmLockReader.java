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
package org.openrewrite.python.internal.pdmlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.MachineTomlException;
import org.openrewrite.python.internal.MachineTomlReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses pdm.lock text into the {@link PdmLock} model. pdm.lock is machine-emitted TOML with a fixed
 * shape, so this reads the {@link MachineTomlReader} value layer and maps it to the typed model;
 * anything uncataloged throws {@link PdmLockFormatException} so a re-emission can never silently
 * drop data.
 */
public final class PdmLockReader {

    private PdmLockReader() {
    }

    public static PdmLock parse(String content) {
        try {
            return doParse(content);
        } catch (MachineTomlException e) {
            throw new PdmLockFormatException(e.getMessage());
        }
    }

    private static PdmLock doParse(String content) {
        String[] lines = content.split("\n", -1);
        int[] pos = {0};

        String header = MachineTomlReader.readHeader(lines, pos);
        if (header == null) {
            throw new PdmLockFormatException("pdm.lock is missing its @generated header comment");
        }

        Map<String, Object> metadata = null;
        List<Map<String, String>> targets = new ArrayList<>();
        List<PdmLockPackage> packages = new ArrayList<>();

        while (true) {
            String headerLine = MachineTomlReader.nextTableHeader(lines, pos);
            if (headerLine == null) {
                break;
            }
            if ("[metadata]".equals(headerLine)) {
                if (metadata != null) {
                    throw new PdmLockFormatException("Duplicate [metadata] table");
                }
                metadata = MachineTomlReader.readKeyValues(lines, pos);
            } else if ("[[metadata.targets]]".equals(headerLine)) {
                targets.add(toStringMap(MachineTomlReader.readKeyValues(lines, pos), "[[metadata.targets]]"));
            } else if ("[[package]]".equals(headerLine)) {
                packages.add(toPackage(MachineTomlReader.readKeyValues(lines, pos)));
            } else {
                throw new PdmLockFormatException("Unrecognized table: " + headerLine);
            }
        }
        if (metadata == null) {
            throw new PdmLockFormatException("pdm.lock is missing the [metadata] table");
        }

        List<String> groups = requireStringList(metadata, "groups", "[metadata]");
        List<String> strategy = requireStringList(metadata, "strategy", "[metadata]");
        String lockVersion = requireString(metadata, "lock_version", "[metadata]");
        if (!"4.5.0".equals(lockVersion)) {
            throw new PdmLockFormatException("Unsupported pdm.lock lock_version: " + lockVersion);
        }
        String contentHash = requireString(metadata, "content_hash", "[metadata]");
        for (String key : metadata.keySet()) {
            switch (key) {
                case "groups":
                case "strategy":
                case "lock_version":
                case "content_hash":
                    break;
                default:
                    throw new PdmLockFormatException("Unrecognized [metadata] key: " + key);
            }
        }
        return new PdmLock(header, groups, strategy, targets, lockVersion, contentHash, packages);
    }

    private static PdmLockPackage toPackage(Map<String, Object> b) {
        String name = requireString(b, "name", "[[package]]");
        String version = requireString(b, "version", name);
        List<String> extras = b.containsKey("extras") ? stringList(b.get("extras"), "extras", name) : null;
        String requiresPython = optString(b, "requires_python", name);
        Boolean editable = b.containsKey("editable") ? bool(b.get("editable"), "editable", name) : null;
        String subdirectory = optString(b, "subdirectory", name);

        String vcs = null;
        String vcsUrl = null;
        for (String kind : new String[]{"git", "hg", "bzr", "svn"}) {
            if (b.containsKey(kind)) {
                vcs = kind;
                vcsUrl = str(b.get(kind), kind, name);
            }
        }
        String ref = optString(b, "ref", name);
        String revision = optString(b, "revision", name);
        String path = optString(b, "path", name);
        String url = optString(b, "url", name);

        String summary = requireString(b, "summary", name);
        List<String> groups = b.containsKey("groups") ? stringList(b.get("groups"), "groups", name) : null;
        String marker = optString(b, "marker", name);
        List<String> dependencies = b.containsKey("dependencies") ?
                stringList(b.get("dependencies"), "dependencies", name) : null;

        List<PdmLockFile> files = null;
        if (b.containsKey("files")) {
            files = new ArrayList<>();
            for (Object item : list(b.get("files"), "files", name)) {
                files.add(toFile(asTable(item, name + " files"), name));
            }
        }

        for (String key : b.keySet()) {
            switch (key) {
                case "name":
                case "version":
                case "extras":
                case "requires_python":
                case "editable":
                case "subdirectory":
                case "git":
                case "hg":
                case "bzr":
                case "svn":
                case "ref":
                case "revision":
                case "path":
                case "url":
                case "summary":
                case "groups":
                case "marker":
                case "dependencies":
                case "files":
                    break;
                default:
                    throw new PdmLockFormatException("Unrecognized package key '" + key + "' in " + name);
            }
        }

        return new PdmLockPackage(name, version, extras, requiresPython, editable, subdirectory,
                vcs, vcsUrl, ref, revision, path, url, summary, groups, marker, dependencies, files);
    }

    private static PdmLockFile toFile(Map<String, Object> table, String pkg) {
        String file = null;
        String url = null;
        String hash = null;
        for (Map.Entry<String, Object> e : table.entrySet()) {
            switch (e.getKey()) {
                case "file":
                    file = str(e.getValue(), "file", pkg);
                    break;
                case "url":
                    url = str(e.getValue(), "url", pkg);
                    break;
                case "hash":
                    hash = str(e.getValue(), "hash", pkg);
                    break;
                default:
                    throw new PdmLockFormatException("Unrecognized file key '" + e.getKey() + "' in " + pkg);
            }
        }
        if (hash == null || (file == null) == (url == null)) {
            throw new PdmLockFormatException("File entry needs a hash and exactly one of file/url in " + pkg);
        }
        return new PdmLockFile(file, url, hash);
    }

    private static Map<String, String> toStringMap(Map<String, Object> map, String context) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            result.put(e.getKey(), str(e.getValue(), e.getKey(), context));
        }
        return result;
    }

    private static String str(Object v, String key, String context) {
        if (!(v instanceof String)) {
            throw new PdmLockFormatException("Expected a string for key '" + key + "' in " + context);
        }
        return (String) v;
    }

    private static boolean bool(Object v, String key, String context) {
        if (!(v instanceof Boolean)) {
            throw new PdmLockFormatException("Expected a boolean for key '" + key + "' in " + context);
        }
        return (Boolean) v;
    }

    private static List<String> stringList(Object v, String key, String context) {
        if (!(v instanceof List)) {
            throw new PdmLockFormatException("Expected an array for key '" + key + "' in " + context);
        }
        List<String> strings = new ArrayList<>();
        for (Object item : (List<?>) v) {
            strings.add(str(item, key, context));
        }
        return strings;
    }

    private static List<Object> list(Object v, String key, String context) {
        if (!(v instanceof List)) {
            throw new PdmLockFormatException("Expected an array for key '" + key + "' in " + context);
        }
        //noinspection unchecked
        return (List<Object>) v;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asTable(Object v, String context) {
        if (!(v instanceof Map)) {
            throw new PdmLockFormatException("Expected an inline table in " + context);
        }
        return (Map<String, Object>) v;
    }

    private static String requireString(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (v == null) {
            throw new PdmLockFormatException("Missing key '" + key + "' in " + context);
        }
        return str(v, key, context);
    }

    private static @Nullable String optString(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        return v == null ? null : str(v, key, context);
    }

    private static List<String> requireStringList(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (v == null) {
            throw new PdmLockFormatException("Missing key '" + key + "' in " + context);
        }
        return stringList(v, key, context);
    }
}
