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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class NativeLockEngineTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        // Pin the registry so discovery is deterministic (never reads a real ~/.npmrc or env).
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    private Result regen(PackageManager pm, String original, String edited, String lock) {
        return NativeLockEngine.regenerate(pm, edited, original, lock, null, Paths.get("package.json"), ctx);
    }

    private static String npmLock(String lockedVersion) {
        return "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"name\": \"x\", \"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\"version\": \"" + lockedVersion + "\"}\n" +
                "  }\n" +
                "}\n";
    }

    @Test
    void closureChangingUpgradeFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.18.0\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.18.0",
                "{\"name\":\"lodash\",\"version\":\"4.18.0\",\"dependencies\":{\"tslib\":\"^2.0.0\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.18.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("lodash");
        assertThat(result.getFailure().getDetail()).contains("dependencies changed");
    }

    @Test
    void addedPeerDependencyFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependencies changed");
    }

    @Test
    void npmEnginesWriteThroughNotYetSupported() {
        // The engine's proof PASSES an engines-only delta (write-through tier), then dispatches to the npm
        // patcher — which applies license/deprecated write-through but defers engines/bin (nested objects
        // need a real golden to pin byte-exact npm output), failing loud SAFELY. The engine and the pnpm
        // patcher fully support write-through; npm engines/bin is a tracked follow-up.
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{},\"engines\":{\"node\":\">=12\"}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{},\"engines\":{\"node\":\">=14\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\"," +
                        "\"integrity\":\"sha512-NEW\",\"shasum\":\"abc\"}}");

        String lock = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"name\": \"x\", \"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.20\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.20.tgz\",\n" +
                "      \"integrity\": \"sha512-OLD\",\n" +
                "      \"engines\": {\"node\": \">=12\"}\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("write-through is not supported");
    }

    @Test
    void pnpmLegacyLockfileVersionUnsupported() {
        Result result = regen(PackageManager.Pnpm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "lockfileVersion: 5.4\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_LOCKFILE_VERSION);
    }

    @Test
    void closureAddTransitiveConflictFailsLoud() {
        // is-odd pulls is-number ^7.0.0, but is-number is already pinned at 6.0.0 top-level — npm would
        // nest a second copy (fork). The greedy-forward resolver refuses rather than reshape (I3/I5).
        routes.put("https://registry.npmjs.org/is-odd", "{\"versions\":{\"3.0.1\":{}}}");
        routes.put("https://registry.npmjs.org/is-odd/3.0.1",
                "{\"name\":\"is-odd\",\"version\":\"3.0.1\",\"dependencies\":{\"is-number\":\"^7.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz\"," +
                        "\"integrity\":\"sha512-ODD\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"is-number\": \"6.0.0\"}},\n" +
                "    \"node_modules/is-number\": {\"version\": \"6.0.0\"}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"is-number\":\"6.0.0\"}}",
                "{\"dependencies\":{\"is-number\":\"6.0.0\",\"is-odd\":\"^3.0.1\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("is-number");
        assertThat(result.getFailure().getDetail()).contains("does not satisfy");
    }

    @Test
    void closureAddCascadeUpgradeFailsLoud() {
        // Adding needy pulls color-name ~1.1.4, but color-name is locked at 1.1.3 top-level. Even though a
        // ^-ranged reverse-dependent would accept 1.1.4, moving an already-placed package is a cascade —
        // deferred to I3 rather than resolved greedily.
        routes.put("https://registry.npmjs.org/needy", "{\"versions\":{\"1.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/needy/1.0.0",
                "{\"name\":\"needy\",\"version\":\"1.0.0\",\"dependencies\":{\"color-name\":\"~1.1.4\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/needy/-/needy-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-NEEDY\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"has-color\": \"1.0.0\"}},\n" +
                "    \"node_modules/color-name\": {\"version\": \"1.1.3\"},\n" +
                "    \"node_modules/has-color\": {\"version\": \"1.0.0\", \"dependencies\": {\"color-name\": \"^1.1.0\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"has-color\":\"1.0.0\"}}",
                "{\"dependencies\":{\"has-color\":\"1.0.0\",\"needy\":\"^1.0.0\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("color-name");
        assertThat(result.getFailure().getDetail()).contains("does not satisfy");
    }

    @Test
    void enginesLeafAddByteExact() {
        // A dependency-free package carrying object metadata (engines) now inserts byte-exact (I1-follow).
        routes.put("https://registry.npmjs.org/is-number", resource("lock/npm/add-meta-engines/http/is-number"));
        routes.put("https://registry.npmjs.org/is-number/7.0.0",
                resource("lock/npm/add-meta-engines/http/is-number-7.0.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/add-meta-engines/pkg-after"),
                resource("lock/npm/add-meta-engines/pkg-before"),
                resource("lock/npm/add-meta-engines/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/add-meta-engines/after"));
    }

    @Test
    void richMetadataLeafAddByteExact() {
        // os (array, groups with scalars) + a derived hasInstallScript + license, then engines (object,
        // groups last) — the full value-kind partition, end-to-end through the engine.
        routes.put("https://registry.npmjs.org/fsevents", resource("lock/npm/add-meta-rich/http/fsevents"));
        routes.put("https://registry.npmjs.org/fsevents/2.3.3",
                resource("lock/npm/add-meta-rich/http/fsevents-2.3.3"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/add-meta-rich/pkg-after"),
                resource("lock/npm/add-meta-rich/pkg-before"),
                resource("lock/npm/add-meta-rich/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/add-meta-rich/after"));
    }

    @Test
    void unserializableMetadataLeafAddFailsLoud() {
        // A dependency-free leaf whose only extra surface is one npm reshapes without a verified golden
        // (bundleDependencies) defers rather than emit a maybe-wrong entry (exhaustive-or-fail).
        routes.put("https://registry.npmjs.org/bundler", "{\"versions\":{\"1.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/bundler/1.0.0",
                "{\"name\":\"bundler\",\"version\":\"1.0.0\",\"dependencies\":{},\"bundleDependencies\":[\"x\"]}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{}}",
                "{\"dependencies\":{\"bundler\":\"^1.0.0\"}}",
                "{\"lockfileVersion\":3,\"packages\":{\"\":{}}}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("bundleDependencies").contains("not yet supported");
    }

    @Test
    void leafAddByteExact() {
        routes.put("https://registry.npmjs.org/left-pad", resource("lock/npm/add-leaf/http/left-pad"));
        routes.put("https://registry.npmjs.org/left-pad/1.3.0", resource("lock/npm/add-leaf/http/left-pad-1.3.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/add-leaf/pkg-after"),
                resource("lock/npm/add-leaf/pkg-before"),
                resource("lock/npm/add-leaf/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/add-leaf/after"));
    }

    @Test
    void unsupportedEntryTypeFailsLoud() {
        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"git+https://github.com/lodash/lodash.git\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
    }

    @Test
    void versionNotFoundFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^5.0.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.VERSION_NOT_FOUND);
    }

    // --- C1: reverse-dependent guard -------------------------------------

    @Test
    void reverseDependentConstraintExcludingTargetFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\"version\": \"4.17.20\"},\n" +
                "    \"node_modules/needs-exact\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"4.17.20\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("lodash");
        assertThat(result.getFailure().getDetail())
                .contains("node_modules/needs-exact").contains("excludes 4.17.21");
    }

    @Test
    void reverseDependentConstraintAcceptingTargetSucceeds() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\"," +
                        "\"integrity\":\"sha512-NEW\",\"shasum\":\"abc\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.20\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.20.tgz\",\n" +
                "      \"integrity\": \"sha512-OLD\"\n" +
                "    },\n" +
                "    \"node_modules/tolerant\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"^4.0.0\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).contains("4.17.21");
    }

    // --- closure surfaces (each fails loud individually) -----------------

    @Test
    void osChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"os\":[\"linux\"]", ",\"os\":[\"darwin\"]", "os changed");
    }

    @Test
    void cpuChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"cpu\":[\"x64\"]", ",\"cpu\":[\"arm64\"]", "cpu changed");
    }

    @Test
    void libcChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"libc\":[\"glibc\"]", ",\"libc\":[\"musl\"]", "libc changed");
    }

    @Test
    void bundleDependenciesChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"bundleDependencies\":[\"x\"]", "bundleDependencies changed");
    }

    @Test
    void optionalDependenciesChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"optionalDependencies\":{\"x\":\"^1.0.0\"}", "optionalDependencies changed");
    }

    @Test
    void peerDependenciesMetaChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"peerDependenciesMeta\":{\"x\":{\"optional\":true}}",
                "peerDependenciesMeta changed");
    }

    @Test
    void hasInstallScriptChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"hasInstallScript\":true", "hasInstallScript changed");
    }

    private void assertClosureSurfaceFailsLoud(String oldExtra, String newExtra, String expectedDetail) {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}" + oldExtra + "}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}" + newExtra + "}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains(expectedDetail);
    }

    // --- other fail-loud guards ------------------------------------------

    @Test
    void forkedMultipleLockedVersionsFailsLoud() {
        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\"version\": \"4.17.20\"},\n" +
                "    \"node_modules/x/node_modules/lodash\": {\"version\": \"3.10.1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("multiple versions");
    }

    @Test
    void overridesOutsideDeclaredDependenciesFailsLoud() {
        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"},\"overrides\":{\"a\":\"1.0.0\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"},\"overrides\":{\"a\":\"2.0.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("outside declared dependencies");
    }

    @Test
    void nullLockFailsLoud() {
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                null, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void nullOriginalManifestFailsLoud() {
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                null, npmLock("4.17.20"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("pre-edit package.json");
    }

    // --- C7: workspace importer targeting --------------------------------

    @Test
    void workspaceMemberBumpTargetsMemberImporterByteExact() {
        routes.put("https://registry.npmjs.org/is-odd", resource("lock/npm/v3/http/is-odd"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.0", resource("lock/npm/v3/http/is-odd-3.0.0"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/v3/http/is-odd-3.0.1"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/workspace/pkg-after"),
                resource("lock/npm/workspace/pkg-before"),
                resource("lock/npm/workspace/before"),
                null, Paths.get("packages/foo/package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/workspace/after"));
    }

    // --- C4: credentials never leak into a failure detail ----------------

    @Test
    void registryTokenInUrlNeverAppearsInFailureDetail() {
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(new NodeRegistry(
                null, "https://user:s3cr3ttoken@registry.example.com/", null, null, null, null, false, null, true, false)));

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).doesNotContain("s3cr3ttoken");
        assertThat(result.getErrorMessage()).doesNotContain("s3cr3ttoken");
    }

    private static String resource(String path) {
        try (InputStream in = NativeLockEngineTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
