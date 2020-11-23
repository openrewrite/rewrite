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
package org.openrewrite.maven.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.xml.XmlParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class MavenMetadata {
    private static final ObjectMapper xmlMapper = new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final XmlParser xmlParser = new XmlParser() {
        @Override
        public boolean accept(URI path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    };

    public static final MavenMetadata EMPTY = new MavenMetadata(new MavenMetadata.Versioning(emptyList()));

    Versioning versioning;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Versioning {
        Collection<String> versions;
    }

    public static MavenMetadata parse(byte[] document) {
        try {
            return xmlMapper.readValue(document, MavenMetadata.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
