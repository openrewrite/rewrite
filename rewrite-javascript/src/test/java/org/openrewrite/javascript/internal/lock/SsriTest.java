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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ssri 5.3.0's, 13.0.0's and 13.0.1's own tests ({@code upstream/ssri-*.jsonl}), and 13.0.0 and 13.0.1 run on these
 * values under Node 24.
 */
class SsriTest {

    private static final String A = "sha512-AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBA" +
                                    "QEBAQEBAQEBAQ==";
    private static final String B = "sha512-AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA" +
                                    "gICAgICAgICAg==";
    private static final String S = "sha1-AwMDAwMDAwMDAwMDAwMDAwMDAwM=";
    private static final String T = "sha1-BAQEBAQEBAQEBAQEBAQEBAQEBAQ=";
    private static final String M = "md5-BQUFBQUFBQUFBQUFBQUFBQ==";

    private static final Ssri.Release V13 = Ssri.Release.V13_0_1;

    private static @Nullable String parse(String sri) {
        Ssri.Integrity parsed = Ssri.parse(sri, V13);
        return parsed == null ? null : parsed.toString();
    }

    private static boolean match(String integrity, String other) {
        return Objects.requireNonNull(Ssri.parse(integrity, V13)).match(other) != null;
    }

    private static String merge(String integrity, String other) {
        Ssri.Integrity merged = Objects.requireNonNull(Ssri.parse(integrity, V13));
        merged.merge(other);
        return merged.toString();
    }

    private static @Nullable String fromHex(String hex) {
        Ssri.Integrity integrity = Ssri.fromHex(hex, "sha1", V13);
        return integrity == null ? null : integrity.toString();
    }

    @Test
    void parse() {
        assertThat(parse("abc")).isNull();
        assertThat(parse("")).isNull();
        assertThat(parse("sha512-")).isNull();
        assertThat(parse("SHA512-x")).isNull();
        assertThat(parse(A + " abc " + S)).isEqualTo(A + " " + S);
        assertThat(parse(A + "?a?b")).isEqualTo(A + "?a?b");
        assertThat(parse(A + "?")).isEqualTo(A + "?");
        assertThat(parse("  " + A + "\n" + S + "  ")).isEqualTo(A + " " + S);
        assertThat(parse(A + " " + A)).isEqualTo(A + " " + A);
        assertThat(parse(S + " " + A + " " + T)).isEqualTo(S + " " + T + " " + A);
    }

    // a hash whose algorithm crypto.getHashes() does not list is dropped by the Hash constructor
    @Test
    void parseDropsAlgorithmsNodeLacks() {
        assertThat(parse("foo-YWJj")).isNull();
        assertThat(parse("whirlpool-YWJj")).isNull();
        assertThat(parse("md4-YWJj")).isNull();
        assertThat(parse("sm3-YWJj")).isEqualTo("sm3-YWJj");
        assertThat(parse(A + " foo-YWJj")).isEqualTo(A);
    }

    @Test
    void match() {
        assertThat(match(A, A)).isTrue();
        assertThat(match(A, B)).isFalse();
        assertThat(match(A, S)).isFalse();
        assertThat(match(S, A)).isFalse();
        assertThat(match(A + " " + S, S)).isTrue();
        assertThat(match(A + " " + S, B + " " + S)).isFalse();
        assertThat(match(A + " " + S, B + " " + T)).isFalse();
        assertThat(match(S + " " + M, M + " " + T)).isFalse();
        assertThat(match(A, "abc")).isFalse();
        assertThat(match(A + " " + S, S + " " + B)).isFalse();
        assertThat(match(M + " " + S, S)).isTrue();
        assertThat(match(A + " " + A, B + " " + A)).isTrue();
    }

    @Test
    void merge() {
        assertThat(merge(S, A)).isEqualTo(S + " " + A);
        assertThat(merge(A, S)).isEqualTo(A + " " + S);
        assertThat(merge(A, A)).isEqualTo(A);
        assertThat(merge(S, A + " " + M)).isEqualTo(S + " " + A + " " + M);
        assertThat(merge(A, "abc")).isEqualTo(A);
        assertThat(merge(A + "?x", A + "?y")).isEqualTo(A + "?x");
        assertThat(merge(S + " " + T, T)).isEqualTo(S + " " + T);
        assertThatThrownBy(() -> merge(A, B)).hasMessage("hashes do not match, cannot update integrity");
        assertThatThrownBy(() -> merge(A + " " + S, B + " " + S))
          .hasMessage("hashes do not match, cannot update integrity");
        assertThatThrownBy(() -> merge(A + " " + S, A + " " + T))
          .hasMessage("hashes do not match, cannot update integrity");
    }

    @Test
    void fromHex() {
        assertThat(fromHex("0303030303030303030303030303030303030303")).isEqualTo(S);
        assertThat(fromHex("03zz")).isEqualTo("sha1-Aw==");
        assertThat(fromHex("")).isNull();
    }

    // ssri 13.0.0 (npm 11.9.0) and 13.0.1 (npm 11.10.0) on integrity strings that name an algorithm Node cannot hash
    // with: 13.0.0 keeps it, and trips over one spelled as a member its Integrity inherits; 13.0.1 drops both
    @Test
    void algorithmNodeLacksBefore13_0_1() {
        Ssri.Release v13_0_0 = Ssri.Release.V13_0_0;
        assertThat(String.valueOf(Ssri.parse("sha999-abc sha512-def", v13_0_0))).isEqualTo("sha999-abc sha512-def");
        assertThat(String.valueOf(Ssri.parse("sha999-abc sha512-def", V13))).isEqualTo("sha512-def");
        assertThat(String.valueOf(Ssri.parse("md4-abc", v13_0_0))).isEqualTo("md4-abc");
        assertThat(Ssri.parse("md4-abc", V13)).isNull();
        assertThat(Ssri.parse("SHA512-abc", v13_0_0)).isNull();
        assertThat(String.valueOf(Ssri.fromHex("deadbeef", "sha999", v13_0_0))).isEqualTo("sha999-3q2+7w==");
        assertThat(Ssri.fromHex("deadbeef", "sha999", V13)).isNull();

        Ssri.Integrity held = Objects.requireNonNull(Ssri.parse("sha999-abc", v13_0_0));
        assertThat(held.match("sha512-def")).isNull();
        held.merge("sha512-def");
        assertThat(held).hasToString("sha999-abc sha512-def");
        Ssri.Integrity known = Objects.requireNonNull(Ssri.parse("sha512-def", v13_0_0));
        known.merge("sha999-abc");
        assertThat(known).hasToString("sha512-def sha999-abc");
        Ssri.Integrity newer = Objects.requireNonNull(Ssri.parse("sha512-def", V13));
        newer.merge("sha999-abc");
        assertThat(newer).hasToString("sha512-def");

        for (String inherited : new String[]{"constructor-abc", "match-abc", "merge-abc", "concat-abc"}) {
            assertThatThrownBy(() -> Ssri.parse(inherited, v13_0_0)).hasMessage("acc[algo].push is not a function");
            assertThat(Ssri.parse(inherited, V13)).isNull();
        }
        assertThatThrownBy(() -> Objects.requireNonNull(Ssri.parse("sha512-def", v13_0_0)).match("match-abc"))
          .hasMessage("acc[algo].push is not a function");
    }

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (Ssri.Release release : Ssri.Release.values()) {
            for (Call call : UpstreamCalls.read("ssri-" + version(release) + ".jsonl.gz")) {
                if (notPorted(release, call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String version(Ssri.Release release) {
        return release.name().substring(1).replace('_', '.');
    }

    // the reason the port does not answer a call, or null when it does
    private static @Nullable String notPorted(Ssri.Release release, Call call) {
        switch (call.getFn()) {
            case "parse":
            case "stringify":
            case "Integrity#match":
            case "Integrity#merge":
                return options(call.arg(1)) ? "options (strict, single, sep): no package manager passes them" : null;
            case "fromHex":
                return options(call.arg(2)) ? "options (strict, single, sep): no package manager passes them" : null;
            case "Integrity#toString":
            case "Integrity#toJSON":
            case "Hash#toString":
            case "Hash#toJSON":
                return options(call.arg(0)) ? "options (strict, single, sep): no package manager passes them" : null;
            case "Integrity#isEmpty":
                return null;
            case "Integrity#pickAlgorithm":
                return call.arg(0) instanceof Map && ((Map<?, ?>) call.arg(0)).containsKey("pickAlgorithm") ?
                        "a pickAlgorithm function: no package manager passes one" : null;
            case "checkData":
                return options(call.arg(2)) ? "checkData's options (error, size): no package manager passes them" :
                        null;
            default:
                return call.getFn() + ": not on a package manager's lock path";
        }
    }

    private static boolean options(@Nullable Object opts) {
        return opts instanceof Map ? !((Map<?, ?>) opts).isEmpty() : opts != EcmaScript.UNDEFINED && opts != null;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(Ssri.Release release, Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> answer(release, call)).hasMessage((String) threw.get("message"));
            return;
        }
        Object answer = answer(release, call);
        Object expected = "Integrity#merge".equals(call.getFn()) ? call.getSelfAfter() : call.getOut();
        assertThat(written(answer)).isEqualTo(written(expected));
    }

    private static @Nullable Object answer(Ssri.Release release, Call call) {
        Object self = call.getSelf() == null ? null : value(release, call.getSelf());
        switch (call.getFn()) {
            case "parse":
                return Ssri.parse(value(release, call.arg(0)), release, EcmaScript::string);
            case "stringify":
                return Ssri.stringify(Objects.requireNonNull(value(release, call.arg(0))), release);
            case "fromHex":
                return Ssri.fromHex(call.arg(0), (String) Objects.requireNonNull(call.arg(1)), release);
            case "Integrity#match":
                Ssri.Hash found = ((Ssri.Integrity) Objects.requireNonNull(self))
                        .match(value(release, call.arg(0)));
                return found == null ? false : found;
            case "Integrity#merge":
                ((Ssri.Integrity) Objects.requireNonNull(self)).merge(value(release, call.arg(0)));
                return self;
            case "Integrity#isEmpty":
                return ((Ssri.Integrity) Objects.requireNonNull(self)).isEmpty();
            case "Integrity#pickAlgorithm":
                List<String> among = new ArrayList<>();
                if (call.arg(1) instanceof List) {
                    for (Object algorithm : (List<?>) call.arg(1)) {
                        among.add((String) algorithm);
                    }
                }
                return ((Ssri.Integrity) Objects.requireNonNull(self)).pickAlgorithm(among);
            case "checkData":
                Ssri.Hash matched = Ssri.checkData(data(call.arg(0)), value(release, call.arg(1)), release);
                return matched == null ? false : matched;
            default:
                return String.valueOf(self);
        }
    }

    // an Integrity or Hash a row names by its string, rebuilt; any other value as the row holds it
    private static @Nullable Object value(Ssri.Release release, @Nullable Object value) {
        if (value instanceof Map && "integrity".equals(((Map<?, ?>) value).get("$"))) {
            Ssri.Integrity parsed = Ssri.parse((String) Objects.requireNonNull(((Map<?, ?>) value).get("string")),
                    release, EcmaScript::string);
            return parsed == null ? new Ssri.Integrity(release) : parsed;
        }
        if (value instanceof Map && "hash".equals(((Map<?, ?>) value).get("$"))) {
            return new Ssri.Hash((String) Objects.requireNonNull(((Map<?, ?>) value).get("source")), release);
        }
        return value;
    }

    // a Buffer a row writes as its hex, or a string as Node hashes it (UTF-8)
    private static byte[] data(@Nullable Object value) {
        if (value instanceof String) {
            return ((String) value).getBytes(StandardCharsets.UTF_8);
        }
        String hex = (String) Objects.requireNonNull(((Map<?, ?>) Objects.requireNonNull(value)).get("hex"));
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    // what a row writes of a value: an Integrity as its string and algorithms, a Hash as its string
    private static @Nullable Object written(@Nullable Object value) {
        if (value instanceof Ssri.Integrity) {
            Ssri.Integrity integrity = (Ssri.Integrity) value;
            return integrity + " " + new ArrayList<>(integrity.getHashes().keySet());
        }
        if (value instanceof Ssri.Hash) {
            return "hash " + value;
        }
        if (value instanceof Map && "integrity".equals(((Map<?, ?>) value).get("$"))) {
            return ((Map<?, ?>) value).get("string") + " " + ((Map<?, ?>) value).get("algorithms");
        }
        if (value instanceof Map && "hash".equals(((Map<?, ?>) value).get("$"))) {
            return "hash " + ((Map<?, ?>) value).get("string");
        }
        return value;
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (Ssri.Release release : Ssri.Release.values()) {
            for (Call call : UpstreamCalls.read("ssri-" + version(release) + ".jsonl.gz")) {
                String reason = notPorted(release, call);
                if (reason != null) {
                    notPorted.merge(version(release) + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        for (String release : new String[]{"13.0.0", "13.0.1", "5.3.0"}) {
            boolean v13 = !"5.3.0".equals(release);
            boolean v13_0_1 = "13.0.1".equals(release);
            String path = ": not on a package manager's lock path";
            expected.put(release + " Hash#hexDigest" + path, 1);
            if (v13) {
                expected.put(release + " Hash#match" + path, v13_0_1 ? 9 : 8);
            }
            expected.put(release + " Integrity#concat" + path, 7);
            expected.put(release + " Integrity#hexDigest" + path, 1);
            expected.put(release + " a pickAlgorithm function: no package manager passes one", 1);
            expected.put(release + " checkData's options (error, size): no package manager passes them", v13 ? 6 : 5);
            expected.put(release + " checkStream" + path, v13 ? 13 : 12);
            expected.put(release + " create" + path, v13 ? 3 : 2);
            expected.put(release + " fromData" + path, v13 ? 13 : 9);
            expected.put(release + " fromStream" + path, 3);
            expected.put(release + " integrityStream" + path, v13 ? 12 : 4);
            expected.put(release + " options (strict, single, sep): no package manager passes them",
              v13_0_1 ? 40 : v13 ? 39 : 23);
        }
        assertThat(notPorted).isEqualTo(expected);
    }
}
