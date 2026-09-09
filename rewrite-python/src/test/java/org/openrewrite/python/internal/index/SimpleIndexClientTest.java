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
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.python.PythonPackageIndex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleIndexClientTest {

    MockWebServer server;
    SimpleIndexClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new SimpleIndexClient(new HttpUrlConnectionSender());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private PythonPackageIndex index() {
        return new PythonPackageIndex("test", server.url("/simple").toString(), true, null, null, false);
    }

    private static String resource(String name) throws IOException {
        try (InputStream is = SimpleIndexClientTest.class.getResourceAsStream(name)) {
            assertThat(is).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void pep691Json() throws Exception {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody(resource("/index/pep691-listing-json")));

        PackageListing listing = client.listFiles(index(), "requests");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/simple/requests/");
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.pypi.simple.v1+json, text/html;q=0.1");

        assertThat(listing.getPackageName()).isEqualTo("requests");
        List<PackageFile> files = listing.getFiles();
        assertThat(files).hasSize(3);

        PackageFile wheel = files.get(0);
        assertThat(wheel.getFilename()).isEqualTo("requests-2.31.0-py3-none-any.whl");
        assertThat(wheel.getUrl()).isEqualTo(server.url("/simple/requests/requests-2.31.0-py3-none-any.whl").toString());
        assertThat(wheel.getSha256()).isEqualTo("58cd2187c01e70e6e26505bca751777aa9f2ee0b7f4300988b709f44e013003f");
        assertThat(wheel.getRequiresPython()).isEqualTo(">=3.7");
        // core-metadata given as a hash dict means available
        assertThat(wheel.getCoreMetadataAvailable()).isTrue();
        assertThat(wheel.isYanked()).isFalse();

        PackageFile sdist = files.get(1);
        // relative url resolved against the listing page
        assertThat(sdist.getUrl()).isEqualTo(server.url("/packages/requests-2.31.0.tar.gz").toString());
        // legacy dist-info-metadata key (PEP 714)
        assertThat(sdist.getCoreMetadataAvailable()).isTrue();
        // string yanked reason means yanked
        assertThat(sdist.isYanked()).isTrue();

        PackageFile old = files.get(2);
        assertThat(old.getUrl()).isEqualTo("https://files.example.com/packages/requests-2.30.0-py3-none-any.whl");
        assertThat(old.getSha256()).isNull();
        assertThat(old.getRequiresPython()).isNull();
        assertThat(old.getCoreMetadataAvailable()).isFalse();
        assertThat(old.isYanked()).isFalse();
    }

    @Test
    void pep503HtmlWhenServerIgnoresAcceptHeader() throws Exception {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "text/html")
          .setBody(resource("/index/pep503-listing.html")));

        PackageListing listing = client.listFiles(index(), "requests");
        List<PackageFile> files = listing.getFiles();
        assertThat(files).hasSize(4);

        PackageFile wheel = files.get(0);
        assertThat(wheel.getFilename()).isEqualTo("requests-2.31.0-py3-none-any.whl");
        assertThat(wheel.getUrl()).isEqualTo(server.url("/simple/requests/requests-2.31.0-py3-none-any.whl").toString());
        assertThat(wheel.getSha256()).isEqualTo("58cd2187c01e70e6e26505bca751777aa9f2ee0b7f4300988b709f44e013003f");
        // &gt;=3.7 entity-unescaped
        assertThat(wheel.getRequiresPython()).isEqualTo(">=3.7");
        assertThat(wheel.getCoreMetadataAvailable()).isTrue();
        assertThat(wheel.isYanked()).isFalse();

        PackageFile sdist = files.get(1);
        assertThat(sdist.getUrl()).isEqualTo(server.url("/packages/requests-2.31.0.tar.gz").toString());
        assertThat(sdist.getSha256()).isEqualTo("942c5a758f98d790eaed1a29cb6eefc7ffb0d1cf7af05c3d2791656dbd6ad1e1");
        assertThat(sdist.getCoreMetadataAvailable()).isTrue();
        assertThat(sdist.isYanked()).isTrue();

        // bare data-yanked attribute
        PackageFile bareYanked = files.get(2);
        assertThat(bareYanked.getUrl()).isEqualTo("https://files.example.com/packages/requests-2.30.0-py3-none-any.whl");
        assertThat(bareYanked.isYanked()).isTrue();
        assertThat(bareYanked.getSha256()).isNull();

        PackageFile noMetadata = files.get(3);
        assertThat(noMetadata.getCoreMetadataAvailable()).isFalse();
        assertThat(noMetadata.isYanked()).isFalse();
    }

    @Test
    void htmlEntitiesUnescapedInRequiresPython() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "text/html")
          .setBody("""
            <html><body>
            <a href="/packages/foo-1.0-py3-none-any.whl" data-requires-python="&gt;=3.8,&#33;=3.9.*,&#x3C;4">foo-1.0-py3-none-any.whl</a>
            </body></html>
            """));

        PackageListing listing = client.listFiles(index(), "foo");

        // named, decimal, and hex entities
        assertThat(listing.getFiles().get(0).getRequiresPython()).isEqualTo(">=3.8,!=3.9.*,<4");
    }

    @Test
    void legacyDistInfoMetadataHonoredInJson() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody("""
            {"files": [
              {"filename": "foo-1.0-py3-none-any.whl", "url": "foo-1.0-py3-none-any.whl", "hashes": {}, "dist-info-metadata": true},
              {"filename": "foo-1.1-py3-none-any.whl", "url": "foo-1.1-py3-none-any.whl", "hashes": {}, "dist-info-metadata": {"sha256": "beef"}},
              {"filename": "foo-1.2-py3-none-any.whl", "url": "foo-1.2-py3-none-any.whl", "hashes": {}, "dist-info-metadata": false}
            ]}
            """));

        PackageListing listing = client.listFiles(index(), "foo");

        // PEP 714: the legacy key is honored when core-metadata is absent
        assertThat(listing.getFiles().get(0).getCoreMetadataAvailable()).isTrue();
        assertThat(listing.getFiles().get(1).getCoreMetadataAvailable()).isTrue();
        assertThat(listing.getFiles().get(2).getCoreMetadataAvailable()).isFalse();
    }

    @Test
    void legacyDistInfoMetadataHonoredInHtml() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "text/html")
          .setBody("""
            <html><body>
            <a href="/packages/foo-1.0-py3-none-any.whl" data-dist-info-metadata="sha256=beef">foo-1.0-py3-none-any.whl</a>
            <a href="/packages/foo-1.1-py3-none-any.whl" data-dist-info-metadata="false">foo-1.1-py3-none-any.whl</a>
            </body></html>
            """));

        PackageListing listing = client.listFiles(index(), "foo");

        assertThat(listing.getFiles().get(0).getCoreMetadataAvailable()).isTrue();
        assertThat(listing.getFiles().get(1).getCoreMetadataAvailable()).isFalse();
    }

    @Test
    void basicAuthHeaderSent() throws Exception {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody("{\"files\": []}"));

        PythonPackageIndex authed = new PythonPackageIndex(
          "test", server.url("/simple").toString(), true, "user", "secret", false);
        client.listFiles(authed, "requests");

        RecordedRequest request = server.takeRequest();
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(request.getHeader("Authorization")).isEqualTo(expected);
    }

    @Test
    void unauthorizedMapsToAuthFailed() {
        server.enqueue(new MockResponse().setResponseCode(401));
        assertThatThrownBy(() -> client.listFiles(index(), "requests"))
          .isInstanceOfSatisfying(PythonIndexException.class, e -> {
              assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.AUTH_FAILED);
              assertThat(e.getIndexUrl()).isEqualTo(index().getUrl());
          });
    }

    @Test
    void forbiddenMapsToAuthFailed() {
        server.enqueue(new MockResponse().setResponseCode(403));
        assertThatThrownBy(() -> client.listFiles(index(), "requests"))
          .isInstanceOfSatisfying(PythonIndexException.class,
            e -> assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.AUTH_FAILED));
    }

    @Test
    void notFoundMapsToNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThatThrownBy(() -> client.listFiles(index(), "no-such-package"))
          .isInstanceOfSatisfying(PythonIndexException.class,
            e -> assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.NOT_FOUND));
    }

    @Test
    void connectionRefusedMapsToUnreachable() throws IOException {
        String url = server.url("/simple").toString();
        server.shutdown();
        PythonPackageIndex dead = new PythonPackageIndex("test", url, true, null, null, false);
        assertThatThrownBy(() -> client.listFiles(dead, "requests"))
          .isInstanceOfSatisfying(PythonIndexException.class, e -> {
              assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.UNREACHABLE);
              assertThat(e.getIndexUrl()).isEqualTo(url);
          });
    }

    @Test
    void unresolvedPlaceholderMapsToUnreachable() {
        PythonPackageIndex unresolved = new PythonPackageIndex(
          "corp", "https://${UNSET}@corp.example.com/simple", true, null, null, true);
        assertThatThrownBy(() -> client.listFiles(unresolved, "requests"))
          .isInstanceOfSatisfying(PythonIndexException.class,
            e -> assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.UNREACHABLE));
    }

    @Test
    void listingsAreCachedPerRun() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody("{\"files\": []}"));

        PackageListing first = client.listFiles(index(), "requests");
        PackageListing second = client.listFiles(index(), "requests");

        assertThat(second).isSameAs(first);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void canonicalName() {
        assertThat(SimpleIndexClient.canonicalName("Django_REST--framework.utils")).isEqualTo("django-rest-framework-utils");
    }

    @Test
    void pep700SizeAndUploadTimeFromJson() {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody("""
            {"files": [
              {"filename": "foo-1.0-py3-none-any.whl", "url": "foo-1.0-py3-none-any.whl", "hashes": {},
               "size": 62552, "upload-time": "2023-05-22T15:12:44.123456Z"},
              {"filename": "foo-0.9-py3-none-any.whl", "url": "foo-0.9-py3-none-any.whl", "hashes": {}}
            ]}
            """));

        PackageListing listing = client.listFiles(index(), "foo");

        PackageFile withPep700 = listing.getFiles().get(0);
        assertThat(withPep700.getSize()).isEqualTo(62552L);
        assertThat(withPep700.getUploadTime()).isEqualTo("2023-05-22T15:12:44.123456Z");
        // pre-PEP 700 responses simply omit the fields
        assertThat(listing.getFiles().get(1).getSize()).isNull();
        assertThat(listing.getFiles().get(1).getUploadTime()).isNull();
    }

    @Test
    void pep700FieldsNullOnHtmlListings() throws Exception {
        server.enqueue(new MockResponse()
          .setHeader("Content-Type", "text/html")
          .setBody(resource("/index/pep503-listing.html")));

        PackageListing listing = client.listFiles(index(), "requests");

        for (PackageFile file : listing.getFiles()) {
            assertThat(file.getSize()).isNull();
            assertThat(file.getUploadTime()).isNull();
        }
    }
}
