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
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

        try {
            // Create a copy of projectPoms that we can modify during resolution
            Map<Path, Pom> projectPoms = new java.util.HashMap<>(resolutionResult.getProjectPoms());

            // Update the resolution result in place. We first update the requested pom on the
            // resolutionResult, then call updateResult which will mutate all markers in the
            // module tree in place.
            ResolvedPom pomWithUpdatedRequested = resolutionResult.getPom().withRequested(requested);

            // Temporarily set the pom so updateResult sees the new requested
            resolutionResult.unsafeSet(
                    resolutionResult.getId(),
                    pomWithUpdatedRequested,
                    resolutionResult.getModules(),
                    resolutionResult.getParent(),
                    resolutionResult.getDependencies()
            );

            // Now update the result (this mutates the marker in place)
            updateResult(ctx, resolutionResult, projectPoms);

            // Return the document with updated markers. Even though we mutated the marker
            // in place, we need to trigger change detection by returning a new markers object.
            return document.withMarkers(document.getMarkers().computeByType(resolutionResult,
                    (original, ignored) -> resolutionResult));
        } catch (MavenDownloadingExceptions e) {
            return e.warn(document);
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
        // Update projectPoms with the current requested pom so that child modules
        // will resolve against the updated parent when they look it up
        Pom requested = resolutionResult.getPom().getRequested();
        Path requestedPath = requested.getSourcePath();
        if (requestedPath != null) {
            projectPoms.put(requestedPath, requested);
        }

        MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx, getResolutionResult().getMavenSettings(),
                getResolutionResult().getActiveProfiles());

        AtomicReference<MavenDownloadingExceptions> exceptions = new AtomicReference<>();
        try {
            ResolvedPom resolved = resolutionResult.getPom().resolve(ctx, downloader);

            // Recursively update modules IN PLACE so that child documents' markers
            // (which reference these same module objects) see the updated values
            for (MavenResolutionResult module : resolutionResult.getModules()) {
                try {
                    updateResult(ctx, module, projectPoms);
                } catch (MavenDownloadingExceptions e) {
                    exceptions.set(MavenDownloadingExceptions.append(exceptions.get(), e));
                }
            }

            // Resolve dependencies
            Map<Scope, List<ResolvedDependency>> dependencies = resolutionResult
                    .withPom(resolved)
                    .resolveDependencies(downloader, ctx)
                    .getDependencies();

            // Mutate the existing marker in place instead of creating a new one.
            // This ensures that child documents' parent references (which point to this
            // same marker object) will see the updated values.
            resolutionResult.unsafeSet(
                    resolutionResult.getId(),
                    resolved,
                    resolutionResult.getModules(),
                    resolutionResult.getParent(),
                    dependencies
            );

            if (exceptions.get() != null) {
                throw exceptions.get();
            }
            return resolutionResult;
        } catch (MavenDownloadingExceptions e) {
            throw MavenDownloadingExceptions.append(exceptions.get(), e);
        } catch (MavenDownloadingException e) {
            throw MavenDownloadingExceptions.append(exceptions.get(), e);
        }
    }
}
