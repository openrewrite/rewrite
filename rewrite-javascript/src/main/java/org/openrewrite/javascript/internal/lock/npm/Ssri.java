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

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The part of ssri (npm 11.17.0) that pacote applies to a registry manifest's {@code dist} and to the
 * integrity the caller holds. An integrity is carried as {@code String(Integrity)}: hashes grouped by
 * algorithm in first-seen order.
 */
final class Ssri {

    private static final Pattern SRI = Pattern.compile("^([a-z0-9]+)-([^?]+)([?\\S*]*)$");

    // ssri's DEFAULT_PRIORITY after Node 24's crypto.getHashes() filter
    private static final List<String> PRIORITY = Arrays.asList("md5", "sha1", "sha224", "sha256", "sha384",
            "sha512", "sha3-256", "sha3-384", "sha3-512");

    private Ssri() {
    }

    /**
     * {@code String(ssri.parse(sri))}: the hashes that parse, or null when none does, as {@code parse}
     * returns null.
     */
    static @Nullable String parse(String sri) {
        Map<String, List<String>> hashes = hashes(sri);
        return hashes.isEmpty() ? null : render(hashes);
    }

    static Set<String> algorithms(String integrity) {
        return hashes(integrity).keySet();
    }

    /**
     * {@code Integrity.match(other)}: over the strongest algorithm both carry, some digest is shared;
     * false when they share no algorithm.
     */
    static boolean match(String integrity, String other) {
        Map<String, List<String>> mine = hashes(integrity);
        Map<String, List<String>> theirs = hashes(other);
        String algorithm = pickAlgorithm(theirs, mine.keySet());
        return algorithm != null && shareDigest(mine.get(algorithm), theirs.get(algorithm));
    }

    /**
     * {@code Integrity.merge(other)}: the algorithms the integrity lacks are added, an algorithm both
     * carry must share a digest.
     */
    static String merge(String integrity, String other) {
        Map<String, List<String>> mine = hashes(integrity);
        for (Map.Entry<String, List<String>> entry : hashes(other).entrySet()) {
            List<String> have = mine.get(entry.getKey());
            if (have == null) {
                mine.put(entry.getKey(), entry.getValue());
            } else if (!shareDigest(have, entry.getValue())) {
                throw new NpmRejection(null, "hashes do not match, cannot update integrity", null);
            }
        }
        return render(mine);
    }

    /**
     * {@code ssri.fromHex(hex, algorithm)}: Node's {@code Buffer.from(hex, 'hex')} stops at the first
     * character pair that is not hex; no byte at all is no integrity.
     */
    static @Nullable String fromHex(String hex, String algorithm) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                break;
            }
            bytes.write(hi << 4 | lo);
        }
        return parse(algorithm + "-" + Base64.getEncoder().encodeToString(bytes.toByteArray()));
    }

    private static @Nullable String pickAlgorithm(Map<String, List<String>> of, Set<String> among) {
        String picked = null;
        for (String algorithm : of.keySet()) {
            if (!among.isEmpty() && !among.contains(algorithm)) {
                continue;
            }
            if (picked == null || PRIORITY.indexOf(algorithm) > PRIORITY.indexOf(picked)) {
                picked = algorithm;
            }
        }
        return picked;
    }

    private static boolean shareDigest(List<String> hashes, List<String> others) {
        for (String hash : hashes) {
            for (String other : others) {
                if (digest(hash).equals(digest(other))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String digest(String hash) {
        Matcher m = SRI.matcher(hash);
        return m.matches() ? m.group(2) : hash;
    }

    private static Map<String, List<String>> hashes(String sri) {
        Map<String, List<String>> byAlgorithm = new LinkedHashMap<>();
        for (String entry : Js.trim(sri).split("\\s+")) {
            Matcher m = SRI.matcher(entry);
            if (m.matches()) {
                byAlgorithm.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(entry);
            }
        }
        return byAlgorithm;
    }

    private static String render(Map<String, List<String>> hashes) {
        List<String> out = new ArrayList<>();
        for (List<String> entries : hashes.values()) {
            out.add(String.join(" ", entries));
        }
        return String.join(" ", out);
    }
}
