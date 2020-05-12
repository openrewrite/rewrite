/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import org.openrewrite.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TreeSerializer {
    private final TypeReference<List<J.CompilationUnit>> cuListType = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    private SimpleModule metadataModule;

    public TreeSerializer() {
        this.metadataModule = new SimpleModule();
        metadataModule.addKeySerializer(Metadata.class, new MetadataKeySerializer());
        metadataModule.addKeyDeserializer(Metadata.class, new MetadataKeyDeserializer());

        var f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        this.mapper = new ObjectMapper(f)
                .registerModule(metadataModule)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String writePretty(J.CompilationUnit cu) {
        try {
            return new ObjectMapper()
                    .registerModule(metadataModule)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cu);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void write(List<J.CompilationUnit> cus, OutputStream out) {
        try {
            mapper.writeValue(out, cus);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(List<J.CompilationUnit> cus) {
        try {
            return mapper.writeValueAsBytes(cus);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(J.CompilationUnit cu, OutputStream out) {
        try {
            mapper.writeValue(out, cu);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(J.CompilationUnit cu) {
        try {
            return mapper.writeValueAsBytes(cu);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<J.CompilationUnit> readList(InputStream input) {
        try {
            return mapper.readValue(input, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<J.CompilationUnit> readList(byte[] bytes) {
        try {
            return mapper.readValue(bytes, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public J.CompilationUnit read(InputStream input) {
        try {
            return mapper.readValue(input, J.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public J.CompilationUnit read(byte[] bytes) {
        try {
            return mapper.readValue(bytes, J.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class MetadataKeyDeserializer extends KeyDeserializer {
        private final Map<String, Function<String, Metadata>> metadataValueByName = new ConcurrentHashMap<>();

        @Override
        public Metadata deserializeKey(String key, DeserializationContext ctxt) {
            String[] classAndValue = key.split("#");
            return metadataValueByName.computeIfAbsent(classAndValue[0], this::loadMetadataClass).apply(classAndValue[1]);
        }

        private Function<String, Metadata> loadMetadataClass(String clazz) {
            try {
                Class<?> metadataClass = Class.forName(clazz);
                return value -> {
                    try {
                        return (Metadata) metadataClass.getDeclaredMethod("valueOf", String.class).invoke(null, value);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static class MetadataKeySerializer extends StdSerializer<Metadata> {
        public MetadataKeySerializer() {
            super(Metadata.class);
        }

        @Override
        public void serialize(Metadata value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName(value.getClass().getName() + "#" + value.toString());
        }
    }
}
