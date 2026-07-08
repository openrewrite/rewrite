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
package org.openrewrite.maven.parity.corpus;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
public class CorpusManifest {
    List<Entry> entries;

    public static CorpusManifest load(Path yaml) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        try {
            return mapper.readValue(yaml.toFile(), CorpusManifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Data
    public static class Entry {
        String name;
        String kind;
        Source source;

        @Nullable
        List<String> groundTruth;

        @Nullable
        String notes;

        boolean deferred;
        boolean fetchOnly;

        public List<String> getGroundTruth() {
            return groundTruth == null ? emptyList() : groundTruth;
        }

        public boolean isPom() {
            return "pom".equals(kind);
        }

        public boolean isReactor() {
            return "reactor".equals(kind);
        }
    }

    @Data
    public static class Source {
        @Nullable
        String gav;

        @Nullable
        Git git;
    }

    @Data
    public static class Git {
        String url;
        String tag;
    }
}
