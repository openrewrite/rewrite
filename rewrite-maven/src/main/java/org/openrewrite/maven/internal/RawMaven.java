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

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public class RawMaven {
    final Xml.Document document;
    final RawPom pom;

    /**
     * Provided when this pom was downloaded from a remote repository.
     */
    @With
    @Nullable
    MavenRepository repository;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawMaven rawMaven = (RawMaven) o;
        return getSourcePath().equals(rawMaven.getSourcePath());
    }

    public Path getSourcePath() {
        return document.getSourcePath();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourcePath());
    }

    @Override
    public String toString() {
        return "RawMaven{" +
                "pom=" + pom +
                '}';
    }

    public static RawMaven parse(Parser.Input source, @Nullable Path relativeTo, @Nullable String snapshotVersion,
                                 ExecutionContext ctx) {
        Xml.Document document = MavenXmlParser.builder().build()
                .parseInputs(singletonList(source), relativeTo, ctx)
                .iterator().next();

        try {
            RawPom pom = MavenXmlMapper.readMapper().readValue(source.getSource(), RawPom.class);
            if (snapshotVersion != null) {
                pom.setSnapshotVersion(snapshotVersion);
            }
            return new RawMaven(document, pom);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse " + source.getPath(), e);
        }
    }

    public Map<String, String> getActiveProperties(Collection<String> activeProfiles) {
        return pom.getActiveProperties(activeProfiles);
    }

    public List<RawPom.Dependency> getActiveDependencies(Collection<String> activeProfiles) {
        return pom.getActiveDependencies(activeProfiles);
    }

    private static class MavenXmlParser extends XmlParser {
        protected MavenXmlParser(Listener onParse) {
            super(onParse);
        }

        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }
}
