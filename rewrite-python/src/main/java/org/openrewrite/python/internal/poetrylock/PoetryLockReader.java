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
package org.openrewrite.python.internal.poetrylock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.MachineTomlException;
import org.openrewrite.python.internal.MachineTomlReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses poetry.lock text into the {@link PoetryLock} model. poetry.lock is machine-emitted TOML
 * with a fixed shape, so this reads the {@link MachineTomlReader} value layer and maps it to the
 * typed model; anything uncataloged throws {@link PoetryLockFormatException} so a re-emission can
 * never silently drop data.
 */
public final class PoetryLockReader {

    private PoetryLockReader() {
    }

    public static PoetryLock parse(String content) {
        try {
            return doParse(content);
        } catch (MachineTomlException e) {
            throw new PoetryLockFormatException(e.getMessage());
        }
    }

    private static PoetryLock doParse(String content) {
        String[] lines = content.split("\n", -1);
        int[] pos = {0};

        String header = MachineTomlReader.readHeader(lines, pos);
        if (header == null) {
            throw new PoetryLockFormatException("poetry.lock is missing its @generated header comment");
        }

        List<PoetryLockPackage> packages = new ArrayList<>();
        Map<String, List<String>> rootExtras = null;
        Map<String, Object> metadata = null;
        RawPackage current = null;

        while (true) {
            String headerLine = MachineTomlReader.nextTableHeader(lines, pos);
            if (headerLine == null) {
                break;
            }
            if ("[[package]]".equals(headerLine)) {
                if (current != null) {
                    packages.add(toPackage(current));
                }
                current = new RawPackage();
                current.body = MachineTomlReader.readKeyValues(lines, pos);
            } else if ("[package.dependencies]".equals(headerLine)) {
                lastPackage(current, headerLine).dependencies = MachineTomlReader.readKeyValues(lines, pos);
            } else if ("[package.extras]".equals(headerLine)) {
                lastPackage(current, headerLine).extras = MachineTomlReader.readKeyValues(lines, pos);
            } else if ("[package.source]".equals(headerLine)) {
                lastPackage(current, headerLine).source = MachineTomlReader.readKeyValues(lines, pos);
            } else if ("[extras]".equals(headerLine)) {
                rootExtras = toStringListMap(MachineTomlReader.readKeyValues(lines, pos));
            } else if ("[metadata]".equals(headerLine)) {
                metadata = MachineTomlReader.readKeyValues(lines, pos);
            } else {
                throw new PoetryLockFormatException("Unrecognized table: " + headerLine);
            }
        }
        if (current != null) {
            packages.add(toPackage(current));
        }
        if (metadata == null) {
            throw new PoetryLockFormatException("poetry.lock is missing the [metadata] table");
        }

        String lockVersion = requireString(metadata, "lock-version", "[metadata]");
        if (!"2.1".equals(lockVersion)) {
            throw new PoetryLockFormatException("Unsupported poetry.lock lock-version: " + lockVersion);
        }
        String pythonVersions = optString(metadata, "python-versions");
        String contentHash = requireString(metadata, "content-hash", "[metadata]");
        for (String key : metadata.keySet()) {
            if (!"lock-version".equals(key) && !"python-versions".equals(key) && !"content-hash".equals(key)) {
                throw new PoetryLockFormatException("Unrecognized [metadata] key: " + key);
            }
        }
        return new PoetryLock(header, packages, rootExtras, lockVersion, pythonVersions, contentHash);
    }

    private static final class RawPackage {
        Map<String, Object> body = new LinkedHashMap<>();
        @Nullable Map<String, Object> dependencies;
        @Nullable Map<String, Object> extras;
        @Nullable Map<String, Object> source;
    }

    private static RawPackage lastPackage(@Nullable RawPackage current, String headerLine) {
        if (current == null) {
            throw new PoetryLockFormatException(headerLine + " table before any [[package]]");
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static PoetryLockPackage toPackage(RawPackage raw) {
        Map<String, Object> b = raw.body;
        String name = requireString(b, "name", "[[package]]");
        String version = requireString(b, "version", name);
        String description = requireString(b, "description", name);
        boolean optional = requireBool(b, "optional", name);
        String pythonVersions = requireString(b, "python-versions", name);
        List<String> groups = requireStringList(b, "groups", name);

        String marker = null;
        Map<String, String> groupMarkers = null;
        Object markerValue = b.get("markers");
        if (markerValue instanceof String) {
            marker = (String) markerValue;
        } else if (markerValue instanceof Map) {
            groupMarkers = toStringMap((Map<String, Object>) markerValue);
        } else if (markerValue != null) {
            throw new PoetryLockFormatException("Unexpected markers value for package " + name);
        }

        List<PoetryLockFile> files = new ArrayList<>();
        for (Object item : requireList(b, "files", name)) {
            Map<String, Object> table = asTable(item, name + " files");
            files.add(new PoetryLockFile(requireString(table, "file", name),
                    requireString(table, "hash", name)));
        }

        Boolean develop = null;
        Object developValue = b.get("develop");
        if (developValue instanceof Boolean) {
            develop = (Boolean) developValue;
        } else if (developValue != null) {
            throw new PoetryLockFormatException("Unexpected develop value for package " + name);
        }

        for (String key : b.keySet()) {
            switch (key) {
                case "name":
                case "version":
                case "description":
                case "optional":
                case "python-versions":
                case "groups":
                case "markers":
                case "files":
                case "develop":
                    break;
                default:
                    throw new PoetryLockFormatException("Unrecognized package key '" + key + "' in " + name);
            }
        }

        List<PoetryLockDependency> dependencies = raw.dependencies == null ? null :
                toDependencies(raw.dependencies, name);
        Map<String, List<String>> extras = raw.extras == null ? null : toStringListMap(raw.extras);
        PoetryLockSource source = raw.source == null ? null : toSource(raw.source, name);

        return new PoetryLockPackage(name, version, description, optional, pythonVersions, groups,
                marker, groupMarkers, files, develop, dependencies, extras, source);
    }

    private static List<PoetryLockDependency> toDependencies(Map<String, Object> table, String pkg) {
        List<PoetryLockDependency> deps = new ArrayList<>();
        for (Map.Entry<String, Object> e : table.entrySet()) {
            List<PoetryLockConstraint> constraints = new ArrayList<>();
            Object v = e.getValue();
            if (v instanceof List) {
                for (Object item : (List<?>) v) {
                    constraints.add(toConstraint(item, pkg));
                }
            } else {
                constraints.add(toConstraint(v, pkg));
            }
            deps.add(new PoetryLockDependency(e.getKey(), constraints));
        }
        return deps;
    }

    @SuppressWarnings("unchecked")
    private static PoetryLockConstraint toConstraint(Object value, String pkg) {
        if (value instanceof String) {
            return PoetryLockConstraint.builder().version((String) value).build();
        }
        if (!(value instanceof Map)) {
            throw new PoetryLockFormatException("Unexpected dependency constraint in package " + pkg);
        }
        Map<String, Object> table = (Map<String, Object>) value;
        PoetryLockConstraint.PoetryLockConstraintBuilder cb = PoetryLockConstraint.builder();
        for (Map.Entry<String, Object> e : table.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            switch (key) {
                case "version":
                    cb.version(str(v, key, pkg));
                    break;
                case "path":
                    cb.path(str(v, key, pkg));
                    break;
                case "url":
                    cb.url(str(v, key, pkg));
                    break;
                case "git":
                case "hg":
                case "bzr":
                case "svn":
                    cb.vcs(key).vcsUrl(str(v, key, pkg));
                    break;
                case "branch":
                    cb.branch(str(v, key, pkg));
                    break;
                case "tag":
                    cb.tag(str(v, key, pkg));
                    break;
                case "rev":
                    cb.rev(str(v, key, pkg));
                    break;
                case "subdirectory":
                    cb.subdirectory(str(v, key, pkg));
                    break;
                case "extras":
                    cb.extras(stringList(v, key, pkg));
                    break;
                case "optional":
                    cb.optional(bool(v, key, pkg));
                    break;
                case "markers":
                    cb.markers(str(v, key, pkg));
                    break;
                case "develop":
                    cb.develop(bool(v, key, pkg));
                    break;
                default:
                    throw new PoetryLockFormatException("Unrecognized dependency constraint key '" + key + "' in " + pkg);
            }
        }
        return cb.build();
    }

    private static PoetryLockSource toSource(Map<String, Object> table, String pkg) {
        String type = null;
        String url = null;
        String reference = null;
        String resolvedReference = null;
        String subdirectory = null;
        for (Map.Entry<String, Object> e : table.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            switch (key) {
                case "type":
                    type = str(v, key, pkg);
                    break;
                case "url":
                    url = str(v, key, pkg);
                    break;
                case "reference":
                    reference = str(v, key, pkg);
                    break;
                case "resolved_reference":
                    resolvedReference = str(v, key, pkg);
                    break;
                case "subdirectory":
                    subdirectory = str(v, key, pkg);
                    break;
                default:
                    throw new PoetryLockFormatException("Unrecognized [package.source] key '" + key + "' in " + pkg);
            }
        }
        if (url == null) {
            throw new PoetryLockFormatException("[package.source] is missing url in " + pkg);
        }
        return new PoetryLockSource(type, url, reference, resolvedReference, subdirectory);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> toStringListMap(Map<String, Object> map) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            result.put(e.getKey(), stringList(e.getValue(), e.getKey(), "table"));
        }
        return result;
    }

    private static Map<String, String> toStringMap(Map<String, Object> map) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            result.put(e.getKey(), str(e.getValue(), e.getKey(), "markers"));
        }
        return result;
    }

    private static String str(Object v, String key, String context) {
        if (!(v instanceof String)) {
            throw new PoetryLockFormatException("Expected a string for key '" + key + "' in " + context);
        }
        return (String) v;
    }

    private static boolean bool(Object v, String key, String context) {
        if (!(v instanceof Boolean)) {
            throw new PoetryLockFormatException("Expected a boolean for key '" + key + "' in " + context);
        }
        return (Boolean) v;
    }

    private static List<String> stringList(Object v, String key, String context) {
        if (!(v instanceof List)) {
            throw new PoetryLockFormatException("Expected an array for key '" + key + "' in " + context);
        }
        List<String> strings = new ArrayList<>();
        for (Object item : (List<?>) v) {
            strings.add(str(item, key, context));
        }
        return strings;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asTable(Object v, String context) {
        if (!(v instanceof Map)) {
            throw new PoetryLockFormatException("Expected an inline table in " + context);
        }
        return (Map<String, Object>) v;
    }

    private static String requireString(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (v == null) {
            throw new PoetryLockFormatException("Missing key '" + key + "' in " + context);
        }
        return str(v, key, context);
    }

    private static @Nullable String optString(Map<String, Object> table, String key) {
        Object v = table.get(key);
        return v == null ? null : str(v, key, "table");
    }

    private static boolean requireBool(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (v == null) {
            throw new PoetryLockFormatException("Missing key '" + key + "' in " + context);
        }
        return bool(v, key, context);
    }

    private static List<String> requireStringList(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (v == null) {
            throw new PoetryLockFormatException("Missing key '" + key + "' in " + context);
        }
        return stringList(v, key, context);
    }

    private static List<Object> requireList(Map<String, Object> table, String key, String context) {
        Object v = table.get(key);
        if (!(v instanceof List)) {
            throw new PoetryLockFormatException("Expected an array for key '" + key + "' in " + context);
        }
        //noinspection unchecked
        return (List<Object>) v;
    }
}
