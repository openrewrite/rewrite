/*
 * Copyright 2021 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public interface ResolutionEventListener {
    ResolutionEventListener NOOP = new ResolutionEventListener() {
    };

    default void clear() {
    }

    default void downloadMetadata(GroupArtifactVersion gav) {
    }

    default void download(GroupArtifactVersion gav) {
    }

    default void downloadSuccess(ResolvedGroupArtifactVersion gav, @Nullable ResolvedPom containing) {
    }

    /**
     * @param gav           - GAV coordinate of the dependency which failed to download
     * @param attemptedUris - The URIs which were attempted, in the order they were attempted, before resolution was determined to have failed
     * @param containing    - The pom containing the dependency which failed to resolve, if resolution was attempted from such a context
     */
    default void downloadError(GroupArtifactVersion gav, List<String> attemptedUris, @Nullable Pom containing) {
    }

    default void parent(Pom parent, Pom containing) {
    }

    default void dependency(Scope scope, ResolvedDependency resolvedDependency, ResolvedPom containing) {
    }

    default void bomImport(ResolvedGroupArtifactVersion gav, Pom containing) {
    }

    default void property(String key, String value, Pom containing) {
    }

    default void dependencyManagement(ManagedDependency dependencyManagement, Pom containing) {
    }

    default void repository(MavenRepository mavenRepository, @Nullable ResolvedPom containing) {
    }

    default void repositoryAccessFailed(String uri, Throwable e) {
    }

    /**
     * This is called if a previous failure to access a repository was cached and used.
     * Useful to log when a persistent maven cache is used to debug issues where repositories are not being attempted for downloads.
      * @param uri The repository URI which was not attempted to access
     */
    default void repositoryAccessFailedPreviously(String uri) {
    }

}
