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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.python.PythonPackageIndex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlatIndexClientTest {

    @TempDir
    Path dir;

    private final FlatIndexClient client = new FlatIndexClient(new HttpUrlConnectionSender());

    private static PythonPackageIndex index(String location) {
        return new PythonPackageIndex("shared", location, true, null, null, false);
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void directoryListingParsesFilenamesAndHashesBytes() throws Exception {
        byte[] wheelBytes = "wheel-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] sdistBytes = "sdist-bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(dir.resolve("foo_pkg-1.0-py3-none-any.whl"), wheelBytes);
        Files.write(dir.resolve("foo-pkg-1.1.tar.gz"), sdistBytes);
        Files.write(dir.resolve("bar-2.0-py3-none-any.whl"), "other".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("README.txt"), "not a dist".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(dir.resolve("subdir"));

        PackageListing listing = client.listFiles(index(dir.toString()), "foo-pkg");

        assertThat(listing.getPackageName()).isEqualTo("foo-pkg");
        assertThat(listing.getFiles()).hasSize(2);

        PackageFile wheel = listing.getFiles().stream()
          .filter(f -> f.getFilename().endsWith(".whl")).findFirst().orElseThrow();
        assertThat(wheel.getFilename()).isEqualTo("foo_pkg-1.0-py3-none-any.whl");
        assertThat(wheel.getUrl()).startsWith("file://").endsWith("/foo_pkg-1.0-py3-none-any.whl");
        assertThat(wheel.getSha256()).isEqualTo(sha256(wheelBytes));
        assertThat(wheel.getRequiresPython()).isNull();
        assertThat(wheel.getCoreMetadataAvailable()).isNull();
        assertThat(wheel.getSize()).isNull();
        assertThat(wheel.getUploadTime()).isNull();

        PackageFile sdist = listing.getFiles().stream()
          .filter(f -> f.getFilename().endsWith(".tar.gz")).findFirst().orElseThrow();
        assertThat(sdist.getSha256()).isEqualTo(sha256(sdistBytes));
    }

    @Test
    void fileUrlLocationAccepted() throws Exception {
        Files.write(dir.resolve("foo-1.0-py3-none-any.whl"), "bytes".getBytes(StandardCharsets.UTF_8));

        PackageListing listing = client.listFiles(index(dir.toUri().toString()), "foo");
        assertThat(listing.getFiles()).hasSize(1);
    }

    @Test
    void packageAbsentFromDirectoryYieldsEmptyListing() throws IOException {
        Files.write(dir.resolve("bar-2.0-py3-none-any.whl"), "other".getBytes(StandardCharsets.UTF_8));

        PackageListing listing = client.listFiles(index(dir.toString()), "foo");
        assertThat(listing.getFiles()).isEmpty();
    }

    @Test
    void missingDirectoryIsUnreachable() {
        String location = dir.resolve("does-not-exist").toString();
        assertThatThrownBy(() -> client.listFiles(index(location), "foo"))
          .isInstanceOfSatisfying(PythonIndexException.class, e -> {
              assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.UNREACHABLE);
              assertThat(e.getIndexUrl()).isEqualTo(location);
          });
    }

    @Test
    void httpFlatPageFiltersByPackageAndReadsHashFragments() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
              .setHeader("Content-Type", "text/html")
              .setBody("""
                <html><body>
                <a href="foo-1.0-py3-none-any.whl#sha256=aaaa">foo-1.0-py3-none-any.whl</a>
                <a href="/packages/foo-1.1.tar.gz">foo-1.1.tar.gz</a>
                <a href="bar-2.0-py3-none-any.whl#sha256=bbbb">bar-2.0-py3-none-any.whl</a>
                </body></html>
                """));

            String pageUrl = server.url("/links/").toString();
            PackageListing listing = client.listFiles(index(pageUrl), "foo");

            RecordedRequest request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/links/");
            assertThat(request.getHeader("Accept")).isEqualTo("text/html");

            List<PackageFile> files = listing.getFiles();
            assertThat(files).hasSize(2);
            assertThat(files.get(0).getUrl()).isEqualTo(server.url("/links/foo-1.0-py3-none-any.whl").toString());
            assertThat(files.get(0).getSha256()).isEqualTo("aaaa");
            // relative to the server root, not the page
            assertThat(files.get(1).getUrl()).isEqualTo(server.url("/packages/foo-1.1.tar.gz").toString());
            assertThat(files.get(1).getSha256()).isNull();
        }
    }

    @Test
    void httpBasicAuthSent() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
              .setHeader("Content-Type", "text/html")
              .setBody("<html><body></body></html>"));

            PythonPackageIndex authed = new PythonPackageIndex(
              "shared", server.url("/links/").toString(), true, "user", "secret", false);
            client.listFiles(authed, "foo");

            RecordedRequest request = server.takeRequest();
            String expected = "Basic " + Base64.getEncoder()
              .encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
            assertThat(request.getHeader("Authorization")).isEqualTo(expected);
        }
    }

    @Test
    void unauthorizedMapsToAuthFailed() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(401));

            String pageUrl = server.url("/links/").toString();
            assertThatThrownBy(() -> client.listFiles(index(pageUrl), "foo"))
              .isInstanceOfSatisfying(PythonIndexException.class,
                e -> assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.AUTH_FAILED));
        }
    }

    @Test
    void missingPageMapsToUnreachable() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(404));

            // the page is the whole index, so a 404 is an unreachable index, not a missing package
            String pageUrl = server.url("/links/").toString();
            assertThatThrownBy(() -> client.listFiles(index(pageUrl), "foo"))
              .isInstanceOfSatisfying(PythonIndexException.class,
                e -> assertThat(e.getReason()).isEqualTo(PythonIndexException.Reason.UNREACHABLE));
        }
    }

    @Test
    void listingsCachedPerLocationAndPackage() throws Exception {
        Files.write(dir.resolve("foo-1.0-py3-none-any.whl"), "bytes".getBytes(StandardCharsets.UTF_8));

        PackageListing first = client.listFiles(index(dir.toString()), "foo");
        PackageListing second = client.listFiles(index(dir.toString()), "foo");
        assertThat(second).isSameAs(first);
    }
}
