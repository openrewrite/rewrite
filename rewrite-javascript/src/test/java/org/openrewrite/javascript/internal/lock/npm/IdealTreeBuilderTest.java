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
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
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

/**
 * Replays every recorded edit {@link NpmScenarios} finds through {@link IdealTreeBuilder} against a stub
 * registry served from the scenario's {@code http/}: the built tree must be the one npm's own {@code after}
 * lock loads to (the same locations, and per location the same name, version, resolved, integrity, flags,
 * link target and lock entry), or, where npm failed, the build must fail with the code or kind the
 * scenario's {@code expect} names. When {@code REWRITE_LOCK_REPLAY_RESULTS} names a file, one line per edit
 * is written there: the outcome and the first difference.
 */
class IdealTreeBuilderTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_REPLAY_RESULTS";
    static final String REG = "https://registry.npmjs.org/";

    /**
     * Where the corpus was recorded; {@code recorded-with} does not say.
     */
    static final Host RECORDED_ON = Host.builder().os("darwin").cpu("arm64").build();

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (NpmScenarios.Scenario scenario : NpmScenarios.discover()) {
            for (NpmScenarios.Edit edit : scenario.edits()) {
                out.add(Arguments.of(edit.toString(), edit));
            }
        }
        return out.stream();
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
     * A registry answering from the scenario's recorded responses, reporting every route it lacks.
     */
    static final class ScenarioRegistry implements HttpSender {
        private final NpmScenarios.Scenario scenario;
        final List<String> missing = new ArrayList<>();
        final List<String> requests = new ArrayList<>();

        ScenarioRegistry(NpmScenarios.Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            String file = file(url, "application/json".equals(header(request, "Accept")));
            requests.add(file);
            String body = scenario.http(file);
            if (body == null) {
                missing.add(file);
            }
            return new Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        }

        private static @Nullable String header(Request request, String name) {
            for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        // http/<name> (abbreviated), http/<name>@full, http/<name>-<version>
        static String file(String url, boolean full) {
            String path = url.startsWith(REG) ? url.substring(REG.length()) : url;
            path = path.replace("%2F", "/").replace("%2f", "/").replace("%40", "@");
            String[] parts = path.split("/");
            int nameParts = parts[0].startsWith("@") ? 2 : 1;
            StringBuilder name = new StringBuilder(parts[0]);
            for (int i = 1; i < nameParts && i < parts.length; i++) {
                name.append('/').append(parts[i]);
            }
            if (parts.length > nameParts) {
                return name + "-" + parts[nameParts];
            }
            return full ? name + "@full" : name.toString();
        }
    }

    static PackumentSource registry(ScenarioRegistry http) {
        NodeRegistry registry = new NodeRegistry(null, REG, null, null, null, null, false, null, true, false);
        return new RegistryPackuments(new NodeRegistries(registry, Collections.emptyMap(), null, null, null),
          new NpmRegistryClient(http));
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String npmrc = scenario.npmrc() == null ? "" : scenario.npmrc() + "\n";
        return builder(edit, http, path, NpmOptions.fromNpmrc(npmrc + scenario.argsAfter()));
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path, NpmOptions options) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        Map<String, String> manifests = edit.manifests();
        if (!manifests.containsKey("")) {
            Map<String, String> fromLock = NpmLockRoundTripTest.manifestsFromLock(edit.getStart().getText());
            fromLock.putAll(manifests);
            manifests = fromLock;
        }
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .lock(new LockFile(edit.getStart().getText()))
          .yarnLock(edit.getInputs().getYarnLock())
          .manifests(manifests)
          .build();
        Instant recordedAt = scenario.recordedAt();
        Host host = RECORDED_ON.toBuilder()
          .nodeVersion(scenario.nodeVersion())
          .now(recordedAt != null ? recordedAt : Instant.now())
          .build();
        return new IdealTreeBuilder(path, files, options, registry(http), host);
    }

    /**
     * What the comparison reads per location the lock writer emits: the fields of the task's contract,
     * then the lock entry the node would be written as.
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
            sb.append(" entry=").append(JsonStringifyNice.stringify(Shrinkwrap.metaFromNode(node, path)));
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

    static String kind(@Nullable String expect) {
        if (expect == null || expect.isEmpty()) {
            return "exact";
        }
        return expect.split("\\s+")[0];
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
        NpmScenarios.Edit edit = edit("add-leaf-dev");
        String path = NpmLockRoundTripTest.projectPath(edit.getStart().getText());
        ScenarioRegistry http = new ScenarioRegistry(edit.getScenario());
        assertThatThrownBy(() -> builder(edit, http, path, NpmOptions.fromNpmrc("before=2021/04/23\n")).buildIdealTree())
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("before=2021/04/23");
        assertThatThrownBy(() -> builder(edit, http, path, NpmOptions.fromNpmrc("before=abc\n")).buildIdealTree())
          .isInstanceOf(NpmRejection.class)
          .hasMessage("Invalid time value");
        assertThatCode(() -> builder(edit, http, path,
          NpmOptions.fromNpmrc("before=2021/04/23\nmin-release-age-exclude=left-pad\n")).buildIdealTree())
          .doesNotThrowAnyException();
    }

    private static NpmScenarios.Edit edit(String scenario) {
        for (NpmScenarios.Scenario s : NpmScenarios.discover()) {
            if (s.getName().equals(scenario)) {
                return s.edits().get(0);
            }
        }
        throw new IllegalArgumentException(scenario);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void buildsWhatNpmWrote(String name, NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String expect = scenario.expect();
        String kind = kind(expect);
        NpmScenarios.Lock result = edit.getResult();
        String path = NpmLockRoundTripTest.projectPath(result != null ? result.getText() : edit.getStart().getText());
        NpmOptions options = NpmOptions.fromNpmrc(scenario.npmrc());
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
            built = builder(edit, http, path).buildIdealTree();
        } catch (RuntimeException e) {
            String code = codeOf(e);
            String thrown = e.getClass().getSimpleName() + (code != null ? "[" + code + "]" : "") + ": " + e.getMessage();
            if (expectTree) {
                record(label, "failed", thrown + missing(http));
                throw new AssertionError(label + " expected npm's tree but the build failed: " + thrown + missing(http), e);
            }
            assertExpectedFailure(label, expect, kind, e, code, thrown, http);
            return;
        }
        if (!expectTree && !treeOrCannotKnow) {
            record(label, "unexpected-success", "expected " + expect);
            fail(label + " expected \"" + expect + "\" but the build succeeded" + missing(http));
        }
        if (result.loadError() != null || kind.equals("unstable") ||
            NpmLockRoundTripTest.lockfileVersion(result.getText()) < 2) {
            // npm's own lock is not what its data loads to (its second install fails on it or rewrites it,
            // or it is a lockfileVersion 1 lock, whose entries hold less than the tree): compare what is written
            String written = built.getMeta().toString();
            if (!written.equals(result.getText())) {
                record(label, "mismatch", "written lock differs" + missing(http));
                assertThat(written).as(label + missing(http)).isEqualTo(result.getText());
            }
            record(label, "exact", "bytes");
            return;
        }
        Map<String, String> expected = describe(NpmLockRoundTripTest.load(result, options, path), path);
        Map<String, String> actual = describe(built, path);
        String difference = firstDifference(expected, actual);
        if (difference != null) {
            record(label, "mismatch", difference + missing(http));
            assertThat(actual).as(label + ": " + difference + missing(http)).isEqualTo(expected);
        }
        record(label, "exact", "");
    }

    private void assertExpectedFailure(String label, @Nullable String expect, String kind, RuntimeException e,
                                       @Nullable String code, String thrown, ScenarioRegistry http) {
        String[] words = expect == null ? new String[0] : expect.split("\\s+");
        boolean ok;
        switch (kind) {
            case "npm-rejects":
                ok = "crash".equals(words[1]) || words[1].equals(code);
                break;
            case "cannot-know":
                ok = e instanceof CannotKnow || e instanceof NpmOptions.NeedsEnvironment ||
                     e instanceof PackageJson.NeedsPackageFiles;
                break;
            case "malformed":
                // a lock npm would discard, or an override key or reference the loader refuses by name
                ok = e instanceof MalformedLock ||
                     e.getMessage() != null && e.getMessage().contains(malformedKey(expect));
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            record(label, "wrong-failure", "expected " + expect + " got " + thrown + missing(http));
            throw new AssertionError(label + " expected \"" + expect + "\" but got " + thrown + missing(http), e);
        }
        record(label, "expected-failure", thrown);
    }

    // "malformed override key <key>" or "malformed override reference <$ref> reached from <name>"
    private static String malformedKey(String expect) {
        String[] words = expect.split("\\s+");
        return expect.startsWith("malformed override reference ") ? words[3] : words[words.length - 1];
    }

    private static String missing(ScenarioRegistry http) {
        return http.missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", http.missing) + "]";
    }
}
