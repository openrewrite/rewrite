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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Ssri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of yarn src/lockfile/index.js (yarn 1.22.22).
 */
@Getter
final class Lockfile {

    private final String source;

    /**
     * The parsed lock: an object from pattern to entry when yarn could read one, {@code null} without a lock.
     */
    private @Nullable Object cache;

    private final @Nullable String parseResultType;

    /**
     * A Yarn Berry lock yarn read and set aside ({@code __metadata}): the cache is empty and yarn resolves
     * everything from scratch.
     */
    private final @Nullable Object lockfilev2;

    /**
     * The prototype of an object-literal cache, once yarn took it for the entry of {@code __proto__}.
     */
    private @Nullable Map<String, @Nullable Object> prototype;

    private final Map<Object, Map<String, @Nullable Object>> arrayEntries = new IdentityHashMap<>();

    Lockfile(@Nullable Object cache, @Nullable String source, @Nullable String parseResultType) {
        this(cache, source, parseResultType, null);
    }

    private Lockfile(@Nullable Object cache, @Nullable String source, @Nullable String parseResultType,
                     @Nullable Object lockfilev2) {
        this.source = source == null ? "" : source;
        this.cache = cache;
        this.parseResultType = parseResultType;
        this.lockfilev2 = lockfilev2;
    }

    Lockfile() {
        this(null, null, null);
    }

    /**
     * {@code fromDirectory} over the text of the directory's yarn.lock, {@code null} when it has none. yarn reads
     * the file with its CRLF line endings turned into LF.
     */
    static Lockfile fromDirectory(@Nullable String rawLockfile) {
        Object lockfile = null;
        String raw = "";
        LockfileParser.ParseResult parseResult = null;
        if (rawLockfile != null) {
            raw = rawLockfile.replace("\r\n", "\n");
            parseResult = LockfileParser.parseLockfile(raw, "yarn.lock");
            lockfile = parseResult.getObject();
        }
        Object lockfilev2 = null;
        if (lockfile instanceof Map && Js.truthy(((Map<?, ?>) lockfile).get("__metadata"))) {
            lockfilev2 = lockfile;
            lockfile = new PlainObject();
        }
        return new Lockfile(lockfile, raw, parseResult == null ? null : parseResult.getType(), lockfilev2);
    }

    static String getName(String pattern) {
        return NormalizedPattern.normalizePattern(pattern).getName();
    }

    // Object.keys of a string lists its characters
    private static @Nullable Object blankObjectUndefined(@Nullable Object obj) {
        boolean hasKeys = obj instanceof Map ? !((Map<?, ?>) obj).isEmpty() :
                obj instanceof List ? !((List<?>) obj).isEmpty() : obj instanceof String && !((String) obj).isEmpty();
        return hasKeys ? obj : null;
    }

    static @Nullable String keyForRemote(PackageRemote remote) {
        if (Js.truthy(remote.resolved)) {
            return remote.resolved;
        }
        return Js.truthy(remote.reference) && Js.truthy(remote.hash) ? remote.reference + "#" + remote.hash : null;
    }

    static String serializeIntegrity(Object integrity) {
        if (integrity instanceof Map && (!(integrity instanceof PlainObject) ||
                                         ((Map<?, ?>) integrity).containsKey("toString"))) {
            throw new JsError(JsError.TYPE_ERROR, "integrity.toString is not a function");
        }
        List<String> parts = new ArrayList<>(Arrays.asList(Js.templateString(integrity).split(" ", -1)));
        parts.sort(String::compareTo);
        return String.join(" ", parts);
    }

    /**
     * The fields of a lock entry {@code getLockfile} passes, as an object literal would hold them.
     */
    static Map<String, @Nullable Object> implodeEntry(String pattern, Map<String, @Nullable Object> obj) {
        String inferredName = getName(pattern);
        Object integrity = obj.get("integrity");
        String serialized = Js.truthy(integrity) ? serializeIntegrity(integrity) : "";
        Map<String, @Nullable Object> imploded = new LinkedHashMap<>();
        imploded.put("name", inferredName.equals(obj.get("name")) ? null : obj.get("name"));
        imploded.put("version", obj.get("version"));
        imploded.put("uid", Js.strictEquals(obj.get("uid"), obj.get("version")) ? null : obj.get("uid"));
        imploded.put("resolved", obj.get("resolved"));
        imploded.put("registry", "npm".equals(obj.get("registry")) ? null : obj.get("registry"));
        imploded.put("dependencies", blankObjectUndefined(obj.get("dependencies")));
        imploded.put("optionalDependencies", blankObjectUndefined(obj.get("optionalDependencies")));
        imploded.put("permissions", blankObjectUndefined(obj.get("permissions")));
        imploded.put("prebuiltVariants", blankObjectUndefined(obj.get("prebuiltVariants")));
        if (!serialized.isEmpty()) {
            imploded.put("integrity", serialized);
        }
        return imploded;
    }

    /**
     * Fills a lock entry's defaults in place, as yarn does when it reads one.
     */
    static Map<String, @Nullable Object> explodeEntry(String pattern, Map<String, @Nullable Object> obj) {
        or(obj, "optionalDependencies", new LinkedHashMap<String, Object>());
        or(obj, "dependencies", new LinkedHashMap<String, Object>());
        or(obj, "uid", obj.get("version"));
        or(obj, "permissions", new LinkedHashMap<String, Object>());
        or(obj, "registry", "npm");
        or(obj, "name", getName(pattern));
        Object integrity = obj.get("integrity");
        if (Js.truthy(integrity) && Js.truthy(Js.get(integrity, "isIntegrity"))) {
            obj.put("integrity", Ssri.parse(integrity, Ssri.Release.V5_3_0, Js::templateString));
        }
        return obj;
    }

    private static void or(Map<String, @Nullable Object> obj, String key, @Nullable Object fallback) {
        Object value = obj.get(key);
        obj.put(key, Js.truthy(value) ? value : fallback);
    }

    /**
     * The entry of a pattern, its defaults filled in; a string entry (an alias in very old locks) names the
     * pattern whose entry it shares.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> getLocked(String pattern) {
        Set<String> aliases = new HashSet<>();
        while (Js.truthy(cache)) {
            Object shrunk = lookup(pattern);
            if (shrunk instanceof String) {
                if (!aliases.add(pattern)) {
                    throw new JsError("RangeError", "Maximum call stack size exceeded");
                }
                pattern = (String) shrunk;
            } else if (shrunk instanceof Map) {
                return explodeEntry(pattern, (Map<String, @Nullable Object>) shrunk);
            } else if (shrunk instanceof List) {
                return explodeEntry(pattern, arrayEntry((List<?>) shrunk));
            } else if (shrunk instanceof PlainObject.BuiltinFunction) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot assign to read only property 'name' of function '" +
                                                      shrunk + "'");
            } else if (Js.truthy(shrunk)) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot create property 'optionalDependencies' on " +
                                                      Js.typeOf(shrunk) + " '" + Js.string(shrunk) + "'");
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * {@code cache[pattern]}, as the integrity check reads the parsed lock.
     */
    @Nullable
    Object cached(String pattern) {
        if (cache instanceof Map || cache instanceof List) {
            return lookup(pattern);
        }
        return Js.get(cache, pattern);
    }

    // `pattern in cache && cache[pattern]`
    private @Nullable Object lookup(String pattern) {
        if (cache instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) cache;
            return map.containsKey(pattern) ? map.get(pattern) : inherited(pattern);
        }
        if (cache instanceof List) {
            List<?> list = (List<?>) cache;
            long index = Js.arrayIndex(pattern);
            if (index >= 0 && index < list.size()) {
                return list.get((int) index);
            }
            return "length".equals(pattern) ? (double) list.size() : inherited(pattern);
        }
        throw new JsError(JsError.TYPE_ERROR, "Cannot use 'in' operator to search for '" + pattern + "' in " +
                                              Js.string(cache));
    }

    // what an object literal or an array inherits, the entry yarn wrote onto the prototype included
    private @Nullable Object inherited(String pattern) {
        if (!(cache instanceof PlainObject) && !(cache instanceof List)) {
            return null;
        }
        PlainObject.BuiltinFunction function = cache instanceof List ? PlainObject.arrayFunction(pattern) :
                PlainObject.objectFunction(pattern);
        if (function != null) {
            return function;
        }
        if (PlainObject.PROTO.equals(pattern)) {
            if (prototype == null) {
                prototype = new LinkedHashMap<>();
            }
            return prototype;
        }
        return prototype == null ? null : prototype.get(pattern);
    }

    // explodeEntry sets its fields on the array itself; an object holding its indices stands for it
    private Map<String, @Nullable Object> arrayEntry(List<?> array) {
        return arrayEntries.computeIfAbsent(array, a -> {
            Map<String, @Nullable Object> entry = new LinkedHashMap<>();
            Js.assign(entry, a);
            return entry;
        });
    }

    void removePattern(String pattern) {
        if (cache instanceof Map) {
            ((Map<?, ?>) cache).remove(pattern);
        }
    }

    /**
     * The entries yarn writes for the resolver's patterns: each pattern in alphabetical order takes the entry of an
     * earlier pattern of the same name and remote, else a new one.
     */
    Map<String, @Nullable Object> getLockfile(Map<String, Manifest> patterns) {
        Map<String, @Nullable Object> lockfile = new LinkedHashMap<>();
        Map<String, Map<String, @Nullable Object>> seen = new HashMap<>();
        List<String> sortedPatternsKeys = new ArrayList<>(Js.keys(patterns));
        sortedPatternsKeys.sort(Misc::sortAlpha);

        for (String pattern : sortedPatternsKeys) {
            Manifest pkg = patterns.get(pattern);
            PackageRemote remote = pkg.remote;
            PackageReference ref = pkg.reference;
            if (ref == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "Package is missing a reference");
            }
            if (remote == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "Package is missing a remote");
            }

            String remoteKey = keyForRemote(remote);
            String pkgName = getName(pattern);

            String seenKey = remoteKey != null ? remoteKey + "#" + pkgName : null;
            Map<String, @Nullable Object> seenPattern = seenKey != null ? seen.get(seenKey) : null;

            if (seenPattern != null) {
                lockfile.put(pattern, seenPattern);
                continue;
            }

            Map<String, @Nullable Object> fields = new LinkedHashMap<>();
            fields.put("name", pkgName);
            fields.put("version", pkg.getVersion());
            fields.put("uid", pkg.getUid());
            fields.put("resolved", remote.resolved);
            fields.put("integrity", remote.integrity);
            fields.put("registry", remote.registry);
            fields.put("dependencies", pkg.get("dependencies"));
            fields.put("peerDependencies", pkg.get("peerDependencies"));
            fields.put("optionalDependencies", pkg.get("optionalDependencies"));
            fields.put("permissions", ref.permissions);
            fields.put("prebuiltVariants", pkg.get("prebuiltVariants"));
            Map<String, @Nullable Object> obj = implodeEntry(pattern, fields);

            lockfile.put(pattern, obj);

            if (seenKey != null) {
                seen.put(seenKey, obj);
            }
        }
        return lockfile;
    }
}
