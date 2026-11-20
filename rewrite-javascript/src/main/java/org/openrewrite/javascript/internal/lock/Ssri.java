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
package org.openrewrite.javascript.internal.lock;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of ssri ({@code parse}, {@code stringify}, {@code fromHex}, {@code checkData}, {@code Hash}, {@code Integrity},
 * without options): 5.3.0, which yarn 1.22.22 bundles, 13.0.1, which npm 11.17.0 and pnpm 11.2.2 bundle, and 13.0.0,
 * which npm 11.6.3 to 11.9.0 bundle and whose answers 10.0.5 to 12.0.0 (npm 10.0.0 to 11.6.2) give too.
 */
public final class Ssri {

    public enum Release {
        V5_3_0,
        V13_0_0,
        V13_0_1
    }

    private static final Pattern SRI_5_3_0 = Pattern.compile("^([^-]+)-([^?]+)([?\\S*]*)\\z");
    private static final Pattern SRI_13_0_1 = Pattern.compile("^([a-z0-9]+)-([^?]+)(\\?[?\\S*]*)?\\z");
    private static final Pattern WHITESPACE = Pattern.compile("[" + EcmaScript.WHITESPACE_CLASS + "]+");

    // the names in Node 24's crypto.getHashes() that 13.0.1's algorithm pattern can match
    private static final Set<String> NODE_HASHES = new HashSet<>(Arrays.asList("blake2b512", "blake2s256", "md5",
            "ripemd", "ripemd160", "rmd160", "sha1", "sha224", "sha256", "sha384", "sha512", "shake128", "shake256",
            "sm3"));

    // DEFAULT_PRIORITY, which 13.0.1 filters by Node 24's crypto.getHashes()
    private static final List<String> PRIORITY_5_3_0 = Arrays.asList("md5", "whirlpool", "sha1", "sha224", "sha256",
            "sha384", "sha512", "sha3", "sha3-256", "sha3-384", "sha3-512", "sha3_256", "sha3_384", "sha3_512");
    private static final List<String> PRIORITY_13_0_1 = Arrays.asList("md5", "sha1", "sha224", "sha256", "sha384",
            "sha512", "sha3-256", "sha3-384", "sha3-512");

    // the members an Integrity inherits whose names 13.0.0's algorithm pattern can match
    private static final Set<String> INHERITED_13_0_0 = new HashSet<>(Arrays.asList("constructor", "concat", "merge",
            "match"));

    private static final Set<String> JAVA_DIGESTS = new HashSet<>(Arrays.asList("md5", "sha1", "sha224", "sha256",
            "sha384", "sha512"));

    private Ssri() {
    }

    @Getter
    public static final class Hash {
        private final String source;

        /**
         * Null where 5.3.0 leaves it undefined; 13.0.0 and 13.0.1 start from the empty string.
         */
        private @Nullable String algorithm;

        private @Nullable String digest;
        private List<String> options = Collections.emptyList();

        Hash(String hash, Release release) {
            this.source = EcmaScript.trim(hash);
            boolean v13 = release != Release.V5_3_0;
            if (v13) {
                algorithm = "";
                digest = "";
            }
            Matcher match = (v13 ? SRI_13_0_1 : SRI_5_3_0).matcher(source);
            // 13.0.1 takes no algorithm Node cannot hash with
            if (!match.find() || release == Release.V13_0_1 && !NODE_HASHES.contains(match.group(1))) {
                return;
            }
            this.algorithm = match.group(1);
            this.digest = match.group(2);
            String rawOpts = match.group(3);
            if (rawOpts != null && !rawOpts.isEmpty()) {
                this.options = Arrays.asList(rawOpts.substring(1).split("\\?", -1));
            }
        }

        @Override
        public String toString() {
            String opts = options.isEmpty() ? "" : "?" + String.join("?", options);
            return (algorithm == null ? "undefined" : algorithm) + "-" + (digest == null ? "undefined" : digest) +
                   opts;
        }
    }

    public static final class Integrity {
        private final Release release;

        /**
         * The hashes by algorithm, the object's own keys.
         */
        @Getter
        private final Map<String, List<Hash>> hashes = new LinkedHashMap<>();

        Integrity(Release release) {
            this.release = release;
        }

        public boolean isEmpty() {
            return hashes.isEmpty();
        }

        @Override
        public String toString() {
            List<String> parts = new ArrayList<>();
            for (String algorithm : EcmaScript.keys(hashes)) {
                List<String> strings = new ArrayList<>();
                for (Hash hash : hashes.get(algorithm)) {
                    String s = hash.toString();
                    if (!s.isEmpty()) {
                        strings.add(s);
                    }
                }
                String joined = String.join(" ", strings);
                if (!joined.isEmpty()) {
                    parts.add(joined);
                }
            }
            return String.join(" ", parts);
        }

        /**
         * {@code integrity.match(other)}: a hash of the strongest algorithm the other names (13.0.1: of those both
         * name) sharing the other's digest, null for {@code false}; 5.3.0's Error when the other names none.
         */
        public @Nullable Hash match(@Nullable Object integrity) {
            Integrity other = parse(integrity, release, EcmaScript::string);
            if (other == null) {
                return null;
            }
            String algo = other.pickAlgorithm(release == Release.V5_3_0 ? Collections.emptyList() : hashes.keySet());
            if (algo == null || !hashes.containsKey(algo) || !other.hashes.containsKey(algo)) {
                return null;
            }
            for (Hash hash : hashes.get(algo)) {
                for (Hash otherHash : other.hashes.get(algo)) {
                    if (String.valueOf(hash.digest).equals(otherHash.digest)) {
                        return hash;
                    }
                }
            }
            return null;
        }

        /**
         * 13.0.1's {@code integrity.merge(other)}: the algorithms this lacks are added; one both name must share a
         * digest, else a JsError.
         */
        public void merge(@Nullable Object integrity) {
            Integrity other = parse(integrity, release, EcmaScript::string);
            if (other == null) {
                return;
            }
            for (Map.Entry<String, List<Hash>> entry : other.hashes.entrySet()) {
                List<Hash> have = hashes.get(entry.getKey());
                if (have == null) {
                    hashes.put(entry.getKey(), entry.getValue());
                    continue;
                }
                boolean shared = false;
                for (Hash hash : have) {
                    for (Hash otherHash : entry.getValue()) {
                        shared |= String.valueOf(hash.digest).equals(otherHash.digest);
                    }
                }
                if (!shared) {
                    throw new JsError(JsError.ERROR, "hashes do not match, cannot update integrity");
                }
            }
        }

        /**
         * {@code pickAlgorithm(opts, among)}: the algorithm of highest priority, over those {@code among} names when
         * it names any (13.0.1 only); 5.3.0's Error for an integrity with no algorithm.
         */
        public @Nullable String pickAlgorithm(Collection<String> among) {
            if (release == Release.V5_3_0 && hashes.isEmpty()) {
                throw new JsError(JsError.ERROR, "No algorithms available for " + EcmaScript.quote(toString()));
            }
            List<String> priority = release == Release.V5_3_0 ? PRIORITY_5_3_0 : PRIORITY_13_0_1;
            String picked = null;
            for (String algorithm : EcmaScript.keys(hashes)) {
                if (!among.isEmpty() && !among.contains(algorithm)) {
                    continue;
                }
                if (picked == null ||
                    priority.indexOf(picked.toLowerCase()) < priority.indexOf(algorithm.toLowerCase())) {
                    picked = algorithm;
                }
            }
            return picked;
        }
    }

    /**
     * {@code ssri.parse(sri)} of a string, a {@link Hash} or an {@link Integrity}.
     */
    public static @Nullable Integrity parse(Object sri, Release release) {
        return parse(sri, release, EcmaScript::string);
    }

    /**
     * {@code ssri.parse(sri)}: 13.0.0 and 13.0.1 answer null for a falsy value and for one with no hash, 5.3.0 an
     * empty {@link Integrity}; another object is read through the port's template literal, {@code `${value}`}.
     */
    public static @Nullable Integrity parse(@Nullable Object sri, Release release,
                                            Function<@Nullable Object, String> template) {
        Integrity parsed;
        if (release != Release.V5_3_0 && !EcmaScript.truthy(sri)) {
            return null;
        }
        if (sri instanceof String) {
            parsed = parseString((String) sri, release);
        } else if (sri instanceof Hash || sri instanceof Integrity) {
            parsed = parseString(stringify(sri, release), release);
        } else {
            if (sri == null || sri == EcmaScript.UNDEFINED) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " + EcmaScript.string(sri) +
                                                      " (reading 'algorithm')");
            }
            parsed = parseString(EcmaScript.truthy(get(sri, "algorithm")) && EcmaScript.truthy(get(sri, "digest")) ?
                    hashString(sri, template) : integrityString(sri, template), release);
        }
        return release != Release.V5_3_0 && parsed.isEmpty() ? null : parsed;
    }

    private static Integrity parseString(String integrity, Release release) {
        Integrity acc = new Integrity(release);
        for (String string : WHITESPACE.split(EcmaScript.trim(integrity), -1)) {
            Hash hash = new Hash(string, release);
            if (EcmaScript.truthy(hash.algorithm) && EcmaScript.truthy(hash.digest)) {
                // 13.0.0 asks the Integrity itself for the algorithm's hashes and finds a member it inherits
                if (release == Release.V13_0_0 && INHERITED_13_0_0.contains(hash.algorithm)) {
                    throw new JsError(JsError.TYPE_ERROR, "acc[algo].push is not a function");
                }
                acc.hashes.computeIfAbsent(hash.algorithm, k -> new ArrayList<>()).add(hash);
            }
        }
        return acc;
    }

    /**
     * {@code ssri.stringify(obj)} of a string, a {@link Hash}, an {@link Integrity}, or an object read as one.
     */
    public static String stringify(Object obj, Release release) {
        if (obj instanceof Hash || obj instanceof Integrity) {
            return obj.toString();
        }
        if (obj instanceof String) {
            Integrity parsed = parse(obj, release);
            if (parsed == null) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'algorithm')");
            }
            return parsed.toString();
        }
        return EcmaScript.truthy(get(obj, "algorithm")) && EcmaScript.truthy(get(obj, "digest")) ?
                hashString(obj, EcmaScript::string) : integrityString(obj, EcmaScript::string);
    }

    // Integrity.prototype.toString.call(obj)
    private static String integrityString(Object obj, Function<@Nullable Object, String> template) {
        List<String> parts = new ArrayList<>();
        for (String k : EcmaScript.keys(obj)) {
            Object hashes = get(obj, k);
            if (hashes == null) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'map')");
            }
            if (!(hashes instanceof List)) {
                throw new JsError(JsError.TYPE_ERROR, "this[k].map is not a function");
            }
            List<String> strings = new ArrayList<>();
            for (Object hash : (List<?>) hashes) {
                strings.add(hashString(hash, template));
            }
            strings.removeIf(String::isEmpty);
            String joined = String.join(" ", strings);
            if (!joined.isEmpty()) {
                parts.add(joined);
            }
        }
        return String.join(" ", parts);
    }

    // Hash.prototype.toString.call(hash)
    private static String hashString(@Nullable Object hash, Function<@Nullable Object, String> template) {
        if (hash == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'options')");
        }
        Object options = hash instanceof Map ? ((Map<?, ?>) hash).get("options") : null;
        String opts = "";
        if (EcmaScript.truthy(options) && EcmaScript.truthy(lengthOf(options))) {
            if (!(options instanceof List)) {
                throw new JsError(JsError.TYPE_ERROR, "this.options.join is not a function");
            }
            List<String> joined = new ArrayList<>();
            for (Object option : (List<?>) options) {
                joined.add(option == null ? "" : template.apply(option));
            }
            opts = "?" + String.join("?", joined);
        }
        return field(hash, "algorithm", template) + "-" + field(hash, "digest", template) + opts;
    }

    private static @Nullable Object lengthOf(Object value) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).get("length");
        }
        if (value instanceof List) {
            return (double) ((List<?>) value).size();
        }
        return value instanceof String ? (Object) (double) ((String) value).length() : null;
    }

    // `${hash[key]}`
    private static String field(Object hash, String key, Function<@Nullable Object, String> template) {
        if (!(hash instanceof Map)) {
            return "undefined";
        }
        Map<?, ?> map = (Map<?, ?>) hash;
        return map.containsKey(key) && map.get(key) == null ? "null" : template.apply(map.get(key));
    }

    // value[key] of a map or an array, null when it has none
    private static @Nullable Object get(Object value, String key) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).get(key);
        }
        long index = EcmaScript.arrayIndex(key);
        if (value instanceof List && index >= 0 && index < ((List<?>) value).size()) {
            return ((List<?>) value).get((int) index);
        }
        return null;
    }

    /**
     * {@code ssri.checkData(data, sri)}: the hash of {@code sri} the data's digest matches, null for {@code false}.
     * @throws CannotKnow for an algorithm whose digest Java 8 lacks
     */
    public static @Nullable Hash checkData(byte[] data, @Nullable Object sri, Release release) {
        Integrity parsed = parse(sri, release, EcmaScript::string);
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        String algorithm = String.valueOf(parsed.pickAlgorithm(Collections.emptyList()));
        Map<String, @Nullable Object> newSri = new LinkedHashMap<>();
        newSri.put("algorithm", algorithm);
        newSri.put("digest", digest(data, algorithm));
        return Objects.requireNonNull(parse(newSri, release, EcmaScript::string)).match(parsed);
    }

    private static String digest(byte[] data, String algorithm) {
        String name = algorithm.toLowerCase(Locale.ROOT);
        if (!JAVA_DIGESTS.contains(name)) {
            throw new CannotKnow("the " + algorithm + " digest of the data (Java 8 has none)");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("md5".equals(name) ? "MD5" :
                    "SHA-" + name.substring("sha".length()));
            return Base64.getEncoder().encodeToString(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code ssri.fromHex(hexDigest, algorithm)}: {@code Buffer.from(hexDigest, 'hex')} (it stops at a pair that is not
     * hexadecimal, an array's items as {@code Number(item) & 255}); ERR_INVALID_ARG_TYPE for any other digest.
     */
    public static @Nullable Integrity fromHex(@Nullable Object hexDigest, String algorithm, Release release) {
        byte[] bytes;
        if (hexDigest instanceof String) {
            bytes = hexToBytes((String) hexDigest);
        } else if (hexDigest instanceof List) {
            List<?> items = (List<?>) hexDigest;
            bytes = new byte[items.size()];
            for (int i = 0; i < bytes.length; i++) {
                double d = EcmaScript.toNumber(items.get(i));
                bytes[i] = (byte) (Double.isNaN(d) || Double.isInfinite(d) ? 0 : (long) d);
            }
        } else {
            throw new JsError("TypeError [ERR_INVALID_ARG_TYPE]", "The first argument must be of type string or an " +
                    "instance of Buffer, ArrayBuffer, or Array or an Array-like Object. Received " +
                    specificType(hexDigest));
        }
        return parse(algorithm + "-" + Base64.getEncoder().encodeToString(bytes), release);
    }

    private static String specificType(@Nullable Object value) {
        if (value == null || value == EcmaScript.UNDEFINED) {
            return "undefined";
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty() ? "[Object: null prototype] {}" : "[Object: null prototype]";
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return "type number (" + (d == 0 && 1 / d < 0 ? "-0" : EcmaScript.numberToString(d)) + ")";
        }
        return "type " + EcmaScript.typeOf(value) + " (" + value + ")";
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        int n = 0;
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                break;
            }
            out[n++] = (byte) ((hi << 4) | lo);
        }
        return Arrays.copyOf(out, n);
    }
}
