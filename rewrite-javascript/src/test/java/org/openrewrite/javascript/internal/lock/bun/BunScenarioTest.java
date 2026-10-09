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
package org.openrewrite.javascript.internal.lock.bun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Replays every recorded edit {@link BunScenarios} finds through {@link BunLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_BUN_SCENARIO_RESULTS} names a file for one line per edit.
 */
class BunScenarioTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_BUN_SCENARIO_RESULTS";

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            if (scenario.isEdit()) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
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
     * The registry as the scenario recorded it: {@code http/<name>}, or {@code http/<name>@full}, whatever its url; a
     * downloaded fixture's empty versions are filled from its {@code http/<name>-<version>} documents.
     */
    static final class ScenarioRegistry implements ManifestSource {
        private static final ObjectMapper JSON = new ObjectMapper();

        private final BunScenarios.Scenario scenario;
        final List<String> missing = new ArrayList<>();

        ScenarioRegistry(BunScenarios.Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean extended) {
            String file = extended ? name + "@full" : name;
            String mirror = mirror(registry);
            String body = mirror == null ? null : scenario.file(mirror + "/" + file);
            if (body == null && mirror != null) {
                body = scenario.file(mirror + "/" + file.replace("/", "%2f"));
            }
            if (body == null) {
                body = scenario.http(file);
            }
            if (body == null) {
                missing.add(file);
                return null;
            }
            return withRecordedVersions(name, body);
        }

        // a registry other than npm's answers from http-<name>/ when the scenario recorded its own documents
        private @Nullable String mirror(String registry) {
            String host = registry.replaceFirst("^[a-z]+://", "").replaceFirst("[/:].*$", "");
            String[] labels = host.split("\\.");
            if (labels.length < 3 || !"registry".equals(labels[0]) || "npmjs".equals(labels[1])) {
                return null;
            }
            String dir = "http-" + labels[1];
            return Files.isDirectory(scenario.getDir().resolve(dir)) ? dir : null;
        }

        private String withRecordedVersions(String name, String body) {
            try {
                JsonNode packument = JSON.readTree(body);
                JsonNode versions = packument.get("versions");
                if (!(versions instanceof ObjectNode)) {
                    return body;
                }
                boolean filled = false;
                Iterator<Map.Entry<String, JsonNode>> entries = versions.fields();
                while (entries.hasNext()) {
                    Map.Entry<String, JsonNode> entry = entries.next();
                    String manifest = entry.getValue().size() == 0 ? scenario.http(name + "-" + entry.getKey()) : null;
                    if (manifest != null) {
                        entry.setValue(JSON.readTree(manifest));
                        filled = true;
                    }
                }
                return filled ? JSON.writeValueAsString(packument) : body;
            } catch (IOException e) {
                return body;
            }
        }

        boolean unservedElsewhereRecording() {
            return !missing.isEmpty() && scenario.recordedWith() == null && scenario.getHttpCache() == null;
        }

        String describe() {
            return missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", missing) + "]";
        }
    }

    static BunLockRegenerator.Inputs inputs(BunScenarios.Scenario scenario) {
        String before = scenario.file("before");
        return BunLockRegenerator.Inputs.builder()
          .lock(before)
          .manifests(scenario.manifests(false, before == null ? "" : before))
          .npmrc(scenario.config(false, "npmrc", ".npmrc"))
          .bunfig(scenario.config(false, "bunfig", "bunfig.toml"))
          .files(scenario.tree(false))
          .paths(scenario.paths(false, before))
          .env(Collections.emptyMap())
          .now(scenario.now())
          .build();
    }

    static String kind(@Nullable String expect) {
        if (expect == null || expect.isEmpty()) {
            return "exact";
        }
        return expect.split("\\s+")[0];
    }

    private static void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatBunWrote(String name, BunScenarios.Scenario scenario) {
        String expect = scenario.expect();
        String kind = kind(expect);
        String after = scenario.file("after");
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        String label = scenario.getLabel();

        Result result = new BunLockRegenerator(inputs(scenario), http).regenerate();
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
          failure.getReason() + ": " + failure.getDetail();

        if (kind.equals("exact") && after != null) {
            if (!result.isSuccess()) {
                failed(label, http, "failed", thrown, " expected bun's lock but regeneration failed: " + thrown);
            }
            if (!after.equals(result.getLockFileContent())) {
                String actual = String.valueOf(result.getLockFileContent());
                failed(label, http, "mismatch", firstDifference(after, actual), "");
                assertThat(result.getLockFileContent()).as(label + http.describe()).isEqualTo(after);
            }
            record(label, "exact", http.describe().trim());
            return;
        }
        if (result.isSuccess()) {
            failed(label, http, "unexpected-success", "expected " + expect,
              " expected \"" + expect + "\" but regeneration succeeded");
        }
        String rest = expect == null ? "" : expect.substring(kind.length()).trim();
        boolean ok;
        switch (kind) {
            case "exact":
                ok = failure.getKind() == Kind.DELETES_LOCK;
                break;
            case "cannot-know":
                ok = failure.getKind() == Kind.CANNOT_KNOW &&
                     failure.getDetail().contains(rest);
                break;
            case "malformed":
                ok = failure.getReason() == Reason.MALFORMED_LOCK;
                break;
            case "pm-rejects":
            case "pm-fails":
                ok = failure.getKind() == (kind.equals("pm-rejects") ? Kind.PM_REJECTS : Kind.PM_FAILS) &&
                     failure.getDetail().contains(rest.replaceFirst("^error: ", "")) && http.missing.isEmpty();
                break;
            case "second-install-rejects":
            case "unstable":
                ok = failure.getKind() == Kind.SECOND_INSTALL;
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            failed(label, http, "wrong-failure", "expected " + expect + " got " + thrown,
              " expected \"" + expect + "\" but got " + thrown);
        }
        record(label, "expected-failure", thrown + http.describe());
    }

    // a replay the stub could not serve is skipped, every other departure from the recording fails
    private static void failed(String label, ScenarioRegistry http, String outcome, String detail, String message) {
        if (http.unservedElsewhereRecording()) {
            record(label, "skipped", "not served offline:" + http.describe());
            assumeTrue(false, label + " cannot be replayed offline" + http.describe());
        }
        record(label, outcome, detail + http.describe());
        if (!message.isEmpty()) {
            fail(label + message + http.describe());
        }
    }

    private static String firstDifference(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);
        for (int i = 0; i < Math.max(e.length, a.length); i++) {
            String x = i < e.length ? e[i] : "";
            String y = i < a.length ? a[i] : "";
            if (!x.equals(y)) {
                return "line " + (i + 1) + " expected \"" + x.trim() + "\" actual \"" + y.trim() + "\"";
            }
        }
        return "differs";
    }
}
