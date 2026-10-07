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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of ssri 5.3.0 ({@code parse}, {@code stringify}, {@code fromHex} and the {@code Hash} and {@code Integrity}
 * strings), as yarn 1.22.22 bundles it.
 */
final class Ssri {

    private static final Pattern SRI_REGEX = Pattern.compile("^([^-]+)-([^?]+)([?\\S*]*)$");
    private static final Pattern WHITESPACE = Pattern.compile("[" + Js.WHITESPACE_CLASS + "]+");
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    private Ssri() {
    }

    static final class Hash {
        final String source;
        @Nullable
        String algorithm;
        @Nullable
        String digest;
        List<String> options = Collections.emptyList();

        Hash(String hash) {
            this.source = Js.trim(hash);
            Matcher match = SRI_REGEX.matcher(source);
            if (!match.find()) {
                return;
            }
            this.algorithm = match.group(1);
            this.digest = match.group(2);
            String rawOpts = match.group(3);
            this.options = rawOpts.isEmpty() ? Collections.emptyList() :
                    Arrays.asList(rawOpts.substring(1).split("\\?", -1));
        }

        @Override
        public String toString() {
            String opts = options.isEmpty() ? "" : "?" + String.join("?", options);
            return algorithm + "-" + digest + opts;
        }
    }

    static final class Integrity {
        final Map<String, List<Hash>> hashes = new LinkedHashMap<>();

        @Override
        public String toString() {
            List<String> parts = new ArrayList<>();
            for (String algorithm : Js.keys(hashes)) {
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
    }

    static Integrity parse(Object sri) {
        if (sri instanceof String) {
            return parseString((String) sri);
        }
        return parseString(stringify(sri));
    }

    private static Integrity parseString(String integrity) {
        Integrity acc = new Integrity();
        for (String string : WHITESPACE.split(Js.trim(integrity), -1)) {
            Hash hash = new Hash(string);
            if (Js.truthy(hash.algorithm) && Js.truthy(hash.digest)) {
                acc.hashes.computeIfAbsent(hash.algorithm, k -> new ArrayList<>()).add(hash);
            }
        }
        return acc;
    }

    static String stringify(Object obj) {
        if (obj instanceof Hash) {
            return obj.toString();
        }
        if (obj instanceof String) {
            return stringify(parse(obj));
        }
        return obj.toString();
    }

    /**
     * @throws JsError Node's {@code ERR_INVALID_ARG_TYPE} from {@code Buffer.from} on a digest that is neither a
     *                 string nor an array, a missing {@code shasum} among them.
     */
    static Integrity fromHex(@Nullable Object hexDigest, String algorithm) {
        byte[] bytes;
        if (hexDigest instanceof String) {
            bytes = hexToBytes((String) hexDigest);
        } else if (hexDigest instanceof List) {
            List<?> items = (List<?>) hexDigest;
            bytes = new byte[items.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) toNumber(items.get(i));
            }
        } else {
            throw JsError.invalidArgType("The first argument must be of type string or an instance of Buffer, " +
                                         "ArrayBuffer, or Array or an Array-like Object", hexDigest);
        }
        return parse(algorithm + "-" + Base64.getEncoder().encodeToString(bytes));
    }

    // Number(item) & 255 as Buffer.from reads an array's elements
    private static int toNumber(@Nullable Object item) {
        double d = 0;
        if (item instanceof Number) {
            d = ((Number) item).doubleValue();
        } else if (item instanceof Boolean) {
            d = (Boolean) item ? 1 : 0;
        } else if (item instanceof String && NUMBER.matcher(Js.trim((String) item)).matches()) {
            d = Double.parseDouble(Js.trim((String) item));
        }
        return Double.isNaN(d) || Double.isInfinite(d) ? 0 : (int) (long) d;
    }

    // Buffer.from(hex, 'hex') stops at the first pair that is not hexadecimal
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
