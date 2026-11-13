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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
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
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class NativeLockEngineTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();
    private final Map<String, byte[]> binaryRoutes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        binaryRoutes.clear();
        HttpSender http = request -> {
            String url = request.getUrl().toString();
            String body = routes.get(url);
            byte[] bytes = binaryRoutes.containsKey(url) ? binaryRoutes.get(url) :
                    (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
            return new HttpSender.Response(body == null && !binaryRoutes.containsKey(url) ? 404 : 200,
                    new ByteArrayInputStream(bytes), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        // Pin the registry so discovery is deterministic (never reads a real ~/.npmrc or env).
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true,
                        false)));
    }

    private Result regen(PackageManager pm, String edited, @Nullable String lock) {
        return regen(pm, edited, lock, null);
    }

    private Result regen(PackageManager pm, String edited, @Nullable String lock,
                         @Nullable NodeResolutionResult marker) {
        return regenerate(pm, Request.builder()
                .packageJson(edited)
                .lock(lock)
                .marker(marker)
                .packageJsonPath(Paths.get("package.json"))
                .build(), ctx);
    }

    private static NodeResolutionResult projectNpmrc(String... keysAndValues) {
        Map<String, String> npmrc = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            npmrc.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(singletonList(
                new Npmrc(NpmrcScope.Project, npmrc)));
    }

    @Test
    void bunRegeneratesFromTheRegistryItsOwnNpmrcNames() {
        String scenario = "lock/bun/small-registry-mirror/";
        routes.put("https://registry.npmmirror.com/is-number", resource(scenario + "http/is-number"));

        Result result = regenerate(PackageManager.Bun, Request.builder()
                .packageJson(resource(scenario + "pkg"))
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

        Result withoutDirectory = regenerate(PackageManager.Bun, request.build(), ctx);
        Result result = regenerate(PackageManager.Bun,
                request.manifest(Paths.get("local/package.json"), resource(scenario + "tree/local/package.json"))
                        .build(),
                ctx);

        assertThat(withoutDirectory.getFailure()).isNotNull();
        assertThat(withoutDirectory.getFailure().getDetail()).startsWith("the engine cannot know directory manifest " +
                "of local");
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunFindsAPatchFileAmongThePathsTheHostHolds() {
        String scenario = "lock/bun/open-patched-dot-slash/";
        String patch = "patches/ms@2.1.3.patch";
        Request.RequestBuilder request = Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"));
        Set<Path> withoutPatch = new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/bun.lock")));
        Set<Path> withPatch = new HashSet<>(withoutPatch);
        withPatch.add(Paths.get("app/" + patch));

        Result noView = regenerate(PackageManager.Bun, request.build(), ctx);
        Result unread = regenerate(PackageManager.Bun, request.projectPaths(withPatch).build(), ctx);
        Result absent = regenerate(PackageManager.Bun, request.projectPaths(withoutPatch).build(),
                ctx);
        Result read = regenerate(PackageManager.Bun, request.projectPaths(withPatch)
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
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.npmrc"))));

        Result unread = regenerate(PackageManager.Bun, request.build(), ctx);
        Result read = regenerate(PackageManager.Bun,
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
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .projectPaths(new HashSet<>(Arrays.asList(Paths.get("app/package.json"), Paths.get("app/.env"))));

        Result unread = regenerate(PackageManager.Bun, request.build(), ctx);
        Result read = regenerate(PackageManager.Bun,
                request.packageManagerFile(Paths.get("app/.env"), resource(scenario + "tree/.env")).build(), ctx);

        assertThat(unread.getFailure().getDetail())
                .isEqualTo("the engine cannot know .env (its contents were not read)");
        assertThat(read.isSuccess()).as(String.valueOf(read.getErrorMessage())).isTrue();
        assertThat(read.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void bunWithoutALockFailsLoud() {
        Result result = regen(PackageManager.Bun, "{\"dependencies\":{\"ms\":\"2.0.0\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void yarnClassicRegeneratesFromTheRegistryItsConfigNames() {
        String scenario = "lock/yarn-classic/small-registry-yarnrc-npmjs/";
        routes.put("https://registry.npmjs.org/which-module", resource(scenario + "http/which-module"));
        binaryRoutes.put("https://registry.npmjs.org/which-module/-/which-module-2.0.1.tgz",
                bytes(scenario + "http/which-module-2.0.1.tgz"));

        Result result = regenerate(PackageManager.YarnClassic, Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .packageManagerFile(Paths.get("app/.yarnrc"), resource(scenario + "yarnrc"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void yarnClassicReadsWorkspaceMembersFromTheProjectManifests() {
        String scenario = "lock/yarn-classic/small-workspace-member-edit/";
        Request request = Request.builder()
                .packageJson(resource(scenario + "tree/packages/a/package.json"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("packages/a/package.json"))
                .manifest(Paths.get("package.json"), resource(scenario + "pkg"))
                .manifest(Paths.get("packages/b/package.json"),
                        resource(scenario + "tree-before/packages/b/package.json"))
                .build();
        // the host names npmjs, which yarn 1.22.22 asks over its own default and fetches from as its own (recorded)
        routes.put("https://registry.npmjs.org/is-number", resource(scenario + "http/is-number"));
        binaryRoutes.put("https://registry.yarnpkg.com/is-number/-/is-number-7.0.0.tgz",
                bytes(scenario + "http/is-number-7.0.0.tgz"));

        Result result = regenerate(PackageManager.YarnClassic, request, ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void yarnClassicWithoutALockFailsLoud() {
        Result result = regen(PackageManager.YarnClassic, "{\"dependencies\":{\"ms\":\"2.0.0\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void yarnBerryRegeneratesFromItsConfigurationFiles() {
        String scenario = "lock/yarn-berry/small-catalog-default-edit/";
        routes.put("https://registry.npmjs.org/path-is-absolute", resource(scenario + "http/path-is-absolute"));
        binaryRoutes.put("https://registry.npmjs.org/path-is-absolute/-/path-is-absolute-1.0.1.tgz",
                bytes(scenario + "http/path-is-absolute-1.0.1.tgz"));

        Result result = regenerate(PackageManager.YarnBerry, Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .packageManagerFile(Paths.get("app/.yarnrc.yml"), resource(scenario + "yarnrc-after"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void yarnBerryReadsWorkspaceMembersFromTheProjectManifests() {
        String scenario = "lock/yarn-berry/ws-member-add/";
        Result result = regenerate(PackageManager.YarnBerry, Request.builder()
                .packageJson(resource(scenario + "tree/packages/app/package.json"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("packages/app/package.json"))
                .manifest(Paths.get("package.json"), resource(scenario + "pkg"))
                .manifest(Paths.get("packages/lib/package.json"), resource(scenario + "tree/packages/lib/package.json"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void yarnBerryWithoutALockFailsLoud() {
        Result result = regen(PackageManager.YarnBerry,
                "{\"packageManager\":\"yarn@4.10.3\",\"dependencies\":{\"ms\":\"2.0.0\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    /**
     * The credentials the host holds for a registry the project's .npmrc names reach yarn's request for a scoped
     * package there, as the home .npmrc's (Yarn Berry's as its npmRegistries entry).
     */
    @Test
    void yarnSendsTheHostsCredentialsToARegistryTheProjectNames() {
        NodeExecutionContextView.view(ctx).setRegistries(emptyList());
        NodeExecutionContextView.view(ctx).setRegistryCredentials(Arrays.asList(
                new NodeRegistryCredentials("corp.example", "corp-token", null, null),
                new NodeRegistryCredentials("registry.npmjs.org", "npm-token", null, null)));
        List<HttpSender.Request> requests = new ArrayList<>();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            requests.add(request);
            return new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
            });
        });
        Request.RequestBuilder request = Request.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .packageJsonPath(Paths.get("package.json"))
                .marker(projectNpmrc("@corp:registry", "https://corp.example/"));

        regenerate(PackageManager.YarnClassic, request
                .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"@corp/a\":\"^1.0.0\"}}")
                .build(), ctx);
        regenerate(PackageManager.YarnBerry, request
                .packageJson("{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"," +
                             "\"dependencies\":{\"@corp/a\":\"^1.0.0\"}}")
                .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n" +
                      "  version: 0.0.0-use.local\n  resolution: \"p@workspace:.\"\n  languageName: unknown\n" +
                      "  linkType: soft\n")
                .packageManagerFile(Paths.get(".yarnrc.yml"),
                        "npmScopes:\n  corp:\n    npmRegistryServer: \"https://corp.example\"\n")
                .build(), ctx);

        assertThat(requests).extracting(r -> r.getUrl().toString()).containsExactly(
                "https://corp.example/@corp%2Fa", "https://corp.example/@corp%2Fa");
        assertThat(requests).allSatisfy(r ->
                assertThat(r.getRequestHeaders()).containsEntry("Authorization", "Bearer corp-token"));
    }

    /**
     * An unscoped package from the private registry a project names gets the host's credentials too: the home file
     * holds them with always-auth (Yarn Berry: its npmRegistries entry's npmAlwaysAuth).
     */
    @Test
    void yarnSendsTheHostsCredentialsForAnUnscopedPackage() {
        NodeExecutionContextView.view(ctx).setRegistries(emptyList());
        NodeExecutionContextView.view(ctx).setRegistryCredentials(singletonList(
                new NodeRegistryCredentials("corp.example", "corp-token", null, null)));
        List<HttpSender.Request> requests = new ArrayList<>();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            requests.add(request);
            return new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
            });
        });
        Request.RequestBuilder request = Request.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .packageJsonPath(Paths.get("package.json"))
                .marker(projectNpmrc("registry", "https://corp.example/"));

        regenerate(PackageManager.YarnClassic, request
                .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\"}}")
                .build(), ctx);
        regenerate(PackageManager.YarnBerry, request
                .packageJson("{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"," +
                             "\"dependencies\":{\"a\":\"^1.0.0\"}}")
                .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n" +
                      "  version: 0.0.0-use.local\n  resolution: \"p@workspace:.\"\n  languageName: unknown\n" +
                      "  linkType: soft\n")
                .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: \"https://corp.example\"\n")
                .build(), ctx);

        assertThat(requests).extracting(r -> r.getUrl().toString())
                .containsExactly("https://corp.example/a", "https://corp.example/a");
        assertThat(requests).allSatisfy(r ->
                assertThat(r.getRequestHeaders()).containsEntry("Authorization", "Bearer corp-token"));
    }

    /**
     * A project's own token stays in its .npmrc: yarn sends it for an unscoped package only under always-auth, and
     * Yarn Berry never reads that file.
     */
    @Test
    void yarnLeavesTheProjectsOwnTokenInItsNpmrc() {
        NodeExecutionContextView.view(ctx).setRegistries(emptyList());
        NodeExecutionContextView.view(ctx).setRegistryCredentials(emptyList());
        List<HttpSender.Request> requests = new ArrayList<>();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            requests.add(request);
            return new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
            });
        });
        Request.RequestBuilder request = Request.builder()
                .lock("# yarn lockfile v1\n\n\n")
                .packageJsonPath(Paths.get("package.json"))
                .marker(projectNpmrc("registry", "https://corp.example/", "//corp.example/:_authToken",
                        "project-token"));

        regenerate(PackageManager.YarnClassic, request
                .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\"}}")
                .build(), ctx);
        regenerate(PackageManager.YarnBerry, request
                .packageJson("{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"," +
                             "\"dependencies\":{\"a\":\"^1.0.0\"}}")
                .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n" +
                      "  version: 0.0.0-use.local\n  resolution: \"p@workspace:.\"\n  languageName: unknown\n" +
                      "  linkType: soft\n")
                .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: \"https://corp.example\"\n")
                .build(), ctx);

        assertThat(requests).extracting(r -> r.getUrl().toString())
                .containsExactly("https://corp.example/a", "https://corp.example/a");
        assertThat(requests).allSatisfy(r -> assertThat(r.getRequestHeaders()).doesNotContainKey("Authorization"));
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
        Map<String, String> user = new LinkedHashMap<>();
        user.put("registry", "https://registry.example/npm/");
        user.put("//registry.example/npm/:_authToken", "s3cret");
        Map<String, String> project = new LinkedHashMap<>();
        project.put("registry", registry);
        project.put("always-auth", "false");
        NodeResolutionResult marker = nodeResolutionResult(PackageManager.Npm).withNpmrcConfigs(Arrays.asList(
                new Npmrc(NpmrcScope.User, user), new Npmrc(NpmrcScope.Project, project)));
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

        Result result = regen(PackageManager.Npm, "{\"name\":\"x\",\"dependencies\":{\"lodash\":\"^4.17.21\"}}", lock,
                marker);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(
                lock.replace("4.17.20", "4.17.21").replace("sha512-OLD", "sha512-NEW"));
        assertThat(requests).isNotEmpty().allSatisfy(request -> {
            assertThat(request.getUrl().toString()).startsWith(registry);
            assertThat(request.getRequestHeaders()).containsEntry("Authorization", "Bearer s3cret");
        });
    }

    @Test
    void pnpmDiscardsALockOfAnotherMajor() {
        Result result = regen(PackageManager.Pnpm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "lockfileVersion: 5.4\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void pnpmRegeneratesFromItsWorkspaceManifest() {
        String scenario = "lock/pnpm/small-overrides-ws-direct/";
        routes.put("https://registry.npmjs.org/wrappy", resource(scenario + "http/wrappy"));

        Result result = regenerate(PackageManager.Pnpm, Request.builder()
                .packageJson(resource(scenario + "pkg"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("app/package.json"))
                .packageManagerFile(Paths.get("app/pnpm-workspace.yaml"), resource(scenario + "workspace"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void pnpmReadsWorkspaceMembersFromTheProjectManifests() {
        String scenario = "lock/pnpm/small-ws-member-bump/";
        routes.put("https://registry.npmjs.org/wrappy", resource(scenario + "http/wrappy"));

        Result result = regenerate(PackageManager.Pnpm, Request.builder()
                .packageJson(resource(scenario + "tree/packages/a/package.json"))
                .lock(resource(scenario + "before"))
                .packageJsonPath(Paths.get("packages/a/package.json"))
                .manifest(Paths.get("package.json"), resource(scenario + "pkg"))
                .manifest(Paths.get("packages/b/package.json"),
                        resource(scenario + "tree-before/packages/b/package.json"))
                .packageManagerFile(Paths.get("pnpm-workspace.yaml"), resource(scenario + "workspace-before"))
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "after"));
    }

    @Test
    void pnpmWithoutALockFailsLoud() {
        Result result = regen(PackageManager.Pnpm, "{\"dependencies\":{\"ms\":\"2.0.0\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void leafAddByteExact() {
        routes.put("https://registry.npmjs.org/left-pad", resource("lock/npm/add-leaf/http/left-pad"));
        routes.put("https://registry.npmjs.org/left-pad/1.3.0", resource("lock/npm/add-leaf/http/left-pad-1.3.0"));

        Result result = regen(PackageManager.Npm, resource("lock/npm/add-leaf/pkg-after"),
                resource("lock/npm/add-leaf/before"));

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/add-leaf/after"));
    }

    @Test
    void versionNotFoundFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^5.0.0\"}}", npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.VERSION_NOT_FOUND);
    }

    @Test
    void nullLockFailsLoud() {
        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void workspaceMemberBumpTargetsMemberImporterByteExact() {
        routes.put("https://registry.npmjs.org/is-odd", resource("lock/npm/v3/http/is-odd"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.0", resource("lock/npm/v3/http/is-odd-3.0.0"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/v3/http/is-odd-3.0.1"));

        Result result = regenerate(PackageManager.Npm, Request.builder()
                .packageJson(resource("lock/npm/workspace/pkg-after"))
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

        Result result = regenerate(PackageManager.Npm, Request.builder()
                .packageJson(after)
                .lock(npmLock("4.17.20"))
                .packageJsonPath(Paths.get("package.json"))
                .manifest(Paths.get("package.json"), before)
                .build(), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).startsWith("{\n  \"name\": \"x\",\n").doesNotContain("null");
    }

    @Test
    void npmrcValueNpmRejectsFailsLoud() {
        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"),
                projectNpmrc("lockfile-version", "abc"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).startsWith("npm fails on this edit: Invalid lockfileVersion " +
                "config: NaN");
    }

    @Test
    void npmrcReadingTheEnvironmentFailsLoud() {
        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"),
                projectNpmrc("legacy-peer-deps", "${LPD}"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).startsWith("the engine cannot know the environment variable LPD ");
    }

    @Test
    void npmrcDryRunLeavesTheLockUnchanged() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\"," +
                        "\"integrity\":\"sha512-NEW\"}}");

        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"),
                projectNpmrc("dry-run", "true"));

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(npmLock("4.17.20"));
    }

    @Test
    void npmrcGlobalFailsLoud() {
        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"),
                projectNpmrc("global", "true"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail())
                .startsWith("npm rejects this edit (ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
    }

    // npm 11.17.0: location=global is global mode, even beside global=false written before it
    @Test
    void npmrcLocationGlobalFailsLoud() {
        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"),
                projectNpmrc("global", "false", "location", "global"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail())
                .startsWith("npm rejects this edit (ESHRINKWRAPGLOBAL): cannot generate lockfile for global packages");
    }

    @Test
    void registryTokenInUrlNeverAppearsInFailureDetail() {
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(new NodeRegistry(
                null, "https://user:s3cr3ttoken@registry.example.com/", null, null, null, null, false, null, true,
                        false)));

        Result result = regen(PackageManager.Npm, "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}", npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).doesNotContain("s3cr3ttoken");
        assertThat(result.getErrorMessage()).doesNotContain("s3cr3ttoken");
    }

    /**
     * Every package manager reads the package.json of each directory its workspace patterns match. A member the host
     * holds without its contents (a parse error, a file too large to parse) is not a workspace left out of the lock.
     */
    @ParameterizedTest
    @EnumSource(value = PackageManager.class, names = {"Npm", "Pnpm", "Bun", "YarnClassic", "YarnBerry"})
    void aWorkspaceMemberTheHostHoldsUnreadIsNotLeftOutOfTheLock(PackageManager pm) {
        String root = pm == PackageManager.Pnpm ? "{\"name\":\"root\",\"version\":\"1.0.0\",\"private\":true}" :
                "{\"name\":\"root\",\"version\":\"1.0.0\",\"private\":true," +
                (pm == PackageManager.YarnBerry ? "\"packageManager\":\"yarn@4.10.3\"," : "") +
                "\"workspaces\":[\"packages/*\"]}";
        Path lock = Paths.get(port(pm).lockFile());
        Set<Path> held = new HashSet<>(Arrays.asList(Paths.get("package.json"), lock,
                Paths.get("packages/a/package.json"), Paths.get("packages/b/package.json")));
        Request.RequestBuilder request = Request.builder()
                .packageJson(root)
                .lock(workspaceLock(pm))
                .marker(nodeResolutionResult(pm))
                .packageJsonPath(Paths.get("package.json"))
                .manifest(Paths.get("package.json"), root)
                .manifest(Paths.get("packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}");
        if (pm == PackageManager.Pnpm) {
            held.add(Paths.get("pnpm-workspace.yaml"));
            request.packageManagerFile(Paths.get("pnpm-workspace.yaml"), "packages:\n  - 'packages/*'\n");
        }

        Result result = regenerate(pm, request.projectPaths(held).build(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(requireNonNull(result.getFailure()).getKind()).as(result.getErrorMessage())
                .isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).contains("packages/b/package.json");
    }

    // the lock of a workspace whose one member, packages/a, has no dependencies
    private static String workspaceLock(PackageManager pm) {
        switch (pm) {
            case Npm:
                return "{\n  \"name\": \"root\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": 3,\n" +
                       "  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": \"root\",\n" +
                       "      \"version\": \"1.0.0\",\n      \"workspaces\": [\n        \"packages/*\"\n      ]\n" +
                       "    },\n    \"node_modules/a\": {\n      \"resolved\": \"packages/a\",\n" +
                       "      \"link\": true\n    },\n    \"packages/a\": {\n      \"version\": \"1.0.0\"\n    }\n" +
                       "  }\n}\n";
            case Pnpm:
                return "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                       "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n\n  packages/a: {}\n";
            case Bun:
                return "{\n  \"lockfileVersion\": 1,\n  \"workspaces\": {\n    \"\": {\n      \"name\": \"root\",\n" +
                       "    },\n    \"packages/a\": {\n      \"name\": \"a\",\n      \"version\": \"1.0.0\",\n    },\n" +
                       "  },\n  \"packages\": {\n    \"a\": [\"a@workspace:packages/a\"],\n  }\n}\n";
            case YarnClassic:
                return "# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.\n# yarn lockfile v1\n\n\n";
            default:
                return "# This file is generated by running \"yarn install\" inside your project.\n" +
                       "# Manual changes might be lost - proceed with caution!\n\n__metadata:\n  version: 8\n" +
                       "  cacheKey: 10c0\n\n\"a@workspace:packages/a\":\n  version: 0.0.0-use.local\n" +
                       "  resolution: \"a@workspace:packages/a\"\n  languageName: unknown\n  linkType: soft\n\n" +
                       "\"root@workspace:.\":\n  version: 0.0.0-use.local\n  resolution: \"root@workspace:.\"\n" +
                       "  languageName: unknown\n  linkType: soft\n";
        }
    }

    private static String resource(String path) {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String path) {
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
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Result regenerate(PackageManager pm, Request request, ExecutionContext ctx) {
        return NativeLockEngine.regenerate(port(pm), request, ctx);
    }

    private static Port port(PackageManager pm) {
        return requireNonNull(NativeLockEngine.port(pm));
    }
}
