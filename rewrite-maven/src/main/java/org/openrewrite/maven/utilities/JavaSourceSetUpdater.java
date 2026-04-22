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
package org.openrewrite.maven.utilities;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Updates {@link JavaSourceSet} markers to reflect dependency changes made by recipes.
 * <p>
 * When dependency-modifying recipes (ChangeDependency, AddDependency) change a project's
 * dependencies, the JavaSourceSet marker on Java source files becomes stale — it still
 * reflects the pre-change classpath. This utility downloads the new dependency's JAR,
 * scans it for type names, and updates the JavaSourceSet accordingly.
 */
public class JavaSourceSetUpdater {
    static final String TYPE_CACHE_KEY = "org.openrewrite.maven.jarTypeCache";

    private final MavenArtifactDownloader downloader;
    private final Map<String, List<JavaType.FullyQualified>> typeCache;

    @SuppressWarnings("unchecked")
    public JavaSourceSetUpdater(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        // Download failures are not fatal for JavaSourceSet updates — degrade gracefully
        Consumer<Throwable> onError = t -> {};
        this.downloader = new MavenArtifactDownloader(mctx.getArtifactCache(), mctx.getSettings(), httpSender, onError);
        this.typeCache = (Map<String, List<JavaType.FullyQualified>>) ctx.getMessages()
                .computeIfAbsent(TYPE_CACHE_KEY, k -> new ConcurrentHashMap<>());
    }

    /**
     * Update a JavaSourceSet to reflect a dependency coordinate change.
     * Removes types from the old dependency and adds types from the new dependency.
     */
    public JavaSourceSet changeDependency(JavaSourceSet sourceSet,
                                          ResolvedDependency oldDep,
                                          ResolvedDependency newDep) {
        String oldGavKey = gavKey(oldDep);
        String newGavKey = gavKey(newDep);
        if (!sourceSet.getGavToTypes().containsKey(oldGavKey)) {
            // Old dependency not tracked. If gavToTypes is non-empty, other dependencies
            // are being tracked but this one wasn't — nothing to change. If gavToTypes is
            // empty, we're in initial population mode (first call on this source set).
            if (!sourceSet.getGavToTypes().isEmpty() || sourceSet.getGavToTypes().containsKey(newGavKey)) {
                return sourceSet;
            }
        }
        sourceSet = sourceSet.removeTypesForGav(oldGavKey);
        List<JavaType.FullyQualified> newTypes = downloadAndScanTypes(newDep);
        if (!newTypes.isEmpty()) {
            sourceSet = sourceSet.addTypesForGav(newGavKey, newTypes);
        }
        return sourceSet;
    }

    /**
     * Update a JavaSourceSet to reflect a newly added dependency.
     * Tries each repository in order until the JAR is successfully downloaded.
     */
    public JavaSourceSet addDependency(JavaSourceSet sourceSet,
                                       String groupId, String artifactId, String version,
                                       List<MavenRepository> repositories) {
        String key = groupId + ":" + artifactId + ":" + version;
        if (sourceSet.getGavToTypes().containsKey(key)) {
            return sourceSet;
        }
        ResolvedGroupArtifactVersion gav = new ResolvedGroupArtifactVersion(
                null, groupId, artifactId, version, null);
        Dependency requested = Dependency.builder()
                .gav(new GroupArtifactVersion(groupId, artifactId, version))
                .build();
        for (MavenRepository repo : repositories) {
            ResolvedDependency dep = ResolvedDependency.builder()
                    .gav(gav)
                    .repository(repo)
                    .requested(requested)
                    .build();
            List<JavaType.FullyQualified> newTypes = downloadAndScanTypes(dep);
            if (!newTypes.isEmpty()) {
                return sourceSet.addTypesForGav(key, newTypes);
            }
        }
        return sourceSet;
    }

    private List<JavaType.FullyQualified> downloadAndScanTypes(ResolvedDependency dep) {
        String key = gavKey(dep);
        List<JavaType.FullyQualified> cached = typeCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Path jarPath = downloader.downloadArtifact(dep);
            if (jarPath == null) {
                return Collections.emptyList();
            }
            List<JavaType.FullyQualified> types = JavaSourceSet.typesFromPath(jarPath, null);
            // Only cache positive results: addDependency tries each repository in order until
            // one succeeds, so caching an empty result would short-circuit later repos.
            if (!types.isEmpty()) {
                typeCache.put(key, types);
            }
            return types;
        } catch (Exception e) {
            // Graceful degradation: if download fails, return empty list
            return Collections.emptyList();
        }
    }

    private static String gavKey(ResolvedDependency dep) {
        return dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
    }
}
