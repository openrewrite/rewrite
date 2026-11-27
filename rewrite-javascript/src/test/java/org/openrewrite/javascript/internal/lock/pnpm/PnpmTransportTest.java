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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ipc.http.HttpSender;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PnpmTransportTest {
    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.5.3", "11.10.0"})
    void projectEnvironmentProxyDoesNotOverrideTheTrustedUserProxy(String version) {
        PnpmOptions options = PnpmOptions.load(null, "https-proxy=${UNTRUSTED_PROXY}\n",
                "https-proxy=http://127.0.0.1:2345\n", null,
                Map.of("UNTRUSTED_PROXY", "http://127.0.0.1:1234"), Objects.requireNonNull(PnpmReleases.runs(version)));
        assertThat(port(options)).isEqualTo(2345);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.5.3", "11.10.0"})
    void literalProjectProxyIsApplied(String version) {
        PnpmOptions options = PnpmOptions.load(null, "https-proxy=http://127.0.0.1:1234\n",
                "https-proxy=http://127.0.0.1:2345\n", null, Map.of(), Objects.requireNonNull(PnpmReleases.runs(version)));
        assertThat(port(options)).isEqualTo(1234);
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.5.3", "11.10.0"})
    void packageManagerBootstrapDoesNotUseTheProjectProxy(String version) {
        PnpmRelease release = Objects.requireNonNull(PnpmReleases.runs(version));
        PnpmOptions options = PnpmOptions.load(null, "https-proxy=http://127.0.0.1:1234\n",
                "https-proxy=http://127.0.0.1:2345\n", null, Map.of(), release);
        assertThat(port(options.packageManagerBootstrap(release))).isEqualTo(2345);
    }

    private static int port(PnpmOptions options) {
        class Recorder implements HttpSender {
            Proxy route;
            public HttpSender withProxy(Proxy proxy) { route = proxy; return this; }
            public Response send(Request request) { throw new AssertionError("selection only"); }
        }
        Recorder sender = new Recorder();
        options.transport().sender(sender, "https://registry.invalid/");
        return ((InetSocketAddress) sender.route.address()).getPort();
    }
}
