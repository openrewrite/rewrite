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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import static org.openrewrite.internal.StringUtils.matchesGlob;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    private final MavenArtifactDownloader downloader;

    public JavaSourceSetUpdater(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        // Use a lenient error handler: download failures are not fatal for JavaSourceSet updates
        Consumer<Throwable> onError = t -> {};
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("rewrite-artifact-cache");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MavenArtifactCache cache = new LocalMavenArtifactCache(tempDir);
        this.downloader = new MavenArtifactDownloader(cache, mctx.getSettings(), httpSender, onError);
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
        // Idempotent: if already changed (old absent, new present), skip
        if (!sourceSet.getGavToTypes().containsKey(oldGavKey) &&
            sourceSet.getGavToTypes().containsKey(newGavKey)) {
            return sourceSet;
        }
        sourceSet = removeTypesForGav(sourceSet, oldGavKey);
        List<JavaType.FullyQualified> newTypes = downloadAndScanTypes(newDep);
        if (!newTypes.isEmpty()) {
            sourceSet = addTypesForGav(sourceSet, newGavKey, newTypes);
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
                return addTypesForGav(sourceSet, key, newTypes);
            }
        }
        return sourceSet;
    }

    /**
     * Update a JavaSourceSet to reflect a removed dependency.
     */
    public JavaSourceSet removeDependency(JavaSourceSet sourceSet,
                                          ResolvedDependency dep) {
        return removeTypesForGav(sourceSet, gavKey(dep));
    }

    /**
     * Remove types from a JavaSourceSet whose GAV keys match the given groupId and artifactId patterns.
     * Does not require downloading; works entirely from existing gavToTypes data.
     */
    public static JavaSourceSet removeTypesMatching(JavaSourceSet sourceSet,
                                                     String groupIdPattern,
                                                     String artifactIdPattern) {
        Map<String, List<JavaType.FullyQualified>> gavToTypes = sourceSet.getGavToTypes();
        if (gavToTypes.isEmpty()) {
            return sourceSet;
        }
        List<String> keysToRemove = new ArrayList<>();
        for (String key : gavToTypes.keySet()) {
            String[] parts = key.split(":");
            if (parts.length >= 2 &&
                matchesGlob(parts[0], groupIdPattern) &&
                matchesGlob(parts[1], artifactIdPattern)) {
                keysToRemove.add(key);
            }
        }
        if (keysToRemove.isEmpty()) {
            return sourceSet;
        }
        Set<JavaType.FullyQualified> typesToRemove = new HashSet<>();
        for (String key : keysToRemove) {
            typesToRemove.addAll(gavToTypes.get(key));
        }
        List<JavaType.FullyQualified> newClasspath = new ArrayList<>(sourceSet.getClasspath().size());
        for (JavaType.FullyQualified type : sourceSet.getClasspath()) {
            if (!typesToRemove.contains(type)) {
                newClasspath.add(type);
            }
        }
        Map<String, List<JavaType.FullyQualified>> newGavToTypes = new LinkedHashMap<>(gavToTypes);
        for (String key : keysToRemove) {
            newGavToTypes.remove(key);
        }
        return sourceSet.withClasspath(newClasspath).withGavToTypes(newGavToTypes);
    }

    private List<JavaType.FullyQualified> downloadAndScanTypes(ResolvedDependency dep) {
        try {
            Path jarPath = downloader.downloadArtifact(dep);
            if (jarPath == null) {
                return Collections.emptyList();
            }
            return JavaSourceSet.typesFromPath(jarPath, null);
        } catch (Exception e) {
            // Graceful degradation: if download fails, return empty list
            return Collections.emptyList();
        }
    }

    private JavaSourceSet removeTypesForGav(JavaSourceSet sourceSet, String gavKey) {
        Map<String, List<JavaType.FullyQualified>> gavToTypes = sourceSet.getGavToTypes();
        if (gavToTypes.isEmpty() || !gavToTypes.containsKey(gavKey)) {
            return sourceSet;
        }
        List<JavaType.FullyQualified> oldTypes = gavToTypes.get(gavKey);
        Set<JavaType.FullyQualified> oldTypesSet = new HashSet<>(oldTypes);

        List<JavaType.FullyQualified> newClasspath = new ArrayList<>(sourceSet.getClasspath().size());
        for (JavaType.FullyQualified type : sourceSet.getClasspath()) {
            if (!oldTypesSet.contains(type)) {
                newClasspath.add(type);
            }
        }

        Map<String, List<JavaType.FullyQualified>> newGavToTypes = new LinkedHashMap<>(gavToTypes);
        newGavToTypes.remove(gavKey);

        return sourceSet.withClasspath(newClasspath).withGavToTypes(newGavToTypes);
    }

    private JavaSourceSet addTypesForGav(JavaSourceSet sourceSet, String gavKey,
                                         List<JavaType.FullyQualified> types) {
        List<JavaType.FullyQualified> newClasspath = new ArrayList<>(sourceSet.getClasspath());
        newClasspath.addAll(types);

        Map<String, List<JavaType.FullyQualified>> newGavToTypes = new LinkedHashMap<>(sourceSet.getGavToTypes());
        newGavToTypes.put(gavKey, types);

        return sourceSet.withClasspath(newClasspath).withGavToTypes(newGavToTypes);
    }

    private static String gavKey(ResolvedDependency dep) {
        return dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
    }
}
