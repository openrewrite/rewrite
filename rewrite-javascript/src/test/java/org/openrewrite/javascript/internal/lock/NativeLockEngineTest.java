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
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

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

    @Test
    void bunRegeneratesFromTheRegistryItsOwnNpmrcNames() {
        String scenario = "lock/bun/small-registry-mirror/";
        routes.put("https://registry.npmmirror.com/is-number", resource(scenario + "http/is-number"));

        Result result = NativeLockEngine.regenerate(PackageManager.Bun, Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .originalPackageJson(resource(scenario + "pkg-before"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .packageManagerFile(Paths.get("app/.npmrc"), resource(scenario + "npmrc"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunReadsAFileDependencyFromTheProjectManifests() {
        String scenario = "lock/bun/small-file-dep-add/";
        routes.put("https://registry.npmjs.org/is-number", resource(scenario + "http/is-number"));
        Request.RequestBuilder request = Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("package.json"));

        Result withoutDirectory = NativeLockEngine.regenerate(PackageManager.Bun, request.build(), ctx);
        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                request.manifest(Paths.get("local/package.json"), resource(scenario + "tree/local/package.json")).build(),
                ctx);

        assertThat(withoutDirectory.getFailure()).isNotNull();
        assertThat(withoutDirectory.getFailure().getDetail()).startsWith("the engine cannot know directory manifest of local");
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunFindsAPatchFileAmongThePathsTheHostHolds() {
        String scenario = "lock/bun/open-patched-dot-slash/";
        String patch = "patches/ms@2.1.3.patch";
        Request.RequestBuilder request = Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .originalPackageJson(resource(scenario + "pkg-before"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"));
        Set<Path> withoutPatch = new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/bun.lock")));
        Set<Path> withPatch = new HashSet<>(withoutPatch);
        withPatch.add(Paths.get("app/" + patch));

        Result noView = NativeLockEngine.regenerate(PackageManager.Bun, request.build(), ctx);
        Result unread = NativeLockEngine.regenerate(PackageManager.Bun, request.projectPaths(withPatch).build(), ctx);
        Result absent = NativeLockEngine.regenerate(PackageManager.Bun, request.projectPaths(withoutPatch).build(),
                ctx);
        Result read = NativeLockEngine.regenerate(PackageManager.Bun, request.projectPaths(withPatch)
                .packageManagerFile(Paths.get("app/" + patch), resource(scenario + "tree/" + patch)).build(), ctx);

        assertThat(noView.getFailure().getDetail()).startsWith("the engine cannot know patch file ./" + patch);
        assertThat(unread.getFailure().getDetail()).startsWith("the engine cannot know patch file ./" + patch);
        assertThat(absent.getFailure().getDetail())
                .isEqualTo("bun rejects this edit: Couldn't find patch file: './" + patch + "'");
        assertThat(read.isSuccess()).as(String.valueOf(read.getErrorMessage())).isTrue();
        assertThat(read.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunCannotKnowAConfigFileTheHostHoldsUnread() {
        String scenario = "lock/bun/small-registry-mirror/";
        routes.put("https://registry.npmmirror.com/is-number", resource(scenario + "http/is-number"));
        Request.RequestBuilder request = Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .originalPackageJson(resource(scenario + "pkg-before"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.npmrc"))));

        Result unread = NativeLockEngine.regenerate(PackageManager.Bun, request.build(), ctx);
        Result read = NativeLockEngine.regenerate(PackageManager.Bun,
                request.packageManagerFile(Paths.get("app/.npmrc"), resource(scenario + "npmrc")).build(), ctx);

        assertThat(unread.getFailure().getDetail())
                .isEqualTo("the engine cannot know .npmrc (its contents were not read)");
        assertThat(read.isSuccess()).as(String.valueOf(read.getErrorMessage())).isTrue();
        assertThat(read.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunReadsTheProjectsEnvFileAndCannotKnowOneTheHostHoldsUnread() {
        String scenario = "lock/bun/open-env-registry/";
        routes.put("https://registry.npmmirror.com/is-number", resource(scenario + "http/is-number"));
        Request.RequestBuilder request = Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .originalPackageJson(resource(scenario + "pkg-before"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.env"))));

        Result unread = NativeLockEngine.regenerate(PackageManager.Bun, request.build(), ctx);
        Result read = NativeLockEngine.regenerate(PackageManager.Bun,
                request.packageManagerFile(Paths.get("app/.env"), resource(scenario + "tree/.env")).build(), ctx);

        assertThat(unread.getFailure().getDetail())
                .isEqualTo("the engine cannot know .env (its contents were not read)");
        assertThat(read.isSuccess()).as(String.valueOf(read.getErrorMessage())).isTrue();
        assertThat(read.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunWithoutALockFailsLoud() {
        Result result = regen(PackageManager.Bun, "{\"dependencies\":{}}", "{\"dependencies\":{\"ms\":\"2.0.0\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
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
    void npmrcTokenWithExplicitDefaultPortRegeneratesLock() {
        String registry = "https://registry.example:443/npm/";
        NodeExecutionContextView.view(ctx).setRegistries(emptyList());
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.User, Map.of(
                        "registry", "https://registry.example/npm/",
                        "//registry.example/npm/:_authToken", "s3cret")),
                new Npmrc(NpmrcScope.Project, Map.of("registry", registry, "always-auth", "false"))));
        routes.put(registry + "lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put(registry + "lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put(registry + "lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"dist\":{\"tarball\":\"" + registry + "lodash/-/lodash-4.17.21.tgz\"," +
                        "\"integrity\":\"sha512-NEW\"}}");
        List<HttpSender.Request> requests = new ArrayList<>();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            requests.add(request);
            if (!"Bearer s3cret".equals(request.getRequestHeaders().get("Authorization"))) {
                return new HttpSender.Response(401, new ByteArrayInputStream(new byte[0]), () -> {
                });
            }
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        });
        String lock = "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"x\",\n" +
                "      \"dependencies\": {\n" +
                "        \"lodash\": \"^4.17.20\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.20\",\n" +
                "      \"resolved\": \"" + registry + "lodash/-/lodash-4.17.20.tgz\",\n" +
                "      \"integrity\": \"sha512-OLD\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"name\":\"x\",\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"name\":\"x\",\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                lock, marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(
                lock.replace("4.17.20", "4.17.21").replace("sha512-OLD", "sha512-NEW"));
        assertThat(requests).isNotEmpty().allSatisfy(request -> {
            assertThat(request.getUrl().toString()).startsWith(registry);
            assertThat(request.getRequestHeaders()).containsEntry("Authorization", "Bearer s3cret");
        });
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
    void versionNotFoundFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^5.0.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.VERSION_NOT_FOUND);
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

    // --- C7: workspace importer targeting --------------------------------

    @Test
    void workspaceMemberBumpTargetsMemberImporterByteExact() {
        routes.put("https://registry.npmjs.org/is-odd", resource("lock/npm/v3/http/is-odd"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.0", resource("lock/npm/v3/http/is-odd-3.0.0"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/v3/http/is-odd-3.0.1"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, Request.builder()
                .packageJson(resource("lock/npm/workspace/pkg-after"))
                .originalPackageJson(resource("lock/npm/workspace/pkg-before"))
                .lock(resource("lock/npm/workspace/before"))
                .packageJsonPath(Paths.get("packages/foo/package.json"))
                .manifest(Paths.get("package.json"), "{\n  \"name\": \"npm-lock-ws\",\n  \"version\": \"1.0.0\",\n" +
                                                     "  \"workspaces\": [\n    \"packages/*\"\n  ]\n}\n")
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/workspace/after"));
    }

    @Test
    void rootManifestWithoutNameAtTheRepositoryRootKeepsTheLockName() {
        String before = "{\n  \"dependencies\": {\n    \"lodash\": \"^4.17.20\"\n  }\n}\n";
        String after = "{\n  \"dependencies\": {}\n}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, Request.builder()
                .packageJson(after)
                .originalPackageJson(before)
                .lock(npmLock("4.17.20"))
                .packageJsonPath(Paths.get("package.json"))
                .manifest(Paths.get("package.json"), before)
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).startsWith("{\n  \"name\": \"x\",\n").doesNotContain("null");
    }

    @Test
    void npmrcValueNpmRejectsFailsLoud() {
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.Project, Map.of("lockfile-version", "abc"))));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                npmLock("4.17.20"), marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).startsWith("npm fails on this edit: Invalid lockfileVersion config: NaN");
    }

    @Test
    void npmrcReadingTheEnvironmentFailsLoud() {
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.Project, Map.of("legacy-peer-deps", "${LPD}"))));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                npmLock("4.17.20"), marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).startsWith("the engine cannot know the environment: \"${LPD}\"");
    }

    @Test
    void npmrcDryRunLeavesTheLockUnchanged() {
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.Project, Map.of("dry-run", "true"))));
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\"," +
                        "\"integrity\":\"sha512-NEW\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                npmLock("4.17.20"), marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(npmLock("4.17.20"));
    }

    @Test
    void npmrcGlobalFailsLoud() {
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.Project, Map.of("global", "true"))));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                npmLock("4.17.20"), marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail())
                .startsWith("npm rejects this edit (ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
    }

    // npm 11.17.0: location=global is global mode, even beside global=false written before it
    @Test
    void npmrcLocationGlobalFailsLoud() {
        Map<String, String> npmrc = new LinkedHashMap<>();
        npmrc.put("global", "false");
        npmrc.put("location", "global");
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(List.of(
                new Npmrc(NpmrcScope.Project, npmrc)));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                npmLock("4.17.20"), marker, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail())
                .startsWith("npm rejects this edit (ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
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
