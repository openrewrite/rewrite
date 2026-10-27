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
import java.util.Arrays;
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
    void packumentAcceptsInstallV1WithJsonFallback() {
        StubHttpSender sender = new StubHttpSender();
        String body = "{\"dist-tags\":{\"latest\":\"4.17.21\"},\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}";
        sender.enqueueJson(200, body);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        String packument = client.getPackumentText(registry(), "lodash", false);

        assertThat(sender.last().getUrl().getPath()).isEqualTo("/lodash");
        assertThat(sender.last().getRequestHeaders())
                .containsEntry("Accept", "application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8, */*");
        assertThat(packument).isEqualTo(body);
    }

    @Test
    void scopedNameEncodesSlashInTheManifestPath() {
        StubHttpSender sender = new StubHttpSender();
        String body = "{\"name\":\"@angular/core\",\"version\":\"17.3.0\",\"license\":\"MIT\"}";
        sender.enqueueJson(200, body);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        String manifest = client.getManifestText(registry(), "@angular/core", "17.3.0");

        assertThat(sender.last().getUrl().getPath()).isEqualTo("/@angular%2Fcore/17.3.0");
        assertThat(sender.last().getRequestHeaders()).containsEntry("Accept", "application/json");
        assertThat(manifest).isEqualTo(body);
    }

    @Test
    void bearerTokenHeader() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry authed = new NodeRegistry(null, "https://registry.test/", "tok123", null, null, null,
                true, null, true, false);

        client.getPackumentText(authed, "lodash", false);

        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", "Bearer tok123");
    }

    @Test
    void basicAuthHeader() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry authed = new NodeRegistry(null, "https://registry.test/", null, "user", "secret", null,
                false, null, true, false);

        client.getPackumentText(authed, "lodash", false);

        String expected = "Basic " +
                          Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", expected);
    }

    @Test
    void preEncodedAuthPassedThroughVerbatim() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry authed = new NodeRegistry(null, "https://registry.test/", null, null, null, "dXNlcjpwYXNz",
                false, null, true, false);

        client.getPackumentText(authed, "lodash", false);

        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", "Basic dXNlcjpwYXNz");
    }

    @Test
    void authNotSentOverHttp() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry authed = new NodeRegistry(null, "http://registry.test/", "tok123", null, null, null,
                true, null, true, false);

        client.getPackumentText(authed, "lodash", false);

        assertThat(sender.last().getRequestHeaders()).doesNotContainKey("Authorization");
    }

    @Test
    void urlUserinfoNeverLeaksIntoFailureDetail() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry authed = new NodeRegistry(null, "https://user:s3cr3ttoken@registry.test/", null, null, null, null,
                false, null, true, false);

        assertThatThrownBy(() -> client.getPackumentText(authed, "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain("s3cr3ttoken");
                    assertThat(e.getRegistryUrl()).doesNotContain("s3cr3ttoken");
                });
    }

    @Test
    void packument404IsPackageNotFound() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "nope", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND));
    }

    @Test
    void manifest404IsVersionNotFound() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(404);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getManifestText(registry(), "lodash", "9.9.9"))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.VERSION_NOT_FOUND));
    }

    @Test
    void unauthorizedIsAuthFailed() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(401);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
    }

    @Test
    void forbiddenIsAuthFailed() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(403);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
    }

    @Test
    void serverErrorIsUnreachable() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueStatus(500);
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE));
    }

    @Test
    void connectionFailureIsUnreachable() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueConnectionFailure();
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE);
                    assertThat(e.getRegistryUrl()).isEqualTo("http://registry.test/");
                });
    }

    @Test
    void unresolvedPlaceholderRefusesBeforeAnyRequest() {
        StubHttpSender sender = new StubHttpSender();
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry unresolved = new NodeRegistry(null, "http://registry.test/", null, null, null, null,
                false, null, true, true);

        assertThatThrownBy(() -> client.getPackumentText(unresolved, "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UNREACHABLE));
        assertThat(sender.sendCount).isZero();
    }

    @Test
    void unresolvedCredentialsAreSentAsWritten() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"versions\":{}}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry unresolved = new NodeRegistry(null, "https://registry.test/", "${NPM_TOKEN}", null, null, null,
                false, null, true, false).withUnresolvedCredentialPlaceholders(singletonList("${NPM_TOKEN}"));

        client.getPackumentText(unresolved, "lodash", false);

        assertThat(sender.last().getRequestHeaders()).containsEntry("Authorization", "Bearer ${NPM_TOKEN}");
    }

    @Test
    void rejectedRequestNamesUnresolvedPlaceholders() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(401, "{}");
        NpmRegistryClient client = new NpmRegistryClient(sender);
        NodeRegistry unresolved = registry()
                .withUnresolvedCredentialPlaceholders(Arrays.asList("${NPM_USER}", "${NPM_PASS}"));

        assertThatThrownBy(() -> client.getPackumentText(unresolved, "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED);
                    assertThat(e.getMessage())
                            .startsWith("HTTP 401 from http://registry.test/lodash")
                            .contains("reference ${NPM_USER}, ${NPM_PASS}, which are not set");
                });
    }

    @Test
    void rejectedRequestWithoutUnresolvedPlaceholdersHasNoHint() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(403, "{}");
        NpmRegistryClient client = new NpmRegistryClient(sender);

        assertThatThrownBy(() -> client.getPackumentText(registry(), "lodash", false))
                .isInstanceOfSatisfying(NodeRegistryException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("HTTP 403 from http://registry.test/lodash"));
    }

    @Test
    void manifestCachedPerRun() {
        StubHttpSender sender = new StubHttpSender();
        sender.enqueueJson(200, "{\"name\":\"lodash\",\"version\":\"4.17.21\"}");
        NpmRegistryClient client = new NpmRegistryClient(sender);

        String first = client.getManifestText(registry(), "lodash", "4.17.21");
        String second = client.getManifestText(registry(), "lodash", "4.17.21");

        assertThat(second).isSameAs(first);
        assertThat(sender.sendCount).isEqualTo(1);
    }

    @Test
    void cafileWithDefaultSenderFailsLoud() {
        NpmRegistryClient client = new NpmRegistryClient(new HttpUrlConnectionSender());
        NodeRegistry withCa = new NodeRegistry(null, "https://corp.example.com/", null, null, null, null,
                false, "/etc/certs/corp.pem", true, false);

        assertThatThrownBy(() -> client.getPackumentText(withCa, "lodash", false))
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
