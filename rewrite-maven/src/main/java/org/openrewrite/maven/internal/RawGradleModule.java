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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.ObjectMappers.propertyBasedMapper;

/**
 * A value object deserialized directly from POM XML
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@XmlRootElement(name = "project")
@SuppressWarnings("unused")
public class RawGradleModule {

    private static final ObjectMapper mapper = propertyBasedMapper(RawGradleModule.class.getClassLoader());

    @Nullable
    String formatVersion;

    @Nullable
    Component component;

    @Nullable
    List<Variant> variants;

    public static RawGradleModule parse(InputStream inputStream) {
        try {
            return mapper.readValue(inputStream, RawGradleModule.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse module", e);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Component {
        String groupId;
        String module;

        @Nullable
        String version;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Variant {
        String name;

        @Nullable
        List<Dependency> dependencies;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        String group;
        String module;

        @Nullable
        Version version;

        @Nullable
        DependencyAttributes attributes;

        GroupArtifactVersion asGav() {
            return new GroupArtifactVersion(group, module, version == null ? null : version.getRequires());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Version {
        String requires;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyAttributes {
        @JsonProperty("org.gradle.category")
        @Nullable
        String category;
    }

    public List<org.openrewrite.maven.tree.Dependency> getDependencies(String variant, String... categories) {
        return variants == null ? emptyList() : variants.stream()
                .filter(v -> v.getName().equals(variant))
                .map(Variant::getDependencies)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(d -> categories.length == 0 || (d.getAttributes() != null && d.getAttributes().getCategory() != null && Arrays.stream(categories).anyMatch(cat -> d.getAttributes().getCategory().equalsIgnoreCase(cat))))
                .map(Dependency::asGav)
                .map(gav -> org.openrewrite.maven.tree.Dependency.builder()
                        .gav(gav)
                        .build())
                .collect(toList());
    }
}
