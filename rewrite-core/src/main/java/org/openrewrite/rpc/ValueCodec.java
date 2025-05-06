/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public interface ValueCodec<T> {
    ValueCodec<Charset> CHARSET = new ValueCodec<Charset>() {
        @Override
        public Charset decode(Object value) {
            return Charset.forName(value.toString());
        }

        @Override
        public Object encode(Charset value) {
            return value.toString();
        }
    };

    ValueCodec<Path> PATH = new ValueCodec<Path>() {
        @Override
        public Path decode(Object value) {
            return Paths.get(value.toString());
        }

        @Override
        public Object encode(Path value) {
            return value.toString();
        }
    };

    ValueCodec<String> STRING = new ValueCodec<String>() {
        @Override
        public String decode(Object value) {
            return value.toString();
        }

        @Override
        public Object encode(String value) {
            return value;
        }
    };

    ValueCodec<UUID> UUID = new ValueCodec<UUID>() {
        @Override
        public UUID decode(Object value) {
            return java.util.UUID.fromString(value.toString());
        }

        @Override
        public Object encode(UUID value) {
            return value.toString();
        }
    };

    ClassValue<ValueCodec<?>> _ENUM_CODECS = new ClassValue<ValueCodec<?>>() {
        @Override
        protected ValueCodec<?> computeValue(Class<?> type) {
            return new ValueCodec<Object>() {

                private final Object[] enumConstants = type.getEnumConstants();

                @Override
                public Object decode(Object value) {
                    return enumConstants[(Integer) value];
                }

                @Override
                public Object encode(Object value) {
                    return ((Enum<?>) value).ordinal();
                }
            };
        }
    };

    static <T extends Enum<T>> ValueCodec<T> forEnum(Class<T> clazz) {
        //noinspection unchecked
        return (ValueCodec<T>) _ENUM_CODECS.get(clazz);
    }

    T decode(Object value);

    Object encode(T value);
}
