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
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveUnneededDependencyOverrides extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove dependency overrides in descendant poms";
    }

    @Override
    public String getDescription() {
        return "For child poms that contain the same dependency version as an ancestor," +
                " remove the explicit version in the child.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindManagedDependencyVersionVisitor();
    }

    private static class FindManagedDependencyVersionVisitor extends MavenVisitor {
        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Collection<Pom.Dependency> dependencies = maven.getModel().getDependencies();
            for (Pom.Dependency dependency : dependencies) {
                String managedVersion = findManagedVersion(maven.getModel(), dependency);
                if (managedVersion != null
                        && dependency.getRequestedVersion() != null
                        && dependency.getVersion().equals(dependency.getRequestedVersion())
                        && managedVersion.equals(dependency.getRequestedVersion())) {
                    doAfterVisit(new RemoveVersionVisitor(maven.getModel().getArtifactId(), dependency.getGroupId(), dependency.getArtifactId(), managedVersion));
                }
            }

            return super.visitMaven(maven, ctx);
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
        private String findManagedVersion(Pom pom, Pom.Dependency dependency) {
            if (pom.getDependencyManagement() != null) {
                Collection<DependencyManagementDependency> managedDependencies = pom.getDependencyManagement().getDependencies();
                for (DependencyManagementDependency managedDependency : managedDependencies) {
                    if (dependency.getGroupId().equals(managedDependency.getGroupId())
                            && dependency.getArtifactId().equals(managedDependency.getArtifactId())) {
                        if (managedDependency.getVersion() != null) {
                            return managedDependency.getVersion();
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
        final private String managedVersion;

        public RemoveVersionVisitor(String moduleArtifactId, String groupId, String artifactId, String managedVersion) {
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
                for (Content content : contents) {
                    Xml.Tag contentTag = (Xml.Tag) content;
                    if (contentTag.getName().equals("version")
                            && contentTag.getValue().isPresent()
                            && managedVersion.equals(contentTag.getValue().get())) {
                        doAfterVisit(new RemoveContentVisitor<>(content, false));
                    }
                }
            }

            return super.visitTag(tag, ctx);
        }
    }

}