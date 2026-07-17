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
package org.openrewrite.python.internal.index;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.python.PythonPackageIndex;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PypiJsonApiTest {

    MockWebServer server;
    HttpSender httpSender = new HttpUrlConnectionSender();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private String baseUrl() {
        String url = server.url("/").toString();
        return url.substring(0, url.length() - 1);
    }

    @Test
    void digests() throws InterruptedException {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("""
            {
              "info": {"name": "requests", "version": "2.31.0"},
              "urls": [
                {"digests": {"md5": "aa", "sha256": "58cd2187c01e70e6e26505bca751777aa9f2ee0b7f4300988b709f44e013003f"}},
                {"digests": {"sha256": "942c5a758f98d790eaed1a29cb6eefc7ffb0d1cf7af05c3d2791656dbd6ad1e1"}}
              ]
            }
            """));

        List<String> digests = PypiJsonApi.sha256Digests(httpSender, baseUrl(), "requests", "2.31.0");
        assertThat(server.takeRequest().getPath()).isEqualTo("/pypi/requests/2.31.0/json");
        assertThat(digests).containsExactly(
          "58cd2187c01e70e6e26505bca751777aa9f2ee0b7f4300988b709f44e013003f",
          "942c5a758f98d790eaed1a29cb6eefc7ffb0d1cf7af05c3d2791656dbd6ad1e1");
    }

    @Test
    void notFoundReturnsNull() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThat(PypiJsonApi.sha256Digests(httpSender, baseUrl(), "requests", "0.0.0")).isNull();
    }

    @Test
    void malformedBodyReturnsNull() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("not json"));
        assertThat(PypiJsonApi.sha256Digests(httpSender, baseUrl(), "requests", "2.31.0")).isNull();
    }

    @Test
    void connectionFailureReturnsNull() throws IOException {
        String base = baseUrl();
        server.shutdown();
        assertThat(PypiJsonApi.sha256Digests(httpSender, base, "requests", "2.31.0")).isNull();
    }

    @Test
    void isPypiOnlyForPypiHosts() {
        assertThat(PypiJsonApi.isPypi(index("https://pypi.org/simple"))).isTrue();
        assertThat(PypiJsonApi.isPypi(index("https://user:pass@pypi.org/simple"))).isTrue();
        assertThat(PypiJsonApi.isPypi(index("https://mirror.example.com/simple"))).isFalse();
        assertThat(PypiJsonApi.isPypi(index("https://notpypi.org/simple"))).isFalse();
    }

    private static PythonPackageIndex index(String url) {
        return new PythonPackageIndex("test", url, true, null, null, false);
    }
}
