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
package org.openrewrite.maven.internal.parity;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures {@link ResolutionEventListener} events as {@code type:key} strings. The snapshot
 * aggregates them into a multiset; ordering is deliberately not part of the parity contract.
 */
public class RecordingResolutionListener implements ResolutionEventListener {

    @Value
    public static class Event {
        String type;
        String key;

        @Override
        public String toString() {
            return type + ":" + key;
        }
    }

    private final List<Event> events = new ArrayList<>();

    public List<Event> getEvents() {
        return events;
    }

    private void record(String type, String key) {
        events.add(new Event(type, key));
    }

    private static String gav(GroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    private static String gav(ResolvedGroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    private static String gav(@Nullable Pom pom) {
        return pom == null ? "null" : gav(pom.getGav());
    }

    private static String gav(@Nullable ResolvedPom pom) {
        return pom == null ? "null" : gav(pom.getGav());
    }

    @Override
    public void clear() {
        record("clear", "");
    }

    @Override
    public void downloadMetadata(GroupArtifactVersion gav) {
        record("downloadMetadata", gav(gav));
    }

    @Override
    public void download(GroupArtifactVersion gav) {
        record("download", gav(gav));
    }

    @Override
    public void downloadSuccess(ResolvedGroupArtifactVersion gav, @Nullable ResolvedPom containing) {
        record("downloadSuccess", gav(gav) + "<-" + gav(containing));
    }

    @Override
    public void downloadError(GroupArtifactVersion gav, List<String> attemptedUris, @Nullable Pom containing) {
        record("downloadError", gav(gav) + "<-" + gav(containing) + ":" + String.join(",", attemptedUris));
    }

    @Override
    public void parent(Pom parent, Pom containing) {
        record("parent", gav(parent) + "<-" + gav(containing));
    }

    @Override
    public void dependency(Scope scope, ResolvedDependency resolvedDependency, ResolvedPom containing) {
        record("dependency", scope + ":" + gav(resolvedDependency.getGav()) + "<-" + gav(containing));
    }

    @Override
    public void bomImport(ResolvedGroupArtifactVersion gav, Pom containing) {
        record("bomImport", gav(gav) + "<-" + gav(containing));
    }

    @Override
    public void property(String key, String value, Pom containing) {
        record("property", key + "=" + value + "<-" + gav(containing));
    }

    @Override
    public void dependencyManagement(ManagedDependency dependencyManagement, Pom containing) {
        record("dependencyManagement", dependencyManagement.getGroupId() + ":" + dependencyManagement.getArtifactId() +
                ":" + dependencyManagement.getVersion() + "<-" + gav(containing));
    }

    @Override
    public void repository(MavenRepository mavenRepository, @Nullable ResolvedPom containing) {
        record("repository", mavenRepository.getUri() + "<-" + gav(containing));
    }

    @Override
    public void repositoryAccessFailed(String uri, Throwable e) {
        record("repositoryAccessFailed", uri + ":" + e.getClass().getSimpleName());
    }

    @Override
    public void repositoryAccessFailedPreviously(String uri) {
        record("repositoryAccessFailedPreviously", uri);
    }
}
