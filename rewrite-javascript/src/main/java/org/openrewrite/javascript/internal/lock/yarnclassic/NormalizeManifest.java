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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.npm.PosixPath;
import org.openrewrite.semver.NodeSemver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of yarn src/util/normalize-manifest (index.js, validate.js, resolve-relative.js, and the parts of fix.js
 * that reach a lock or decide an install: the version cleaned, the name and version defaulted, an engines array
 * expanded, {@code bundledDependencies} renamed, each dependency map's {@code //} key dropped and empty values made
 * {@code ""}). The rest of fix.js (people, readme, license, bin, man, scripts) reads package files and never reaches
 * a lock.
 */
final class NormalizeManifest {

    private static final List<String> STRINGS = Arrays.asList("name", "version");
    private static final List<String> DEPENDENCY_KEYS = Arrays.asList("optionalDependencies", "dependencies",
            "devDependencies");
    private static final List<String> DEPENDENCY_TYPES = Arrays.asList("devDependencies", "dependencies",
            "optionalDependencies", "peerDependencies");
    private static final List<String> MANIFEST_FIELDS = Arrays.asList("resolutions", "devDependencies",
            "dependencies", "optionalDependencies", "peerDependencies");
    private static final Pattern ILLEGAL_NAME_CHARS = Pattern.compile("[/@" + Js.WHITESPACE_CLASS + "+%:]");
    private static final Pattern PATH_PREFIX = Pattern.compile("^\\.(/|$)");
    private static final Pattern ENGINES_SPLIT = Pattern.compile(" +");

    private NormalizeManifest() {
    }

    /**
     * Normalizes a manifest in place.
     *
     * @param moduleLoc      the package's directory, relative to the project's ({@code ""} the project itself)
     * @param lockfileFolder the directory of the lock, relative to the project's
     * @param warnings       where the warnings yarn prints are collected
     * @throws MessageError a name yarn rejects, or a name or version that is not a string.
     */
    static Manifest normalizeManifest(Manifest info, String moduleLoc, String lockfileFolder, boolean isRoot,
                                      boolean looseSemver, List<String> warnings) {
        Object name = info.getName();
        Object version = info.getVersion();
        String human = null;
        if (name instanceof String) {
            human = (String) name;
        }
        if (human != null && version instanceof String && !((String) version).isEmpty()) {
            human += "@" + version;
        }
        String prefix = human == null ? "" : human + ": ";

        fix(info, looseSemver);
        resolveRelative(info, moduleLoc, lockfileFolder);
        try {
            validate(info, isRoot, msg -> warnings.add(prefix + msg));
        } catch (MessageError err) {
            throw new MessageError(prefix + err.getMessage(), err.getCode());
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    static void fix(Manifest info, boolean looseSemver) {
        Object version = info.getVersion();
        if (version instanceof String) {
            String cleaned = NodeSemver.clean((String) version,
                    looseSemver ? YarnClassicOptions.LOOSE_SEMVER : YarnClassicOptions.SEMVER);
            info.put("version", cleaned != null && !cleaned.isEmpty() ? cleaned : version);
        }
        info.put("name", Js.truthy(info.getName()) ? info.getName() : "");
        info.put("version", Js.truthy(info.getVersion()) ? info.getVersion() : "");

        Object engines = info.get("engines");
        if (engines instanceof List) {
            Map<String, @Nullable Object> expanded = new LinkedHashMap<>();
            for (Object str : (List<?>) engines) {
                if (str instanceof String) {
                    String[] parts = ENGINES_SPLIT.split(Js.trim((String) str), -1);
                    expanded.put(parts[0], String.join(" ", Arrays.asList(parts).subList(1, parts.length)));
                }
            }
            info.put("engines", expanded);
        }

        if (Js.truthy(info.get("bundledDependencies"))) {
            info.put("bundleDependencies", info.get("bundledDependencies"));
            info.data.remove("bundledDependencies");
        }

        for (String dependencyType : MANIFEST_FIELDS) {
            Object dependencyList = info.get(dependencyType);
            if (dependencyList instanceof Map) {
                Map<String, @Nullable Object> deps = (Map<String, @Nullable Object>) dependencyList;
                deps.remove("//");
                for (String name : new ArrayList<>(deps.keySet())) {
                    Object value = deps.get(name);
                    deps.put(name, Js.truthy(value) ? value : "");
                }
            }
        }
    }

    /**
     * {@code file:} and {@code link:} dependencies made relative to the lock's directory.
     */
    @SuppressWarnings("unchecked")
    static void resolveRelative(Manifest info, String moduleLoc, @Nullable String lockfileFolder) {
        if (lockfileFolder == null) {
            return;
        }
        for (String dependencyType : DEPENDENCY_TYPES) {
            Object dependencies = info.get(dependencyType);
            if (!(dependencies instanceof Map)) {
                continue;
            }
            Map<String, @Nullable Object> deps = (Map<String, @Nullable Object>) dependencies;
            for (String name : Js.keys(deps)) {
                Object raw = deps.get(name);
                if (!(raw instanceof String)) {
                    continue;
                }
                String value = (String) raw;
                if (value.startsWith("/")) {
                    value = "file:" + value;
                }
                String prefix;
                if (value.startsWith("file:")) {
                    prefix = "file:";
                } else if (value.startsWith("link:")) {
                    prefix = "link:";
                } else {
                    continue;
                }
                String unprefixed = value.substring(prefix.length());
                boolean hasPathPrefix = PATH_PREFIX.matcher(unprefixed).find();
                String root = PosixPath.resolve("/", lockfileFolder);
                String absoluteTarget = PosixPath.resolve(root, moduleLoc, unprefixed);
                String relativeTarget = PosixPath.relative(root, absoluteTarget);
                if (relativeTarget.isEmpty()) {
                    relativeTarget = ".";
                }
                if (absoluteTarget.equals(root)) {
                    relativeTarget = ".";
                } else if (hasPathPrefix && !relativeTarget.matches("^\\.{0,2}/.*")) {
                    relativeTarget = "./" + relativeTarget;
                }
                deps.put(name, prefix + relativeTarget);
            }
        }
    }

    interface Warn {
        void warn(String msg);
    }

    static void validate(Manifest info, boolean isRoot, Warn warn) {
        Object name = info.getName();
        if (name instanceof String) {
            String n = (String) name;
            if (n.startsWith(".")) {
                throw new MessageError("Name can't start with a dot");
            }
            if (!isValidPackageName(n)) {
                throw new MessageError("Name contains illegal characters");
            }
            String lower = n.toLowerCase(Locale.ROOT);
            if ("node_modules".equals(lower) || "favicon.ico".equals(lower)) {
                throw new MessageError("Name is blacklisted");
            }
        }
        for (String key : STRINGS) {
            Object val = info.get(key);
            if (Js.truthy(val) && !(val instanceof String)) {
                throw new MessageError(Lang.format("$0 is not a string", key));
            }
        }
        cleanDependencies(info, isRoot, warn);
    }

    static boolean isValidName(String name) {
        return !ILLEGAL_NAME_CHARS.matcher(name).find() && encodeURIComponent(name).equals(name);
    }

    static boolean isValidScopedName(String name) {
        if (!name.startsWith("@")) {
            return false;
        }
        String[] parts = name.substring(1).split("/", -1);
        return parts.length == 2 && isValidName(parts[0]) && isValidName(parts[1]);
    }

    static boolean isValidPackageName(String name) {
        return isValidName(name) || isValidScopedName(name);
    }

    private static String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("%21", "!").replace("%27", "'")
                    .replace("%28", "(").replace("%29", ")").replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A name listed in several dependency maps keeps the first non-trivial range found in the order
     * optionalDependencies, dependencies, devDependencies, and stays only in the first map that lists it.
     */
    @SuppressWarnings("unchecked")
    static void cleanDependencies(Manifest info, boolean isRoot, Warn warn) {
        List<String> types = new ArrayList<>();
        List<Map<String, @Nullable Object>> depTypes = new ArrayList<>();
        for (String type : DEPENDENCY_KEYS) {
            Object deps = info.get(type);
            if (!Js.truthy(deps) || !(deps instanceof Map)) {
                continue;
            }
            types.add(type);
            depTypes.add((Map<String, @Nullable Object>) deps);
        }

        Map<String, String[]> nonTrivialDeps = new LinkedHashMap<>();
        for (int t = 0; t < depTypes.size(); t++) {
            Map<String, @Nullable Object> deps = depTypes.get(t);
            for (String name : Js.keys(deps)) {
                Object version = deps.get(name);
                if (!nonTrivialDeps.containsKey(name) && Js.truthy(version) && !"*".equals(version)) {
                    nonTrivialDeps.put(name, new String[]{types.get(t), Js.string(version)});
                }
            }
        }

        Set<String> setDeps = new LinkedHashSet<>();
        for (int t = 0; t < depTypes.size(); t++) {
            Map<String, @Nullable Object> deps = depTypes.get(t);
            for (String name : Js.keys(deps)) {
                Object version = deps.get(name);
                String[] dep = nonTrivialDeps.get(name);
                if (dep != null) {
                    if (Js.truthy(version) && !"*".equals(version) && !dep[1].equals(Js.string(version)) && isRoot) {
                        warn.warn(Lang.format("$0 has dependency $1 with range $2 that collides with a dependency in " +
                                              "$3 of the same name with version $4", dep[0], name, dep[1],
                                types.get(t), version));
                    }
                    version = dep[1];
                }
                if (setDeps.contains(name)) {
                    deps.remove(name);
                } else {
                    deps.put(name, version);
                    setDeps.add(name);
                }
            }
        }
    }
}
