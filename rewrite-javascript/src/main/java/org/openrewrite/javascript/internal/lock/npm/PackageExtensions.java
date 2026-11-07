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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.semver.NodeSemver;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/package-extensions.js (npm 12.0.0): the root's {@code packageExtensions}, which add
 * dependency and peer declarations to a candidate's manifest before its edges are read.
 */
@Getter
final class PackageExtensions {

    static final List<String> EXTENSION_FIELDS = Collections.unmodifiableList(Arrays.asList(
            "dependencies", "optionalDependencies", "peerDependencies", "peerDependenciesMeta"));

    private static final List<String> NORMAL_DEP_FIELDS = Collections.unmodifiableList(Arrays.asList(
            "dependencies", "optionalDependencies"));

    /**
     * {@code EEXTENSIONROOT}, {@code EEXTENSIONSELECTOR}, {@code EEXTENSIONVALUE}, {@code EEXTENSIONFIELD}, {@code
     * EEXTENSIONDELETE}, {@code EEXTENSIONCONFLICT}, {@code EEXTENSIONDUPDEP} or {@code EEXTENSIONORPHANMETA}.
     */
    @Getter
    static final class ExtensionError extends IllegalStateException implements NpmError {
        private final String code;

        ExtensionError(String message, String code) {
            super(message);
            this.code = code;
        }
    }

    @Value
    static class Selector {
        String key;
        String name;

        /**
         * Null for a key that is a name only, which matches every version.
         */
        @Nullable
        String range;

        Object ext;
    }

    /**
     * A manifest copy with a selector's fields added, and what the node records of it.
     */
    @Value
    static class Extended {
        Map<String, Object> pkg;
        Map<String, Object> applied;
    }

    private final @Nullable Object raw;
    private final boolean present;
    private final List<Selector> selectors = new ArrayList<>();

    // semver's { loose: true }, which every range and version of a selector is read with
    @Getter(AccessLevel.NONE)
    private final NodeSemver.Options loose;

    /**
     * The digest of the rule set as the lock's root entry records it; null without one.
     */
    private final @Nullable String hash;

    /**
     * @param raw    the root package's {@code packageExtensions}, {@link Js#UNDEFINED} when it has none
     * @param semver the node-semver of the npm release that reads the rules
     * @throws ExtensionError for a rule set, a selector or an extension npm rejects
     */
    PackageExtensions(@Nullable Object raw, NodeSemver.Release semver) {
        this.raw = raw;
        this.loose = NodeSemver.Options.LOOSE.release(semver);
        this.present = raw != Js.UNDEFINED;
        if (!present) {
            this.hash = null;
            return;
        }
        if (!(raw instanceof Map)) {
            throw new ExtensionError("packageExtensions must be an object", "EEXTENSIONROOT");
        }
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(raw)) {
            String key = entry.getKey();
            String[] selector = parseSelector(key);
            validateExtensionObject(key, entry.getValue());
            selectors.add(new Selector(key, selector[0], selector[1], entry.getValue()));
        }
        this.hash = canonicalHash(raw);
    }

    // { name, range }: a package name with an optional semver range after the first @ that does not open a scope
    private @Nullable String[] parseSelector(String key) {
        if (key.isEmpty()) {
            throw new ExtensionError("Invalid packageExtensions selector: " + Js.jsonStringify(key),
                    "EEXTENSIONSELECTOR");
        }
        int at = key.indexOf('@', key.startsWith("@") ? 1 : 0);
        String name = at == -1 ? key : key.substring(0, at);
        String range = at == -1 ? null : key.substring(at + 1);

        if (!ValidateNpmPackageName.validForOldPackages(name, ValidateNpmPackageName.Release.V7_0_2)) {
            throw new ExtensionError("Invalid package name in packageExtensions selector: \"" + key + "\"",
                    "EEXTENSIONSELECTOR");
        }
        if (range != null && Js.trim(range).isEmpty()) {
            throw new ExtensionError("Invalid packageExtensions selector: \"" + key +
                                     "\". Use the name only to match every version.", "EEXTENSIONSELECTOR");
        }
        if (range != null && NodeSemver.validRange(range, loose) == null) {
            throw new ExtensionError("Invalid version range in packageExtensions selector: \"" + key +
                                     "\". Selectors accept a package name with an optional semver range only.",
                    "EEXTENSIONSELECTOR");
        }
        return new String[]{name, range};
    }

    // a name-only selector matches every version; a versioned one only a version that parses and satisfies its range
    private boolean rangeMatches(@Nullable String range, @Nullable Object version) {
        if (range == null) {
            return true;
        }
        return version instanceof String && NodeSemver.valid((String) version, loose) != null &&
               NodeSemver.satisfies((String) version, range, loose);
    }

    private static void validateExtensionObject(String key, @Nullable Object ext) {
        if (!(ext instanceof Map)) {
            throw new ExtensionError("packageExtensions[\"" + key + "\"] must be an object", "EEXTENSIONVALUE");
        }
        for (String field : Js.keys(ext)) {
            if (!EXTENSION_FIELDS.contains(field)) {
                throw new ExtensionError("packageExtensions[\"" + key + "\"] has unsupported field \"" + field +
                                         "\". Supported fields: " + String.join(", ", EXTENSION_FIELDS) + ".",
                        "EEXTENSIONFIELD");
            }
            if (!(Js.get(ext, field) instanceof Map)) {
                throw new ExtensionError("packageExtensions[\"" + key + "\"]." + field + " must be an object",
                        "EEXTENSIONVALUE");
            }
        }
        for (String field : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
            Object deps = Js.get(ext, field);
            for (Map.Entry<String, @Nullable Object> dep : Js.entries(truthy(deps) ? deps : Collections.emptyMap())) {
                Object spec = dep.getValue();
                if (spec == null || Boolean.FALSE.equals(spec) || "-".equals(spec)) {
                    throw new ExtensionError("packageExtensions[\"" + key + "\"]." + field + "." + dep.getKey() +
                                             " attempts deletion, which is not supported.", "EEXTENSIONDELETE");
                }
            }
        }
        Object metas = Js.get(ext, "peerDependenciesMeta");
        for (Map.Entry<String, @Nullable Object> meta : Js.entries(truthy(metas) ? metas : Collections.emptyMap())) {
            if (!(meta.getValue() instanceof Map)) {
                throw new ExtensionError("packageExtensions[\"" + key + "\"].peerDependenciesMeta." + meta.getKey() +
                                         " must be an object", "EEXTENSIONVALUE");
            }
        }
    }

    // a copy of the manifest with the fields an extension may touch cloned and extended; the manifest itself is shared
    // with the cache of fetched manifests and is never changed
    @SuppressWarnings("unchecked")
    private static Extended applyExtension(Map<String, Object> pkg, Selector selector) {
        String key = selector.getKey();
        Object ext = selector.getExt();
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("selector", key);

        Map<String, Object> next = new LinkedHashMap<>(pkg);
        for (String field : EXTENSION_FIELDS) {
            Object value = pkg.get(field);
            if (value instanceof Map || value instanceof List) {
                Map<String, Object> clone = new LinkedHashMap<>();
                for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
                    clone.put(entry.getKey(), "peerDependenciesMeta".equals(field) ? spread(entry.getValue()) :
                            entry.getValue());
                }
                next.put(field, clone);
            }
        }

        for (String field : NORMAL_DEP_FIELDS) {
            Object adds = Js.get(ext, field);
            if (!truthy(adds)) {
                continue;
            }
            for (Map.Entry<String, @Nullable Object> add : Js.entries(adds)) {
                String name = add.getKey();
                for (String existingField : NORMAL_DEP_FIELDS) {
                    Object existing = next.get(existingField);
                    if (truthy(existing) && in(name, existing)) {
                        throw new ExtensionError("packageExtensions[\"" + key + "\"]." + field + "." + name +
                                                 " conflicts with the package's existing " + existingField + "." +
                                                 name + ". Use overrides to change a dependency version; " +
                                                 "packageExtensions only adds missing dependencies.",
                                "EEXTENSIONDUPDEP");
                    }
                }
                set(next, field, name, add.getValue());
                ((List<Object>) applied.computeIfAbsent(field, f -> new ArrayList<>())).add(name);
            }
        }

        Object peers = Js.get(ext, "peerDependencies");
        if (truthy(peers)) {
            for (Map.Entry<String, @Nullable Object> peer : Js.entries(peers)) {
                set(next, "peerDependencies", peer.getKey(), peer.getValue());
                ((List<Object>) applied.computeIfAbsent("peerDependencies", f -> new ArrayList<>())).add(
                        peer.getKey());
            }
        }

        Object metas = Js.get(ext, "peerDependenciesMeta");
        if (truthy(metas)) {
            for (Map.Entry<String, @Nullable Object> meta : Js.entries(metas)) {
                String name = meta.getKey();
                Object current = next.get("peerDependenciesMeta");
                Map<String, Object> merged = spread(truthy(current) ? Js.get(current, name) : Js.UNDEFINED);
                merged.putAll(spread(meta.getValue()));
                set(next, "peerDependenciesMeta", name, merged);
                ((List<Object>) applied.computeIfAbsent("peerDependenciesMeta", f -> new ArrayList<>())).add(name);
                Object peerDependencies = next.get("peerDependencies");
                if (!truthy(peerDependencies) || !in(name, peerDependencies)) {
                    throw new ExtensionError("packageExtensions[\"" + key + "\"].peerDependenciesMeta." + name +
                                             " has no corresponding peerDependencies." + name +
                                             " after extension application.", "EEXTENSIONORPHANMETA");
                }
            }
        }
        return new Extended(next, applied);
    }

    // next[field] = next[field] || {}; next[field][name] = value: a primitive takes no property, and __proto__ names
    // none of a plain object's own
    @SuppressWarnings("unchecked")
    private static void set(Map<String, Object> next, String field, String name, @Nullable Object value) {
        Object target = next.get(field);
        if (!truthy(target)) {
            target = new LinkedHashMap<String, Object>();
            next.put(field, target);
        }
        if (target instanceof Map && !"__proto__".equals(name)) {
            ((Map<String, Object>) target).put(name, value);
        }
    }

    // name in object: its own properties and what it inherits; JavaScript's TypeError for a primitive
    private static boolean in(String name, Object object) {
        if (!(object instanceof Map) && !(object instanceof List)) {
            throw new IllegalArgumentException("Cannot use 'in' operator to search for '" + name + "' in " +
                                               Js.string(object));
        }
        return Js.get(object, name) != Js.UNDEFINED;
    }

    // { ...value }: the own properties of an object, an array or a string, none of anything else
    private static Map<String, Object> spread(@Nullable Object value) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (value instanceof Map || value instanceof List || value instanceof String) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    // keys sorted at every level, strings and numbers as JSON.stringify writes them, no whitespace
    static String canonicalStringify(@Nullable Object val) {
        if (val instanceof List) {
            StringBuilder out = new StringBuilder("[");
            List<?> list = (List<?>) val;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(canonicalStringify(list.get(i)));
            }
            return out.append(']').toString();
        }
        if (val instanceof Map) {
            List<String> keys = new ArrayList<>(Js.keys(val));
            Collections.sort(keys);
            StringBuilder out = new StringBuilder("{");
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(Js.jsonStringify(keys.get(i))).append(':')
                        .append(canonicalStringify(((Map<?, ?>) val).get(keys.get(i))));
            }
            return out.append('}').toString();
        }
        return String.valueOf(Js.jsonStringify(val));
    }

    // ssri.fromData(canonicalStringify(packageExtensions), { algorithms: ['sha512'] }).toString()
    static String canonicalHash(@Nullable Object packageExtensions) {
        return sha512(utf8(canonicalStringify(packageExtensions)));
    }

    /**
     * The sha512 SSRI string of {@code data}, as ssri.fromData gives it.
     */
    static String sha512(byte[] data) {
        try {
            return "sha512-" + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // a string's UTF-8 bytes as Node writes them: U+FFFD for a lone surrogate
    private static byte[] utf8(String text) {
        try {
            ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .replaceWith(new byte[]{(byte) 0xEF, (byte) 0xBF, (byte) 0xBD})
                    .encode(CharBuffer.wrap(text));
            return Arrays.copyOf(bytes.array(), bytes.limit());
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Whether any selector matches the candidate; it never throws.
     */
    boolean wouldMatch(@Nullable Object name, @Nullable Object version) {
        for (Selector s : selectors) {
            if (Js.strictEquals(s.getName(), name) && rangeMatches(s.getRange(), version)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @throws ExtensionError {@code EEXTENSIONCONFLICT} when more than one selector matches the candidate
     */
    @Nullable
    Selector match(@Nullable Object name, @Nullable Object version) {
        List<Selector> matches = new ArrayList<>();
        for (Selector s : selectors) {
            if (Js.strictEquals(s.getName(), name) && rangeMatches(s.getRange(), version)) {
                matches.add(s);
            }
        }
        if (matches.size() > 1) {
            List<String> keys = new ArrayList<>();
            for (Selector s : matches) {
                keys.add("\"" + s.getKey() + "\"");
            }
            throw new ExtensionError("Multiple packageExtensions selectors match " + Js.string(name) + "@" +
                                     Js.string(version) + ": " + String.join(", ", keys) +
                                     ". Narrow or remove one of the overlapping rules.", "EEXTENSIONCONFLICT");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * The manifest with the matching selector's extension applied to a copy, or null when no selector matches.
     * @throws ExtensionError for selectors that conflict or an extension that does not merge
     */
    @Nullable
    Extended apply(Map<String, Object> pkg) {
        if (!present || selectors.isEmpty() || !truthy(pkg.get("name"))) {
            return null;
        }
        Selector sel = match(pkg.get("name"), Js.get(pkg, "version"));
        return sel == null ? null : applyExtension(pkg, sel);
    }
}
