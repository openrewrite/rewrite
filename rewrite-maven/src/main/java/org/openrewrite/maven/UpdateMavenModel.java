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

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;

public class UpdateMavenModel<P> extends MavenVisitor<P> {

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        Xml.Document d = document;

        if (!(p instanceof ExecutionContext)) {
            throw new IllegalArgumentException("UpdateMavenModel must be provided an ExecutionContext");
        }
        ExecutionContext ctx = (ExecutionContext) p;

        d = d.withMarkers(d.getMarkers().computeByType(getResolutionResult(), (resolutionResult, ignored) -> {
            Pom requested = resolutionResult.getPom().getRequested();

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
                    requestedDependencies.add(new Dependency(
                            new GroupArtifactVersion(
                                    dependency.getChildValue("groupId").orElse(null),
                                    dependency.getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("Dependency must have artifactId")),
                                    dependency.getChildValue("version").orElse(null)
                            ),
                            dependency.getChildValue("classifier").orElse(null),
                            dependency.getChildValue("type").orElse(null),
                            dependency.getChildValue("scope").orElse("compile"),
                            mapExclusions(dependency),
                            dependency.getChildValue("optional").map(Boolean::parseBoolean).orElse(false)
                    ));
                }
                requested = requested.withDependencies(requestedDependencies);
            } else if (!requested.getDependencies().isEmpty()) {
                requested = requested.withDependencies(Collections.emptyList());
            }

            Optional<Xml.Tag> dependencyManagement = document.getRoot().getChild("dependencyManagement");
            if (dependencyManagement.isPresent()) {
                dependencies = dependencyManagement.get().getChild("dependencies");
                if (dependencies.isPresent()) {
                    List<Xml.Tag> eachDependency = dependencies.get().getChildren("dependency");
                    List<ManagedDependency> requestedManagedDependencies = new ArrayList<>(eachDependency.size());
                    for (Xml.Tag dependency : eachDependency) {
                        String scope = dependency.getChildValue("scope").orElse("compile");
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
                requested = requested.withDependencyManagement(Collections.emptyList());
            }

            return updateResult(ctx, resolutionResult.withPom(resolutionResult.getPom().withRequested(requested)),
                    resolutionResult.getProjectPoms());
        }));

        return d;
    }

    @Nullable
    private List<GroupArtifact> mapExclusions(Xml.Tag tag) {
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

    private MavenResolutionResult updateResult(ExecutionContext ctx, MavenResolutionResult resolutionResult, Map<Path, Pom> projectPoms) {
        MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx);
        ResolvedPom resolved = resolutionResult.getPom().resolve(ctx, downloader);
        return resolutionResult
                .withPom(resolved)
                .withModules(ListUtils.map(resolutionResult.getModules(), module -> updateResult(ctx, module, projectPoms)))
                .resolveDependencies(downloader, ctx);
    }
}
