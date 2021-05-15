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
package org.openrewrite.maven.utilities;

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import static org.openrewrite.Tree.randomId;

public class MavenArtifactHelper {

    private static final MavenRepository SUPER_POM_REPOSITORY = new MavenRepository("central",
            URI.create("https://repo.maven.apache.org/maven2"), true, false, null, null);

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenPomCache mavenPomCache) {
        List<MavenRepository> repositories = new ArrayList<>();
        repositories.add(SUPER_POM_REPOSITORY);
        return downloadArtifactAndDependencies(groupId, artifactId, version, ctx, mavenPomCache, repositories);
    }

    /**
     * Download artifact specified by GAV from specified maven repositories, plus all runtime dependencies. Maven central
     * will be added as a repository by default.
     *
     * @param groupId Group ID
     * @param artifactId Artifact ID
     * @param version Version
     * @param ctx Execution context
     * @param mavenPomCache Pom download cache
     * @param repositories Maven repositories to download from
     * @return List of paths to downloaded artifacts
     */
    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenPomCache mavenPomCache, List<MavenRepository> repositories) {
        return downloadArtifactAndDependenciesInternal(groupId, artifactId, version, ctx, mavenPomCache, repositories, ReadOnlyLocalMavenArtifactCache.mavenLocal().orElse(
                new LocalMavenArtifactCache(Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "artifacts"))
        ));
    }

    /**
     * Download artifact specified by GAV from specified maven repositories, plus all runtime dependencies. Maven central
     * will be added as a repository by default.
     *
     * @param groupId Group ID
     * @param artifactId Artifact ID
     * @param version Version
     * @param ctx Execution context
     * @param mavenPomCache Pom download cache
     * @param repositories Maven repositories to download from
     * @param path Path of local Maven cache
     * @return List of paths to downloaded artifacts
     */
    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenPomCache mavenPomCache, List<MavenRepository> repositories, Path path) {
        return downloadArtifactAndDependenciesInternal(groupId, artifactId, version, ctx, mavenPomCache, repositories, new LocalMavenArtifactCache(path));
    }

    private static List<Path> downloadArtifactAndDependenciesInternal(String groupId, String artifactId, String version, ExecutionContext ctx, MavenPomCache mavenPomCache, List<MavenRepository> repositories, MavenArtifactCache mavenArtifactCache) {
        MavenPomDownloader mavenPomDownloader = new MavenPomDownloader(mavenPomCache,
                Collections.emptyMap(), ctx);
        RawMaven rawMaven = mavenPomDownloader.download(groupId, artifactId, version, null, null,
                repositories, ctx);
        if (rawMaven == null) {
            return Collections.emptyList();
        }
        MavenExecutionContextView mavenCtx = new MavenExecutionContextView(ctx);
        mavenCtx.setRepositories(repositories);
        Xml.Document xml = new RawMavenResolver(mavenPomDownloader, Collections.emptyList(), true, ctx, null).resolve(rawMaven, new HashMap<>());
        if (xml == null) {
            return Collections.emptyList();
        }
        Maven maven = new Maven(xml);
        Pom pom = maven.getModel();
        MavenArtifactDownloader mavenArtifactDownloader = new MavenArtifactDownloader(mavenArtifactCache, null, ctx.getOnError());
        List<Path> artifactPaths = new ArrayList<>();
        Path downloadedArtifact = null;
        for (MavenRepository pomRepository : pom.getRepositories()) {
            downloadedArtifact = mavenArtifactDownloader.downloadArtifact(new Pom.Dependency(pomRepository, Scope.Compile, null, null, false, new Pom(
                    randomId(),
                    pom.getGroupId(),
                    pom.getArtifactId(),
                    pom.getVersion(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList(),
                    new Pom.DependencyManagement(Collections.emptyList()),
                    Collections.emptyList(),
                    repositories,
                    Collections.emptyMap(),
                    Collections.emptyMap()
            ), null, pom.getSnapshotVersion(), Collections.emptySet()));
            if (downloadedArtifact != null) {
                break;
            }
        }
        if (downloadedArtifact == null) {
            throw new UncheckedIOException(new FileNotFoundException("Could not find " + pom.toString() + " in any repositories."));
        }
        artifactPaths.add(downloadedArtifact);
        for (Pom.Dependency dependency : collectDependencies(maven.getModel().getDependencies(),
                d -> !d.isOptional() && d.getScope() != Scope.Test)) {
            artifactPaths.add(mavenArtifactDownloader.downloadArtifact(dependency));
        }
        return artifactPaths;
    }

    /**
     * Collect all downstream dependencies of the specified set of dependencies that match the specified filter
     *
     * @param dependencies Original set of maven dependencies to collect dependencies for
     * @param dependencyFilter Downstream dependency filter, which can be used to limit downstream dependency scopes, for example
     * @return List of downstream dependencies
     */
    public static List<Pom.Dependency> collectDependencies(Collection<Pom.Dependency> dependencies, Predicate<Pom.Dependency> dependencyFilter) {
        return new ArrayList<>(traverseDependencies(dependencies, new LinkedHashMap<>(), dependencyFilter).values());
    }

    private static Map<DependencyKey, Pom.Dependency> traverseDependencies(
            Collection<Pom.Dependency> dependencies,
            final Map<DependencyKey, Pom.Dependency> dependencyMap,
            Predicate<Pom.Dependency> dependencyFilter) {
        if (dependencies == null) {
            return dependencyMap;
        }
        dependencies.stream()
                .filter(dependencyFilter)
                .forEach(d -> {
                    DependencyKey key = getDependencyKey(d);
                    if (!dependencyMap.containsKey(key)) {
                        dependencyMap.put(key, d);
                        traverseDependencies(d.getModel().getDependencies(), dependencyMap, dependencyFilter);
                    }
                });
        return dependencyMap;
    }

    private static DependencyKey getDependencyKey(Pom.Dependency dependency) {
        return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                dependency.getExclusions());
    }

    @Value
    static class DependencyKey {
        String groupId;
        String artifactId;
        String version;
        Set<GroupArtifact> exclusions;
    }
}
