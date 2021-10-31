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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public class TreeSerializer<S extends Tree> {
    private final TypeReference<S> sourceType = new TypeReference<S>() {
    };

    private final TypeReference<List<S>> sourceListType = new TypeReference<List<S>>() {
    };

    private final ObjectMapper mapper;

    public TreeSerializer() {
        JsonMapper.Builder mBuilder;
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        mBuilder = JsonMapper.builder(f);

        // to be able to construct classes that have @Data and a single field
        // see https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0
        ObjectMapper m = mBuilder.constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build()
                .registerModule(new RelativePathModule())
                .registerModule(new ParameterNamesModule())
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.mapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    public void write(Iterable<S> sources, OutputStream out) {
        try {
            mapper.writeValue(out, sources);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(Iterable<S> sources) {
        try {
            return mapper.writeValueAsBytes(sources);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(S source, OutputStream out) {
        try {
            mapper.writeValue(out, source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(S source) {
        try {
            return mapper.writeValueAsBytes(source);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<S> readList(InputStream input) {
        try {
            return mapper.readValue(input, sourceListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<S> readList(byte[] bytes) {
        try {
            return mapper.readValue(bytes, sourceListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public S read(InputStream input) {
        try {
            return mapper.readValue(input, sourceType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public S read(byte[] bytes) {
        try {
            return mapper.readValue(bytes, sourceType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class RelativePathModule extends SimpleModule {
        public RelativePathModule() {
            addSerializer(new RelativePathSerializer());
        }

        private static class RelativePathSerializer extends StdSerializer<Path> {
            protected RelativePathSerializer() {
                super(Path.class);
            }

            @Override
            public void serialize(Path value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value.toString());
            }
        }
    }
}
