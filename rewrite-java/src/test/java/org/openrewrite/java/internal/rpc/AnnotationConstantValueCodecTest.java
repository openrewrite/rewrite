/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.internal.rpc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnotationConstantValueCodecTest {

    @Test
    void encodesNullAsNull() {
        assertThat(AnnotationConstantValueCodec.encode(null)).isNull();
        assertThat(AnnotationConstantValueCodec.decode(null)).isNull();
        assertThat(AnnotationConstantValueCodec.decode("n")).isNull();
    }

    @Test
    void roundTripsEachSupportedType() {
        // Round-trip preserves the exact runtime class for each constant.
        assertThat(roundTrip("hello")).isEqualTo("hello").isInstanceOf(String.class);
        assertThat(roundTrip(true)).isEqualTo(true).isInstanceOf(Boolean.class);
        assertThat(roundTrip(false)).isEqualTo(false).isInstanceOf(Boolean.class);
        assertThat(roundTrip(42)).isEqualTo(42).isInstanceOf(Integer.class);
        assertThat(roundTrip(42L)).isEqualTo(42L).isInstanceOf(Long.class);
        assertThat(roundTrip((short) 42)).isEqualTo((short) 42).isInstanceOf(Short.class);
        assertThat(roundTrip((byte) 42)).isEqualTo((byte) 42).isInstanceOf(Byte.class);
        assertThat(roundTrip(1.5f)).isEqualTo(1.5f).isInstanceOf(Float.class);
        assertThat(roundTrip(1.5d)).isEqualTo(1.5d).isInstanceOf(Double.class);
        assertThat(roundTrip('c')).isEqualTo('c').isInstanceOf(Character.class);
    }

    @Test
    void distinguishesOverlappingNumericValues() {
        // The whole point of the envelope: int 42 vs long 42 round-trip distinctly.
        assertThat(roundTrip(42)).isEqualTo(42).isNotEqualTo(42L);
        assertThat(roundTrip(42L)).isEqualTo(42L).isNotEqualTo(42);
        // Float vs Double round-trip distinctly.
        assertThat(roundTrip(1.5f)).isEqualTo(1.5f).isNotEqualTo(1.5d);
        assertThat(roundTrip(1.5d)).isEqualTo(1.5d).isNotEqualTo(1.5f);
        // Single-char string must not collapse to char.
        assertThat(roundTrip("c")).isEqualTo("c").isNotEqualTo('c');
    }

    @Test
    void preservesStringsContainingColons() {
        assertThat(roundTrip("foo:bar")).isEqualTo("foo:bar");
        assertThat(roundTrip(":")).isEqualTo(":");
        assertThat(roundTrip("")).isEqualTo("");
    }

    @Test
    void rejectsUnsupportedRuntimeTypes() {
        assertThatThrownBy(() -> AnnotationConstantValueCodec.encode(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported annotation constant value type");
        assertThatThrownBy(() -> AnnotationConstantValueCodec.encode(java.util.UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedEnvelope() {
        assertThatThrownBy(() -> AnnotationConstantValueCodec.decode("x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed");
        assertThatThrownBy(() -> AnnotationConstantValueCodec.decode("z:foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void roundTripsListIncludingNulls() {
        Object[] originals = {"a", 1, 2L, true, null, 'x'};
        List<String> encoded = AnnotationConstantValueCodec.encodeList(originals);
        Object[] decoded = AnnotationConstantValueCodec.decodeArray(encoded);
        assertThat(decoded).containsExactly("a", 1, 2L, true, null, 'x');
    }

    @Test
    void emptyListRoundTrips() {
        Object[] empty = {};
        List<String> encoded = AnnotationConstantValueCodec.encodeList(empty);
        assertThat(encoded).isEmpty();
        Object[] decoded = AnnotationConstantValueCodec.decodeArray(encoded);
        assertThat(decoded).isEmpty();
    }

    @Test
    void nullListMapsToNull() {
        assertThat(AnnotationConstantValueCodec.encodeList(null)).isNull();
        assertThat(AnnotationConstantValueCodec.decodeArray(null)).isNull();
    }

    @Test
    void roundTripsNonAsciiStrings() {
        assertThat(roundTrip("héllo wörld 🚀")).isEqualTo("héllo wörld 🚀");
        // Verify list shape too — ensures encodeList iterates without UTF mishap.
        List<String> encoded = AnnotationConstantValueCodec.encodeList(new Object[]{"héllo", "wörld"});
        assertThat(AnnotationConstantValueCodec.decodeArray(encoded))
                .containsExactly("héllo", "wörld");
    }

    @Test
    void boundaryNumericValues() {
        // Make sure max/min values for each numeric kind round-trip.
        for (Object o : Arrays.asList(
                Integer.MAX_VALUE, Integer.MIN_VALUE,
                Long.MAX_VALUE, Long.MIN_VALUE,
                Short.MAX_VALUE, Short.MIN_VALUE,
                Byte.MAX_VALUE, Byte.MIN_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE,
                Double.MAX_VALUE, -Double.MAX_VALUE)) {
            assertThat(roundTrip(o)).isEqualTo(o).isInstanceOf(o.getClass());
        }
    }

    private static Object roundTrip(Object value) {
        return AnnotationConstantValueCodec.decode(AnnotationConstantValueCodec.encode(value));
    }
}
