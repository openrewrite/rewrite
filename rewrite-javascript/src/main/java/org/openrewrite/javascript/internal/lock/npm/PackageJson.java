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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/package-json 7.0.5 as arborist reads a manifest: {@link #normalize(String, NpmRelease)} runs the
 * {@code normalizeSteps}, {@link #syncNormalize(Map, NpmRelease)} the synchronous ones, as {@code Node} does on its
 * package.
 */
@Getter
public final class PackageJson {

    /**
     * The @npmcli/package-json release whose {@code bin} step normalizes a fetched manifest: 7.0.5, which npm 11.17.0
     * bundles and every release from 6.0.1 reads as there; 5.2.0, which like 5.1.1 drops a name or a path that starts
     * with a dot; or 5.1.0, which like read-package-json-fast before it keeps a colon in a path.
     */
    public enum Release {
        V5_1_0, V5_2_0, V7_0_5
    }

    /**
     * Thrown where npm would read the package's files: {@code directories.bin} without {@code bin}.
     */
    public static final class NeedsPackageFiles extends IllegalStateException {
        /**
         * The manifest normalized but for the {@code bin} npm fills from those files.
         */
        @Getter
        private final transient Map<String, Object> content;

        NeedsPackageFiles(String message, Map<String, Object> content) {
            super(message);
            this.content = content;
        }
    }

    /**
     * The TypeError a normalize step raises at a field's value, with JavaScript's message.
     */
    @Getter
    static final class FieldError extends JsError {
        private final String field;

        FieldError(String field, String message) {
            super(TYPE_ERROR, message);
            this.field = field;
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
     * {@code PackageJson.normalize(path)} of the library the npm {@code release} bundles: JsonParseException as {@code
     * Invalid package.json}, an Error for a falsy value, a TypeError for a truthy one that is not an object or array.
     */
    @SuppressWarnings("unchecked")
    public static PackageJson normalize(String text, NpmRelease release) {
        JsonParseEvenBetterErrors.Parsed parsed;
        try {
            parsed = JsonParseEvenBetterErrors.parse(text);
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            throw new JsonParseEvenBetterErrors.JsonParseException("Invalid package.json: " + e.getMessage(), e);
        }
        Object value = parsed.getValue();
        if (!truthy(value)) {
            throw new JsError(JsError.ERROR, "Can not normalize without content");
        }
        if (!(value instanceof Map) && !(value instanceof List)) {
            throw new IllegalArgumentException(
                    "Cannot use 'in' operator to search for 'bundleDependencies' in " + Js.string(value));
        }
        // an array has none of the properties the steps read, so it normalizes to an empty manifest
        Map<String, Object> data = value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
        // read-package-json-fast keeps the _ fields of a file before npm 7.5.6 (2.0.2)
        List<String> steps = new ArrayList<>(NORMALIZE_STEPS);
        if (!release.stripsUnderscoreFieldsOfFiles()) {
            steps.remove("_attributes");
        }
        Object directories = data.get("directories");
        Object binDir = directories instanceof Map ? ((Map<?, ?>) directories).get("bin") : null;
        // read-package-json-fast reads no directories.bin before npm 7.20.1 (2.0.3)
        if (truthy(binDir) && !truthy(data.get("bin")) && release.readsBinDirectoryOfFiles()) {
            if (!(binDir instanceof String)) {
                throw new FieldError("directories.bin", "ref.replace is not a function");
            }
            data.remove("bin");
            throw new NeedsPackageFiles("\"directories\": {\"bin\": " + Js.quote((String) binDir) +
                                        "} names the package's files, which are not known",
                    syncSteps(data, steps, release));
        }
        normalizeBin(data);
        syncSteps(data, steps, release);
        return new PackageJson(data, parsed.getIndent(), parsed.getNewline());
    }

    /**
     * A manifest already parsed, as the lock holds one: its content as it is, with no formatting of its own.
     */
    static PackageJson parsed(Map<String, Object> content) {
        return new PackageJson(content, null, null);
    }

    static Map<String, Object> syncNormalize(Map<String, Object> data, NpmRelease release) {
        List<String> steps = new ArrayList<>(NORMALIZE_STEPS);
        steps.remove("_attributes");
        return syncSteps(data, steps, release);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> syncSteps(Map<String, Object> data, List<String> steps, NpmRelease release) {
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

        // the overrides step, which 8.0.0 adds: the field is left absent or an object with keys
        if (data.containsKey("overrides") && release.normalizesOverrides()) {
            Object ov = data.get("overrides");
            if (!(ov instanceof Map) || ((Map<?, ?>) ov).isEmpty()) {
                data.remove("overrides");
            }
        }

        Object dependencies = data.get("dependencies");
        Object optionalDependencies = data.get("optionalDependencies");
        if (release.foldsOptionalDependencies()) {
            // read-package-json-fast 1.2.1's foldinOptionalDeps: the optional dependencies are listed as dependencies too
            if (optionalDependencies instanceof Map || optionalDependencies instanceof List) {
                if (!truthy(dependencies)) {
                    dependencies = new LinkedHashMap<String, Object>();
                    data.put("dependencies", dependencies);
                }
                for (Map.Entry<String, Object> optional : Js.entries(optionalDependencies)) {
                    if (dependencies instanceof Map && !"__proto__".equals(optional.getKey())) {
                        ((Map<String, Object>) dependencies).put(optional.getKey(), optional.getValue());
                    }
                }
            }
        } else if (truthy(dependencies) &&
                   (optionalDependencies instanceof Map || optionalDependencies instanceof List)) {
            for (String optional : Js.keys(optionalDependencies)) {
                Js.deleteSloppy(dependencies, optional);
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

    /**
     * npm-normalize-package-bin, in place.
     */
    @SuppressWarnings("unchecked")
    static void normalizeBin(Map<String, Object> pkg) {
        Object bin = pkg.get("bin");
        if (truthy(bin)) {
            if (bin instanceof String && truthy(pkg.get("name"))) {
                Map<String, Object> object = new LinkedHashMap<>();
                object.put(Js.string(pkg.get("name")), bin);
                bin = object;
                pkg.put("bin", bin);
            } else if (bin instanceof List) {
                Map<String, Object> object = new LinkedHashMap<>();
                for (Object k : (List<Object>) bin) {
                    if (!(k instanceof String)) {
                        throw new IllegalArgumentException(
                                "The \"path\" argument must be of type string. Received " + Js.received(k));
                    }
                    object.put(PosixPath.basename((String) k), k);
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

    /**
     * The {@code bin} step of {@code release} over a fetched manifest.
     */
    @SuppressWarnings("unchecked")
    static void normalizeBin(Map<String, Object> pkg, Release release) {
        if (release == Release.V7_0_5) {
            normalizeBin(pkg);
            return;
        }
        if (release == Release.V5_1_0) {
            // its loop reads as npm-normalize-package-bin's, which already ran over the manifest when it was picked
            normalizePackageBin(pkg);
            return;
        }
        Object bin = pkg.get("bin");
        if (truthy(bin)) {
            if (bin instanceof String && truthy(pkg.get("name"))) {
                Map<String, Object> object = new LinkedHashMap<>();
                object.put(Js.string(pkg.get("name")), bin);
                bin = object;
                pkg.put("bin", bin);
            } else if (bin instanceof List) {
                Map<String, Object> object = new LinkedHashMap<>();
                for (Object k : (List<Object>) bin) {
                    if (!(k instanceof String)) {
                        throw new IllegalArgumentException(
                                "The \"path\" argument must be of type string. Received " + Js.received(k));
                    }
                    object.put(PosixPath.basename((String) k), k);
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
                    String base = PosixPath.basename(secureAndUnixifyPath5_2_0(binKey));
                    if (base.isEmpty()) {
                        bins.remove(binKey);
                        continue;
                    }
                    String binTarget = secureAndUnixifyPath5_2_0((String) target);
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

    // 5.1.1 and 5.2.0: securePath empties every path whose first name starts with a dot, not only ./
    private static String secureAndUnixifyPath5_2_0(String ref) {
        String secured = PosixPath.join(".", PosixPath.join("/", unixifyPath(ref)));
        return unixifyPath(secured.startsWith(".") ? "" : secured);
    }

    /**
     * Port of npm-normalize-package-bin 5.0.0, the same since 3.0.1: {@code bin} as a new object of base names to
     * paths under the package, a colon in a path kept.
     */
    @SuppressWarnings("unchecked")
    static void normalizePackageBin(Map<String, Object> pkg) {
        Object bin = pkg.get("bin");
        if (!truthy(bin)) {
            pkg.remove("bin");
            return;
        }
        Map<String, Object> orig;
        if (bin instanceof String) {
            if (!truthy(pkg.get("name"))) {
                pkg.remove("bin");
                return;
            }
            orig = new LinkedHashMap<>();
            orig.put(Js.string(pkg.get("name")), bin);
        } else if (bin instanceof List) {
            orig = new LinkedHashMap<>();
            for (Object k : (List<Object>) bin) {
                if (!(k instanceof String)) {
                    throw new IllegalArgumentException(
                            "The \"path\" argument must be of type string. Received " + Js.received(k));
                }
                orig.put(PosixPath.basename((String) k), k);
            }
        } else if (bin instanceof Map) {
            orig = (Map<String, Object>) bin;
        } else {
            pkg.remove("bin");
            return;
        }
        Map<String, Object> clean = new LinkedHashMap<>();
        for (String binKey : Js.keys(orig)) {
            String base = PosixPath.join("/", PosixPath.basename(binKey.replace('\\', '/').replace(':', '/')))
                    .substring(1);
            Object target = orig.get(binKey);
            if (!(target instanceof String) || base.isEmpty()) {
                continue;
            }
            String binTarget = PosixPath.join("/", ((String) target).replace('\\', '/')).replace('\\', '/')
                    .substring(1);
            if (binTarget.isEmpty()) {
                continue;
            }
            clean.put(base, binTarget);
        }
        if (clean.isEmpty()) {
            pkg.remove("bin");
        } else {
            pkg.put("bin", clean);
        }
    }

    private static String unixifyPath(String ref) {
        return ref.replace('\\', '/').replace(':', '/');
    }

    private static String secureAndUnixifyPath(String ref) {
        String secured = unixifyPath(PosixPath.join(".", PosixPath.join("/", unixifyPath(ref))));
        return secured.startsWith("./") ? "" : secured;
    }
}
