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

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

@SuppressWarnings("NotNullFieldNotInitialized")
public class MavenVisitor extends XmlVisitor<ExecutionContext> {
    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    private static final XPathMatcher MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");
    private static final XPathMatcher PROPERTY_MATCHER = new XPathMatcher("/project/properties/*");
    private static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("/project/*/plugins/plugin");
    private static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");

    protected MavenResolutionResult resolutionResult;
    protected List<MavenResolutionResult> modules;

    @Override
    public String getLanguage() {
        return "maven";
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof Maven;
    }

    public Maven visitMaven(Maven maven, ExecutionContext ctx) {
        this.resolutionResult = maven.getMavenResolutionResult();
        if (resolutionResult == null) {
            return maven;
        }
        this.modules = maven.getModules();
        return (Maven) visitDocument(maven, ctx);
    }

    @Override
    public final Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
        // Maven visitors should not attempt to apply themselves to non-Maven Xml.Documents
        if (!document.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
            return document;
        }
        Xml.Document refactored = (Xml.Document) super.visitDocument(document, ctx);
        if (refactored != document && refactored.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
            return new Maven(refactored);
        }
        return refactored;
    }

    public boolean isPropertyTag() {
        return PROPERTY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag() {
        return DEPENDENCY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag(String groupId, @Nullable String artifactId) {
        return isDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    public boolean isManagedDependencyTag() {
        return MANAGED_DEPENDENCY_MATCHER.matches(getCursor());
    }

    public boolean isManagedDependencyTag(String groupId, @Nullable String artifactId) {
        return isManagedDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    public boolean isPluginTag() {
        return PLUGIN_MATCHER.matches(getCursor());
    }

    public boolean isPluginTag(String groupId, @Nullable String artifactId) {
        return isPluginTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    public boolean isParentTag() {
        return PARENT_MATCHER.matches(getCursor());
    }

    private boolean hasGroupAndArtifact(String groupId, @Nullable String artifactId) {
        return hasGroupId(groupId) && hasArtifactId(artifactId);
    }

    private boolean hasGroupId(String groupId) {
        Xml.Tag tag = getCursor().getValue();
        boolean isGroupIdFound = groupId.equals(tag.getChildValue("groupId").orElse(resolutionResult.getPom().getGroupId()));
        if (!isGroupIdFound && resolutionResult.getPom().getProperties() != null) {
            if (tag.getChildValue("groupId").isPresent() && tag.getChildValue("groupId").get().trim().startsWith("${")) {
                String propertyKey = tag.getChildValue("groupId").get().trim();
                String value = resolutionResult.getPom().getValue(propertyKey);
                isGroupIdFound = value != null && StringUtils.matchesGlob(value, groupId);
            }
        }
        return isGroupIdFound;
    }

    private boolean hasArtifactId(@Nullable String artifactId) {
        Xml.Tag tag = getCursor().getValue();
        boolean isArtifactIdFound = tag.getChildValue("artifactId")
                .map(a -> a.equals(artifactId))
                .orElse(artifactId == null);

        if (!isArtifactIdFound && artifactId != null && resolutionResult.getPom().getProperties() != null) {
            if (tag.getChildValue("artifactId").isPresent() && tag.getChildValue("artifactId").get().trim().startsWith("${")) {
                String propertyKey = tag.getChildValue("artifactId").get().trim();
                String value = resolutionResult.getPom().getValue(propertyKey);
                isArtifactIdFound = value != null && StringUtils.matchesGlob(value, artifactId);
            }
        }
        return isArtifactIdFound;
    }

    @Nullable
    public ResolvedDependency findDependency(Xml.Tag tag) {
        for (List<ResolvedDependency> scope : resolutionResult.getDependencies().values()) {
            for (ResolvedDependency d : scope) {
                if (tag.getChildValue("groupId").orElse(resolutionResult.getPom().getGroupId()).equals(d.getGroupId()) &&
                        tag.getChildValue("artifactId").orElse(resolutionResult.getPom().getArtifactId()).equals(d.getArtifactId())) {
                    return d;
                }
            }
        }
        return null;
    }

    @Nullable
    public ResolvedDependency findDependency(Xml.Tag tag, @Nullable Scope inClasspathOf) {
        for (Map.Entry<Scope, List<ResolvedDependency>> scope : resolutionResult.getDependencies().entrySet()) {
            if(inClasspathOf == null || scope.getKey().isInClasspathOf(inClasspathOf)) {
                for (ResolvedDependency d : scope.getValue()) {
                    if (tag.getChildValue("groupId").orElse(resolutionResult.getPom().getGroupId()).equals(d.getGroupId()) &&
                            tag.getChildValue("artifactId").orElse(resolutionResult.getPom().getArtifactId()).equals(d.getArtifactId())) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds dependencies in the model that match the provided group and artifact ids.
     *
     * @param groupId    The groupId to match
     * @param artifactId The artifactId to match.
     * @return dependencies (including transitive dependencies) with any version matching the provided group and artifact id, if any.
     */
    public Collection<ResolvedDependency> findDependencies(String groupId, String artifactId) {
        return findDependencies(d -> StringUtils.matchesGlob(d.getGroupId(), groupId) && StringUtils.matchesGlob(d.getArtifactId(), artifactId));
    }

    /**
     * Finds dependencies in the model that match the given predicate.
     *
     * @param matcher A dependency test
     * @return dependencies (including transitive dependencies) with any version matching the given predicate.
     */
    public Collection<ResolvedDependency> findDependencies(Predicate<ResolvedDependency> matcher) {
        List<ResolvedDependency> found = null;
        for (List<ResolvedDependency> scope : resolutionResult.getDependencies().values()) {
            for (ResolvedDependency d : scope) {
                if (matcher.test(d)) {
                    if (found == null) {
                        found = new ArrayList<>();
                    }
                    found.add(d);
                }
            }
        }
        return found == null ? emptyList() : found;
    }
}
