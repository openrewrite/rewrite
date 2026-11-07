/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.Value;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;

public class UpdateMavenModel<P> extends MavenVisitor<P> {

    private static final String LATEST_PROJECT_POMS = "org.openrewrite.maven.UpdateMavenModel.latestProjectPoms";
    private static final String LATEST_PROJECT_POMS_VERSION = "org.openrewrite.maven.UpdateMavenModel.latestProjectPomsVersion";
    private static final String REFRESHED_MODELS = "org.openrewrite.maven.UpdateMavenModel.refreshedModels";

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        if (!(p instanceof ExecutionContext)) {
            throw new IllegalArgumentException("UpdateMavenModel must be provided an ExecutionContext");
        }
        ExecutionContext ctx = (ExecutionContext) p;

        MavenResolutionResult resolutionResult = getResolutionResult();
        Pom requested = resolutionResult.getPom().getRequested();
        requested.getProperties().clear();

        Optional<Xml.Tag> properties = document.getRoot().getChild("properties");
        if (properties.isPresent()) {
            for (final Xml.Tag propertyTag : properties.get().getChildren()) {
                requested.getProperties().put(propertyTag.getName(),
                        propertyTag.getValue().orElse(""));
            }
        }
        // for backwards compatibility with ASTs that were serialized before userProperties was added
        //noinspection ConstantValue
        if (resolutionResult.getUserProperties() != null) {
            requested.getProperties().putAll(resolutionResult.getUserProperties());
        }

        Optional<Xml.Tag> parent = document.getRoot().getChild("parent");
        if (parent.isPresent()) {
            Parent updatedParent = new Parent(new GroupArtifactVersion(
                    parent.get().getChildValue("groupId").orElse(null),
                    parent.get().getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("GAV must have artifactId")),
                    parent.get().getChildValue("version").orElse(null)
            ), parent.get().getChildValue("relativePath").orElse(null));
            requested = requested.withParent(updatedParent);
        } else if (requested.getParent() != null) {
            requested = requested.withParent(null);
        }

        Optional<Xml.Tag> dependencies = document.getRoot().getChild("dependencies");
        if (dependencies.isPresent()) {
            List<Xml.Tag> eachDependency = dependencies.get().getChildren("dependency");
            List<Dependency> requestedDependencies = new ArrayList<>(eachDependency.size());
            for (Xml.Tag dependency : eachDependency) {
                requestedDependencies.add(Dependency.builder()
                        .gav(new GroupArtifactVersion(
                                dependency.getChildValue("groupId").orElse(null),
                                dependency.getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("Dependency must have artifactId")),
                                dependency.getChildValue("version").orElse(null)
                        ))
                        .classifier(dependency.getChildValue("classifier").orElse(null))
                        .type(dependency.getChildValue("type").orElse(null))
                        .scope(dependency.getChildValue("scope").orElse("compile"))
                        .exclusions(mapExclusions(dependency))
                        .optional(dependency.getChildValue("optional").orElse(null))
                        .build()
                );
            }
            requested = requested.withDependencies(requestedDependencies);
        } else if (!requested.getDependencies().isEmpty()) {
            requested = requested.withDependencies(emptyList());
        }

        Optional<Xml.Tag> dependencyManagement = document.getRoot().getChild("dependencyManagement");
        if (dependencyManagement.isPresent()) {
            dependencies = dependencyManagement.get().getChild("dependencies");
            if (dependencies.isPresent()) {
                List<Xml.Tag> eachDependency = dependencies.get().getChildren("dependency");
                List<ManagedDependency> requestedManagedDependencies = new ArrayList<>(eachDependency.size());
                for (Xml.Tag dependency : eachDependency) {
                    String scope = dependency.getChildValue("scope").orElse(null);
                    GroupArtifactVersion gav = new GroupArtifactVersion(
                            dependency.getChildValue("groupId").orElse(null),
                            dependency.getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("Dependency must have artifactId")),
                            dependency.getChildValue("version").orElse(null)
                    );

                    if ("import".equals(scope)) {
                        requestedManagedDependencies.add(new ManagedDependency.Imported(gav));
                    } else {
                        requestedManagedDependencies.add(new ManagedDependency.Defined(gav, scope,
                                dependency.getChildValue("type").orElse(null),
                                dependency.getChildValue("classifier").orElse(null),
                                mapExclusions(dependency)));
                    }
                }
                requested = requested.withDependencyManagement(requestedManagedDependencies);
            }
        } else if (!requested.getDependencyManagement().isEmpty()) {
            requested = requested.withDependencyManagement(emptyList());
        }

        Optional<Xml.Tag> repos = document.getRoot().getChild("repositories");
        if (repos.isPresent()) {
            requested = requested.withRepositories(repos.get().getChildren("repository").stream().map(t -> new MavenRepository(
                    t.getChildValue("id").orElse(null),
                    t.getChildValue("url").get(),
                    t.getChild("releases").flatMap(s -> s.getChildValue("enabled")).orElse(null),
                    t.getChild("snapshots").flatMap(s -> s.getChildValue("enabled")).orElse(null),
                    null,
                    null,
                    null
            )).collect(toList()));
        } else {
            requested = requested.withRepositories(emptyList());
        }

        try {
            Map<Path, Pom> latest = latestProjectPoms(ctx);
            Map<Path, Pom> projectPoms = withLatest(resolutionResult.getProjectPoms(), latest);
            Path sourcePath = requested.getSourcePath();
            if (sourcePath != null) {
                projectPoms.put(sourcePath, requested);
                latest.put(sourcePath, requested);
                ctx.computeMessageIfAbsent(LATEST_PROJECT_POMS_VERSION, k -> new AtomicInteger()).incrementAndGet();
            }
            MavenResolutionResult updated = updateResult(ctx, resolutionResult.withPom(resolutionResult.getPom().withRequested(requested)),
                    projectPoms, latest);
            markDirtyForAmbiguityRecipes(ctx, document, updated);
            return document.withMarkers(document.getMarkers().computeByType(getResolutionResult(),
                    (original, ignored) -> updated));
        } catch (MavenDownloadingExceptions e) {
            return e.warn(document);
        }
    }

    /**
     * Record that this pom's project — and every aggregated child module's project — has had its
     * resolved dependency set rewritten. Ambiguity-sensitive consumer recipes
     * ({@code OrderImports}, {@code ChangePackage}, {@code ChangeType}) read this registry and fall
     * back to the safe path when their {@code JavaSourceSet} classpath snapshot may be stale.
     */
    private void markDirtyForAmbiguityRecipes(ExecutionContext ctx, Xml.Document document, MavenResolutionResult updated) {
        JavaSourceSet.markDirty(ctx, document);
        markModulesRecursive(ctx, updated);
    }

    private static void markModulesRecursive(ExecutionContext ctx, MavenResolutionResult result) {
        for (MavenResolutionResult module : result.getModules()) {
            String moduleName = module.getPom().getRequested().getName();
            if (moduleName == null || moduleName.isEmpty()) {
                moduleName = module.getPom().getArtifactId();
            }
            if (moduleName != null && !moduleName.isEmpty()) {
                JavaSourceSet.markDirty(ctx, moduleName);
            }
            markModulesRecursive(ctx, module);
        }
    }

    private @Nullable List<GroupArtifact> mapExclusions(Xml.Tag tag) {
        return tag.getChild("exclusions")
                .map(exclusions -> {
                    List<Xml.Tag> eachExclusion = exclusions.getChildren("exclusion");
                    List<GroupArtifact> requestedExclusions = new ArrayList<>(eachExclusion.size());
                    for (Xml.Tag exclusion : eachExclusion) {
                        requestedExclusions.add(new GroupArtifact(
                                exclusion.getChildValue("groupId").orElse(null),
                                exclusion.getChildValue("artifactId").orElse(null)
                        ));
                    }
                    return requestedExclusions;
                })
                .orElse(null);
    }

    private static MavenResolutionResult updateResult(ExecutionContext ctx, MavenResolutionResult resolutionResult,
                                                      Map<Path, Pom> projectPoms, Map<Path, Pom> latest) throws MavenDownloadingExceptions {
        MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx,
                MavenExecutionContextView.view(ctx).effectiveSettings(resolutionResult),
                resolutionResult.getActiveProfiles());

        try {
            ResolvedPom resolved = resolutionResult.getPom().resolve(ctx, downloader);
            return resolutionResult
                    .withPom(resolved)
                    // Re-resolve modules best-effort: a module that is transiently unresolvable mid-recipe keeps
                    // its previous resolution rather than discarding this pom's own valid update.
                    .withModules(ListUtils.map(resolutionResult.getModules(), module -> {
                        MavenResolutionResult latestModule = withLatestRequested(module, latest);
                        try {
                            return updateResult(ctx, latestModule, projectPoms, latest);
                        } catch (MavenDownloadingExceptions e) {
                            return withResolvedPomOnly(latestModule, downloader, ctx, module);
                        }
                    }))
                    .resolveDependencies(downloader, ctx);
        } catch (MavenDownloadingException e) {
            throw MavenDownloadingExceptions.append(null, e);
        }
    }

    // The most recent requested pom of every project pom updated in this run, keyed by source path
    private static Map<Path, Pom> latestProjectPoms(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(LATEST_PROJECT_POMS, k -> new ConcurrentHashMap<>());
    }

    // Only overlay poms this result already knows about, so unrelated projects in the same run don't leak in
    private static Map<Path, Pom> withLatest(Map<Path, Pom> projectPoms, Map<Path, Pom> latest) {
        for (Map.Entry<Path, Pom> entry : projectPoms.entrySet()) {
            Pom newer = latest.get(entry.getKey());
            if (newer != null) {
                entry.setValue(newer);
            }
        }
        return projectPoms;
    }

    private static MavenResolutionResult withLatestRequested(MavenResolutionResult mrr, Map<Path, Pom> latest) {
        Path path = mrr.getPom().getRequested().getSourcePath();
        Pom newer = path == null ? null : latest.get(path);
        return newer == null || newer == mrr.getPom().getRequested() ? mrr : mrr.withPom(mrr.getPom().withRequested(newer));
    }

    // Re-resolve a pom whose marker still holds local ancestors as they were before changes earlier in this run
    static Xml.Document withRefreshedModel(Xml.Document document, ExecutionContext ctx) {
        Map<Path, Pom> latest = ctx.getMessage(LATEST_PROJECT_POMS);
        if (latest == null || latest.isEmpty()) {
            return document;
        }
        MavenResolutionResult mrr = document.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
        if (mrr == null || !hasChangedAncestor(mrr, latest)) {
            return document;
        }
        // Unchanged documents keep their stale marker, so remember the refresh for every later visitor in the run
        int version = ctx.computeMessageIfAbsent(LATEST_PROJECT_POMS_VERSION, k -> new AtomicInteger()).get();
        Map<MavenResolutionResult, Refreshed> cache = ctx.computeMessageIfAbsent(REFRESHED_MODELS,
                k -> synchronizedMap(new IdentityHashMap<>()));
        Refreshed cached = cache.get(mrr);
        if (cached != null && cached.version == version) {
            return document.withMarkers(document.getMarkers().computeByType(mrr, (original, ignored) -> cached.result));
        }
        try {
            Map<Path, Pom> projectPoms = withLatest(mrr.getProjectPoms(), latest);
            MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx,
                    MavenExecutionContextView.view(ctx).effectiveSettings(mrr), mrr.getActiveProfiles());
            MavenResolutionResult base = withLatestRequested(mrr, latest)
                    .withParent(refreshAncestors(mrr.getParent(), latest, downloader, ctx));
            MavenResolutionResult refreshed;
            try {
                refreshed = updateResult(ctx, base, projectPoms, latest);
            } catch (MavenDownloadingExceptions e) {
                refreshed = withResolvedPomOnly(base, downloader, ctx, null);
                if (refreshed == null) {
                    return document;
                }
            }
            MavenResolutionResult result = refreshed;
            cache.put(mrr, new Refreshed(version, result));
            return document.withMarkers(document.getMarkers().computeByType(mrr, (original, ignored) -> result));
        } catch (MavenDownloadingException e) {
            return document;
        }
    }

    // A pom whose dependencies no longer resolve (e.g. mid-migration) still gets its parent's changes
    @Contract("_, _, _, !null -> !null")
    private static @Nullable MavenResolutionResult withResolvedPomOnly(MavenResolutionResult mrr, MavenPomDownloader downloader,
                                                                       ExecutionContext ctx, @Nullable MavenResolutionResult fallback) {
        try {
            return mrr.withPom(mrr.getPom().resolve(ctx, downloader));
        } catch (MavenDownloadingException e) {
            return fallback;
        }
    }

    @Value
    private static class Refreshed {
        int version;
        MavenResolutionResult result;
    }

    private static boolean hasChangedAncestor(MavenResolutionResult mrr, Map<Path, Pom> latest) {
        for (MavenResolutionResult ancestor = mrr.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            Path path = ancestor.getPom().getRequested().getSourcePath();
            Pom newer = path == null ? null : latest.get(path);
            if (newer != null && newer != ancestor.getPom().getRequested()) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable MavenResolutionResult refreshAncestors(@Nullable MavenResolutionResult ancestor, Map<Path, Pom> latest,
                                                                    MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingException {
        if (ancestor == null) {
            return null;
        }
        MavenResolutionResult refreshedParent = refreshAncestors(ancestor.getParent(), latest, downloader, ctx);
        MavenResolutionResult updated = withLatestRequested(ancestor, latest);
        if (updated == ancestor && refreshedParent == ancestor.getParent()) {
            return ancestor;
        }
        return updated.withParent(refreshedParent).withPom(updated.getPom().resolve(ctx, downloader));
    }
}
