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
package org.openrewrite.python.internal.metadata;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class Pep658MetadataFetcherTest {

    private final HttpSender http = new HttpUrlConnectionSender();
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void sidecarFound() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0
          Requires-Dist: bar (>=2.0)
          """));

        String fileUrl = server.url("/packages/foo-1.0-py3-none-any.whl").toString();
        CoreMetadata metadata = Pep658MetadataFetcher.fetch(http, fileUrl);

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("foo");
        assertThat(metadata.getRequiresDist()).containsExactly("bar (>=2.0)");
        assertThat(server.takeRequest().getPath()).isEqualTo("/packages/foo-1.0-py3-none-any.whl.metadata");
    }

    @Test
    void metadataSuffixInsertedBeforeQueryString() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0
          """));

        String fileUrl = server.url("/packages/foo-1.0-py3-none-any.whl").toString() + "?X-Sig=abc123";
        CoreMetadata metadata = Pep658MetadataFetcher.fetch(http, fileUrl);

        assertThat(metadata).isNotNull();
        assertThat(server.takeRequest().getPath())
          .isEqualTo("/packages/foo-1.0-py3-none-any.whl.metadata?X-Sig=abc123");
    }

    @Test
    void sidecarNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThat(Pep658MetadataFetcher.fetch(http, server.url("/packages/foo-1.0-py3-none-any.whl").toString()))
          .isNull();
    }

    @Test
    void connectionFailure() throws IOException {
        String fileUrl = server.url("/packages/foo-1.0-py3-none-any.whl").toString();
        server.shutdown();
        assertThat(Pep658MetadataFetcher.fetch(http, fileUrl)).isNull();
    }
}
