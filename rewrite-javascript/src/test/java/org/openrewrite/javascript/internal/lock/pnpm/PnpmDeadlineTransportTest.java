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
package org.openrewrite.javascript.internal.lock.pnpm;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.LockDirectory;
import org.openrewrite.javascript.internal.lock.NativeLockEngine;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class PnpmDeadlineTransportTest {
    @Test
    void refusesEffectiveWorkspaceDeadlineBeforeBindingTransport() {
        PnpmOptions options = PnpmOptions.load("fetchTimeout: 1500\n", null, null, null,
                Collections.emptyMap(), PnpmRelease.of("11.10.0"));
        assertThatThrownBy(options::transport)
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("fetchTimeout")
                .hasMessageContaining("cannot safely apply");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.10.0"})
    void defaultsAndIgnoredSpellingDoNotRequestADeadline(String version) {
        accepts(version, null, null, null, Collections.emptyMap());
        accepts(version, "fetch-timeout: 1500\n", null, null, Collections.emptyMap());
        accepts(version, "fetchTimeout: null\n", null, null, Collections.emptyMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.10.0"})
    void explicitDefaultAndDisabledDeadlinesAlsoCannotBeApplied(String version) {
        refuses(version, "fetchTimeout: 60000\n", null, null, Collections.emptyMap());
        refuses(version, "fetchTimeout: false\n", null, null, Collections.emptyMap());
        refuses(version, "fetchTimeout: 0\n", null, null, Collections.emptyMap());
    }

    @Test
    void pnpm11FiltersNpmrcAndNpmEnvironmentBeforeTransport() {
        accepts("11.10.0", null, "fetch-timeout=1500\n", "fetch-timeout=1500\n",
                Map.of("npm_config_fetch_timeout", "1500"));
        refuses("11.10.0", null, null, null, Map.of("pnpm_config_fetch_timeout", "1500"));
        refuses("11.10.0", null, null, null, Map.of("PNPM_CONFIG_FETCH_TIMEOUT", "1500"));
        accepts("11.10.0", null, null, null, Map.of("pnpm_config_fetch_timeout", "false"));
        // An invalid numeric environment value leaves the workspace value in force.
        refuses("11.10.0", "fetchTimeout: 1500\n", null, null,
                Map.of("pnpm_config_fetch_timeout", "false"));
        accepts("11.10.0", "fetchTimeout: null\n", "fetch-timeout=1500\n", null, Map.of());
    }

    @Test
    void pnpm10ReadsNpmrcAndNpmEnvironmentButIgnoresPnpmEnvironment() {
        refuses("10.34.9", null, "fetch-timeout=1500\n", null, Map.of());
        refuses("10.34.9", null, null, "fetch-timeout=1500\n", Map.of());
        refuses("10.34.9", null, null, null, Map.of("npm_config_fetch_timeout", "1500"));
        refuses("10.34.9", null, null, null, Map.of("NPM_CONFIG_FETCH_TIMEOUT", "1500"));
        accepts("10.34.9", null, null, null, Map.of("pnpm_config_fetch_timeout", "1500"));
        accepts("10.34.9", null, null, null, Map.of("PNPM_CONFIG_FETCH_TIMEOUT", "1500"));
        // The workspace is read after npm-conf, so null resets an npmrc/environment deadline.
        accepts("10.34.9", "fetchTimeout: null\n", "fetch-timeout=1500\n", null,
                Map.of("npm_config_fetch_timeout", "1500"));
        refuses("10.34.9", "fetchTimeout: 1500\n", null, null,
                Map.of("pnpm_config_fetch_timeout", "false"));
    }

    @Test
    void onlyPnpm10ProjectsTheTrustedUserDeadline() {
        HostInputs host = HostInputs.builder().now(Instant.EPOCH)
                .npmrc(new Npmrc(NpmrcScope.User, Map.of("fetch-timeout", "1500")))
                .npmrc(new Npmrc(NpmrcScope.Project, Map.of("fetch-timeout", "9999"))).build();
        refuses("10.34.9", null, null, host.userPnpmrc(HostInputs.USER, true), Map.of());
        accepts("11.10.0", null, null, host.userPnpmrc(HostInputs.USER, false), Map.of());
        assertThat(host.userPnpmrc(HostInputs.USER, true)).doesNotContain("9999");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.10.0"})
    void liveAdapterRefusesBeforeRequestsAndReturnsNoPartialFiles(String version) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            Request request = Request.builder()
                    .packageJson("{\"name\":\"probe\",\"version\":\"1.0.0\",\"dependencies\":{\"deadline-probe\":\"1.0.0\"}}")
                    .packageJsonPath(Paths.get("package.json"))
                    .packageManagerFile(Paths.get(".npmrc"), "registry=http://127.0.0.1:" + server.getAddress().getPort() + "/\n")
                    .packageManagerFile(Paths.get("pnpm-workspace.yaml"), "fetchTimeout: 1500\nminimumReleaseAge: 0\n")
                    .lock("lockfileVersion: '9.0'\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\nimporters:\n  .: {}\n").build();
            HostInputs host = HostInputs.builder().now(Instant.EPOCH).packageManager(PackageManager.Pnpm)
                    .packageManagerVersion(version).build();
            var result = NativeLockEngine.port(PackageManager.Pnpm).regenerate(LockDirectory.of(request), host,
                    new NpmRegistryClient(new HttpUrlConnectionSender()));
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("fetchTimeout", "cannot safely apply");
            assertThat(result.getLockFileContent()).isNull();
            assertThat(result.getFiles()).isEmpty();
            assertThat(requests).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void selectedHostDeadlineDoesNotEnterUnknownPlaceholderEnvironment() {
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(new Environment() {
            public String getenv(String key) { return "pnpm_config_fetch_timeout".equals(key) ? "1500" : null; }
            public Path userHome() { return Paths.get("/nonexistent-deadline-test"); }
            public String osName() { return "Linux"; }
        }).build();
        assertThat(host.getEnvironment().process()).isNull();
        assertThatThrownBy(() -> host.getEnvironment().reference("pnpm_config_fetch_timeout"))
                .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> PnpmOptions.load(null, null, null, null, null,
                PnpmRelease.of("11.10.0")).transport(host.pnpmRequestEnvironment()))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("fetchTimeout");
    }

    @Test
    void selectedHostDeadlineKeepsManagerPrecedenceAndIgnoredValues() {
        assertThatCode(() -> PnpmOptions.load("fetchTimeout: null\n", null, null, null, null,
                PnpmRelease.of("10.34.9")).transport(Map.of("npm_config_fetch_timeout", "1500")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PnpmOptions.load("fetchTimeout: null\n", null, null, null, null,
                PnpmRelease.of("11.10.0")).transport(Map.of("PNPM_CONFIG_FETCH_TIMEOUT", "1500")))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("fetchTimeout");
        assertThatCode(() -> PnpmOptions.load(null, null, null, null, null,
                PnpmRelease.of("11.10.0")).transport(Map.of("npm_config_fetch_timeout", "1500",
                "pnpm_config_fetch_timeout", "false"))).doesNotThrowAnyException();
        assertThatCode(() -> PnpmOptions.load(null, null, null, null, null,
                PnpmRelease.of("10.34.9")).transport(Map.of("pnpm_config_fetch_timeout", "1500")))
                .doesNotThrowAnyException();
    }

    private static void accepts(String version, String workspace, String project, String user, Map<String, String> env) {
        assertThatCode(() -> PnpmOptions.load(workspace, project, user, null, env,
                PnpmRelease.of(version)).transport()).doesNotThrowAnyException();
    }

    private static void refuses(String version, String workspace, String project, String user, Map<String, String> env) {
        assertThatThrownBy(() -> PnpmOptions.load(workspace, project, user, null, env,
                PnpmRelease.of(version)).transport())
                .isInstanceOf(CannotKnow.class)
                .hasMessage("the engine cannot know pnpm network policy requires fetchTimeout, which this transport cannot safely apply");
    }
}
