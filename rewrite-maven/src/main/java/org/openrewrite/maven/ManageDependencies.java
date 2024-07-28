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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Make existing dependencies "dependency managed", moving the version to the dependencyManagement
 * section of the POM.
 * <p>
 * All dependencies that match {@link #groupPattern} and {@link #artifactPattern} should be
 * align-able to the same version (either the version provided to this visitor or the maximum matching
 * version if none is provided).
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ManageDependencies extends ScanningRecipe<Map<GroupArtifactVersion, Collection<ResolvedDependency>>> {
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                          "Group is the first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.*")
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                          "Artifact is the second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Add to the root POM",
            description = "Add to the root POM where root is the eldest parent of the pom within the source set.",
            required = false)
    @Nullable
    Boolean addToRootPom;

    @Option(displayName = "Skip model updates",
            description = "Optionally skip updating the dependency model after managing dependencies. " +
                          "Updating the model does not affect the source code of the POM," +
                          "but will cause the resolved dependency model to reflect the changes made to the POM. " +
                          "If this recipe is ran standalone, it is not necessary to update the model.",
            required = false)
    @Nullable
    Boolean skipModelUpdate;

    @Override
    public String getDisplayName() {
        return "Manage dependencies";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupPattern, artifactPattern);
    }

    @Override
    public String getDescription() {
        return "Make existing dependencies managed by moving their version to be specified in the dependencyManagement section of the POM.";
    }

    @Override
    public Map<GroupArtifactVersion, Collection<ResolvedDependency>> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<GroupArtifactVersion, Collection<ResolvedDependency>> rootGavToDependencies) {
        return Preconditions.check(Boolean.TRUE.equals(addToRootPom), new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document doc = super.visitDocument(document, ctx);
                Collection<ResolvedDependency> manageableDependencies = findDependencies(groupPattern, artifactPattern != null ? artifactPattern : "*");
                ResolvedGroupArtifactVersion root = findRootPom(getResolutionResult()).getPom().getGav();
                rootGavToDependencies.computeIfAbsent(new GroupArtifactVersion(root.getGroupId(), root.getArtifactId(), root.getVersion()), v -> new ArrayList<>()).addAll(manageableDependencies);
                return doc;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<GroupArtifactVersion, Collection<ResolvedDependency>> rootGavToDependencies) {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml maven = super.visitDocument(document, ctx);

                Collection<ResolvedDependency> manageableDependencies;
                if (Boolean.TRUE.equals(addToRootPom)) {
                    ResolvedPom pom = getResolutionResult().getPom();
                    GroupArtifactVersion gav = new GroupArtifactVersion(pom.getGav().getGroupId(), pom.getGav().getArtifactId(), pom.getGav().getVersion());
                    manageableDependencies = rootGavToDependencies.get(gav);
                } else {
                    manageableDependencies = findDependencies(groupPattern, artifactPattern != null ? artifactPattern : "*");
                }

                if (manageableDependencies != null) {
                    Map<GroupArtifact, ResolvedDependency> maxVersionByGroupArtifact = new HashMap<>(manageableDependencies.size());

                    for (ResolvedDependency rmd : manageableDependencies) {
                        String alreadyManagedVersion = getResolutionResult().getPom().getManagedVersion(rmd.getGroupId(), rmd.getArtifactId(), rmd.getType(),
                                rmd.getClassifier());
                        if (rmd.getDepth() <= 1 && alreadyManagedVersion == null) {
                            maxVersionByGroupArtifact.compute(new GroupArtifact(rmd.getGroupId(), rmd.getArtifactId()),
                                    (ga, existing) -> existing == null || existing.getVersion().compareTo(rmd.getVersion()) < 0 ?
                                            rmd : existing);
                        }
                    }

                    for (ResolvedDependency rmd : maxVersionByGroupArtifact.values()) {
                        doAfterVisit(new AddManagedDependencyVisitor(rmd.getGroupId(),
                                rmd.getArtifactId(), rmd.getVersion(), null,
                                null, rmd.getRequested().getClassifier()));
                        if (!Boolean.TRUE.equals(skipModelUpdate)) {
                            maybeUpdateModel();
                        }
                    }
                }

                doAfterVisit(new RemoveVersionTagVisitor(groupPattern, artifactPattern != null ? artifactPattern : "*"));
                return maven;
            }
        };
    }

    private MavenResolutionResult findRootPom(MavenResolutionResult pom) {
        if (pom.getParent() == null) {
            return pom;
        }
        return findRootPom(pom.getParent());
    }

    private static class RemoveVersionTagVisitor extends MavenIsoVisitor<ExecutionContext> {
        private final String groupPattern;
        private final String artifactPattern;

        public RemoveVersionTagVisitor(String groupPattern, String artifactPattern) {
            this.groupPattern = groupPattern;
            this.artifactPattern = artifactPattern;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag() && isDependencyTag(groupPattern, artifactPattern)) {
                tag.getChild("version").ifPresent(versionTag -> doAfterVisit(new RemoveContentVisitor<>(versionTag, false)));
                return tag;
            }
            return super.visitTag(tag, ctx);
        }
    }
}
