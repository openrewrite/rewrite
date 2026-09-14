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
package org.openrewrite.gradle.marker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Dependency management that the {@code io.spring.dependency-management} plugin layers on top of Gradle's own.
 * A BOM can be imported without appearing in any build script, as the Spring Boot plugin imports
 * {@code spring-boot-dependencies} programmatically, so the plugin's own state is the only place to read it from.
 */
@Value
@With
@Builder
@AllArgsConstructor
public class SpringDependencyManagementPlugin implements Serializable {

    /**
     * The BOMs imported into this project, whether by a {@code mavenBom} entry in a script or programmatically.
     */
    @Builder.Default
    List<GroupArtifactVersion> importedBoms = emptyList();

    /**
     * The properties the imported BOMs define. A project property of the same name overrides the BOM's value.
     */
    @Builder.Default
    Map<String, String> importedProperties = emptyMap();

    /**
     * The versions the imported BOMs manage, keyed by {@code "group:artifact"}. Placeholders are already resolved,
     * so these do not say which property governed which artifact.
     */
    @Builder.Default
    Map<String, String> managedVersions = emptyMap();
}
