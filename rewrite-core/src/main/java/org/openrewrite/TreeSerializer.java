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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class TreeSerializer<S extends SourceFile> {
    private final TypeReference<S> sourceType = new TypeReference<S>() {
    };

    private final TypeReference<List<S>> sourceListType = new TypeReference<List<S>>() {
    };

    private final ObjectMapper mapper;

    public TreeSerializer() {
        SimpleModule markerModule = new SimpleModule();

        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);

        this.mapper = new ObjectMapper(f)
                .registerModule(markerModule)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
}
