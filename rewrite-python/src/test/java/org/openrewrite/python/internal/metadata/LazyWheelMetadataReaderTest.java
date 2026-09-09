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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LazyWheelMetadataReaderTest {

    private static final String METADATA = """
      Metadata-Version: 2.1
      Name: foo
      Version: 1.0
      Requires-Dist: requests (>=2.0)
      Requires-Dist: idna ; extra == 'test'
      Requires-Python: >=3.8
      Provides-Extra: test

      Long description body that is not part of the headers.
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

    private String wheelUrl() {
        return server.url("/packages/foo-1.0-py3-none-any.whl").toString();
    }

    private void assertFooMetadata(CoreMetadata metadata) {
        assertThat(metadata).isNotNull();
        assertThat(metadata.getName()).isEqualTo("foo");
        assertThat(metadata.getVersion()).isEqualTo("1.0");
        assertThat(metadata.getRequiresDist()).containsExactly("requests (>=2.0)", "idna ; extra == 'test'");
        assertThat(metadata.getRequiresPython()).isEqualTo(">=3.8");
        assertThat(metadata.getProvidesExtra()).containsExactly("test");
    }

    @Test
    void smallWheelEntirelyInTailUsesSingleRangedRequest() throws InterruptedException {
        byte[] wheel = wheel(null, entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false));
        server.setDispatcher(new RangeDispatcher(wheel, true, true));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl()));

        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getHeader("Range")).isEqualTo("bytes=-131072");
    }

    @Test
    void metadataOutsideTailFetchedWithSecondRangedRequest() throws InterruptedException {
        byte[] wheel = wheel(null,
          entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false),
          entry("foo/padding.bin", randomBytes(64 * 1024), false));
        RangeDispatcher dispatcher = new RangeDispatcher(wheel, true, true);
        server.setDispatcher(dispatcher);

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl(), 1024));

        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(server.takeRequest().getHeader("Range")).isEqualTo("bytes=-1024");
        assertThat(server.takeRequest().getHeader("Range")).startsWith("bytes=0-");
        // only partial content ever left the server
        assertThat(dispatcher.servedLengths).allSatisfy(length -> assertThat(length).isLessThan(wheel.length));
    }

    @Test
    void centralDirectoryOutsideTailFetchedWithExtraRangedRequest() throws InterruptedException {
        byte[] wheel = wheel(null,
          entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false),
          entry("foo/padding.bin", randomBytes(8 * 1024), false));
        RangeDispatcher dispatcher = new RangeDispatcher(wheel, true, true);
        server.setDispatcher(dispatcher);

        // 64-byte tail holds the EOCD but not the central directory
        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl(), 64));

        assertThat(server.getRequestCount()).isEqualTo(3);
        assertThat(server.takeRequest().getHeader("Range")).isEqualTo("bytes=-64");
        assertThat(server.takeRequest().getHeader("Range")).startsWith("bytes=");
        assertThat(server.takeRequest().getHeader("Range")).startsWith("bytes=0-");
        assertThat(dispatcher.servedLengths).allSatisfy(length -> assertThat(length).isLessThan(wheel.length));
    }

    @Test
    void rangeIgnoredStreamsTheSingleFullResponse() {
        byte[] wheel = wheel(null,
          entry("foo/other.py", "print('hi')".getBytes(StandardCharsets.UTF_8), false),
          entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false));
        server.setDispatcher(new RangeDispatcher(wheel, false, true));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl()));

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void storedEntry() {
        byte[] wheel = wheel(null, entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), true));
        server.setDispatcher(new RangeDispatcher(wheel, true, true));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl()));
    }

    @Test
    void archiveCommentTolerated() {
        byte[] wheel = wheel("Generated by a build tool that likes zip comments",
          entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false));
        server.setDispatcher(new RangeDispatcher(wheel, true, true));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl()));
    }

    @Test
    void missingContentRangeHeaderInferredFromZipStructure() {
        byte[] wheel = wheel(null,
          entry("foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false),
          entry("foo/padding.bin", randomBytes(16 * 1024), false));
        server.setDispatcher(new RangeDispatcher(wheel, true, false));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl(), 1024));
    }

    @Test
    void zip64EndOfCentralDirectory() {
        byte[] wheel = zip64Wheel(METADATA.getBytes(StandardCharsets.UTF_8));
        server.setDispatcher(new RangeDispatcher(wheel, true, true));

        assertFooMetadata(LazyWheelMetadataReader.read(http, wheelUrl()));
    }

    @Test
    void noMetadataEntryReturnsNull() {
        byte[] wheel = wheel(null,
          entry("foo/code.py", "pass".getBytes(StandardCharsets.UTF_8), false),
          // nested dist-info must not match
          entry("vendored/foo-1.0.dist-info/METADATA", METADATA.getBytes(StandardCharsets.UTF_8), false));
        server.setDispatcher(new RangeDispatcher(wheel, true, true));

        assertThat(LazyWheelMetadataReader.read(http, wheelUrl())).isNull();
    }

    @Test
    void httpErrorReturnsNull() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(404);
            }
        });
        assertThat(LazyWheelMetadataReader.read(http, wheelUrl())).isNull();
    }

    @Test
    void corruptZipReturnsNull() {
        server.setDispatcher(new RangeDispatcher(randomBytes(4096), true, true));
        assertThat(LazyWheelMetadataReader.read(http, wheelUrl())).isNull();
    }

    static final class RangeDispatcher extends Dispatcher {
        private final byte[] file;
        private final boolean honorRange;
        private final boolean sendContentRange;
        final List<Integer> servedLengths = new CopyOnWriteArrayList<>();

        RangeDispatcher(byte[] file, boolean honorRange, boolean sendContentRange) {
            this.file = file;
            this.honorRange = honorRange;
            this.sendContentRange = sendContentRange;
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            String range = request.getHeader("Range");
            if (!honorRange || range == null || !range.startsWith("bytes=")) {
                servedLengths.add(file.length);
                return new MockResponse().setResponseCode(200).setBody(new Buffer().write(file));
            }
            String spec = range.substring("bytes=".length());
            long start;
            long end;
            if (spec.startsWith("-")) {
                start = Math.max(0, file.length - Long.parseLong(spec.substring(1)));
                end = file.length - 1;
            } else {
                int dash = spec.indexOf('-');
                start = Long.parseLong(spec.substring(0, dash));
                String endSpec = spec.substring(dash + 1);
                end = endSpec.isEmpty() ? file.length - 1 : Math.min(Long.parseLong(endSpec), file.length - 1);
            }
            byte[] slice = Arrays.copyOfRange(file, (int) start, (int) end + 1);
            servedLengths.add(slice.length);
            MockResponse response = new MockResponse().setResponseCode(206).setBody(new Buffer().write(slice));
            if (sendContentRange) {
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + file.length);
            }
            return response;
        }
    }

    record Entry(String name, byte[] data, boolean stored) {
    }

    static Entry entry(String name, byte[] data, boolean stored) {
        return new Entry(name, data, stored);
    }

    static byte[] wheel(String comment, Entry... entries) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                if (comment != null) {
                    zip.setComment(comment);
                }
                for (Entry entry : entries) {
                    ZipEntry zipEntry = new ZipEntry(entry.name());
                    if (entry.stored()) {
                        zipEntry.setMethod(ZipEntry.STORED);
                        zipEntry.setSize(entry.data().length);
                        zipEntry.setCompressedSize(entry.data().length);
                        CRC32 crc = new CRC32();
                        crc.update(entry.data());
                        zipEntry.setCrc(crc.getValue());
                    }
                    zip.putNextEntry(zipEntry);
                    zip.write(entry.data());
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Hand-built minimal ZIP64: one stored entry, EOCD fields at their 0xFFFF/0xFFFFFFFF
     * sentinels, real values in the ZIP64 EOCD record reached via the locator.
     */
    static byte[] zip64Wheel(byte[] metadata) {
        byte[] name = "foo-1.0.dist-info/METADATA".getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(metadata);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // local header
        le32(out, 0x04034b50L);
        le16(out, 45); // version needed
        le16(out, 0);  // flags
        le16(out, 0);  // method: stored
        le16(out, 0);  // time
        le16(out, 0);  // date
        le32(out, crc.getValue());
        le32(out, metadata.length);
        le32(out, metadata.length);
        le16(out, name.length);
        le16(out, 0);  // extra len
        out.writeBytes(name);
        out.writeBytes(metadata);

        long cdOffset = out.size();
        le32(out, 0x02014b50L);
        le16(out, 45); // version made by
        le16(out, 45); // version needed
        le16(out, 0);  // flags
        le16(out, 0);  // method
        le16(out, 0);  // time
        le16(out, 0);  // date
        le32(out, crc.getValue());
        le32(out, metadata.length);
        le32(out, metadata.length);
        le16(out, name.length);
        le16(out, 0);  // extra len
        le16(out, 0);  // comment len
        le16(out, 0);  // disk start
        le16(out, 0);  // internal attrs
        le32(out, 0);  // external attrs
        le32(out, 0);  // local header offset
        out.writeBytes(name);
        long cdSize = out.size() - cdOffset;

        long zip64RecordOffset = out.size();
        le32(out, 0x06064b50L);
        le64(out, 44); // size of remainder of record
        le16(out, 45);
        le16(out, 45);
        le32(out, 0);  // disk number
        le32(out, 0);  // disk with CD
        le64(out, 1);  // entries on disk
        le64(out, 1);  // total entries
        le64(out, cdSize);
        le64(out, cdOffset);

        // zip64 locator
        le32(out, 0x07064b50L);
        le32(out, 0);
        le64(out, zip64RecordOffset);
        le32(out, 1);

        // EOCD with sentinel values
        le32(out, 0x06054b50L);
        le16(out, 0);
        le16(out, 0);
        le16(out, 0xFFFF);
        le16(out, 0xFFFF);
        le32(out, 0xFFFFFFFFL);
        le32(out, 0xFFFFFFFFL);
        le16(out, 0);
        return out.toByteArray();
    }

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    private static void le16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private static void le32(ByteArrayOutputStream out, long value) {
        le16(out, (int) (value & 0xFFFF));
        le16(out, (int) ((value >>> 16) & 0xFFFF));
    }

    private static void le64(ByteArrayOutputStream out, long value) {
        le32(out, value & 0xFFFFFFFFL);
        le32(out, (value >>> 32) & 0xFFFFFFFFL);
    }
}
