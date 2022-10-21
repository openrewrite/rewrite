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

import org.openrewrite.internal.lang.Nullable;

public interface ResolutionEventListener {
    ResolutionEventListener NOOP = new ResolutionEventListener() {
        @Override
        public void downloadError(GroupArtifactVersion gav, Pom containing) {
        }

        @Override
        public void parent(Pom parent, Pom containing) {
        }

        @Override
        public void dependency(Scope scope, ResolvedDependency resolvedDependency, ResolvedPom containing) {
        }

        @Override
        public void bomImport(ResolvedGroupArtifactVersion gav, Pom containing) {
        }

        @Override
        public void property(String key, String value, Pom containing) {
        }

        @Override
        public void dependencyManagement(ManagedDependency dependencyManagement, Pom containing) {
        }

        @Override
        public void clear() {
        }

        @Override
        public void downloadSuccess(ResolvedGroupArtifactVersion gav, @Nullable ResolvedPom containing) {
        }
    };

    void clear();

    default void downloadSuccess(ResolvedGroupArtifactVersion gav, @Nullable ResolvedPom containing) {
    }

    void downloadError(GroupArtifactVersion gav, Pom containing);
    void parent(Pom parent, Pom containing);
    void dependency(Scope scope, ResolvedDependency resolvedDependency, ResolvedPom containing);
    void bomImport(ResolvedGroupArtifactVersion gav, Pom containing);
    void property(String key, String value, Pom containing);
    void dependencyManagement(ManagedDependency dependencyManagement, Pom containing);
}
