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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The part of ssri that pacote applies to a registry manifest's {@code dist}: parsing an integrity string
 * back to its canonical form, and a sha1 integrity from a hex {@code shasum}.
 */
final class Ssri {

    private static final Pattern SRI = Pattern.compile("^([a-z0-9]+)-([^?]+)([?\\S*]*)$");

    private Ssri() {
    }

    /**
     * {@code String(ssri.parse(sri))}: the hashes that parse, by algorithm in first-seen order.
     */
    static String parse(String sri) {
        Map<String, List<String>> byAlgorithm = new LinkedHashMap<>();
        for (String entry : Js.trim(sri).split("\\s+")) {
            Matcher m = SRI.matcher(entry);
            if (!m.matches()) {
                continue;
            }
            byAlgorithm.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(m.group(1) + "-" + m.group(2) +
                                                                                 m.group(3));
        }
        List<String> out = new ArrayList<>();
        for (List<String> hashes : byAlgorithm.values()) {
            out.add(String.join(" ", hashes));
        }
        return String.join(" ", out);
    }

    /**
     * {@code Integrity.match(other)}: over the strongest algorithm both carry, some digest is shared. Two
     * integrities with no algorithm in common match.
     */
    static boolean match(String integrity, String other) {
        Map<String, List<String>> mine = digests(integrity);
        Map<String, List<String>> theirs = digests(other);
        String algorithm = null;
        for (String candidate : theirs.keySet()) {
            if (mine.containsKey(candidate) && (algorithm == null || priority(candidate) > priority(algorithm))) {
                algorithm = candidate;
            }
        }
        if (algorithm == null) {
            return true;
        }
        for (String digest : mine.get(algorithm)) {
            if (theirs.get(algorithm).contains(digest)) {
                return true;
            }
        }
        return false;
    }

    private static int priority(String algorithm) {
        switch (algorithm) {
            case "sha256":
                return 0;
            case "sha384":
                return 1;
            case "sha512":
                return 2;
            default:
                return -1;
        }
    }

    private static Map<String, List<String>> digests(String sri) {
        Map<String, List<String>> byAlgorithm = new LinkedHashMap<>();
        for (String entry : Js.trim(sri).split("\\s+")) {
            Matcher m = SRI.matcher(entry);
            if (m.matches()) {
                byAlgorithm.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(m.group(2));
            }
        }
        return byAlgorithm;
    }

    /**
     * {@code ssri.fromHex(hex, algorithm)}: Node's {@code Buffer.from(hex, 'hex')} stops at the first
     * character pair that is not hex.
     */
    static String fromHex(String hex, String algorithm) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                break;
            }
            bytes.write(hi << 4 | lo);
        }
        return algorithm + "-" + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
