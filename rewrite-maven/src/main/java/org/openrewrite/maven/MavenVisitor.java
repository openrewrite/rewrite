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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenPomDownloader;
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
import static java.util.Collections.emptyMap;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@SuppressWarnings("NotNullFieldNotInitialized")
public class MavenVisitor<P> extends XmlVisitor<P> {
    static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    static final XPathMatcher MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");
    static final XPathMatcher PROPERTY_MATCHER = new XPathMatcher("/project/properties/*");
    static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("/project/*/plugins/plugin");
    static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");

    private transient MavenResolutionResult resolutionResult;

    @Override
    public String getLanguage() {
        return "maven";
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return super.isAcceptable(sourceFile, p) &&
                sourceFile.getMarkers().findFirst(MavenResolutionResult.class).isPresent();
    }

    protected MavenResolutionResult getResolutionResult() {
        //noinspection ConstantConditions
        if (resolutionResult == null) {
            resolutionResult = ((Xml.Document) getCursor()
                    .getPath(Xml.Document.class::isInstance)
                    .next())
                    .getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new IllegalStateException("Maven visitors should not be visiting XML documents without a Maven marker"));
        }
        return resolutionResult;
    }

    public boolean isPropertyTag() {
        return PROPERTY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag() {
        return DEPENDENCY_MATCHER.matches(getCursor());
    }

    /**
     * Is a tag a dependency that matches the group and artifact?
     *
     * @param groupId The group ID glob expression to compare the tag against.
     * @param artifactId The artifact ID glob expression to compare the tag against.
     * @return true if the tag matches.
     */
    public boolean isDependencyTag(String groupId, String artifactId) {
        if (!isDependencyTag()) {
            return false;
        }
        Xml.Tag tag = getCursor().getValue();
        Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
        for (Scope scope : Scope.values()) {
            if (dependencies.containsKey(scope)) {
                for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                    if (matchesGlob(resolvedDependency.getGroupId(), groupId) && matchesGlob(resolvedDependency.getArtifactId(), artifactId)) {
                        String scopeName = tag.getChildValue("scope").orElse(null);
                        Scope tagScope = scopeName != null ? Scope.fromName(scopeName) : null;
                        if (tagScope == null) {
                            tagScope = getResolutionResult().getPom().getManagedScope(
                                    groupId,
                                    artifactId,
                                    tag.getChildValue("type").orElse(null),
                                    tag.getChildValue("classifier").orElse(null)
                            );
                            if (tagScope == null) {
                                tagScope = Scope.Compile;
                            }
                        }
                        Dependency req = resolvedDependency.getRequested();
                        String reqGroup = req.getGroupId();
                        if  ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                                req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                                scope == tagScope) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isManagedDependencyTag() {
        return MANAGED_DEPENDENCY_MATCHER.matches(getCursor());
    }

    /**
     * Is a tag a managed dependency that matches the group and artifact?
     *
     * @param groupId The group ID glob expression to compare the tag against.
     * @param artifactId The artifact ID glob expression to compare the tag against.
     * @return true if the tag matches.
     */
    public boolean isManagedDependencyTag(String groupId, String artifactId) {
        if (!isManagedDependencyTag()) {
            return false;
        }
        Xml.Tag tag = getCursor().getValue();
        for (ResolvedManagedDependency dm : getResolutionResult().getPom().getDependencyManagement()) {
            if (matchesGlob(dm.getGroupId(), groupId) && matchesGlob(dm.getArtifactId(), artifactId)) {
                ManagedDependency req = dm.getRequested();
                String reqGroup = req.getGroupId();
                if (reqGroup.equals(tag.getChildValue("groupId").orElse(null)) &&
                        req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                        dm.getScope() == Scope.fromName(tag.getChildValue("scope").orElse("compile"))) {
                    return true;
                }
            }
            if (dm.getBomGav() != null) {
                if (matchesGlob(dm.getBomGav().getGroupId(), groupId) && matchesGlob(dm.getBomGav().getArtifactId(), artifactId)) {
                    ManagedDependency requestedBom = dm.getRequestedBom();
                    //noinspection ConstantConditions
                    if  (requestedBom.getGroupId().equals(tag.getChildValue("groupId").orElse(null)) &&
                            requestedBom.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isManagedDependencyImportTag(String groupId, String artifactId) {
        if (!isManagedDependencyTag(groupId, artifactId)) {
            return false;
        }
        Xml.Tag tag = getCursor().getValue();
        return tag.getChildValue("type").map("pom"::equalsIgnoreCase).orElse(false)
                && tag.getChildValue("scope").map("import"::equalsIgnoreCase).orElse(false);
    }

    public void maybeUpdateModel() {
        for (TreeVisitor<Xml, P> afterVisit : getAfterVisit()) {
            if(afterVisit instanceof UpdateMavenModel) {
                return;
            }
        }
        doAfterVisit(new UpdateMavenModel<>());
    }

    public boolean isPluginTag() {
        return PLUGIN_MATCHER.matches(getCursor());
    }

    public boolean isPluginTag(String groupId, @Nullable String artifactId) {
        return isPluginTag() && hasPluginGroupId(groupId) && hasPluginArtifactId(artifactId);
    }

    private boolean hasPluginGroupId(String groupId) {
        Xml.Tag tag = getCursor().getValue();
        boolean isGroupIdFound = matchesGlob(tag.getChildValue("groupId").orElse(getResolutionResult().getPom().getGroupId()), groupId);
        if (!isGroupIdFound && getResolutionResult().getPom().getProperties() != null) {
            if (tag.getChildValue("groupId").isPresent() && tag.getChildValue("groupId").get().trim().startsWith("${")) {
                String propertyKey = tag.getChildValue("groupId").get().trim();
                String value = getResolutionResult().getPom().getValue(propertyKey);
                isGroupIdFound = value != null && matchesGlob(value, groupId);
            }
        }
        return isGroupIdFound;
    }

    private boolean hasPluginArtifactId(@Nullable String artifactId) {
        Xml.Tag tag = getCursor().getValue();
        boolean isArtifactIdFound = tag.getChildValue("artifactId")
                .map(a -> matchesGlob(a, artifactId))
                .orElse(artifactId == null);
        if (!isArtifactIdFound && artifactId != null && getResolutionResult().getPom().getProperties() != null) {
            if (tag.getChildValue("artifactId").isPresent() && tag.getChildValue("artifactId").get().trim().startsWith("${")) {
                String propertyKey = tag.getChildValue("artifactId").get().trim();
                String value = getResolutionResult().getPom().getValue(propertyKey);
                isArtifactIdFound = value != null && matchesGlob(value, artifactId);
            }
        }
        return isArtifactIdFound;
    }


    public boolean isParentTag() {
        return PARENT_MATCHER.matches(getCursor());
    }

    @Nullable
    public ResolvedDependency findDependency(Xml.Tag tag) {
        Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
        Scope scope = Scope.fromName(tag.getChildValue("scope").orElse("compile"));
        if (dependencies.containsKey(scope)) {
            for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                Dependency req = resolvedDependency.getRequested();
                String reqGroup = req.getGroupId();
                String reqVersion = req.getVersion();
                if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                        req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                        (reqVersion == null || reqVersion.equals(tag.getChildValue("version").orElse(null))) &&
                        (req.getClassifier() == null || req.getClassifier().equals(tag.getChildValue("classifier").orElse(null)))) {
                    return resolvedDependency;
                }
            }
        }
        return null;
    }

    @Nullable
    public ResolvedManagedDependency findManagedDependency(Xml.Tag tag) {
        String groupId = getResolutionResult().getPom().getValue(tag.getChildValue("groupId").orElse(getResolutionResult().getPom().getGroupId()));
        String artifactId = getResolutionResult().getPom().getValue(tag.getChildValue("artifactId").orElse(""));
        if (groupId != null && artifactId != null) {
            return findManagedDependency(groupId, artifactId);
        }
        return null;
    }

    @Nullable
    public ResolvedManagedDependency findManagedDependency(String groupId, String artifactId) {
        for (ResolvedManagedDependency d : getResolutionResult().getPom().getDependencyManagement()) {
            if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    public ResolvedManagedDependency findManagedDependency(Xml.Tag tag, @Nullable Scope inClasspathOf) {
        Scope tagScope = Scope.fromName(tag.getChildValue("scope").orElse("compile"));
        if (inClasspathOf != null && tagScope != inClasspathOf && !tagScope.isInClasspathOf(inClasspathOf)) {
            return null;
        }
        return findManagedDependency(tag);
    }

    @Nullable
    public ResolvedDependency findDependency(Xml.Tag tag, @Nullable Scope inClasspathOf) {
        Scope tagScope = Scope.fromName(tag.getChildValue("scope").orElse("compile"));
        if (inClasspathOf != null && tagScope != inClasspathOf && !tagScope.isInClasspathOf(inClasspathOf)) {
            return null;
        }
        for (Map.Entry<Scope, List<ResolvedDependency>> scope : getResolutionResult().getDependencies().entrySet()) {
            if (inClasspathOf == null || scope.getKey() == inClasspathOf || scope.getKey().isInClasspathOf(inClasspathOf)) {
                for (ResolvedDependency d : scope.getValue()) {
                    if (tag.getChildValue("groupId").orElse(getResolutionResult().getPom().getGroupId()).equals(d.getGroupId()) &&
                            tag.getChildValue("artifactId").orElse(getResolutionResult().getPom().getArtifactId()).equals(d.getArtifactId())) {
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
     * Note: The list may contain the same dependency multiple times, if it is present in multiple scopes.
     *
     * @param groupId    The groupId to match
     * @param artifactId The artifactId to match.
     * @return dependencies (including transitive dependencies) with any version matching the provided group and artifact id, if any.
     */
    public List<ResolvedDependency> findDependencies(String groupId, String artifactId) {
        return getResolutionResult().findDependencies(groupId, artifactId, null);
    }

    /**
     * Finds dependencies in the model that match the given predicate.
     *
     * @param matcher A dependency test
     * @return dependencies (including transitive dependencies) with any version matching the given predicate.
     */
    public Collection<ResolvedDependency> findDependencies(Predicate<ResolvedDependency> matcher) {
        List<ResolvedDependency> found = null;
        for (List<ResolvedDependency> scope : getResolutionResult().getDependencies().values()) {
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

    public MavenMetadata downloadMetadata(String groupId, String artifactId, ExecutionContext ctx) throws MavenDownloadingException {
        return new MavenPomDownloader(emptyMap(), ctx, getResolutionResult().getMavenSettings(), getResolutionResult().getActiveProfiles())
                .downloadMetadata(new GroupArtifact(groupId, artifactId), null, getResolutionResult().getPom().getRepositories());
    }
}
