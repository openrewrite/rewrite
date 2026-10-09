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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ssri 13.0.1 (npm 11.17.0) run on these values under Node 24.
 */
class SsriTest {

    private static final String A = "sha512-AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBA" +
                                    "QEBAQEBAQEBAQ==";
    private static final String B = "sha512-AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA" +
                                    "gICAgICAgICAg==";
    private static final String S = "sha1-AwMDAwMDAwMDAwMDAwMDAwMDAwM=";
    private static final String T = "sha1-BAQEBAQEBAQEBAQEBAQEBAQEBAQ=";
    private static final String M = "md5-BQUFBQUFBQUFBQUFBQUFBQ==";

    @Test
    void parse() {
        assertThat(Ssri.parse("abc")).isNull();
        assertThat(Ssri.parse("")).isNull();
        assertThat(Ssri.parse("sha512-")).isNull();
        assertThat(Ssri.parse("SHA512-x")).isNull();
        assertThat(Ssri.parse(A + " abc " + S)).isEqualTo(A + " " + S);
        assertThat(Ssri.parse(A + "?a?b")).isEqualTo(A + "?a?b");
        assertThat(Ssri.parse(A + "?")).isEqualTo(A + "?");
        assertThat(Ssri.parse("  " + A + "\n" + S + "  ")).isEqualTo(A + " " + S);
        assertThat(Ssri.parse(A + " " + A)).isEqualTo(A + " " + A);
        assertThat(Ssri.parse(S + " " + A + " " + T)).isEqualTo(S + " " + T + " " + A);
    }

    // a hash whose algorithm crypto.getHashes() does not list is dropped by the Hash constructor
    @Test
    void parseDropsAlgorithmsNodeLacks() {
        assertThat(Ssri.parse("foo-YWJj")).isNull();
        assertThat(Ssri.parse("whirlpool-YWJj")).isNull();
        assertThat(Ssri.parse("md4-YWJj")).isNull();
        assertThat(Ssri.parse("sm3-YWJj")).isEqualTo("sm3-YWJj");
        assertThat(Ssri.parse(A + " foo-YWJj")).isEqualTo(A);
    }

    @Test
    void match() {
        assertThat(Ssri.match(A, A)).isTrue();
        assertThat(Ssri.match(A, B)).isFalse();
        assertThat(Ssri.match(A, S)).isFalse();
        assertThat(Ssri.match(S, A)).isFalse();
        assertThat(Ssri.match(A + " " + S, S)).isTrue();
        assertThat(Ssri.match(A + " " + S, B + " " + S)).isFalse();
        assertThat(Ssri.match(A + " " + S, B + " " + T)).isFalse();
        assertThat(Ssri.match(S + " " + M, M + " " + T)).isFalse();
        assertThat(Ssri.match(A, "abc")).isFalse();
        assertThat(Ssri.match(A + " " + S, S + " " + B)).isFalse();
        assertThat(Ssri.match(M + " " + S, S)).isTrue();
        assertThat(Ssri.match(A + " " + A, B + " " + A)).isTrue();
    }

    @Test
    void merge() {
        assertThat(Ssri.merge(S, A)).isEqualTo(S + " " + A);
        assertThat(Ssri.merge(A, S)).isEqualTo(A + " " + S);
        assertThat(Ssri.merge(A, A)).isEqualTo(A);
        assertThat(Ssri.merge(S, A + " " + M)).isEqualTo(S + " " + A + " " + M);
        assertThat(Ssri.merge(A, "abc")).isEqualTo(A);
        assertThat(Ssri.merge(A + "?x", A + "?y")).isEqualTo(A + "?x");
        assertThat(Ssri.merge(S + " " + T, T)).isEqualTo(S + " " + T);
        assertThatThrownBy(() -> Ssri.merge(A, B)).hasMessage("hashes do not match, cannot update integrity");
        assertThatThrownBy(() -> Ssri.merge(A + " " + S, B + " " + S))
          .hasMessage("hashes do not match, cannot update integrity");
        assertThatThrownBy(() -> Ssri.merge(A + " " + S, A + " " + T))
          .hasMessage("hashes do not match, cannot update integrity");
    }

    @Test
    void fromHex() {
        assertThat(Ssri.fromHex("0303030303030303030303030303030303030303", "sha1")).isEqualTo(S);
        assertThat(Ssri.fromHex("03zz", "sha1")).isEqualTo("sha1-Aw==");
        assertThat(Ssri.fromHex("", "sha1")).isNull();
    }
}
