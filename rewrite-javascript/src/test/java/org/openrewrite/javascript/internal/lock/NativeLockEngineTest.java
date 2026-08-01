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
    void bumpIntroducingNewTransitiveWithPeerFailsLoud() {
        // A clean single-leaf add-during-bump is now placed byte-exact (see NpmAddDuringBumpLockRegenTest); a NEW
        // transitive that declares a non-optional peer (which npm auto-installs) is beyond the conservative slice,
        // so it still defers rather than guess the peer placement.
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.18.0\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.18.0",
                "{\"name\":\"lodash\",\"version\":\"4.18.0\",\"dependencies\":{\"tslib\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/lodash/-/lodash-4.18.0.tgz\"," +
                        "\"integrity\":\"sha512-LODASH\"}}");
        routes.put("https://registry.npmjs.org/tslib",
                "{\"name\":\"tslib\",\"dist-tags\":{},\"versions\":{\"2.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/tslib/2.0.0",
                "{\"name\":\"tslib\",\"version\":\"2.0.0\",\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/tslib/-/tslib-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-TSLIB\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.18.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("tslib");
        // The resolver made the deeper attempt and also defers (its peer/optional gate); its detail is preferred.
        assertThat(result.getFailure().getDetail()).contains("peer/optional dependencies");
    }

    @Test
    void bunVisitorThrownFailureDefersGracefully() {
        // A bun bump whose new version gains a dependencies map throws EngineFailure from INSIDE the rewrite-json
        // visitor, so it arrives wrapped in a RecipeRunException; regenerate must unwrap it into a graceful Failure
        // rather than let the wrapper escape and crash the recipe run.
        routes.put("https://registry.npmjs.org/foo",
                "{\"name\":\"foo\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/foo/1.0.0",
                "{\"name\":\"foo\",\"version\":\"1.0.0\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/foo/2.0.0",
                "{\"name\":\"foo\",\"version\":\"2.0.0\",\"dependencies\":{\"tslib\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/foo/-/foo-2.0.0.tgz\",\"integrity\":\"sha512-FOO2\"}}");

        String lock = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n  \"workspaces\": {\n" +
                "    \"\": {\n      \"name\": \"crash-test\",\n      \"dependencies\": {\n" +
                "        \"foo\": \"^1.0.0\",\n      },\n    },\n  },\n  \"packages\": {\n" +
                "    \"foo\": [\"foo@1.0.0\", \"\", {}, \"sha512-FOO\"],\n\n" +
                "    \"tslib\": [\"tslib@2.0.0\", \"\", {}, \"sha512-TSLIB\"],\n  }\n}\n";

        Result result = regen(PackageManager.Bun,
                "{\"dependencies\":{\"foo\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"foo\":\"^2.0.0\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("gains a dependencies map");
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
        // The resolver's from-scratch attempt also defers on the peer surface (its detail is preferred).
        assertThat(result.getFailure().getDetail()).contains("peer/optional dependencies");
    }

    @Test
    void npmEnginesWriteThrough() {
        // An engines-only delta (write-through tier) is patched, not failed loud: the npm patcher rewrites the
        // entry's `engines` object to the new value at npm's field position. (Byte-exact goldens for engines
        // live in NpmOrphanPruneLockRegenTest#enginesChangeV3.)
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

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).contains("\"version\": \"4.17.21\"");
        assertThat(result.getLockFileContent()).contains("\">=14\"");
        assertThat(result.getLockFileContent()).doesNotContain("\">=12\"");
    }

    @Test
    void npmBinWriteThroughNotYetSupported() {
        // A `bin` delta still fails loud (npm normalizes string/object bin; needs a real golden to pin bytes).
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{},\"bin\":{\"lodash\":\"old.js\"}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{},\"bin\":{\"lodash\":\"new.js\"}," +
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
                "      \"bin\": {\"lodash\": \"old.js\"}\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        // The resolver's from-scratch attempt also defers on the bin entry shape (its detail is preferred).
        assertThat(result.getFailure().getDetail()).contains("declares bin");
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
        // is-odd pulls is-number ^7.0.0, but is-number is locked at 6.0.0 top-level under a *ranged* pin
        // (^6.0.0). Nesting requires proof the top slot is frozen (an exact pin); a ranged pin might move
        // (cascade), so the resolver conservatively defers rather than fork. (Exact-pin nesting: I5 add-nest.)
        routes.put("https://registry.npmjs.org/is-odd", "{\"versions\":{\"3.0.1\":{}}}");
        routes.put("https://registry.npmjs.org/is-odd/3.0.1",
                "{\"name\":\"is-odd\",\"version\":\"3.0.1\",\"dependencies\":{\"is-number\":\"^7.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz\"," +
                        "\"integrity\":\"sha512-ODD\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"is-number\": \"^6.0.0\"}},\n" +
                "    \"node_modules/is-number\": {\"version\": \"6.0.0\"}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"is-number\":\"^6.0.0\"}}",
                "{\"dependencies\":{\"is-number\":\"^6.0.0\",\"is-odd\":\"^3.0.1\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("is-number");
        assertThat(result.getFailure().getDetail()).contains("does not satisfy");
    }

    @Test
    void addNestOfNonLeafMemberFailsLoud() {
        // wrapper needs tool ^2.0.0, but root pins tool 1.0.0 exact (frozen top). tool@2.0.0 would nest under
        // wrapper — but it carries its own dependency (sub), so nesting it would cascade further: deferred.
        routes.put("https://registry.npmjs.org/wrapper", "{\"versions\":{\"1.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/wrapper/1.0.0",
                "{\"name\":\"wrapper\",\"version\":\"1.0.0\",\"dependencies\":{\"tool\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/wrapper/-/wrapper-1.0.0.tgz\",\"integrity\":\"sha512-W\"}}");
        routes.put("https://registry.npmjs.org/tool", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/tool/2.0.0",
                "{\"name\":\"tool\",\"version\":\"2.0.0\",\"dependencies\":{\"sub\":\"^1.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/tool/-/tool-2.0.0.tgz\",\"integrity\":\"sha512-T\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"tool\": \"1.0.0\"}},\n" +
                "    \"node_modules/tool\": {\"version\": \"1.0.0\"}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"tool\":\"1.0.0\"}}",
                "{\"dependencies\":{\"tool\":\"1.0.0\",\"wrapper\":\"^1.0.0\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("tool");
        assertThat(result.getFailure().getDetail()).contains("non-leaf");
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
        // The resolver's from-scratch attempt also defers on the bundleDependencies shape (its detail is preferred).
        assertThat(result.getFailure().getDetail()).contains("bundleDependencies").contains("entry shape not yet reproduced");
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

    // --- C1 / I5: reverse-dependent nesting ------------------------------

    @Test
    void singleReverseDependentExactPinNestsUnderDependent() {
        // needs-exact pins lodash 4.17.20 (excludes the bumped 4.17.21) -> npm keeps 4.17.21 top-level and
        // nests 4.17.20 under needs-exact (I5). The engine emits the nest; the patcher relocates it.
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
                "    \"node_modules/needs-exact\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"4.17.20\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent())
                .contains("\"node_modules/needs-exact/node_modules/lodash\"")
                .contains("sha512-OLD")   // the nested copy keeps the old integrity
                .contains("sha512-NEW");  // the top-level slot takes the bumped one
    }

    @Test
    void multipleReverseDependentsExcludingTargetFailLoud() {
        // Two locked packages each exclude the new version -> npm would nest a copy under each; nesting more
        // than one reverse-dependent reshapes further and is deferred.
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\"version\": \"4.17.20\"},\n" +
                "    \"node_modules/needs-exact\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"4.17.20\"}},\n" +
                "    \"node_modules/also-exact\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"4.17.20\"}}\n" +
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
                .contains("more than one reverse-dependent");
    }

    @Test
    void reverseDependentNestingANonLeafFailsLoud() {
        // The excluded old version is not a leaf (its lock entry carries its own dependencies), so relocating
        // it could force a further nest — deferred rather than guessed.
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.20\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.20.tgz\",\n" +
                "      \"integrity\": \"sha512-OLD\",\n" +
                "      \"dependencies\": {\"tiny\": \"^1.0.0\"}\n" +
                "    },\n" +
                "    \"node_modules/needs-exact\": {\"version\": \"1.0.0\", \"dependencies\": {\"lodash\": \"4.17.20\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                lock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("non-leaf");
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

    // The expected detail is the resolver's from-scratch deferral (preferred over the surgical message): a leaf
    // surface it cannot yet reproduce reports "declares <field>", and an optional-deps surface trips its
    // peer/optional gate first.
    @Test
    void osChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"os\":[\"linux\"]", ",\"os\":[\"darwin\"]", "declares os");
    }

    @Test
    void cpuChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"cpu\":[\"x64\"]", ",\"cpu\":[\"arm64\"]", "declares cpu");
    }

    @Test
    void libcChangeFailsLoud() {
        assertClosureSurfaceFailsLoud(",\"libc\":[\"glibc\"]", ",\"libc\":[\"musl\"]", "declares libc");
    }

    @Test
    void bundleDependenciesChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"bundleDependencies\":[\"x\"]", "declares bundleDependencies");
    }

    @Test
    void optionalDependenciesChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"optionalDependencies\":{\"x\":\"^1.0.0\"}", "peer/optional dependencies");
    }

    @Test
    void peerDependenciesMetaChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"peerDependenciesMeta\":{\"x\":{\"optional\":true}}",
                "declares peerDependenciesMeta");
    }

    @Test
    void hasInstallScriptChangeFailsLoud() {
        assertClosureSurfaceFailsLoud("", ",\"hasInstallScript\":true", "declares hasInstallScript");
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
