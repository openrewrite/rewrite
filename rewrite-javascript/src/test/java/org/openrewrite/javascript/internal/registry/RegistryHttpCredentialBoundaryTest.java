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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeRegistry;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryHttpCredentialBoundaryTest {
    @ParameterizedTest
    @CsvSource({"token,packument", "token,manifest", "token,document", "token,tarball",
            "base64,packument", "base64,manifest", "base64,document", "base64,tarball",
            "username,packument", "username,manifest", "username,document", "username,tarball"})
    void httpCredentialsCannotBecomeAnonymousRequests(String kind, String operation) {
        List<HttpSender.Request> sent = new ArrayList<>();
        NpmRegistryClient client = client(sent);
        NodeRegistry registry = registry("http://registry.test/", kind);
        assertThatThrownBy(() -> {
            switch (operation) {
                case "manifest": client.getManifestText(registry, "synthetic", "1.0.0"); break;
                case "document": client.getDocumentText(registry, "synthetic", "1.0.0", registry.getUrl() + "attestation"); break;
                case "tarball": client.getTarball(registry, "synthetic", "1.0.0", registry.getUrl() + "package.tgz"); break;
                default: client.getPackumentText(registry, "synthetic", false);
            }
        }).isInstanceOf(NodeRegistryException.class).hasMessageContaining("credential")
                .hasMessageNotContaining("synthetic-secret");
        assertThat(sent).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"tarball,http://registry.test/package.tgz", "document,http://registry.test/attestation",
            "tarball,https://other.test/package.tgz", "document,https://other.test/attestation",
            "tarball,https://registry.test:444/package.tgz", "document,https://registry.test:444/attestation"})
    void registryCredentialsCannotEscapeThroughDocumentOrTarballTargets(String operation, String target) {
        List<HttpSender.Request> sent = new ArrayList<>();
        NpmRegistryClient client = client(sent);
        NodeRegistry registry = registry("https://registry.test/", "token");
        assertThatThrownBy(() -> {
            if ("tarball".equals(operation)) client.getTarball(registry, "synthetic", "1.0.0", target);
            else client.getDocumentText(registry, "synthetic", "1.0.0", target);
        }).isInstanceOf(NodeRegistryException.class).hasMessageContaining("credential")
                .hasMessageNotContaining("synthetic-secret");
        assertThat(sent).isEmpty();
    }

    @Test
    void anonymousHttpAndEquivalentHttpsAuthoritiesRemainAvailable() {
        List<HttpSender.Request> sent = new ArrayList<>();
        NpmRegistryClient client = client(sent);
        assertThat(client.getPackumentText(registry("http://registry.test/", "none"), "synthetic", false)).isEqualTo("{}");
        assertThat(sent.get(0).getRequestHeaders()).doesNotContainKey("Authorization");
        client.getTarball(registry("https://registry.test/base/", "token"), "synthetic", "1.0.0",
                "https://REGISTRY.TEST:443/package.tgz");
        assertThat(sent.get(1).getRequestHeaders()).containsEntry("Authorization", "Bearer synthetic-secret");
    }

    private static NpmRegistryClient client(List<HttpSender.Request> sent) {
        return new NpmRegistryClient(request -> {
            sent.add(request);
            return new HttpSender.Response(200, new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), () -> {});
        });
    }

    private static NodeRegistry registry(String url, String kind) {
        return new NodeRegistry(null, url, "token".equals(kind) ? "synthetic-secret" : null,
                "username".equals(kind) ? "synthetic-user" : null,
                "username".equals(kind) ? "synthetic-secret" : null,
                "base64".equals(kind) ? "c3ludGhldGljOnNlY3JldA==" : null, false, null, true, false);
    }
}
