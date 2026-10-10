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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * minimatch 3.0.4's and 10.2.5's own tests ({@code upstream/minimatch-*}), the answers of the minimatch npm bundles
 * (the npm-arborist minimatch-cases fixture) and the globs a Yarn classic resolution or workspaces list takes.
 */
class MinimatchTest {

    private static final Minimatch.Release V3 = Minimatch.Release.V3_0_4;
    private static final Minimatch.Release V10 = Minimatch.Release.V10_2_5;

    // the options each release's port reads; any other option minimatch reads leaves a row out
    private static final List<String> READ_3_0_4 = Arrays.asList("dot", "nocomment", "nonegate", "nocase", "nobrace",
            "noglobstar", "noext", "matchBase", "flipNegate");
    private static final List<String> READ_10_2_5 = Arrays.asList("dot", "nocomment", "nonegate", "noext", "partial",
            "windowsPathsNoEscape");
    private static final List<String> MINIMATCH_OPTIONS = Arrays.asList("dot", "nocomment", "nonegate", "nocase",
            "nobrace", "noglobstar", "noext", "matchBase", "flipNegate", "partial", "windowsPathsNoEscape", "nonull",
            "optimizationLevel", "platform", "preserveMultipleSlashes", "magicalBraces", "windowsNoMagicRoot",
            "allowWindowsEscape", "nocaseMagicOnly", "dotRelative", "includeChildMatches", "maxGlobstarRecursion",
            "maxExtglobRecursion");

    static Stream<Arguments> pairs() {
        JsonNode cases;
        try (InputStream is = MinimatchTest.class.getResourceAsStream("/npm-arborist/minimatch-cases")) {
            cases = new ObjectMapper().readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Arguments> out = new ArrayList<>();
        for (JsonNode pair : cases.get("pairs")) {
            out.add(Arguments.of(pair.get("path").asText(), pair.get("pattern").asText(), pair.get("options"),
                                 pair.get("matches").asBoolean()));
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0} ~ {1} {2}")
    @MethodSource("pairs")
    void matchesAsMinimatch10Does(String path, String pattern, JsonNode options, boolean expected) {
        Minimatch.Options o = Minimatch.Options.builder(V10)
          .dot(options.path("dot").asBoolean())
          .partial(options.path("partial").asBoolean())
          .nocomment(options.path("nocomment").asBoolean())
          .nonegate(options.path("nonegate").asBoolean())
          .windowsPathsNoEscape(options.path("windowsPathsNoEscape").asBoolean())
          .build();
        assertThat(Minimatch.minimatch(path, pattern, o)).isEqualTo(expected);
    }

    // rows printed by the yarnclassic test resources' generate/minimatch-rows.js
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = ';', value = {
            "**/ms;ms;true",
            "**/ms;debug/ms;true",
            "**/ms;a/b/ms;true",
            "**/ms;msx;false",
            "**/ms;debug/msx;false",
            "**/ms;.x/ms;false",
            "debug/ms;debug/ms;true",
            "debug/ms;morgan/debug/ms;false",
            "debug/ms;ms;false",
            "**/debug/ms;debug/ms;true",
            "**/debug/ms;morgan/debug/ms;true",
            "**/debug/ms;debug/x/ms;false",
            "a/**/ms;a/ms;true",
            "a/**/ms;a/b/ms;true",
            "a/**/ms;a/b/c/ms;true",
            "a/**/ms;b/ms;false",
            "**/@scope/pkg;@scope/pkg;true",
            "**/@scope/pkg;x/@scope/pkg;true",
            "**/@scope/pkg;x/@scope/other;false",
            "@scope/pkg/**/ms;@scope/pkg/ms;true",
            "@scope/pkg/**/ms;@scope/pkg/x/ms;true",
            "workspace-aggregator-x/a/left-pad;workspace-aggregator-x/a/left-pad;true",
            "workspace-aggregator-x/a/left-pad;a/left-pad;false",
            "**/a/left-pad;workspace-aggregator-x/a/left-pad;true",
            "**/a/left-pad;a/left-pad;true",
            "**/a/left-pad;b/left-pad;false",
            "packages/*/+(package.json);packages/a/package.json;true",
            "packages/*/+(package.json);packages/a/b/package.json;false",
            "packages/*/+(package.json);packages/.a/package.json;false",
            "packages/*/+(package.json);packages/a/package.jsonpackage.json;true",
            "packages/*/+(package.json);other/a/package.json;false",
            "packages/*/node_modules/**/+(package.json);packages/a/node_modules/x/package.json;true",
            "packages/*/node_modules/**/+(package.json);packages/a/package.json;false",
            "packages/**/+(package.json);packages/a/package.json;true",
            "packages/**/+(package.json);packages/a/b/package.json;true",
            "packages/**/+(package.json);packages/a/node_modules/x/package.json;true",
            "packages/**/+(package.json);packages/.a/b/package.json;false",
            "{a,b}/+(package.json);a/package.json;true",
            "{a,b}/+(package.json);b/package.json;true",
            "{a,b}/+(package.json);c/package.json;false",
            "pkg-[ab]/+(package.json);pkg-a/package.json;true",
            "pkg-[ab]/+(package.json);pkg-c/package.json;false",
            "pkg-[!a]/+(package.json);pkg-a/package.json;false",
            "pkg-[!a]/+(package.json);pkg-c/package.json;true",
            "pkg-?/+(package.json);pkg-a/package.json;true",
            "pkg-?/+(package.json);pkg-ab/package.json;false",
            "!(x)/+(package.json);a/package.json;true",
            "!(x)/+(package.json);x/package.json;true",
            "@(a|b)/+(package.json);a/package.json;true",
            "@(a|b)/+(package.json);b/package.json;true",
            "@(a|b)/+(package.json);c/package.json;false",
            "packages/a/+(package.json);packages/a/package.json;true",
            "packages/a/+(package.json);packages/b/package.json;false",
            "*;ms;true",
            "*;.ms;false",
            "*;a/ms;false",
            "a\\*b;a*b;true",
            "a\\*b;axb;false",
            "'#comment';'#comment';false",
            "!ms;ms;false",
            "!ms;debug;true",
            "{1..3}/x;1/x;true",
            "{1..3}/x;2/x;true",
            "{1..3}/x;4/x;false",
            "[z-a]/x;[z-a]/x;true",
            "[z-a]/x;b/x;false",
    })
    void matchesAsMinimatch3Does(String pattern, String path, boolean match) {
        assertThat(Minimatch.minimatch(path, pattern, V3)).isEqualTo(match);
    }

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (Minimatch.Release release : Minimatch.Release.values()) {
            for (Call call : UpstreamCalls.read(file(release))) {
                if (notPorted(release, call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String file(Minimatch.Release release) {
        return "minimatch-" + release.name().substring(1).replace('_', '.') + ".jsonl.gz";
    }

    // the reason the port does not answer a call, or null when it does
    private static @Nullable String notPorted(Minimatch.Release release, Call call) {
        Object options;
        String pattern;
        switch (call.getFn()) {
            case "minimatch":
                options = call.arg(2);
                pattern = call.arg(1) instanceof String && call.arg(0) instanceof String ? (String) call.arg(1) : null;
                break;
            case "match":
                options = call.arg(2);
                pattern = call.arg(1) instanceof String && call.arg(0) instanceof List ? (String) call.arg(1) : null;
                break;
            case "Minimatch#match":
                Map<?, ?> self = (Map<?, ?>) call.getSelf();
                options = self == null ? null : self.get("options");
                pattern = self != null && self.get("pattern") instanceof String && call.arg(0) instanceof String &&
                          !call.has(1) ? (String) self.get("pattern") : null;
                break;
            default:
                return call.getFn() + ": the port has no regular expression source, brace expansion is its own port";
        }
        if (pattern == null) {
            return "an argument that is not a string, or a partial argument";
        }
        if (options instanceof Map) {
            for (Map.Entry<?, ?> option : ((Map<?, ?>) options).entrySet()) {
                Object key = option.getKey();
                Object value = option.getValue();
                // a POSIX platform, the default optimization level and nonull outside match() are what the port does
                boolean asPorted = "platform".equals(key) && !"win32".equals(value) ||
                                   "optimizationLevel".equals(key) && Double.valueOf(1).equals(value) ||
                                   "nonull".equals(key) && !"match".equals(call.getFn());
                if (MINIMATCH_OPTIONS.contains(key) && EcmaScript.truthy(value) && !asPorted &&
                    !(release == V3 ? READ_3_0_4 : READ_10_2_5).contains(key)) {
                    return "the " + option.getKey() + " option, which no package manager passes";
                }
            }
        }
        return null;
    }

    private static Minimatch.Options options(Minimatch.Release release, @Nullable Object options) {
        Map<?, ?> o = options instanceof Map ? (Map<?, ?>) options : new LinkedHashMap<>();
        return Minimatch.Options.builder(release)
          .dot(EcmaScript.truthy(o.get("dot")))
          .nocomment(EcmaScript.truthy(o.get("nocomment")))
          .nonegate(EcmaScript.truthy(o.get("nonegate")))
          .nocase(EcmaScript.truthy(o.get("nocase")))
          .nobrace(EcmaScript.truthy(o.get("nobrace")))
          .noglobstar(EcmaScript.truthy(o.get("noglobstar")))
          .noext(EcmaScript.truthy(o.get("noext")))
          .matchBase(EcmaScript.truthy(o.get("matchBase")))
          .flipNegate(EcmaScript.truthy(o.get("flipNegate")))
          .partial(EcmaScript.truthy(o.get("partial")))
          .windowsPathsNoEscape(EcmaScript.truthy(o.get("windowsPathsNoEscape")))
          .build();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    @SuppressWarnings("unchecked")
    void answersAsTheReleaseDoes(Minimatch.Release release, Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> answer(release, call)).hasMessage((String) threw.get("message"));
        } else {
            assertThat(answer(release, call)).isEqualTo(call.getOut());
        }
    }

    @SuppressWarnings("unchecked")
    private static Object answer(Minimatch.Release release, Call call) {
        Object answer;
        switch (call.getFn()) {
            case "minimatch":
                answer = Minimatch.minimatch((String) call.arg(0), (String) call.arg(1), options(release, call.arg(2)));
                break;
            case "match":
                answer = Minimatch.match((List<String>) call.arg(0), (String) call.arg(1),
                  options(release, call.arg(2)));
                break;
            default:
                Map<?, ?> self = (Map<?, ?>) call.getSelf();
                answer = new Minimatch((String) self.get("pattern"), options(release, self.get("options")))
                  .match((String) call.arg(0));
        }
        return answer;
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (Minimatch.Release release : Minimatch.Release.values()) {
            for (Call call : UpstreamCalls.read(file(release))) {
                String reason = notPorted(release, call);
                if (reason != null) {
                    notPorted.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        String noSource = ": the port has no regular expression source, brace expansion is its own port";
        String option = " option, which no package manager passes";
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V10_2_5 Minimatch#makeRe" + noSource, 395);
        expected.put("V10_2_5 an argument that is not a string, or a partial argument", 27);
        expected.put("V10_2_5 braceExpand" + noSource, 21);
        expected.put("V10_2_5 escape" + noSource, 406);
        expected.put("V10_2_5 filter" + noSource, 1);
        expected.put("V10_2_5 makeRe" + noSource, 209);
        expected.put("V10_2_5 the flipNegate" + option, 6);
        expected.put("V10_2_5 the matchBase" + option, 5);
        expected.put("V10_2_5 the maxGlobstarRecursion" + option, 1);
        expected.put("V10_2_5 the nocase" + option, 32);
        expected.put("V10_2_5 the noglobstar" + option, 2);
        expected.put("V10_2_5 the nonull" + option, 18);
        expected.put("V10_2_5 the optimizationLevel" + option, 3);
        expected.put("V10_2_5 the platform" + option, 73);
        expected.put("V10_2_5 unescape" + noSource, 406);
        expected.put("V3_0_4 Minimatch#makeRe" + noSource, 192);
        expected.put("V3_0_4 braceExpand" + noSource, 9);
        expected.put("V3_0_4 the nonull" + option, 18);
        assertThat(notPorted).isEqualTo(expected);
    }
}
