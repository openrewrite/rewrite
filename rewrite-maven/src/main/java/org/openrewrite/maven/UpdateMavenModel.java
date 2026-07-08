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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.engine.PomXmlRegistry;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class UpdateMavenModel<P> extends MavenVisitor<P> {

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

        // Re-read each profile's <properties> from the document so a recipe that edits a profile property (e.g.
        // ChangePropertyValue in a <profile>) doesn't leave the re-resolved model's profile properties stale.
        List<Xml.Tag> profileTags = document.getRoot().getChild("profiles")
                .map(ps -> ps.getChildren("profile")).orElse(emptyList());
        if (!requested.getProfiles().isEmpty() && !profileTags.isEmpty()) {
            requested = requested.withProfiles(ListUtils.map(requested.getProfiles(), profile -> {
                for (Xml.Tag profileTag : profileTags) {
                    if (Objects.equals(profileTag.getChildValue("id").orElse(null), profile.getId())) {
                        Map<String, String> profileProperties = new LinkedHashMap<>();
                        profileTag.getChild("properties").ifPresent(pt -> {
                            for (Xml.Tag propertyTag : pt.getChildren()) {
                                profileProperties.put(propertyTag.getName(), propertyTag.getValue().orElse(""));
                            }
                        });
                        return profile.withProperties(profileProperties);
                    }
                }
                return profile;
            }));
        }

        try {
            Map<Path, Pom> projectPoms = resolutionResult.getProjectPoms();
            Path sourcePath = requested.getSourcePath();
            if (sourcePath != null) {
                projectPoms.put(sourcePath, requested);
            }
            // XML-first re-resolution: feed the engine the mutated document's current bytes and bump the reactor epoch
            // so any GAV-keyed engine cache re-reads them (DESIGN §5.5). Inert unless the maven/shadow engine is active.
            PomXmlRegistry.put(ctx, requested, document.printAll().getBytes(StandardCharsets.UTF_8));
            PomXmlRegistry.setInjectedProperties(ctx, resolutionResult.getUserProperties());
            PomXmlRegistry.bumpEpoch(ctx);
            MavenResolutionResult updated = updateResult(ctx, resolutionResult.withPom(resolutionResult.getPom().withRequested(requested)),
                    projectPoms);
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

    private MavenResolutionResult updateResult(ExecutionContext ctx, MavenResolutionResult resolutionResult, Map<Path, Pom> projectPoms) throws MavenDownloadingExceptions {
        MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx, getResolutionResult().getMavenSettings(),
                getResolutionResult().getActiveProfiles());

        try {
            ResolvedPom resolved = resolutionResult.getPom().resolve(ctx, downloader);
            return resolutionResult
                    .withPom(resolved)
                    // Re-resolve modules best-effort: a module that is transiently unresolvable mid-recipe keeps
                    // its previous resolution rather than discarding this pom's own valid update.
                    .withModules(ListUtils.map(resolutionResult.getModules(), module -> {
                        try {
                            return updateResult(ctx, module, projectPoms);
                        } catch (MavenDownloadingExceptions e) {
                            return module;
                        }
                    }))
                    .resolveDependencies(downloader, ctx);
        } catch (MavenDownloadingException e) {
            throw MavenDownloadingExceptions.append(null, e);
        }
    }
}
