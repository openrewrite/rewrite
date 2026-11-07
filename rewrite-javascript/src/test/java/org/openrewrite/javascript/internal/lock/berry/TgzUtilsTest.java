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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.EcmaScript;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Yarn's packages/yarnpkg-core/tests/tgzUtils.test.ts at {@code compressionLevel: 0}, less "without any workers" (no
 * worker pool); {@code REWRITE_BERRY_TGZ_FIXTURES} names a directory holding the uncommitted 808 KB fixture.
 */
class TgzUtilsTest {

    @Test
    void convertsATgzWithoutCompression() throws IOException {
        byte[] tar = resource("parse5-0.0.28.tgz");
        assertThat(tar[0]).isNotEqualTo((byte) 0x1f);
        assertThat(sha512(tar, ".", 0)).isEqualTo("c43ced62833bb3d06b926b4b6993f83adac36b04905adc79821bfe02590d4b" +
                                                 "1d12228454cafb31d2e114fbf5507983194e83ecd7f0894ebdc06d396a945cba02");
        assertThat(Cache.checksum(tar, StructUtils.makeIdent(null, "parse5"), Configuration.defaults()))
                .isEqualTo("10c0/9959b32e2b21283b46b0f240aafba623e7f21cddce511e8cabf0bf7c0b774b15e45e4ce17ade302ab5" +
                           "2702684b491ca4ae0fcae4f53ccd44bc2c1773f71a50ab");
    }

    @Test
    void readsTheSameEntriesGzippedOrNot() throws IOException {
        byte[] tar = resource("parse5-0.0.28.tgz");
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(gzipped)) {
            out.write(tar);
        }
        assertThat(sha512(gzipped.toByteArray(), ".", 0)).isEqualTo(sha512(tar, ".", 0));
    }

    @Test
    void aCorruptGzipStreamFailsAsZlibDoes() {
        assertThatThrownBy(() -> TgzUtils.convertToZip(new byte[]{0x1f, (byte) 0x8b, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                ".", 0))
                .isInstanceOf(ReportError.class)
                .hasMessage("zlib: unknown compression method");
    }

    @Test
    void convertsATarWithALotOfFiles() throws IOException {
        String dir = System.getenv("REWRITE_BERRY_TGZ_FIXTURES");
        Path fixture = dir == null ? null : Paths.get(dir, "carbon-icons-svelte-10.21.0.tgz");
        assumeTrue(fixture != null && Files.isRegularFile(fixture));
        byte[] tgz = Files.readAllBytes(fixture);
        assertThat(sha512(tgz, ".", 0)).isEqualTo("9d2c2748de33b0fcd0e980308f97a1e5090151615769f542d64722165780985b" +
                                                 "3539df5ac6064773fbc0e46242703a6cf2b54058d1b14a63ed426873bd01a89a");
        assertThat(Cache.checksum(tgz, StructUtils.makeIdent(null, "carbon-icons-svelte"), Configuration.defaults()))
                .isEqualTo("10c0/84acc5fd20f022a17adb1d948a9e38247e447999a0f4a172dde8e70b3692ca09041506c31ab4e943" +
                           "8d875ea4c5210ef15ab3fca0c97aaef102ae54107eccb5f2");
    }

    // node-tar 6.1.11's entries for headers with a NUL before a line terminator and long pax numbers, and for an
    // entry larger than the archive with a valid header after it (tar-headers.js)
    @ParameterizedTest
    @ValueSource(strings = {"node-tar-headers", "node-tar-oversize"})
    void readsHeadersAsNodeTarDoes(String archive) throws IOException {
        List<Object> expected = new ArrayList<>();
        for (String line : new String(resource(archive + ".jsonl"), StandardCharsets.UTF_8).split("\n")) {
            expected.add(EcmaScript.jsonParse(line));
        }
        List<Object> entries = new ArrayList<>();
        for (TgzUtils.TarEntry entry : TgzUtils.parseTar(TgzUtils.maybeGunzip(resource(archive + ".tgz")))) {
            Map<String, Object> read = new LinkedHashMap<>();
            read.put("type", entry.type);
            read.put("path", entry.path);
            read.put("linkpath", Js.truthy(entry.linkpath) ? entry.linkpath : null);
            read.put("length", (double) entry.body.length);
            read.put("text", new String(entry.body, StandardCharsets.UTF_8).replaceAll("\0+\\z", ""));
            entries.add(read);
        }
        assertThat(entries).isEqualTo(expected);
    }

    // node-tar 6.1.11 over a header whose octal size is signed, then a valid file of 5 bytes: a size whose block count
    // stays zero reads nothing, one below a block throws (recorded)
    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "-0000001"})
    void aSignedSizeWithinABlockReadsNothing(String size) {
        List<String> entries = new ArrayList<>();
        for (TgzUtils.TarEntry entry : TgzUtils.parseTar(signedSizeTar(size))) {
            entries.add(entry.path + " " + entry.body.length);
        }
        assertThat(entries).containsExactly("package/neg 0", "package/after 5");
    }

    @Test
    void aSignedSizeBelowABlockFailsAsNodeTarDoes() {
        assertThatThrownBy(() -> TgzUtils.parseTar(signedSizeTar("-0001000")))
          .hasMessageContaining("writing more to entry than is appropriate");
    }

    private static byte[] signedSizeTar(String size) {
        byte[] tar = new byte[512 * 6];
        System.arraycopy(header("package/neg", size), 0, tar, 0, 512);
        System.arraycopy(header("package/after", "00000000005"), 0, tar, 512, 512);
        System.arraycopy("hello".getBytes(StandardCharsets.US_ASCII), 0, tar, 1024, 5);
        return tar;
    }

    private static byte[] header(String name, String size) {
        byte[] header = new byte[512];
        put(header, 0, name);
        put(header, 100, "0000644");
        put(header, 108, "0000000");
        put(header, 116, "0000000");
        put(header, 124, size);
        put(header, 136, "00000000000");
        header[156] = '0';
        put(header, 257, "ustar");
        put(header, 263, "00");
        for (int i = 148; i < 156; i++) {
            header[i] = ' ';
        }
        long sum = 0;
        for (byte b : header) {
            sum += b & 0xff;
        }
        put(header, 148, String.format("%06o", sum));
        header[154] = 0;
        header[155] = ' ';
        return header;
    }

    private static void put(byte[] header, int offset, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }

    private static String sha512(byte[] tgz, String prefixPath, int stripComponents) {
        return HashUtils.sha512Hex(TgzUtils.convertToZip(tgz, prefixPath, stripComponents).toByteArray());
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = TgzUtilsTest.class.getResourceAsStream("/yarn-berry/tgz-utils/" + name)) {
            assertThat(in).as(name).isNotNull();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
