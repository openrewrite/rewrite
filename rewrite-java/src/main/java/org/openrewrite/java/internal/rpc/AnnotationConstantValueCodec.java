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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire codec for {@link org.openrewrite.java.tree.JavaType.Annotation} constant
 * element values. Plain JSON cannot distinguish e.g. {@code Integer 42} from
 * {@code Long 42} or {@code Character 'c'} from {@code String "c"}, so each
 * constant is encoded as a tagged string on the wire.
 * <p>
 * Encoding: {@code "<kind>:<lexical>"} where {@code <kind>} is one of
 * {@code s} (String), {@code b} (Boolean), {@code i} (Integer), {@code l} (Long),
 * {@code S} (Short), {@code B} (Byte), {@code f} (Float), {@code d} (Double),
 * {@code c} (Character). A null constant is encoded as the literal string
 * {@code "n"} (or {@code null} on the wire — both are accepted on receive).
 * <p>
 * Only these primitive-like values appear as {@code constantValue} per
 * {@code ReloadableJava*TypeMapping.annotationElementValue} — class literals
 * and enum constants flow through the {@code referenceValue} branch instead.
 */
final class AnnotationConstantValueCodec {

    private AnnotationConstantValueCodec() {
    }

    static @Nullable String encode(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return "s:" + value;
        }
        if (value instanceof Boolean) {
            return "b:" + value;
        }
        if (value instanceof Integer) {
            return "i:" + value;
        }
        if (value instanceof Long) {
            return "l:" + value;
        }
        if (value instanceof Short) {
            return "S:" + value;
        }
        if (value instanceof Byte) {
            return "B:" + value;
        }
        if (value instanceof Float) {
            return "f:" + value;
        }
        if (value instanceof Double) {
            return "d:" + value;
        }
        if (value instanceof Character) {
            return "c:" + value;
        }
        throw new IllegalArgumentException(
                "Unsupported annotation constant value type: " + value.getClass().getName());
    }

    static @Nullable Object decode(@Nullable String encoded) {
        if (encoded == null || "n".equals(encoded)) {
            return null;
        }
        if (encoded.length() < 2 || encoded.charAt(1) != ':') {
            throw new IllegalArgumentException("Malformed annotation constant value envelope: " + encoded);
        }
        char kind = encoded.charAt(0);
        String body = encoded.substring(2);
        switch (kind) {
            case 's':
                return body;
            case 'b':
                return Boolean.parseBoolean(body);
            case 'i':
                return Integer.parseInt(body);
            case 'l':
                return Long.parseLong(body);
            case 'S':
                return Short.parseShort(body);
            case 'B':
                return Byte.parseByte(body);
            case 'f':
                return Float.parseFloat(body);
            case 'd':
                return Double.parseDouble(body);
            case 'c':
                if (body.isEmpty()) {
                    throw new IllegalArgumentException("Malformed char envelope: empty body");
                }
                return body.charAt(0);
            default:
                throw new IllegalArgumentException("Unknown annotation constant value kind: " + kind);
        }
    }

    static @Nullable List<String> encodeList(Object @Nullable [] values) {
        if (values == null) {
            return null;
        }
        List<String> encoded = new ArrayList<>(values.length);
        for (Object value : values) {
            encoded.add(encode(value));
        }
        return encoded;
    }

    static Object @Nullable [] decodeArray(@Nullable List<String> encoded) {
        if (encoded == null) {
            return null;
        }
        Object[] decoded = new Object[encoded.size()];
        for (int i = 0; i < encoded.size(); i++) {
            decoded[i] = decode(encoded.get(i));
        }
        return decoded;
    }
}
