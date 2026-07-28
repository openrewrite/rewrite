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

import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistryException.Reason;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NpmRegistryClientTest {

    private static NodeRegistry registry() {
        return new NodeRegistry(null, "http://registry.test/", null, null, null, null, false, null, true, false);
    }

    @Test
    void packumentSendsInstallV1Accept() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"dist-tags\":{\"latest\":\"4.17.21\"}," +
                "\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        var client = new NpmRegistryClient(sender);

        AbbreviatedPackument packument = client.getPackument(registry(), "lodash");

        assertThat(sender.last().getUrl().getPath()).isEqualTo("/lodash");
        assertThat(sender.last().getRequestHeaders()).containsEntry("Accept", "application/vnd.npm.install-v1+json");
        assertThat(packument.getVersions()).containsExactly("4.17.20", "4.17.21");
        assertThat(packument.getDistTags()).containsEntry("latest", "4.17.21");
    }

    @Test
    void scopedNameEncodesSlashAndReadsLicenseFromFullManifest() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"name\":\"@angular/core\",\"version\":\"17.3.0\",\"license\":\"MIT\"," +
                "\"dist\":{\"tarball\":\"https://r/t.tgz\",\"shasum\":\"abc\",\"integrity\":\"sha512-x\"}}");
        var client = new NpmRegistryClient(sender);

        VersionManifest manifest = client.getManifest(registry(), "@angular/core", "17.3.0");

        assertThat(sender.last().getUrl().getPath()).isEqualTo("/@angular%2Fcore/17.3.0");
        assertThat(sender.last().getRequestHeaders()).containsEntry("Accept", "application/json");
        assertThat(manifest.getLicenseString()).isEqualTo("MIT");
        assertThat(manifest.getDist().getIntegrity()).isEqualTo("sha512-x");
        assertThat(manifest.getDist().getTarball()).isEqualTo("https://r/t.tgz");
    }

    @Test
    void manifestParsesLayoutSurfaces() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"name\":\"esbuild\",\"version\":\"0.20.0\"," +
                "\"dependencies\":{\"a\":\"^1.0.0\"},\"optionalDependencies\":{\"b\":\"^2.0.0\"}," +
                "\"peerDependencies\":{\"react\":\">=17\"},\"peerDependenciesMeta\":{\"react\":{\"optional\":true}}," +
                "\"os\":[\"linux\"],\"cpu\":[\"x64\"],\"engines\":{\"node\":\">=18\"}," +
                "\"scripts\":{\"postinstall\":\"node install.js\"}," +
                "\"bundledDependencies\":[\"a\"]," +
                "\"dist\":{\"tarball\":\"t\",\"shasum\":\"s\",\"integrity\":\"sha512-y\"}}");
        var client = new NpmRegistryClient(sender);

        VersionManifest manifest = client.getManifest(registry(), "esbuild", "0.20.0");

        assertThat(manifest.getDependencies()).containsEntry("a", "^1.0.0");
        assertThat(manifest.getOptionalDependencies()).containsEntry("b", "^2.0.0");
        assertThat(manifest.getPeerDependencies()).containsEntry("react", ">=17");
        assertThat(manifest.getPeerDependenciesMeta().get("react").getOptional()).isTrue();
        assertThat(manifest.getOs()).containsExactly("linux");
        assertThat(manifest.getCpu()).containsExactly("x64");
        assertThat(manifest.getEngines()).containsEntry("node", ">=18");
        assertThat(manifest.getBundleDependencies()).containsExactly("a");
        // hasInstallScript absent -> derived from an install script in `scripts`
        assertThat(manifest.getHasInstallScript()).isTrue();
    }

    @Test
    void bearerTokenHeader() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        var client = new NpmRegistryClient(sender);
        var authed = new NodeRegistry(null, "https://registry.test/", "tok123", null, null, null,
                true, null, true, false);

        client.getPackument(authed, "lodash");

        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", "Bearer tok123");
    }

    @Test
    void basicAuthHeader() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        var client = new NpmRegistryClient(sender);
        var authed = new NodeRegistry(null, "https://registry.test/", null, "user", "secret", null,
                false, null, true, false);

        client.getPackument(authed, "lodash");

        var expected = "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", expected);
    }

    @Test
    void preEncodedAuthPassedThroughVerbatim() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        var client = new NpmRegistryClient(sender);
        var authed = new NodeRegistry(null, "https://registry.test/", null, null, null, "dXNlcjpwYXNz",
                false, null, true, false);

        client.getPackument(authed, "lodash");

        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", "Basic dXNlcjpwYXNz");
    }

    @Test
    void authNotSentOverHttp() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        var client = new NpmRegistryClient(sender);
        var authed = new NodeRegistry(null, "http://registry.test/", "tok123", null, null, null,
                true, null, true, false);

        client.getPackument(authed, "lodash");

        assertThat(sender.last().getRequestHeaders()).doesNotContainKey("Authorization");
    }

    @Test
    void urlUserinfoNeverLeaksIntoFailureDetail() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        var client = new NpmRegistryClient(sender);
        var authed = new NodeRegistry(null, "https://user:s3cr3ttoken@registry.test/", null, null, null, null,
                false, null, true, false);

        assertThatThrownBy(() -> client.getPackument(authed, "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain("s3cr3ttoken");
                    assertThat(e.getRegistryUrl()).doesNotContain("s3cr3ttoken");
                });
    }

    @Test
    void packument404IsPackageNotFound() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackument(registry(), "nope"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND));
    }

    @Test
    void manifest404IsVersionNotFound() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getManifest(registry(), "lodash", "9.9.9"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.VERSION_NOT_FOUND));
    }

    @Test
    void unauthorizedIsAuthFailed() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(401);
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackument(registry(), "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
    }

    @Test
    void forbiddenIsAuthFailed() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(403);
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackument(registry(), "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
    }

    @Test
    void serverErrorIsUnreachable() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(500);
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackument(registry(), "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE));
    }

    @Test
    void connectionFailureIsUnreachable() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueConnectionFailure();
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackument(registry(), "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE);
                    assertThat(e.getRegistryUrl()).isEqualTo("http://registry.test/");
                });
    }

    @Test
    void unresolvedPlaceholderRefusesBeforeAnyRequest() {
        StubHttpSender sender = new StubHttpSender();
        var client = new NpmRegistryClient(sender);
        var unresolved = new NodeRegistry(null, "http://registry.test/", null, null, null, null,
                false, null, true, true);

        assertThatThrownBy(() -> client.getPackument(unresolved, "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE));
        assertThat(sender.sendCount).isZero();
    }

    @Test
    void malformedManifestIsMalformed() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "not json {");
        var client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getManifest(registry(), "lodash", "4.17.21"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.MALFORMED_MANIFEST));
    }

    @Test
    void manifestCachedPerRun() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"name\":\"lodash\",\"version\":\"4.17.21\"," +
                "\"dist\":{\"tarball\":\"t\",\"shasum\":\"s\",\"integrity\":\"i\"}}");
        var client = new NpmRegistryClient(sender);

        VersionManifest first = client.getManifest(registry(), "lodash", "4.17.21");
        VersionManifest second = client.getManifest(registry(), "lodash", "4.17.21");

        assertThat(second).isSameAs(first);
        assertThat(sender.sendCount).isEqualTo(1);
    }

    @Test
    void cafileWithDefaultSenderFailsLoud() {
        var client = new NpmRegistryClient(new HttpUrlConnectionSender());
        var withCa = new NodeRegistry(null, "https://corp.example.com/", null, null, null, null,
                false, "/etc/certs/corp.pem", true, false);

        assertThatThrownBy(() -> client.getPackument(withCa, "lodash"))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE);
                    assertThat(e.getMessage()).contains("custom CA");
                });
    }

    static final class StubHttpSender implements HttpSender {
        final List<Request> requests = new ArrayList<>();
        final Deque<Supplier<Response>> responses = new ArrayDeque<>();
        int sendCount;

        void enqueueJson(int code, String body) {
            responses.add(() -> new Response(code,
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                    singletonMap("Content-Type", singletonList("application/json")),
                    () -> {
                    }));
        }

        void enqueueStatus(int code) {
            responses.add(() -> new Response(code, null, () -> {
            }));
        }

        void enqueueConnectionFailure() {
            responses.add(() -> {
                throw new UncheckedIOException(new IOException("Connection refused"));
            });
        }

        Request last() {
            return requests.get(requests.size() - 1);
        }

        @Override
        public Response send(Request request) {
            requests.add(request);
            sendCount++;
            Supplier<Response> next = responses.poll();
            if (next == null) {
                throw new IllegalStateException("No response enqueued");
            }
            return next.get();
        }
    }
}
