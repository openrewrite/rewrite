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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PnpmRegistryAuthTest {
    private static final String REGISTRY = "https://private.example/";
    private static final String TOKEN_KEY = "//private.example/:_authToken";

    @Test
    void projectEnvironmentCredentialsAreNotSent(@TempDir Path home) {
        check("11.10.0", "${LOCK_VALIDATION_TOKEN}", null, true, null, false, home);
    }

    static Stream<Arguments> nativePolicies() {
        List<Arguments> cases = new ArrayList<>();
        // Exact install controls, not pnpm view: npm-conf's view command differs on 10.34.9.
        for (String version : List.of("10.0.0", "10.11.0", "10.34.9", "11.0.3", "11.1.2", "11.1.3",
          "11.5.2", "11.5.3", "11.10.0", "11.16.0")) {
            boolean ignoresProjectEnv = List.of("10.34.9", "11.5.3", "11.10.0", "11.16.0").contains(version);
            boolean bounded10 = version.startsWith("10.");
            cases.add(Arguments.of(version, "${LOCK_VALIDATION_TOKEN}", null, true,
              ignoresProjectEnv || bounded10 ? null : "Bearer synthetic-env", bounded10));
            cases.add(Arguments.of(version, "synthetic-literal", null, true, "Bearer synthetic-literal", false));
            cases.add(Arguments.of(version, null, "${LOCK_VALIDATION_TOKEN}", true, "Bearer synthetic-env", false));
            cases.add(Arguments.of(version, "${LOCK_VALIDATION_TOKEN}", "synthetic-user", true,
              bounded10 ? null : ignoresProjectEnv ? "Bearer synthetic-user" : "Bearer synthetic-env", bounded10));
            cases.add(Arguments.of(version, "synthetic-literal", "${LOCK_VALIDATION_TOKEN}", true,
              "Bearer synthetic-literal", false));
            // Old 11 releases send unresolved placeholders. The engine explicitly declines instead of leaking them.
            boolean refusesPlaceholder = List.of("11.0.3", "11.1.2").contains(version);
            cases.add(Arguments.of(version, "${LOCK_VALIDATION_TOKEN}", null, false, null, refusesPlaceholder || bounded10));
            cases.add(Arguments.of(version, null, "${LOCK_VALIDATION_TOKEN}", false, null, refusesPlaceholder));
        }
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("nativePolicies")
    void registryRequestsFollowTheInstallingRelease(String version, @Nullable String projectToken,
                                                    @Nullable String userToken, boolean set,
                                                    @Nullable String expected, boolean refuses,
                                                    @TempDir Path home) {
        check(version, projectToken, userToken, set, expected, refuses, home);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9", "11.0.3", "11.5.3", "11.10.0", "11.16.0"})
    void globalConfigIsReadOnlyByPnpm10(String version, @TempDir Path home) {
        check(version, null, null, true, version.startsWith("10.") ? "Bearer synthetic-env" : null,
          false, home, "${LOCK_VALIDATION_TOKEN}");
    }

    private static void check(String version, @Nullable String projectToken, @Nullable String userToken,
                              boolean set, @Nullable String expected, boolean refuses, Path home) {
        check(version, projectToken, userToken, set, expected, refuses, home, null);
    }

    private static void check(String version, @Nullable String projectToken, @Nullable String userToken,
                              boolean set, @Nullable String expected, boolean refuses, Path home,
                              @Nullable String globalToken) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("@lock-validation:registry", REGISTRY);
        String project = "@lock-validation:registry=" + REGISTRY + "\n";
        if (projectToken != null) {
            properties.put(TOKEN_KEY, projectToken);
            project += TOKEN_KEY + "=" + projectToken + "\n";
        }
        Map<String, String> env = set ? Map.of("LOCK_VALIDATION_TOKEN", "synthetic-env") : Map.of();
        HostInputs.HostInputsBuilder host = HostInputs.builder().now(Instant.EPOCH)
          .packageManager(PackageManager.Pnpm).packageManagerVersion(version)
          .environment(HostEnvironment.of(env)).system(new Environment() {
              public @Nullable String getenv(String name) { return env.get(name); }
              public Path userHome() { return home; }
              public String osName() { return "Linux"; }
          }).npmrc(new Npmrc(NpmrcScope.Project, properties));
        if (globalToken != null) {
            host.npmrc(new Npmrc(NpmrcScope.Global, Map.of("@lock-validation:registry", REGISTRY, TOKEN_KEY, globalToken)));
        }
        if (userToken != null) {
            host.npmrc(new Npmrc(NpmrcScope.User, Map.of("@lock-validation:registry", REGISTRY, TOKEN_KEY, userToken)));
        }
        Request request = Request.builder()
          .packageJson("{\"name\":\"auth-test\",\"packageManager\":\"pnpm@" + version +
            "\",\"dependencies\":{\"@lock-validation/private-counter\":\"1.0.0\"}}")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".npmrc"), project)
          .packageManagerFile(Paths.get("pnpm-workspace.yaml"), "packages:\n  - .\nminimumReleaseAge: 0\n")
          .lock("lockfileVersion: '9.0'\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\nimporters:\n  .: {}\n").build();
        List<HttpSender.Request> requests = new ArrayList<>();
        HttpSender sender = req -> {
            requests.add(req);
            // The private registry rejects every anonymous request. A declined credential produces no lock patch.
            boolean authorized = expected != null && expected.equals(req.getRequestHeaders().get("Authorization"));
            String body = authorized ? "{\"name\":\"@lock-validation/private-counter\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"@lock-validation/private-counter\",\"version\":\"1.0.0\",\"dist\":{\"integrity\":\"sha512-c3ludGhldGlj\",\"tarball\":\"https://private.example/private.tgz\"}}}}" : "{\"error\":\"authentication required\"}";
            return new HttpSender.Response(authorized ? 200 : 401,
              new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), () -> {});
        };
        Result result;
        try {
            result = PnpmInputs.PORT.regenerate(LockDirectory.of(request), host.build(), new NpmRegistryClient(sender));
        } catch (NodeRegistryException e) {
            // NativeLockEngine converts transport rejection to a failure with no patch.
            result = Result.failure(LockOnlyInstall.failure(e));
        }
        if (refuses) {
            assertThat(requests).isEmpty();
        } else {
            assertThat(requests).isNotEmpty().allSatisfy(req -> {
                assertThat(req.getRequestHeaders().get("Authorization")).isEqualTo(expected);
                if (expected != null) {
                    assertThat(req.getRequestHeaders().get("Authorization")).doesNotContain("${");
                }
            });
        }
        if (expected == null) {
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getLockFileContent()).isNull();
            assertThat(result.getFiles()).isEmpty();
        } else {
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getLockFileContent()).contains("@lock-validation/private-counter");
        }
    }
}
