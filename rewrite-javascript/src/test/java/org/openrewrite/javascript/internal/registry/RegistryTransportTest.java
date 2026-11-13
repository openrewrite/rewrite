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
package org.openrewrite.javascript.internal.registry;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryTransportTest {
    private static NodeRegistry registry(String url) {
        return new NodeRegistry(null, url, null, null, null, null, false, null, true, false);
    }

    @Test
    void httpsUsesConnectAndDoesNotFallBackToTheRegistry() throws Exception {
        try (ServerSocket proxy = new ServerSocket(0)) {
            CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
                try (var socket = proxy.accept()) {
                    socket.setSoTimeout(5000);
                    String first = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
                    socket.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    return first;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            NpmRegistryClient client = new NpmRegistryClient(new HttpUrlConnectionSender())
                    .withTransport(new RegistryTransport(null, "http://127.0.0.1:" + proxy.getLocalPort(), ""));
            assertThatThrownBy(() -> client.getPackumentText(registry("https://registry.invalid/"), "synthetic", false))
                    .isInstanceOf(NodeRegistryException.class).hasMessageContaining("configured registry transport");
            assertThat(request.get(10, TimeUnit.SECONDS)).isEqualTo("CONNECT registry.invalid:443 HTTP/1.1");
        }
    }

    @Test
    void anExplicitBypassUsesDirectAndADifferentTransportCannotReuseItsCache() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger direct = new AtomicInteger();
        server.createContext("/", exchange -> {
            direct.incrementAndGet();
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        int refused;
        try (ServerSocket port = new ServerSocket(0)) {
            refused = port.getLocalPort();
        }
        try {
            NodeRegistry registry = registry("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            NpmRegistryClient base = new NpmRegistryClient(new HttpUrlConnectionSender(Duration.ofSeconds(1), Duration.ofSeconds(1)));
            String proxy = "http://127.0.0.1:" + refused;
            NpmRegistryClient bypass = base.withTransport(new RegistryTransport(proxy, proxy, ".127.0.0.1"));
            assertThat(bypass.getPackumentText(registry, "synthetic", false)).isEqualTo("{}");
            assertThat(bypass.getPackumentText(registry, "synthetic", false)).isEqualTo("{}");
            assertThat(direct).hasValue(1);
            assertThatThrownBy(() -> base.withTransport(new RegistryTransport(proxy, proxy, ""))
                    .getPackumentText(registry, "synthetic", false))
                    .isInstanceOf(NodeRegistryException.class).hasMessageContaining("configured registry transport");
            assertThat(direct).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unsupportedCapabilitiesDoNotReachAnInjectedSenderOrExposeSecrets() {
        AtomicInteger sent = new AtomicInteger();
        HttpSender sender = request -> {
            sent.incrementAndGet();
            throw new AssertionError("must not send");
        };
        for (String proxy : new String[]{"http://user:synthetic-secret@localhost:1234", "https://localhost:1234", "socks5://localhost:1234", "http://[invalid"}) {
            assertThatThrownBy(() -> new NpmRegistryClient(sender).withTransport(new RegistryTransport(proxy, proxy, ""))
                    .getPackumentText(registry("https://registry.invalid/"), "synthetic", false))
                    .isInstanceOf(CannotKnow.class).hasMessageNotContaining("synthetic-secret");
        }
        assertThatThrownBy(() -> new RegistryTransport("http://localhost:1234", null, "").sender(sender, "http://registry.invalid/"))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("withProxy");
        assertThat(sent).hasValue(0);
        assertThat(RegistryTransport.DEFAULT.sender(sender, "https://registry.invalid/")).isSameAs(sender);
        assertThatThrownBy(() -> new RegistryTransport("http://localhost:1234", null, "registry.invalid")
                .sender(sender, "http://registry.invalid/"))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("withProxy");
    }

    @Test
    void npmAndPnpmUseTheirOwnProxyPrecedenceAndHostnameBypassRules() {
        ProxyRecorder sender = new ProxyRecorder();
        RegistryTransport npm = RegistryTransport.npm(Map.of("proxy", "http://localhost:1234", "https-proxy", "http://localhost:2345", "noproxy", ".example.test"), Map.of("HTTPS_PROXY", "http://localhost:3456"));
        npm.sender(sender, "https://registry.invalid/");
        assertThat(((InetSocketAddress) sender.route.address()).getPort()).isEqualTo(2345);
        npm.sender(sender, "http://registry.invalid/");
        assertThat(((InetSocketAddress) sender.route.address()).getPort()).isEqualTo(2345);
        npm.sender(sender, "https://example.test/");
        assertThat(sender.route).isEqualTo(Proxy.NO_PROXY);
        npm.sender(sender, "https://a.example.test/");
        assertThat(sender.route).isEqualTo(Proxy.NO_PROXY);
        npm.sender(sender, "https://notexample.test/");
        assertThat(sender.route).isNotEqualTo(Proxy.NO_PROXY);
        for (String noProxy : new String[]{"*", "example.test:443", "EXAMPLE.test"}) {
            RegistryTransport.npm(Map.of("proxy", "http://localhost:1234", "noproxy", noProxy), Map.of())
                    .sender(sender, "https://example.test/");
            assertThat(sender.route).isNotEqualTo(Proxy.NO_PROXY);
        }
        RegistryTransport pnpm = RegistryTransport.pnpm(Map.of("httpsProxy", "http://localhost:1234", "httpProxy", "http://localhost:2345"), Map.of());
        pnpm.sender(sender, "https://registry.invalid/");
        assertThat(((InetSocketAddress) sender.route.address()).getPort()).isEqualTo(1234);
        pnpm.sender(sender, "http://registry.invalid/");
        assertThat(((InetSocketAddress) sender.route.address()).getPort()).isEqualTo(2345);
        for (String key : new String[]{"ca", "cert", "key", "local-address"}) {
            assertThatThrownBy(() -> RegistryTransport.npm(Map.of(key, "strict-ssl".equals(key) ? false : "synthetic"), Map.of()))
                    .isInstanceOf(CannotKnow.class);
        }
    }

    @Test
    void theHostsProxyVariablesAreTheHostSendersWhileConfiguredRoutesMustStillBeBound() {
        Map<String, String> host = Map.of("HTTPS_PROXY", "http://127.0.0.1:3128", "HTTP_PROXY", "http://127.0.0.1:3128",
                "NO_PROXY", "localhost,127.0.0.1");
        HttpSender custom = request -> { throw new AssertionError("selection only"); };
        for (RegistryTransport transport : java.util.List.of(RegistryTransport.npm(Map.of(), host),
                RegistryTransport.pnpm(Map.of(), host), RegistryTransport.pnpmBootstrap(Map.of(), host),
                RegistryTransport.npm(Map.of(), Map.of("HTTPS_PROXY", "http://user:synthetic-secret@proxy.invalid:3128")))) {
            assertThat(transport.sender(custom, "https://registry.npmjs.org/")).isSameAs(custom);
            assertThat(transport.sender(custom, "http://registry.npmjs.org/")).isSameAs(custom);
        }
        // A host sender that can bind a route is still given the host's.
        ProxyRecorder recorder = new ProxyRecorder();
        RegistryTransport.npm(Map.of(), host).sender(recorder, "https://registry.npmjs.org/");
        assertThat(((InetSocketAddress) recorder.route.address()).getPort()).isEqualTo(3128);
        // The default URL sender binds the host's route as before.
        assertThat(RegistryTransport.npm(Map.of(), host).sender(new HttpUrlConnectionSender(), "https://registry.npmjs.org/"))
                .isInstanceOf(HttpUrlConnectionSender.class);
        // What a manager's configuration routes, a sender that cannot bind it still refuses.
        for (RegistryTransport configured : java.util.List.of(
                RegistryTransport.npm(Map.of("https-proxy", "http://127.0.0.1:3128"), host),
                RegistryTransport.npm(Map.of(), Map.of("npm_config_https_proxy", "http://127.0.0.1:3128")),
                RegistryTransport.npm(Map.of("noproxy", "example.test"), host),
                RegistryTransport.pnpm(Map.of("httpsProxy", "http://127.0.0.1:3128"), host),
                RegistryTransport.pnpm(Map.of("proxy", "http://127.0.0.1:3128"), host),
                RegistryTransport.pnpm(Map.of("noProxy", "example.test"), host),
                RegistryTransport.pnpmBootstrap(Map.of("https-proxy", "http://127.0.0.1:3128"), host))) {
            assertThatThrownBy(() -> configured.sender(custom, "https://registry.npmjs.org/"))
                    .isInstanceOf(CannotKnow.class).hasMessageContaining("withProxy");
        }
    }

    @Test
    void tlsOptionsPreserveTheExistingInjectedSenderContract() {
        HttpSender custom = request -> { throw new AssertionError("selection only"); };
        for (Map<String, Object> config : java.util.List.of(Map.<String, Object>of("cafile", "synthetic-ca.pem"),
                Map.<String, Object>of("strict-ssl", false))) {
            RegistryTransport transport = RegistryTransport.npm(config, Map.of());
            assertThatThrownBy(() -> transport.sender(new HttpUrlConnectionSender(), "https://registry.invalid/"))
                    .isInstanceOf(CannotKnow.class).hasMessageContaining("TLS-capable HttpSender");
            assertThat(transport.sender(custom, "https://registry.invalid/")).isSameAs(custom);
        }
    }

    @Test
    void redirectsCannotCarryAnExplicitBypassOutsideItsHost() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger target = new AtomicInteger();
        server.createContext("/synthetic", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + server.getAddress().getPort() + "/other");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/other", exchange -> {
            target.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            NpmRegistryClient client = new NpmRegistryClient(new HttpUrlConnectionSender())
                    .withTransport(new RegistryTransport("http://127.0.0.1:1", null, "127.0.0.1"));
            assertThatThrownBy(() -> client.getPackumentText(registry("http://127.0.0.1:" + server.getAddress().getPort()), "synthetic", false))
                    .isInstanceOf(NodeRegistryException.class).hasMessageContaining("302");
            assertThat(target).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    static class ProxyRecorder implements HttpSender {
        Proxy route;

        @Override
        public HttpSender withProxy(Proxy proxy) {
            route = proxy;
            return this;
        }

        @Override
        public Response send(Request request) {
            throw new AssertionError("selection only");
        }
    }
}
