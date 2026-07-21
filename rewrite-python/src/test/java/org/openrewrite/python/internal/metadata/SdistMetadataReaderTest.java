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
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SdistMetadataReaderTest {

    private static final String PKG_INFO_21 = """
      Metadata-Version: 2.1
      Name: pkg
      Version: 1.0
      Requires-Dist: requests (>=2.0)
      """;

    private static final String PKG_INFO_22 = """
      Metadata-Version: 2.2
      Name: pkg
      Version: 1.0
      Requires-Dist: requests (>=2.0)
      """;

    private static final String PKG_INFO_22_DYNAMIC = """
      Metadata-Version: 2.2
      Name: pkg
      Version: 1.0
      Dynamic: Requires-Dist
      """;

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

    private String serve(String fileName, byte[] body) {
        server.enqueue(new MockResponse().setBody(new Buffer().write(body)));
        return server.url("/packages/" + fileName).toString();
    }

    @Test
    void tarGzTopLevelPkgInfo() {
        byte[] sdist = targz(
          tarDir("pkg-1.0/"),
          tarFile("pkg-1.0/PKG-INFO", PKG_INFO_21),
          tarFile("pkg-1.0/setup.py", "from setuptools import setup"),
          tarFile("pkg-1.0/pkg.egg-info/PKG-INFO", "Metadata-Version: 2.1\nName: wrong\nVersion: 9.9\n"));

        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("pkg");
        assertThat(metadata.getVersion()).isEqualTo("1.0");
        assertThat(metadata.getRequiresDist()).containsExactly("requests (>=2.0)");
        assertThat(metadata.hasStaticRequiresDist()).isFalse();
    }

    @Test
    void shallowestPkgInfoWinsEvenWhenDeeperComesFirst() {
        byte[] sdist = targz(
          tarFile("pkg-1.0/src/pkg.egg-info/PKG-INFO", "Metadata-Version: 2.1\nName: wrong\nVersion: 9.9\n"),
          tarFile("pkg-1.0/PKG-INFO", PKG_INFO_21));

        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("pkg");
    }

    @Test
    void metadata22IsStaticallyTrustworthy() {
        byte[] sdist = targz(tarFile("pkg-1.0/PKG-INFO", PKG_INFO_22));
        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));
        assertThat(metadata).isNotNull();
        assertThat(metadata.hasStaticRequiresDist()).isTrue();
    }

    @Test
    void metadata22WithDynamicRequiresDistIsNot() {
        byte[] sdist = targz(tarFile("pkg-1.0/PKG-INFO", PKG_INFO_22_DYNAMIC));
        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));
        assertThat(metadata).isNotNull();
        assertThat(metadata.hasStaticRequiresDist()).isFalse();
    }

    @Test
    void ustarPrefixField() {
        String longDir = "a".repeat(120) + "-1.0";
        byte[] sdist = targz(tarEntry("PKG-INFO", longDir, (byte) '0', PKG_INFO_21.getBytes(StandardCharsets.UTF_8)));

        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("pkg");
    }

    @Test
    void paxAndGnuSpecialEntriesSkipped() {
        byte[] sdist = targz(
          tarEntry("pax_global_header", null, (byte) 'g', "52 comment=abcdef\n".getBytes(StandardCharsets.UTF_8)),
          tarEntry("pkg-1.0/PaxHeaders/PKG-INFO", null, (byte) 'x', "30 mtime=1700000000.0\n".getBytes(StandardCharsets.UTF_8)),
          tarFile("pkg-1.0/PKG-INFO", PKG_INFO_21));

        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist));

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("pkg");
    }

    @Test
    void tgzExtension() {
        byte[] sdist = targz(tarFile("pkg-1.0/PKG-INFO", PKG_INFO_21));
        assertThat(SdistMetadataReader.read(http, serve("pkg-1.0.tgz", sdist))).isNotNull();
    }

    @Test
    void zipSdist() {
        byte[] sdist = zip(
          new String[]{"pkg-1.0/setup.py", "from setuptools import setup"},
          new String[]{"pkg-1.0/PKG-INFO", PKG_INFO_21});

        CoreMetadata metadata = SdistMetadataReader.read(http, serve("pkg-1.0.zip", sdist));

        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("pkg");
        assertThat(metadata.getRequiresDist()).containsExactly("requests (>=2.0)");
    }

    @Test
    void missingPkgInfoReturnsNull() {
        byte[] sdist = targz(tarFile("pkg-1.0/setup.py", "from setuptools import setup"));
        assertThat(SdistMetadataReader.read(http, serve("pkg-1.0.tar.gz", sdist))).isNull();
    }

    @Test
    void notFoundReturnsNull() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertThat(SdistMetadataReader.read(http, server.url("/packages/pkg-1.0.tar.gz").toString())).isNull();
    }

    @Test
    void unsupportedArchiveTypeReturnsNull() {
        assertThat(SdistMetadataReader.read(http, server.url("/packages/pkg-1.0.tar.bz2").toString())).isNull();
        assertThat(server.getRequestCount()).isZero();
    }

    private static byte[] tarFile(String name, String content) {
        return tarEntry(name, null, (byte) '0', content.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] tarDir(String name) {
        return tarEntry(name, null, (byte) '5', new byte[0]);
    }

    private static byte[] tarEntry(String name, String prefix, byte typeflag, byte[] data) {
        byte[] header = new byte[512];
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);
        writeOctal(header, 100, 8, 0644);
        writeOctal(header, 108, 8, 0);
        writeOctal(header, 116, 8, 0);
        writeOctal(header, 124, 12, data.length);
        writeOctal(header, 136, 12, 0);
        header[156] = typeflag;
        byte[] magic = "ustar\00000".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, header, 257, magic.length);
        if (prefix != null) {
            byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(prefixBytes, 0, header, 345, prefixBytes.length);
        }
        Arrays.fill(header, 148, 156, (byte) ' ');
        int sum = 0;
        for (byte b : header) {
            sum += b & 0xFF;
        }
        byte[] checksum = String.format("%06o", sum).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(checksum, 0, header, 148, 6);
        header[154] = 0;
        header[155] = ' ';

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(header);
        out.writeBytes(data);
        out.writeBytes(new byte[(512 - data.length % 512) % 512]);
        return out.toByteArray();
    }

    private static void writeOctal(byte[] header, int offset, int length, long value) {
        byte[] octal = String.format("%0" + (length - 1) + "o", value).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(octal, 0, header, offset, octal.length);
    }

    private static byte[] targz(byte[]... entries) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                for (byte[] entry : entries) {
                    gzip.write(entry);
                }
                gzip.write(new byte[1024]); // end-of-archive marker
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] zip(String[]... nameContentPairs) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (String[] pair : nameContentPairs) {
                    ZipEntry entry = new ZipEntry(pair[0]);
                    zip.putNextEntry(entry);
                    zip.write(pair[1].getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
