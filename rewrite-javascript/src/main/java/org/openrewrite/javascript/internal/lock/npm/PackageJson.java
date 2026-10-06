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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/package-json 7.0.5 as arborist reads a manifest: {@link #normalize(String)} parses the
 * text and runs the {@code normalizeSteps}; {@link #syncNormalize(Map)} runs the synchronous ones, as
 * {@code Node} does on its package.
 */
@Getter
public final class PackageJson {

    /**
     * Thrown where npm would read the package's files: {@code directories.bin} without {@code bin}.
     */
    public static final class NeedsPackageFiles extends IllegalStateException {
        NeedsPackageFiles(String message) {
            super(message);
        }
    }

    private static final List<String> NORMALIZE_STEPS = Arrays.asList(
            "_id", "_attributes", "bundledDependencies", "bundleDependencies", "optionalDedupe", "scripts",
            "funding", "bin", "binDir");

    private final Map<String, Object> content;

    /**
     * The text's indentation and newline, as {@code json-parse-even-better-errors} noted them.
     */
    private final @Nullable String indent;
    private final @Nullable String newline;

    private PackageJson(Map<String, Object> content, @Nullable String indent, @Nullable String newline) {
        this.content = content;
        this.indent = indent;
        this.newline = newline;
    }

    /**
     * {@code PackageJson.normalize(path)}: the manifest at a package's root as arborist loads it.
     *
     * @throws JsonParseEvenBetterErrors.JsonParseException as {@code Invalid package.json}
     * @throws IllegalStateException    for a falsy JSON value ({@code Can not normalize without content})
     * @throws IllegalArgumentException for a truthy one that is not an object or array
     */
    @SuppressWarnings("unchecked")
    public static PackageJson normalize(String text) {
        JsonParseEvenBetterErrors.Parsed parsed;
        try {
            parsed = JsonParseEvenBetterErrors.parse(text);
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            throw new JsonParseEvenBetterErrors.JsonParseException("Invalid package.json: " + e.getMessage(), e);
        }
        Object value = parsed.getValue();
        if (!truthy(value)) {
            throw new IllegalStateException("Can not normalize without content");
        }
        if (!(value instanceof Map) && !(value instanceof List)) {
            throw new IllegalArgumentException(
                    "Cannot use 'in' operator to search for 'bundleDependencies' in " + Js.string(value));
        }
        // an array has none of the properties the steps read, so it normalizes to an empty manifest
        Map<String, Object> data = value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
        normalizeBin(data);
        syncSteps(data, NORMALIZE_STEPS);
        return new PackageJson(data, parsed.getIndent(), parsed.getNewline());
    }

    static Map<String, Object> syncNormalize(Map<String, Object> data) {
        List<String> steps = new ArrayList<>(NORMALIZE_STEPS);
        steps.remove("_attributes");
        return syncSteps(data, steps);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> syncSteps(Map<String, Object> data, List<String> steps) {
        Object name = data.get("name");
        Object version = data.get("version");
        String pkgId = name + "@" + version;

        if (steps.contains("_attributes")) {
            for (String key : Js.keys(data)) {
                if (key.startsWith("_")) {
                    data.remove(key);
                }
            }
        }

        if (truthy(name) && truthy(version)) {
            data.put("_id", pkgId);
        }

        if (!data.containsKey("bundleDependencies") && data.containsKey("bundledDependencies")) {
            data.put("bundleDependencies", data.get("bundledDependencies"));
        }
        data.remove("bundledDependencies");

        Object bd = data.get("bundleDependencies");
        if (Boolean.FALSE.equals(bd)) {
            data.put("bundleDependencies", new ArrayList<>());
        } else if (Boolean.TRUE.equals(bd)) {
            Object deps = data.get("dependencies");
            data.put("bundleDependencies", Js.keys(truthy(deps) ? deps : Collections.emptyMap()));
        } else if (bd instanceof Map) {
            data.put("bundleDependencies", Js.keys(bd));
        } else if (!(bd instanceof List) && data.containsKey("bundleDependencies")) {
            data.remove("bundleDependencies");
        }

        Object dependencies = data.get("dependencies");
        Object optionalDependencies = data.get("optionalDependencies");
        if (truthy(dependencies) && (optionalDependencies instanceof Map || optionalDependencies instanceof List)) {
            if (dependencies instanceof Map) {
                for (String optional : Js.keys(optionalDependencies)) {
                    ((Map<String, Object>) dependencies).remove(optional);
                }
            }
            if (Js.keys(dependencies).isEmpty()) {
                data.remove("dependencies");
            }
        }

        Object scripts = data.get("scripts");
        if (scripts instanceof Map) {
            ((Map<String, Object>) scripts).values().removeIf(script -> !(script instanceof String));
        } else if (scripts != null && !(scripts instanceof List)) {
            data.remove("scripts");
        }

        Object funding = data.get("funding");
        if (funding instanceof String && !((String) funding).isEmpty()) {
            Map<String, Object> url = new LinkedHashMap<>();
            url.put("url", funding);
            data.put("funding", url);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static void normalizeBin(Map<String, Object> pkg) {
        Object directories = pkg.get("directories");
        Object binDir = directories instanceof Map ? ((Map<?, ?>) directories).get("bin") : null;
        if (truthy(binDir) && !truthy(pkg.get("bin"))) {
            throw new NeedsPackageFiles("\"directories\": {\"bin\": " + Js.quote(String.valueOf(binDir)) +
                                        "} names the package's files, which are not known");
        }
        Object bin = pkg.get("bin");
        if (truthy(bin)) {
            if (bin instanceof String && truthy(pkg.get("name"))) {
                Map<String, Object> object = new LinkedHashMap<>();
                object.put(String.valueOf(pkg.get("name")), bin);
                bin = object;
                pkg.put("bin", bin);
            } else if (bin instanceof List) {
                Map<String, Object> object = new LinkedHashMap<>();
                for (Object k : (List<Object>) bin) {
                    object.put(PosixPath.basename(String.valueOf(k)), k);
                }
                bin = object;
                pkg.put("bin", bin);
            }
            if (bin instanceof Map) {
                Map<String, Object> bins = (Map<String, Object>) bin;
                for (String binKey : Js.keys(bins)) {
                    Object target = bins.get(binKey);
                    if (!(target instanceof String)) {
                        bins.remove(binKey);
                        continue;
                    }
                    String base = PosixPath.basename(secureAndUnixifyPath(binKey));
                    if (base.isEmpty()) {
                        bins.remove(binKey);
                        continue;
                    }
                    String binTarget = secureAndUnixifyPath((String) target);
                    if (binTarget.isEmpty()) {
                        bins.remove(binKey);
                        continue;
                    }
                    if (!base.equals(binKey)) {
                        bins.remove(binKey);
                    }
                    bins.put(base, binTarget);
                }
                if (bins.isEmpty()) {
                    pkg.remove("bin");
                }
                return;
            }
        }
        pkg.remove("bin");
    }

    private static String unixifyPath(String ref) {
        return ref.replace('\\', '/').replace(':', '/');
    }

    private static String secureAndUnixifyPath(String ref) {
        String secured = unixifyPath(PosixPath.join(".", PosixPath.join("/", unixifyPath(ref))));
        return secured.startsWith("./") ? "" : secured;
    }
}
