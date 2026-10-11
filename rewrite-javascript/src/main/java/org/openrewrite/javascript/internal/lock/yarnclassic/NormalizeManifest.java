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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.UriComponent;
import org.openrewrite.javascript.semver.NodeSemver;

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
 * Port of yarn src/util/normalize-manifest (yarn 1.22.22), fix.js only where it reaches a lock or an install.
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
    private static final Pattern PATH_PREFIX = Pattern.compile("^\\.(/|\\z)");
    private static final Pattern RELATIVE_PREFIX = Pattern.compile("^\\.{0,2}/");
    private static final Pattern ENGINES_SPLIT = Pattern.compile(" +");

    private NormalizeManifest() {
    }

    /**
     * Normalizes a manifest in place ({@code moduleLoc}, null for yarn's cache, and {@code lockfileFolder} relative to
     * the project); throws MessageError for a name yarn rejects, or a name or version that is not a string.
     */
    static Manifest normalizeManifest(Manifest info, @Nullable String moduleLoc, @Nullable String lockfileFolder,
                                      boolean isRoot, boolean looseSemver) {
        Object name = info.getName();
        Object version = info.getVersion();
        String human = null;
        if (name instanceof String) {
            human = (String) name;
        }
        if (Js.truthy(human) && version instanceof String && !((String) version).isEmpty()) {
            human += "@" + version;
        }
        if (isRoot && Js.truthy(info.get("_loc"))) {
            human = Js.string(info.get("_loc"));
        }
        String prefix = Js.truthy(human) ? human + ": " : "";

        fix(info, looseSemver);
        resolveRelative(info, moduleLoc, lockfileFolder);
        try {
            validate(info);
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
                    looseSemver ? Config.LOOSE_SEMVER : Config.SEMVER);
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
            Map<String, @Nullable Object> deps = asObject(info, dependencyType);
            if (deps != null) {
                deps.remove("//");
                for (String name : new ArrayList<>(deps.keySet())) {
                    Object value = deps.get(name);
                    deps.put(name, Js.truthy(value) ? value : "");
                }
            }
        }
    }

    // typeof === 'object': an array is one too, and yarn reads it by its indices from here on
    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, @Nullable Object> asObject(Manifest info, String key) {
        Object value = info.get(key);
        if (value instanceof List) {
            Map<String, @Nullable Object> indexed = new LinkedHashMap<>();
            Js.assign(indexed, value);
            info.put(key, indexed);
            return indexed;
        }
        return value instanceof Map ? (Map<String, @Nullable Object>) value : null;
    }

    /**
     * {@code file:} and {@code link:} dependencies made relative to the lock's directory; an absolute target, or any
     * in yarn's cache, becomes an {@link UnknownPath}, as yarn writes it relative to where the project or cache sits.
     */
    @SuppressWarnings("unchecked")
    static void resolveRelative(Manifest info, @Nullable String moduleLoc, @Nullable String lockfileFolder) {
        if (lockfileFolder == null) {
            return;
        }
        for (String dependencyType : DEPENDENCY_TYPES) {
            Object dependencies = info.get(dependencyType);
            if (!Js.truthy(dependencies)) {
                continue;
            }
            for (String name : Js.keys(dependencies)) {
                Object raw = Js.get(dependencies, name);
                if (!(raw instanceof String)) {
                    throw Js.invalidArgType("The \"path\" argument must be of type string", raw);
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
                // yarn computes the target first, but a string's index is read-only wherever the project sits
                if (dependencies instanceof String) {
                    throw new JsError(JsError.TYPE_ERROR, "Cannot assign to read only property '" + name +
                                                          "' of string '" + dependencies + "'");
                }
                Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) dependencies;
                String unprefixed = value.substring(prefix.length());
                if (unprefixed.startsWith("/")) {
                    map.put(name, new UnknownPath("where the project sits on disk, which the " + dependencyType +
                                                  " entry " + name + "@" + value + " is written relative to"));
                    continue;
                }
                if (moduleLoc == null) {
                    map.put(name, new UnknownPath("where yarn's cache sits, which the " + dependencyType + " entry " +
                                                  name + "@" + value + " of " + Js.string(info.getName()) +
                                                  " is written relative to"));
                    continue;
                }
                boolean hasPathPrefix = PATH_PREFIX.matcher(unprefixed).find();
                String project = deeperThanAnyClimb(lockfileFolder, moduleLoc, unprefixed);
                String root = PosixPath.resolve(project, lockfileFolder);
                String absoluteTarget = PosixPath.resolve(project, moduleLoc, unprefixed);
                String relativeTarget = PosixPath.relative(root, absoluteTarget);
                if (relativeTarget.isEmpty()) {
                    relativeTarget = ".";
                }
                if (absoluteTarget.equals(root)) {
                    relativeTarget = ".";
                } else if (hasPathPrefix && !RELATIVE_PREFIX.matcher(relativeTarget).find()) {
                    relativeTarget = "./" + relativeTarget;
                }
                map.put(name, prefix + relativeTarget);
            }
        }
    }

    // a target that climbs out of the project keeps its `..`, as on disk wherever the project sits deeper than that
    private static String deeperThanAnyClimb(String... relativePaths) {
        StringBuilder project = new StringBuilder();
        for (String path : relativePaths) {
            for (String segment : path.split("/", -1)) {
                if ("..".equals(segment)) {
                    project.append("/\0");
                }
            }
        }
        return project.append("/\0").toString();
    }

    static void validate(Manifest info) {
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
        cleanDependencies(info);
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
            return UriComponent.encodeURIComponent(s);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.URI_ERROR, "URI malformed");
        }
    }

    /**
     * A name listed in several dependency maps keeps the first non-trivial range found in the order
     * optionalDependencies, dependencies, devDependencies, and stays only in the first map that lists it.
     */
    static void cleanDependencies(Manifest info) {
        List<String> types = new ArrayList<>();
        List<Map<String, @Nullable Object>> depTypes = new ArrayList<>();
        for (String type : DEPENDENCY_KEYS) {
            Map<String, @Nullable Object> deps = asObject(info, type);
            if (deps == null) {
                continue;
            }
            types.add(type);
            depTypes.add(deps);
        }

        Map<String, DependencyTuple> nonTrivialDeps = new LinkedHashMap<>();
        for (int t = 0; t < depTypes.size(); t++) {
            Map<String, @Nullable Object> deps = depTypes.get(t);
            for (String name : Js.keys(deps)) {
                Object version = deps.get(name);
                if (!nonTrivialDeps.containsKey(name) && Js.truthy(version) && !"*".equals(version)) {
                    nonTrivialDeps.put(name, new DependencyTuple(types.get(t), version));
                }
            }
        }

        Set<String> setDeps = new LinkedHashSet<>();
        for (int t = 0; t < depTypes.size(); t++) {
            Map<String, @Nullable Object> deps = depTypes.get(t);
            for (String name : Js.keys(deps)) {
                Object version = deps.get(name);
                DependencyTuple dep = nonTrivialDeps.get(name);
                if (dep != null) {
                    version = dep.version;
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

    private static final class DependencyTuple {
        final String type;
        final Object version;

        DependencyTuple(String type, Object version) {
            this.type = type;
            this.version = version;
        }
    }

}
