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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Make existing dependencies "dependency managed", moving the version to the dependencyManagement
 * section of the POM.
 * <p>
 * All dependencies that match {@link #groupPattern} and {@link #artifactPattern} should be
 * align-able to the same version (either the version provided to this visitor or the maximum matching
 * version if none is provided).
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ManageDependencies extends Recipe {
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                    "Group is the the first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.*")
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                    "Artifact is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Version",
            description = "Version to use for the dependency in dependency management. " +
                    "Defaults to the existing version found on the matching dependency.",
            example = "1.0.0",
            required = false)
    @Nullable
    String version;

    @Override
    public String getDisplayName() {
        return "Manage dependencies";
    }

    @Override
    public String getDescription() {
        return "Make existing dependencies managed by moving their version to be specified in the dependencyManagement section of the POM.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private String selectedVersion = version;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Collection<ResolvedDependency> manageableDependencies = findDependencies(groupPattern, artifactPattern);

                if (!manageableDependencies.isEmpty()) {
                    if (version == null) {
                        selectedVersion = manageableDependencies.stream()
                                .map(ResolvedDependency::getVersion)
                                .max(Comparator.comparing(Version::new))
                                .get();
                    }

                    List<GroupArtifact> requiresDependencyManagement = manageableDependencies.stream()
                            .filter(d -> getResolutionResult().getPom().getManagedVersion(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier()) == null)
                            .map(d -> new GroupArtifact(d.getGroupId(), d.getArtifactId()))
                            .distinct()
                            .collect(toList());

                    if (!requiresDependencyManagement.isEmpty()) {
                        Xml.Tag root = document.getRoot();
                        if (!root.getChild("dependencyManagement").isPresent()) {
                            doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencyManagement>\n<dependencies/>\n</dependencyManagement>"),
                                    new MavenTagInsertionComparator(root.getContent())));
                        }

                        for (GroupArtifact ga : requiresDependencyManagement) {
                            doAfterVisit(new InsertDependencyInOrder(ga.getGroupId(), ga.getArtifactId(), selectedVersion));
                            maybeUpdateModel();
                        }
                    }
                }

                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isManagedDependencyTag() && isDependencyTag(groupPattern, artifactPattern)) {
                    tag.getChild("version").ifPresent(version -> {
                        doAfterVisit(new ChangeTagValueVisitor<>(
                                version,
                                selectedVersion
                        ));
                    });
                } else if (isDependencyTag() && isDependencyTag(groupPattern, artifactPattern)) {
                    tag.getChild("version").ifPresent(version -> doAfterVisit(new RemoveContentVisitor<>(version, false)));
                    return tag;
                }

                return super.visitTag(tag, ctx);
            }
        };
    }

    private static class InsertDependencyInOrder extends MavenIsoVisitor<ExecutionContext> {
        private final String groupId;
        private final String artifactId;
        private final String version;

        private InsertDependencyInOrder(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                Xml.Tag dependencyTag = Xml.Tag.build(
                        "\n<dependency>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + version + "</version>\n" +
                                "</dependency>"
                );

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));

                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }
}
