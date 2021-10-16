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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.DependencyManagementDependency;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRedundantDependencyVersions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependency versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions when a parent pom's dependencyManagement " +
                "specifies the same explicit version.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindManagedDependencyVersionVisitor();
    }

    private static class FindManagedDependencyVersionVisitor extends MavenVisitor {
        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Maven newMaven = super.visitMaven(maven, ctx);
            List<Pom.Dependency> dependencies = new ArrayList<>(maven.getModel().getDependencies());
            for (Pom.Dependency dependency : dependencies) {
                DependencyManagementDependency.Defined managedVersion = findManagedVersion(maven.getModel(), dependency);
                Scope scope = managedVersion != null ? managedVersion.getScope() : null;

                if (managedVersion != null && managedVersion.getVersion() != null
                        && dependency.getRequestedVersion() != null
                        && dependency.getVersion().equals(dependency.getRequestedVersion())
                        && managedVersion.getVersion().equals(dependency.getRequestedVersion())
                        && scopeMatches(scope, dependency.getScope())) {
                    doAfterVisit(new RemoveVersionVisitor(maven.getModel().getArtifactId(), dependency.getGroupId(), dependency.getArtifactId(), managedVersion));
                }
            }

            return newMaven;
        }

        /**
         * Because no defined scope defaults to "compile", this utility will coalesce null scopes to compile.
         */
        private static Scope scopeOrDefault(@Nullable Scope scope) {
            if (scope == null) {
                return Scope.Compile;
            }
            return scope;
        }

        /**
         * Check whether two scopes match. A null scope counts as whatever the default scope is (Compile)
         */
        private static boolean scopeMatches(@Nullable Scope firstScope, @Nullable Scope secondScope) {
            return Objects.equals(scopeOrDefault(firstScope), scopeOrDefault(secondScope));
        }

        /**
         * Given a Pom and a specific dependency, find the nearest ancestor that manages this dependency
         * and return that dependency's explicit version.
         *
         * It searches first in this pom's dependencyManagement, then recurses on the parent. As soon
         * as we find a reference to the dependency, we return that version.
         *
         * @param pom The current pom to search and then traverse upward
         * @param dependency The dependency for which we are searching for a managed version
         * @return Returns the managed dependency version. Returns null if it is never found in any dependencyManagement block.
         */
        @Nullable
        private DependencyManagementDependency.Defined findManagedVersion(Pom pom, Pom.Dependency dependency) {
            if (pom.getDependencyManagement() != null) {
                Collection<DependencyManagementDependency> managedDependencies = pom.getDependencyManagement().getDependencies();
                for (DependencyManagementDependency managedDependency : managedDependencies) {
                    if (!(managedDependency instanceof DependencyManagementDependency.Defined)) {
                        continue;
                    }
                    DependencyManagementDependency.Defined definedDependency = (DependencyManagementDependency.Defined) managedDependency;
                    if (dependency.getGroupId().equals(definedDependency.getGroupId())
                            && dependency.getArtifactId().equals(definedDependency.getArtifactId())) {
                        if (definedDependency.getVersion() != null) {
                            return definedDependency;
                        }
                    }
                }
            }

            if (pom.getParent() == null) {
                return null;
            }

            return findManagedVersion(pom.getParent(), dependency);
        }
    }

    /**
     * This visitor, given an artifact id from a module (active module), a groupId and artifactId of a dependency,
     * and the managed version (i.e. nearest version in a dependencyManagement block), remove any
     * "version" tags of the dependency on the active module's dependencies section.
     */
    private static class RemoveVersionVisitor extends MavenVisitor {

        final private String moduleArtifactId;
        final private String groupId;
        final private String artifactId;
        final private DependencyManagementDependency.Defined managedVersion;

        public RemoveVersionVisitor(String moduleArtifactId, String groupId, String artifactId, DependencyManagementDependency.Defined managedVersion) {
            this.moduleArtifactId = moduleArtifactId;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.managedVersion = managedVersion;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (this.model.getArtifactId().equals(moduleArtifactId)
                    && isDependencyTag(groupId, artifactId)
                    && tag.getContent() != null) {
                List<? extends Content> contents = tag.getContent();
                Xml.Tag versionTag = null;
                Scope scope = null;
                for (Content content : contents) {
                    Xml.Tag contentTag = (Xml.Tag) content;
                    if (contentTag.getName().equals("version")
                            && contentTag.getValue().isPresent()
                            && managedVersion.getVersion() != null
                            && managedVersion.getVersion().equals(contentTag.getValue().get())) {
                        versionTag = contentTag;
                    } else if (contentTag.getName().equals("scope") && contentTag.getValue().isPresent()) {
                        scope = Scope.fromName(contentTag.getValue().get());
                    }
                }
                if (versionTag != null
                        && FindManagedDependencyVersionVisitor.scopeMatches(scope, managedVersion.getScope())) {
                    doAfterVisit(new RemoveContentVisitor<>(versionTag, false));
                }
            }

            return super.visitTag(tag, ctx);
        }
    }

}
