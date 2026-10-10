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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.npm.ScenarioReplay.ScenarioRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.javascript.internal.lock.npm.ScenarioReplay.missing;

/**
 * Replays every recorded edit {@link NpmScenarios} finds through {@link IdealTreeBuilder}: the tree npm's {@code after}
 * loads to, or npm's failure; {@code REWRITE_LOCK_REPLAY_RESULTS} names a file for one line per edit.
 */
class IdealTreeBuilderTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_REPLAY_RESULTS";

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        return ScenarioReplay.edits();
    }

    @AfterAll
    static void writeResults() {
        String file = System.getenv(RESULTS_ENV);
        if (file == null || file.isEmpty()) {
            return;
        }
        try {
            List<String> sorted = new ArrayList<>(results);
            Collections.sort(sorted);
            Files.write(Paths.get(file), (String.join("\n", sorted) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * What the comparison reads per location the lock writer emits: name, version, resolved, integrity, flags and
     * link target, then the lock entry the node would be written as.
     */
    static Map<String, String> describe(Node tree, String path) {
        Map<String, String> out = new TreeMap<>();
        for (Node node : tree.getInventory().values()) {
            if (!node.isRoot() && Shrinkwrap.isStaleTopLevelEntry(node, Relpath.relpath(path, node.getPath()))) {
                continue;
            }
            if (node.isInert() && !Js.truthy(node.getPackage().get("version"))) {
                // a failed optional dependency: the lock writer leaves it out
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("name=").append(node.getName());
            sb.append(" version=").append(Js.string(node.getVersion()));
            sb.append(" resolved=").append(Js.string(node.getResolved()));
            sb.append(" integrity=").append(Js.string(node.getIntegrity()));
            sb.append(" flags=").append(flags(node));
            sb.append(" target=").append(node.isLink() ? node.getTarget().getLocation() : "");
            sb.append(" entry=")
              .append(JsonStringifyNice.stringify(Shrinkwrap.metaFromNode(node, path, NpmOptions.DEFAULT)));
            out.put(node.getLocation(), sb.toString());
        }
        return out;
    }

    private static String flags(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node.isExtraneous()) {
            sb.append("extraneous,");
        }
        if (node.isDev()) {
            sb.append("dev,");
        }
        if (node.isOptional()) {
            sb.append("optional,");
        }
        if (node.isDevOptional()) {
            sb.append("devOptional,");
        }
        if (node.isPeer()) {
            sb.append("peer,");
        }
        return sb.toString();
    }

    /**
     * The first location where the two descriptions differ, with the field, or null when equal.
     */
    static @Nullable String firstDifference(Map<String, String> expected, Map<String, String> actual) {
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String a = actual.get(e.getKey());
            if (a == null) {
                return "missing " + e.getKey() + " (" + e.getValue() + ")";
            }
            if (!a.equals(e.getValue())) {
                return "differs " + e.getKey() + " " + firstField(e.getValue(), a);
            }
        }
        for (String location : actual.keySet()) {
            if (!expected.containsKey(location)) {
                return "extra " + location + " (" + actual.get(location) + ")";
            }
        }
        return null;
    }

    private static String firstField(String expected, String actual) {
        String[] e = expected.split(" (?=[a-z]+=)");
        String[] a = actual.split(" (?=[a-z]+=)");
        for (int i = 0; i < Math.min(e.length, a.length); i++) {
            if (!e[i].equals(a[i])) {
                return "expected " + e[i] + " actual " + a[i];
            }
        }
        return "expected " + expected + " actual " + actual;
    }

    private static @Nullable String codeOf(Throwable t) {
        return t instanceof NpmError ? ((NpmError) t).getCode() : null;
    }

    private static void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    // pacote renders --before with toISOString() in every fetcher's constructor, so the date is read at the
    // first fetch it filters: an exempted package never reads it
    @Test
    void beforeIsReadAtTheFirstFetchItFilters() {
        NpmScenarios.Edit edit = ScenarioReplay.edit("add-leaf-dev");
        String path = ScenarioReplay.projectPath(edit.getStart().getText());
        ScenarioRegistry http = new ScenarioRegistry(edit.getScenario());
        assertThatThrownBy(() -> ScenarioReplay.builder(edit, http, path,
          NpmOptions.fromNpmrc("before=2021/04/23\n", HostEnvironment.UNKNOWN)).buildIdealTree())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("before=2021/04/23");
        assertThatThrownBy(() -> ScenarioReplay.builder(edit, http, path,
          NpmOptions.fromNpmrc("before=abc\n", HostEnvironment.UNKNOWN)).buildIdealTree())
          .isInstanceOf(NpmRejection.class)
          .hasMessage("Invalid time value");
        assertThatCode(() -> ScenarioReplay.builder(edit, http, path, NpmOptions.fromNpmrc(
          "before=2021/04/23\nmin-release-age-exclude=left-pad\n", HostEnvironment.UNKNOWN)).buildIdealTree())
          .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void buildsWhatNpmWrote(String name, NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String expect = scenario.expect();
        String kind = ScenarioReplay.kind(expect);
        NpmScenarios.Lock result = edit.getResult();
        String path = ScenarioReplay.projectPath(result != null ? result.getText() : edit.getStart().getText());
        NpmOptions options = NpmOptions.fromNpmrc(scenario.npmrc(), HostEnvironment.UNKNOWN);
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        String label = scenario.getLabel() + "/" + edit.getStart().getFileName();

        // the directory's name is the regenerator's input, not the builder's: here it is given
        boolean expectTree = result != null && (kind.equals("exact") || kind.equals("peer-conflict") ||
                                                kind.equals("unstable") ||
                                                expect.startsWith("cannot-know the project directory name"));
        // what npm read from the disk may be among the manifests the project supplies (a workspace tree):
        // then the build is expected to succeed with npm's tree instead
        boolean treeOrCannotKnow = result != null && kind.equals("cannot-know");
        Node built;
        try {
            built = ScenarioReplay.builder(edit, http, path).buildIdealTree();
        } catch (RuntimeException e) {
            String code = codeOf(e);
            String thrown = e.getClass().getSimpleName() + (code != null ? "[" + code + "]" : "") + ": " +
              e.getMessage();
            if (expectTree) {
                failed(label, http, "failed", thrown);
                throw new AssertionError(label + " expected npm's tree but the build failed: " + thrown + missing(http),
                  e);
            }
            assertExpectedFailure(label, expect, kind, e, code, thrown, http);
            return;
        }
        if (!expectTree && !treeOrCannotKnow) {
            failed(label, http, "unexpected-success", "expected " + expect);
            fail(label + " expected \"" + expect + "\" but the build succeeded" + missing(http));
        }
        if (result.loadError() != null || kind.equals("unstable") ||
            ScenarioReplay.lockfileVersion(result.getText()) < 2) {
            // npm's own lock is not what its data loads to (its second install fails on it or rewrites it,
            // or it is a lockfileVersion 1 lock, whose entries hold less than the tree): compare what is written
            String written = built.getMeta().toString();
            if (!written.equals(result.getText())) {
                failed(label, http, "mismatch", "written lock differs");
                assertThat(written).as(label + missing(http)).isEqualTo(result.getText());
            }
            record(label, "exact", "bytes" + missing(http));
            return;
        }
        Map<String, String> expected = describe(ScenarioReplay.load(result, options, path), path);
        Map<String, String> actual = describe(built, path);
        String difference = firstDifference(expected, actual);
        if (difference != null) {
            failed(label, http, "mismatch", difference);
            assertThat(actual).as(label + ": " + difference + missing(http)).isEqualTo(expected);
        }
        record(label, "exact", missing(http).trim());
    }

    private void assertExpectedFailure(String label, @Nullable String expect, String kind, RuntimeException e,
                                       @Nullable String code, String thrown, ScenarioRegistry http) {
        String[] words = expect == null ? new String[0] : expect.split("\\s+");
        boolean ok;
        switch (kind) {
            case "npm-rejects":
                // a route the stub lacked may be the failure's cause instead, unless npm's own answer was the 404
                ok = words[1].equals(code) && (http.missing.isEmpty() || "E404".equals(words[1]));
                break;
            case "pm-fails":
                // an error without a code of npm's own
                ok = code == null && !(e instanceof CannotKnow) && http.missing.isEmpty();
                break;
            case "cannot-know":
                ok = e instanceof CannotKnow ||
                     e instanceof PackageJson.NeedsPackageFiles;
                break;
            case "malformed":
                // a lock npm would discard, or an override key or reference the loader refuses by name
                ok = e instanceof MalformedLock ||
                     e.getMessage() != null && e.getMessage().contains(ScenarioReplay.malformedKey(expect));
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            failed(label, http, "wrong-failure", "expected " + expect + " got " + thrown);
            throw new AssertionError(label + " expected \"" + expect + "\" but got " + thrown + missing(http), e);
        }
        record(label, "expected-failure", thrown + missing(http));
    }

    // a replay the stub could not serve is skipped, every other departure from the recording fails
    private static void failed(String label, ScenarioRegistry http, String outcome, String detail) {
        if (http.unservedElsewhereRecording()) {
            record(label, "skipped", "not served offline:" + missing(http));
            assumeTrue(false, label + " cannot be replayed offline" + missing(http));
        }
        record(label, outcome, detail + missing(http));
    }
}
