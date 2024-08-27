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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.xml.XmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class MavenMetadata {
    private static final XmlParser xmlParser = new XmlParser() {
        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    };

    public static final MavenMetadata EMPTY = new MavenMetadata(new MavenMetadata.Versioning(emptyList(), emptyList(), null));

    Versioning versioning;

    public MavenMetadata(Versioning versioning) {
        this.versioning = versioning;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Getter
    public static class Versioning {

        List<String> versions;

        @Nullable
        List<SnapshotVersion> snapshotVersions;

        @Nullable
        Snapshot snapshot;

        public Versioning(
                @JacksonXmlElementWrapper(localName = "versions") List<String> versions,
                @JacksonXmlElementWrapper(localName = "snapshotVersions") @Nullable List<SnapshotVersion> snapshotVersions,
                @Nullable Snapshot snapshot) {
            this.versions = versions;
            this.snapshotVersions = snapshotVersions;
            this.snapshot = snapshot;
        }
    }

    public static MavenMetadata parse(InputStream document) {
        try {
            return MavenXmlMapper.readMapper().readValue(document, MavenMetadata.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static @Nullable MavenMetadata parse(byte[] document) throws IOException {
        MavenMetadata metadata = MavenXmlMapper.readMapper().readValue(document, MavenMetadata.class);
        if (metadata != null && metadata.getVersioning() != null && metadata.getVersioning().getVersions() == null) {
            return new MavenMetadata(new Versioning(emptyList(), metadata.getVersioning().getSnapshotVersions(), metadata.getVersioning().getSnapshot()));
        }
        return metadata;
    }

    @Value
    public static class Snapshot {
        String timestamp;
        String buildNumber;
    }

    @Value
    public static class SnapshotVersion {
        String extension;
        String value;
        String updated;
    }
}
