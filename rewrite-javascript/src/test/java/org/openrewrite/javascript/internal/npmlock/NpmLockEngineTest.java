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
package org.openrewrite.javascript.internal.npmlock;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.npmlock.NpmLockWriterTest.resource;

/**
 * Offline replays of scenarios recorded from real npm: the before-lock, the golden
 * after-lock, and the packuments were captured together (see
 * {@code src/test/resources/npmlock/record.sh}), so the engine's output is asserted
 * byte-identical to what npm itself produced for the same edit. The routed sender
 * both serves the recordings and proves nothing escaped to the network.
 */
public class NpmLockEngineTest {

    private static final String REGISTRY = "https://registry.npmjs.org/";

    public static final class RoutedHttp implements HttpSender {
        final Map<String, byte[]> routes = new LinkedHashMap<>();
        final List<String> requests = new ArrayList<>();

        public void route(String packageName, String scenario) {
            String encoded = packageName.replace("/", "%2f");
            routes.put(REGISTRY + encoded,
              resource("/npmlock/" + scenario + "/http/" + encoded + ".json")
                .getBytes(StandardCharsets.UTF_8));
        }

        void routeBody(String packageName, String body) {
            routes.put(REGISTRY + packageName.replace("/", "%2f"), body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            requests.add(url);
            byte[] body = routes.get(url);
            if (body == null) {
                return new Response(404, new ByteArrayInputStream(new byte[0]), () -> {
                });
            }
            return new Response(200, new ByteArrayInputStream(body), () -> {
            });
        }
    }

    private RoutedHttp http;

    private ExecutionContext ctx() {
        http = new RoutedHttp();
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        return ctx;
    }

    private Result replay(String scenario, String... recordedPackuments) {
        ExecutionContext ctx = ctx();
        for (String packageName : recordedPackuments) {
            http.route(packageName, scenario);
        }
        return NpmLockEngine.regenerate(
          resource("/npmlock/" + scenario + "/package.json"),
          resource("/npmlock/" + scenario + "/package.json.before"),
          resource("/npmlock/" + scenario + "/package-lock.before.json"),
          null,
          ctx);
    }

    private void assertGolden(String scenario, Result result) {
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent())
          .isEqualTo(resource("/npmlock/" + scenario + "/package-lock.after.json"));
    }

    @Test
    void upgradeMovesLeafPin() {
        Result result = replay("upgrade-leaf", "is-number");
        assertGolden("upgrade-leaf", result);
        assertThat(http.requests).containsExactly(REGISTRY + "is-number");
    }

    /**
     * The edit set is diffed against the lock's recorded root entry, so an edit made
     * by an earlier recipe in the same run (already in the manifest, not yet in the
     * captured lock) is reconciled even though this recipe's own before-state
     * already contained it.
     */
    @Test
    void reconcilesEditsFromEarlierRecipesInTheSameRun() {
        ExecutionContext ctx = ctx();
        http.route("is-number", "upgrade-leaf");
        String manifest = resource("/npmlock/upgrade-leaf/package.json");
        Result result = NpmLockEngine.regenerate(manifest, manifest,
          resource("/npmlock/upgrade-leaf/package-lock.before.json"), null, ctx);
        assertGolden("upgrade-leaf", result);
    }

    @Test
    void rangeEditSatisfiedByPinTouchesOnlyRecordedRanges() {
        Result result = replay("range-satisfied");
        assertGolden("range-satisfied", result);
        assertThat(http.requests).as("a still-valid pin needs no network").isEmpty();
    }

    @Test
    void cascadingUpgradeFailsLoud() {
        Result result = replay("cascade-fails", "is-odd");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-number");
        assertThat(result.getLockFileContent()).isNull();
    }

    @Test
    void removalSweepsOrphanedSubtree() {
        Result result = replay("remove-orphans");
        assertGolden("remove-orphans", result);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void upgradeSweepsDependenciesTheNewVersionDropped() {
        Result result = replay("upgrade-orphans", "chalk");
        assertGolden("upgrade-orphans", result);
        assertThat(http.requests).containsExactly(REGISTRY + "chalk");
    }

    @Test
    void addsLeafDependencyAtTopLevel() {
        Result result = replay("add-leaf", "is-buffer");
        assertGolden("add-leaf", result);
        assertThat(http.requests).containsExactly(REGISTRY + "is-buffer");
    }

    /**
     * The recorded golden shows npm handles an override that moves a pin by
     * re-placing the package (dropping the hoisted copy, nesting a fresh one under
     * the dependent) — placement the engine must not guess at. It fails loud, and
     * the fixture documents the behavior a future placement-aware phase must match.
     */
    @Test
    void overrideMovingAPinFailsLoud() {
        Result result = replay("override");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("is-buffer");
        assertThat(http.requests).as("fails before any registry traffic").isEmpty();
    }

    @Test
    void removalRecolorsSurvivorsReachableOnlyThroughDev() {
        Result result = replay("dev-recolor");
        assertGolden("dev-recolor", result);
        assertThat(http.requests).isEmpty();
    }

    /**
     * Recorded from real npm: a root declaring the same package in both
     * devDependencies and peerDependencies gets a dev edge (arborist loads peer
     * first and lets dev replace it), so npm writes the entry with "dev": true.
     * The replay would fail with a flag-drift MALFORMED_LOCK if the engine's edge
     * precedence disagreed with npm's.
     */
    @Test
    void devEdgeWinsOverPeerEdgeAtTheRoot() {
        Result result = replay("dev-peer-overlap", "is-number");
        assertGolden("dev-peer-overlap", result);
    }

    @Test
    void scopedPackagesEncodeTheRegistryPath() {
        Result result = replay("scoped", "@isaacs/string-locale-compare");
        assertGolden("scoped", result);
        assertThat(http.requests).containsExactly(REGISTRY + "@isaacs%2fstring-locale-compare");
    }

    // --- Fail-loud guards over synthetic inputs ----------------------------

    private static final String MANIFEST_V1 = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
      "  \"dependencies\": {\n    \"is-number\": \"^6.0.0\"\n  }\n}\n";

    @Test
    void lockfileVersion2FailsLoud() {
        String lock = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 2,\n" +
          "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"fixture\",\n" +
          "      \"version\": \"1.0.0\"\n    }\n  }\n}\n";
        Result result = NpmLockEngine.regenerate(MANIFEST_V1, null, lock, null, ctx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_LOCK_VERSION);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void workspacesFailLoud() {
        String manifest = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
          "  \"workspaces\": [\n    \"packages/*\"\n  ]\n}\n";
        String lock = resource("/npmlock/range-satisfied/package-lock.before.json");
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void gitDependencyEditFailsLoud() {
        String manifest = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
          "  \"dependencies\": {\n    \"is-number\": \"github:jonschlinkert/is-number\"\n  }\n}\n";
        String lock = resource("/npmlock/range-satisfied/package-lock.before.json");
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void handEditedLockFailsLoud() {
        String canonical = resource("/npmlock/range-satisfied/package-lock.before.json");
        String reordered = canonical.replace(
          "  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",",
          "  \"version\": \"1.0.0\",\n  \"name\": \"fixture\",");
        assertThat(reordered).isNotEqualTo(canonical);
        Result result = NpmLockEngine.regenerate(MANIFEST_V1, null, reordered, null, ctx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void unreachableRegistryFailsLoud() {
        String manifest = resource("/npmlock/upgrade-leaf/package.json");
        String lock = resource("/npmlock/upgrade-leaf/package-lock.before.json");
        // No routes: the packument request 404s.
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND);
        assertThat(result.getFailure().getRegistryUrl()).isEqualTo(REGISTRY);
    }

    @Test
    void versionOutsidePublishedRangeFailsLoud() {
        ExecutionContext ctx = ctx();
        http.route("is-number", "upgrade-leaf");
        String manifest = resource("/npmlock/upgrade-leaf/package.json")
          .replace("^6.0.0", "^99.0.0");
        String lock = resource("/npmlock/upgrade-leaf/package-lock.before.json");
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.VERSION_NOT_FOUND);
    }

    @Test
    void malformedShasumFailsLoud() {
        ExecutionContext ctx = ctx();
        http.routeBody("tiny-sha1", "{\n" +
          "  \"name\": \"tiny-sha1\",\n" +
          "  \"dist-tags\": { \"latest\": \"1.0.1\" },\n" +
          "  \"versions\": {\n" +
          "    \"1.0.1\": {\n" +
          "      \"name\": \"tiny-sha1\",\n" +
          "      \"version\": \"1.0.1\",\n" +
          "      \"dist\": {\n" +
          "        \"tarball\": \"https://registry.npmjs.org/tiny-sha1/-/tiny-sha1-1.0.1.tgz\",\n" +
          "        \"shasum\": \"xyz\"\n" +
          "      }\n" +
          "    }\n" +
          "  }\n" +
          "}");
        String manifest = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
          "  \"dependencies\": {\n    \"tiny-sha1\": \"^1.0.0\"\n  }\n}\n";
        String lock = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n" +
          "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"fixture\",\n" +
          "      \"version\": \"1.0.0\"\n    }\n  }\n}\n";
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.INTEGRITY_UNAVAILABLE);
    }

    @Test
    void stringBinNormalizesToBasenamedObject() {
        ExecutionContext ctx = ctx();
        http.routeBody("@scope/tiny-bin", "{\n" +
          "  \"name\": \"@scope/tiny-bin\",\n" +
          "  \"dist-tags\": { \"latest\": \"1.0.0\" },\n" +
          "  \"versions\": {\n" +
          "    \"1.0.0\": {\n" +
          "      \"name\": \"@scope/tiny-bin\",\n" +
          "      \"version\": \"1.0.0\",\n" +
          "      \"bin\": \"./cli.js\",\n" +
          "      \"dist\": {\n" +
          "        \"tarball\": \"https://registry.npmjs.org/@scope/tiny-bin/-/tiny-bin-1.0.0.tgz\",\n" +
          "        \"integrity\": \"sha512-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\"\n" +
          "      }\n" +
          "    }\n" +
          "  }\n" +
          "}");
        String manifest = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
          "  \"dependencies\": {\n    \"@scope/tiny-bin\": \"^1.0.0\"\n  }\n}\n";
        String lock = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n" +
          "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"fixture\",\n" +
          "      \"version\": \"1.0.0\"\n    }\n  }\n}\n";
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("\"bin\": {\n        \"tiny-bin\": \"cli.js\"\n      }");
    }

    @Test
    void sha1OnlyDistFallsBackToShasumConversion() {
        ExecutionContext ctx = ctx();
        // npm converts a hex shasum to "sha1-<base64>" when the registry has no integrity.
        http.routeBody("tiny-sha1", "{\n" +
          "  \"name\": \"tiny-sha1\",\n" +
          "  \"dist-tags\": { \"latest\": \"1.0.1\" },\n" +
          "  \"versions\": {\n" +
          "    \"1.0.1\": {\n" +
          "      \"name\": \"tiny-sha1\",\n" +
          "      \"version\": \"1.0.1\",\n" +
          "      \"dist\": {\n" +
          "        \"tarball\": \"https://registry.npmjs.org/tiny-sha1/-/tiny-sha1-1.0.1.tgz\",\n" +
          "        \"shasum\": \"5f2eaa1bc1e34d0f64101bc7e64e8fdd9e50634a\"\n" +
          "      }\n" +
          "    }\n" +
          "  }\n" +
          "}");
        String manifest = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n" +
          "  \"dependencies\": {\n    \"tiny-sha1\": \"^1.0.0\"\n  }\n}\n";
        String lock = "{\n  \"name\": \"fixture\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n" +
          "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"fixture\",\n" +
          "      \"version\": \"1.0.0\"\n    }\n  }\n}\n";
        Result result = NpmLockEngine.regenerate(manifest, null, lock, null, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("\"integrity\": \"sha1-Xy6qG8HjTQ9kEBvH5k6P3Z5QY0o=\"");
    }
}
