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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * js-yaml 3.13.1's and 3.14.1's own tests, js-yaml 4.1.0's run against @zkochan/js-yaml 0.0.11, and the documents
 * where the loaders differ ({@code upstream/js-yaml-*}), for the calls yarn, Berry and pnpm make.
 */
class JsYamlTest {

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (JsYaml.Release release : JsYaml.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (notPorted(release, call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(JsYaml.Release release) {
        return "js-yaml-" + release.name().toLowerCase().replaceFirst("^v", "").replace('_', '.')
                .replaceFirst("^zkochan\\.", "zkochan-") + ".jsonl.gz";
    }

    private static @Nullable String notPorted(JsYaml.Release release, Call call) {
        if (nestedPastTheRows(call.getOut())) {
            return "a document that holds itself, which the rows write only so deep";
        }
        if (release == JsYaml.Release.ZKOCHAN_0_0_11) {
            if (!"load".equals(call.getFn())) {
                return call.getFn() + ": pnpm loads one document, and dumps with lockfileYamlDump's options";
            }
            return call.has(1) ? "load with options: pnpm passes none" : null;
        }
        if (!"load".equals(call.getFn()) && !"safeLoad".equals(call.getFn())) {
            return call.getFn() + ": yarn and Berry load one document";
        }
        Object options = call.arg(1);
        if (!(options instanceof Map) || !"FAILSAFE_SCHEMA".equals(schema((Map<?, ?>) options))) {
            return "a schema other than FAILSAFE_SCHEMA: yarn and Berry ask that one";
        }
        for (Object key : ((Map<?, ?>) options).keySet()) {
            if (!"schema".equals(key) && !"json".equals(key)) {
                return "options." + key + ": yarn and Berry pass json only";
            }
        }
        return null;
    }

    private static boolean nestedPastTheRows(@Nullable Object value) {
        if (value instanceof Map) {
            Object kind = ((Map<?, ?>) value).get("$");
            if ("deep".equals(kind) || "large".equals(kind)) {
                return true;
            }
            for (Object item : ((Map<?, ?>) value).values()) {
                if (nestedPastTheRows(item)) {
                    return true;
                }
            }
        } else if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (nestedPastTheRows(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable Object schema(Map<?, ?> options) {
        Object schema = options.get("schema");
        return schema instanceof Map ? ((Map<?, ?>) schema).get("name") : null;
    }

    private static @Nullable Object answer(JsYaml.Release release, Call call) {
        String input = (String) call.arg(0);
        if (release == JsYaml.Release.ZKOCHAN_0_0_11) {
            return JsYaml.load(input);
        }
        boolean json = EcmaScript.truthy(((Map<?, ?>) call.arg(1)).get("json"));
        Object document = JsYaml.safeLoadFailsafe(input, new JsYaml.Options(release, json, LinkedHashMap::new));
        // safeLoadFailsafe answers null where 3.x's safeLoad answers undefined, for a stream without documents
        return document == null && JsYaml.load(input) == EcmaScript.UNDEFINED ? EcmaScript.UNDEFINED : document;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(JsYaml.Release release, Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw == null) {
            assertThat(UpstreamCalls.shape(written(answer(release, call))))
              .isEqualTo(UpstreamCalls.shape(call.getOut()));
        } else {
            assertThatThrownBy(() -> answer(release, call)).hasMessage((String) threw.get("message"));
        }
    }

    // a value as the rows write it: a Date by its time, bytes as hex
    private static @Nullable Object written(@Nullable Object value) {
        if (value instanceof JsYaml.Timestamp) {
            Map<String, @Nullable Object> date = new LinkedHashMap<>();
            date.put("$", "date");
            date.put("time", ((JsYaml.Timestamp) value).getTime());
            return date;
        }
        if (value instanceof byte[]) {
            StringBuilder hex = new StringBuilder();
            for (byte b : (byte[]) value) {
                hex.append(String.format("%02x", b));
            }
            Map<String, @Nullable Object> bytes = new LinkedHashMap<>();
            bytes.put("$", "bytes");
            bytes.put("hex", hex.toString());
            return bytes;
        }
        if (value instanceof Map) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                out.put(String.valueOf(e.getKey()), written(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<@Nullable Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(written(item));
            }
            return out;
        }
        return value;
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (JsYaml.Release release : JsYaml.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                String reason = notPorted(release, call);
                if (reason != null) {
                    notPorted.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V3_13_1 a document that holds itself, which the rows write only so deep", 1);
        expected.put("V3_13_1 a schema other than FAILSAFE_SCHEMA: yarn and Berry ask that one", 139);
        expected.put("V3_13_1 dump: yarn and Berry load one document", 97);
        expected.put("V3_13_1 loadAll: yarn and Berry load one document", 95);
        expected.put("V3_13_1 safeDump: yarn and Berry load one document", 51);
        expected.put("V3_13_1 safeLoadAll: yarn and Berry load one document", 1);
        expected.put("V3_14_1 a document that holds itself, which the rows write only so deep", 1);
        expected.put("V3_14_1 a schema other than FAILSAFE_SCHEMA: yarn and Berry ask that one", 143);
        expected.put("V3_14_1 dump: yarn and Berry load one document", 101);
        expected.put("V3_14_1 loadAll: yarn and Berry load one document", 99);
        expected.put("V3_14_1 safeDump: yarn and Berry load one document", 51);
        expected.put("V3_14_1 safeLoadAll: yarn and Berry load one document", 5);
        expected.put("ZKOCHAN_0_0_11 a document that holds itself, which the rows write only so deep", 2);
        expected.put("ZKOCHAN_0_0_11 dump: pnpm loads one document, and dumps with lockfileYamlDump's options", 204);
        expected.put("ZKOCHAN_0_0_11 load with options: pnpm passes none", 49);
        expected.put("ZKOCHAN_0_0_11 loadAll: pnpm loads one document, and dumps with lockfileYamlDump's options", 101);
        assertThat(notPorted).isEqualTo(expected);
    }
}
