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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A minified, serializable form of {@link org.apache.maven.model.Model}
 * for inclusion as a data element of {@link Maven.Pom}.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class MavenModel {
    @With
    @Nullable
    MavenModel parent;

    @EqualsAndHashCode.Include
    ModuleVersionId moduleVersion;

    @Nullable
    @With
    DependencyManagement dependencyManagement;

    @With
    List<Dependency> dependencies;

    @With
    Map<String, String> properties;

    /**
     * Modules inheriting from the POM this model represents. To cut the
     * object cycle, the parent of all of these modules will be null.
     */
    Collection<MavenModel> inheriting;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        @With
        ModuleVersionId moduleVersion;

        /**
         * The version written into the POM file, which may be a dynamic constraint or property reference.
         */
        @With
        String requestedVersion;

        @With
        String scope;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ModuleVersionId {
        @With
        String groupId;

        @With
        String artifactId;

        @With
        String version;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        @With
        List<Dependency> dependencies;
    }

    public String valueOf(String value) {
        value = value.trim();
        return value.startsWith("${") && value.endsWith("}") ?
                properties.get(value.substring(2, value.length() - 1)) :
                value;
    }
}
