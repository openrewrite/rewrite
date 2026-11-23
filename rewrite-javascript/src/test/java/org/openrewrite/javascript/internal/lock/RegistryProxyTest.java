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

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.NpmOptions;
import org.openrewrite.javascript.internal.lock.npm.NpmReleases;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryProxyTest {
    @ParameterizedTest
    @ValueSource(strings = {"token", "base64", "username"})
    void bunCannotForwardHostCredentialsWhenTheirOriginalAuthPathIsUnknown(String credential) {
        AtomicInteger sent = new AtomicInteger();
        NodeRegistry registry = new NodeRegistry("@proxy-control", "https://registry.invalid/private/",
                "token".equals(credential) ? "synthetic-secret" : null,
                "username".equals(credential) ? "synthetic-user" : null, null,
                "base64".equals(credential) ? "c3ludGhldGljOnNlY3JldA==" : null, false, null, true, false);
        NodeRegistries registries = new NodeRegistries(registry, Map.of("@proxy-control", registry), null, null, null);
        BunRegistryManifests source = new BunRegistryManifests(registries, new NpmRegistryClient(r -> {
            sent.incrementAndGet();
            throw new IllegalStateException("synthetic sender boundary");
        }));
        assertThatThrownBy(() -> source.packument(registry.getUrl(), "@proxy-control/synthetic", false))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("credential path").hasMessageNotContaining("synthetic-secret");
        assertThat(sent).hasValue(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--proxy", "--https-proxy", "--network-timeout",
            "--install.proxy", "--install.https-proxy", "--install.network-timeout"})
    void commandStyleYarnRequestOptionsCannotDisappearBeforeTheLiveAdapter(String option) {
        AtomicInteger sent = new AtomicInteger();
        String value = option.endsWith("network-timeout") ? "1500" : "http://127.0.0.1:1";
        Request request = Request.builder()
                .packageJson("{\"name\":\"proxy-control\",\"version\":\"1.0.0\",\"dependencies\":{\"synthetic\":\"1.0.0\"}}")
                .packageJsonPath(Paths.get("package.json"))
                .packageManagerFile(Paths.get(".npmrc"), "registry=http://registry.invalid/\n")
                .packageManagerFile(Paths.get(".yarnrc"), option + " \"" + value + "\"\n")
                .lock("# yarn lockfile v1\n").build();
        Environment environment = new Environment() {
            public String getenv(String name) { return null; }
            public Path userHome() { return Paths.get("/nonexistent-registry-proxy-test"); }
            public String osName() { return "Linux"; }
        };
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(environment)
                .packageManager(PackageManager.YarnClassic).packageManagerVersion("1.22.22").build();
        Result result;
        try {
            result = YarnClassicInputs.PORT.regenerate(LockDirectory.of(request), host,
                    new NpmRegistryClient(r -> {
                        sent.incrementAndGet();
                        throw new IllegalStateException("synthetic sender boundary");
                    }));
        } catch (NodeRegistryException e) {
            result = Result.failure(LockOnlyInstall.failure(e));
        }
        assertThat(sent).hasValue(0);
        assertThat(result.getErrorMessage()).contains("network policy");
    }

    @ParameterizedTest
    @CsvSource({"YARN_ENABLE_OFFLINE_MODE,false", "YARN_ENABLE_OFFLINE_MODE,0",
            "YARN_ENABLE_NETWORK,true", "YARN_ENABLE_NETWORK,1",
            "YARN_ENABLE_STRICT_SSL,true", "YARN_ENABLE_STRICT_SSL,1"})
    void ordinaryYarnHostDefaultsDoNotRequireAnUnboundPolicy(String key, String value) {
        AtomicInteger sent = new AtomicInteger();
        Environment environment = new Environment() {
            public String getenv(String name) { return key.equals(name) ? value : null; }
            public Path userHome() { return Paths.get("/nonexistent-registry-proxy-test"); }
            public String osName() { return "Linux"; }
        };
        Request request = Request.builder()
                .packageJson("{\"name\":\"proxy-control\",\"version\":\"1.0.0\",\"dependencies\":{\"synthetic\":\"1.0.0\"}}")
                .packageJsonPath(Paths.get("package.json"))
                .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: http://registry.invalid/\n")
                .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n").build();
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(environment)
                .packageManager(PackageManager.YarnBerry).packageManagerVersion("4.17.0").build();
        Result result;
        try {
            result = YarnBerryInputs.PORT.regenerate(LockDirectory.of(request), host,
                    new NpmRegistryClient(r -> {
                        sent.incrementAndGet();
                        throw new IllegalStateException("synthetic sender boundary");
                    }));
        } catch (NodeRegistryException e) {
            result = Result.failure(LockOnlyInstall.failure(e));
        }
        assertThat(sent).hasValue(1);
        assertThat(result.getErrorMessage()).doesNotContain("network policy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"yarn1", "yarn4", "bun"})
    void anUnboundManagerPolicyCannotSendDirectRequests(String manager) {
        AtomicInteger sent = new AtomicInteger();
        String registry = "http://registry.invalid/";
        String proxy = "http://synthetic-user:synthetic-password@127.0.0.1:1";
        Request.RequestBuilder request = Request.builder()
                .packageJson("{\"name\":\"proxy-control\",\"version\":\"1.0.0\",\"dependencies\":{\"@proxy-control/synthetic\":\"1.0.0\"}}")
                .packageJsonPath(Paths.get("package.json"));
        if ("yarn1".equals(manager)) {
            request.lock("# yarn lockfile v1\n")
                    .packageManagerFile(Paths.get(".npmrc"), "registry=" + registry + "\n")
                    .packageManagerFile(Paths.get(".yarnrc"), "proxy \"" + proxy + "\"\n");
        } else if ("yarn4".equals(manager)) {
            request.lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n")
                    .packageManagerFile(Paths.get(".yarnrc.yml"), "npmRegistryServer: " + registry + "\nhttpProxy: " + proxy + "\nunsafeHttpWhitelist: [registry.invalid]\n");
        } else {
            request.lock("{\"lockfileVersion\":1,\"workspaces\":{\"\":{\"name\":\"proxy-control\"}},\"packages\":{}}")
                    .packageManagerFile(Paths.get(".npmrc"), "registry=" + registry + "\n");
        }
        Environment environment = new Environment() {
            public String getenv(String name) { return "bun".equals(manager) && "HTTP_PROXY".equals(name) ? proxy : null; }
            public Path userHome() { return Paths.get("/nonexistent-registry-proxy-test"); }
            public String osName() { return "Linux"; }
        };
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(environment)
                .packageManager("yarn4".equals(manager) ? PackageManager.YarnBerry : "yarn1".equals(manager) ? PackageManager.YarnClassic : PackageManager.Bun)
                .packageManagerVersion("yarn4".equals(manager) ? "4.17.0" : "yarn1".equals(manager) ? "1.22.22" : "1.3.14").build();
        Result result = ("yarn1".equals(manager) ? YarnClassicInputs.PORT : "yarn4".equals(manager) ? YarnBerryInputs.PORT : BunInputs.PORT).regenerate(
                LockDirectory.of(request.build()), host, new NpmRegistryClient(r -> {
                    sent.incrementAndGet();
                    throw new IllegalStateException("a direct request escaped the configured proxy policy");
                }));
        assertThat(sent).hasValue(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).contains("network policy").doesNotContain("synthetic-password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafile", "strict-ssl"})
    void trustedHostTlsSettingsReachTheExistingRegistryGuard(String setting) {
        HostInputs host = HostInputs.builder().now(Instant.EPOCH)
                .npmrc(new Npmrc(NpmrcScope.User, Map.of("registry", "https://registry.invalid/", setting,
                        "cafile".equals(setting) ? "/nonexistent-synthetic-ca.pem" : "false"))).build();
        var source = new NpmRegistryPackuments(host.transport(), new NpmRegistryClient(new HttpUrlConnectionSender()))
                .withOptions(NpmOptions.fromNpmrc(NpmReleases.installedAs("11.19.1"),
                        host.userNpmrcWithNetwork(HostInputs.GLOBAL_AND_USER), null));
        assertThatThrownBy(() -> source.packument("https://registry.invalid/", "synthetic", false))
                .isInstanceOf(NodeRegistryException.class).hasMessageContaining("TLS-capable HttpSender");
        // A workspace-selected destination may be absent from the discovered .npmrc registry URLs.
        assertThatThrownBy(() -> source.packument("https://workspace-registry.invalid/", "synthetic", false))
                .isInstanceOfAny(CannotKnow.class, NodeRegistryException.class).hasMessageContaining("TLS-capable HttpSender");
    }

    @Test
    void hostNetworkProjectionKeepsManagerKeysAndTrustedSources() {
        HostInputs host = HostInputs.builder().now(Instant.EPOCH)
                .npmrc(new Npmrc(NpmrcScope.User, Map.of("proxy", "http://localhost:1234", "no-proxy", "pnpm.test",
                        "noproxy", "npm.test", "http-proxy", "http://localhost:2345")))
                .npmrc(new Npmrc(NpmrcScope.Project, Map.of("https-proxy", "${HOST_SECRET}", "http-proxy", "http://localhost:3456")))
                .build();
        String npm = host.userNpmrcWithNetwork(HostInputs.GLOBAL_AND_USER);
        String pnpm = host.userPnpmrc(HostInputs.USER, false);
        assertThat(npm).contains("noproxy=", "npm.test").doesNotContain("pnpm.test", "http-proxy=", "HOST_SECRET", "3456");
        assertThat(pnpm).contains("no-proxy=", "pnpm.test", "http-proxy=", "2345").doesNotContain("noproxy=", "HOST_SECRET", "3456");
    }

    @ParameterizedTest
    @ValueSource(strings = {"npm", "pnpm"})
    void aRefusedConfiguredProxyReturnsNoLockOrManifestPatch(String manager) throws Exception {
        int port;
        try (ServerSocket reserved = new ServerSocket(0)) {
            port = reserved.getLocalPort();
        }
        boolean npm = "npm".equals(manager);
        Request request = Request.builder()
                .packageJson("{\"name\":\"proxy-control\",\"version\":\"1.0.0\",\"dependencies\":{\"@proxy-control/synthetic\":\"1.0.0\"}}")
                .packageJsonPath(Paths.get("package.json"))
                .packageManagerFile(Paths.get(".npmrc"), "registry=https://registry.invalid/\nhttps-proxy=http://127.0.0.1:" + port + "\nnoproxy=\n")
                .packageManagerFile(Paths.get("pnpm-workspace.yaml"), "packages:\n  - .\nminimumReleaseAge: 0\n")
                .lock(npm ? "{\"name\":\"proxy-control\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"packages\":{\"\":{\"name\":\"proxy-control\",\"version\":\"1.0.0\"}}}" :
                        "lockfileVersion: '9.0'\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\nimporters:\n  .: {}\n").build();
        Environment environment = new Environment() {
            public String getenv(String name) { return null; }
            public Path userHome() { return Paths.get("/nonexistent-registry-proxy-test"); }
            public String osName() { return "Linux"; }
        };
        HostInputs host = HostInputs.builder().now(Instant.EPOCH).system(environment)
                .packageManager(npm ? PackageManager.Npm : PackageManager.Pnpm)
                .packageManagerVersion(npm ? "11.19.1" : "11.10.0").build();
        Result result;
        try {
            result = (npm ? NpmInputs.PORT : PnpmInputs.PORT).regenerate(LockDirectory.of(request), host,
                    new NpmRegistryClient(new HttpUrlConnectionSender()));
        } catch (NodeRegistryException e) {
            result = Result.failure(LockOnlyInstall.failure(e));
        }
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getLockFileContent()).isNull();
        assertThat(result.getFiles()).isEmpty();
        assertThat(result.getErrorMessage()).contains("configured registry transport", "ConnectException");
    }

    @Test
    void configuredProxyReceivesTheRegistryRequest() throws Exception {
        HttpServer registry = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger direct = new AtomicInteger();
        AtomicInteger proxied = new AtomicInteger();
        registry.createContext("/", exchange -> {
            direct.incrementAndGet();
            byte[] body = "{\"route\":\"direct\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        proxy.createContext("/", exchange -> {
            proxied.incrementAndGet();
            byte[] body = "{\"route\":\"proxy\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        registry.start();
        proxy.start();
        try {
            String url = "http://127.0.0.1:" + registry.getAddress().getPort() + "/";
            NodeRegistry route = new NodeRegistry(null, url, null, null, null, null, false, null, true, false);
            NodeRegistries registries = new NodeRegistries(route, Collections.emptyMap(),
                    "http://127.0.0.1:" + proxy.getAddress().getPort(), null, "");
            NpmRegistryPackuments source = new NpmRegistryPackuments(registries,
                    new NpmRegistryClient(new HttpUrlConnectionSender()));
            assertThat(source.packument(url, "synthetic", false)).containsEntry("route", "proxy");
            // The install's config wins over the discovery snapshot, including a changed project .npmrc.
            var bound = source.withOptions(NpmOptions.fromNpmrc(NpmReleases.installedAs("11.19.1"),
                    "proxy=http://127.0.0.1:1", "proxy=http://127.0.0.1:" + proxy.getAddress().getPort() + "\nnoproxy="));
            assertThat(bound.packument(url, "synthetic", false)).containsEntry("route", "proxy");
            assertThat(proxied).hasValue(2);
            assertThat(direct).hasValue(0);
        } finally {
            registry.stop(0);
            proxy.stop(0);
        }
    }
}
