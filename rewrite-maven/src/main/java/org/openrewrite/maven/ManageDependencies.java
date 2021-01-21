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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.internal.Version;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.AddToTagProcessor;
import org.openrewrite.xml.ChangeTagValueProcessor;
import org.openrewrite.xml.RemoveContentProcessor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.required;

/**
 * Make existing dependencies "dependency managed", moving the version to the dependencyManagement
 * section of the POM.
 * <p>
 * All dependencies that match {@link #groupPattern} and {@link #artifactPattern} should be
 * align-able to the same version (either the version provided to this visitor or the maximum matching
 * version if none is provided).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManageDependencies extends Recipe {

    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    private final String groupPattern;

    @Nullable
    private final String artifactPattern;

    @Nullable
    private final String version;

    @Override
    public Validated validate() {
        return required("groupPattern", groupPattern);
    }

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ManageDependenciesProcessor(
                groupPattern == null ? null : Pattern.compile(groupPattern.replace("*", ".*")),
                artifactPattern == null ? null : Pattern.compile(artifactPattern.replace("*", ".*"))
        );
    }

    private class ManageDependenciesProcessor extends MavenProcessor<ExecutionContext> {

        @Nullable
        private final Pattern groupPattern;

        @Nullable
        private final Pattern artifactPattern;

        private String selectedVersion;

        private ManageDependenciesProcessor(Pattern groupPattern, @Nullable Pattern artifactPattern) {
            this.groupPattern = groupPattern;
            this.artifactPattern = artifactPattern;
            setCursoringOn();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            model = maven.getModel();

            Collection<Pom.Dependency> manageableDependencies = findDependencies(d ->
                    groupPattern.matcher(d.getGroupId()).matches() && (artifactPattern == null || artifactPattern.matcher(d.getArtifactId()).matches()));

            selectedVersion = version;

            if (!manageableDependencies.isEmpty()) {
                if (version == null) {
                    selectedVersion = manageableDependencies.stream()
                            .map(Pom.Dependency::getVersion)
                            .max(Comparator.comparing(Version::new))
                            .get();
                }

                List<GroupArtifact> requiresDependencyManagement = manageableDependencies.stream()
                        .filter(d -> model.getManagedVersion(d.getGroupId(), d.getArtifactId()) == null)
                        .map(d -> new GroupArtifact(d.getGroupId(), d.getArtifactId()))
                        .distinct()
                        .collect(toList());

                if (!requiresDependencyManagement.isEmpty()) {
                    Xml.Tag root = maven.getRoot();
                    if (!root.getChild("dependencyManagement").isPresent()) {
                        doAfterVisit(new AddToTagProcessor<>(root, Xml.Tag.build("<dependencyManagement>\n<dependencies/>\n</dependencyManagement>"),
                                new MavenTagInsertionComparator(root.getChildren())));
                    }

                    for (GroupArtifact ga : requiresDependencyManagement) {
                        doAfterVisit(new InsertDependencyInOrder(ga.getGroupId(), ga.getArtifactId(), selectedVersion));
                    }
                }
            }

            return super.visitMaven(maven, ctx);
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isManagedDependencyTag() && hasMatchingGroupArtifact(tag)) {
                doAfterVisit(new ChangeTagValueProcessor<>(
                        tag.getChild("version")
                                .orElseThrow(() -> new IllegalStateException("Version tag must exist")),
                        selectedVersion
                ));
            } else if (isDependencyTag() && hasMatchingGroupArtifact(tag)) {
                tag.getChild("version").ifPresent(version -> doAfterVisit(new RemoveContentProcessor<>(version, false)));
                return tag;
            }

            return super.visitTag(tag, ctx);
        }

        private boolean hasMatchingGroupArtifact(Xml.Tag tag) {
            return groupPattern.matcher(tag.getChildValue("groupId").orElse(model.getGroupId())).matches() &&
                    (artifactPattern == null || artifactPattern.matcher(tag.getChildValue("artifactId")
                            .orElse(model.getArtifactId())).matches());
        }

        private class InsertDependencyInOrder extends MavenProcessor<ExecutionContext> {

            private final String groupId;
            private final String artifactId;
            private final String version;

            private InsertDependencyInOrder(String groupId, String artifactId, String version) {
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                setCursoringOn();
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                    Xml.Tag dependencyTag = Xml.Tag.build(
                            "\n<dependency>\n" +
                                    "<groupId>" + groupId + "</groupId>\n" +
                                    "<artifactId>" + artifactId + "</artifactId>\n" +
                                    (version == null ? "" :
                                            "<version>" + version + "</version>\n") +
                                    "</dependency>"
                    );

                    doAfterVisit(new AddToTagProcessor<>(tag, dependencyTag,
                            new InsertDependencyComparator(tag.getChildren(), dependencyTag)));

                    return tag;
                }

                return super.visitTag(tag, ctx);
            }
        }
    }
}
