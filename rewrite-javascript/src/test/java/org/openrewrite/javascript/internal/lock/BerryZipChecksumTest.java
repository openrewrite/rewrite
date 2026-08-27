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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the Yarn Berry {@code checksum} reproduction is byte-exact: each fixture is the verbatim registry
 * tarball, and the asserted checksum is what a real {@code yarn install} (yarn 4.5.3, {@code cacheKey: 10c0})
 * wrote into its {@code yarn.lock}. The tarballs span a leaf, nested directories with an executable {@code bin}
 * script, and scoped packages — the structural cases that exercise directory-entry synthesis and mode bits.
 */
class BerryZipChecksumTest {

    private static final String CACHE_KEY = "10c0";

    @Test
    void leaf() {
        assertThat(checksumOf("ms-2.1.3.tgz", "ms")).isEqualTo(
                "10c0/d924b57e7312b3b63ad21fc5b3dc0af5e78d61a1fc7cfb5457edaf26326bf62be5307cc87ffb6862ef1c2b33b0233cdb5d4f01c4c958cc0d660948b65a287a48");
    }

    @Test
    void nestedDirectoriesAndExecutableBin() {
        // semver has classes/functions/internal/ranges/ subdirectories and an executable bin/semver.js (0755).
        assertThat(checksumOf("semver-7.6.3.tgz", "semver")).isEqualTo(
                "10c0/88f33e148b210c153873cb08cfe1e281d518aaa9a666d4d148add6560db5cd3c582f3a08ccb91f38d5f379ead256da9931234ed122057f40bb5766e65e58adaf");
    }

    @Test
    void scopedPackage() {
        assertThat(checksumOf("babel-code-frame-7.24.7.tgz", "@babel/code-frame")).isEqualTo(
                "10c0/ab0af539473a9f5aeaac7047e377cb4f4edd255a81d84a76058595f8540784cc3fbe8acf73f1e073981104562490aabfb23008cd66dc677a456a4ed5390fdde6");
    }

    @Test
    void esmPackage() {
        assertThat(checksumOf("chalk-5.3.0.tgz", "chalk")).isEqualTo(
                "10c0/8297d436b2c0f95801103ff2ef67268d362021b8210daf8ddbe349695333eb3610a71122172ff3b0272f1ef2cf7cc2c41fdaa4715f52e49ffe04c56340feed09");
    }

    @Test
    void rejectsNonGzip() {
        assertThatThrownBy(() -> BerryZipChecksum.checksum("not a tarball".getBytes(), "ms", CACHE_KEY))
                .isInstanceOf(EngineFailure.class)
                .hasMessageContaining("gzip");
    }

    private static String checksumOf(String fixture, String name) {
        return BerryZipChecksum.checksum(bytes("lock/yarn-berry/checksum/" + fixture), name, CACHE_KEY);
    }

    private static byte[] bytes(String resource) {
        try (InputStream in = BerryZipChecksumTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
