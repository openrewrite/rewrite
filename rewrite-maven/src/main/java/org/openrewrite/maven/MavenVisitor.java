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

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.maven.tree.Plugin.PLUGIN_DEFAULT_GROUPID;

public class MavenVisitor<P> extends XmlVisitor<P> {

    static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    static final XPathMatcher PROFILE_DEPENDENCY_MATCHER = new XPathMatcher("/project/profiles/profile/dependencies/dependency");
    static final XPathMatcher PLUGIN_DEPENDENCY_MATCHER = new XPathMatcher("//plugins/plugin/dependencies/dependency");
    static final XPathMatcher PROFILE_PLUGIN_DEPENDENCY_MATCHER = new XPathMatcher("/project/profiles/profile/build/plugins/plugin/dependencies/dependency");
    static final XPathMatcher MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");
    static final XPathMatcher PROFILE_MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/profiles/profile/dependencyManagement/dependencies/dependency");
    static final XPathMatcher PROPERTY_MATCHER = new XPathMatcher("/project/properties/*");
    static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("//plugins/plugin");
    static final XPathMatcher ANNOTATION_PROCESSORS_PATH_MATCHER = new XPathMatcher("//annotationProcessorPaths/path");
    static final XPathMatcher MANAGED_PLUGIN_MATCHER = new XPathMatcher("//pluginManagement/plugins/plugin");
    static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");
    static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

    // there are several implicitly defined version properties that we should never attempt to update
    private static final Set<String> IMPLICITLY_DEFINED_VERSION_PROPERTIES = new HashSet<>(Arrays.asList(
            "${version}",
            "${project.version}",
            "${pom.version}",
            "${project.parent.version}",
            "${revision}",
            "${sha1}",
            "${changelist}"
    ));

    private transient Xml.@Nullable Document document;

    @Nullable
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
        Iterator<Object> itr = getCursor()
                .getPath(Xml.Document.class::isInstance);
        if (itr.hasNext()) {
            Xml.Document newDocument = (Xml.Document) itr.next();
            if (document != null && document != newDocument) {
                throw new IllegalStateException(
                        "The same MavenVisitor instance has been used on two different XML documents. " +
                                "This violates the Recipe contract that they will return a unique visitor instance every time getVisitor() is called.");
            }
            document = newDocument;
        }
        if (resolutionResult == null) {
            resolutionResult = Optional.ofNullable(document)
                    .map(Xml.Document::getMarkers)
                    .flatMap(markers -> markers.findFirst(MavenResolutionResult.class))
                    .orElseThrow(() -> new IllegalStateException("Maven visitors should not be visiting XML documents without a Maven marker"));
        }
        return resolutionResult;
    }

    public boolean isPropertyTag() {
        return PROPERTY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag() {
        return isTag("dependency") && DEPENDENCY_MATCHER.matches(getCursor());
    }

    /**
     * Is a tag a dependency that matches the group and artifact?
     *
     * @param groupId    The group ID glob expression to compare the tag against.
     * @param artifactId The artifact ID glob expression to compare the tag against.
     * @return true if the tag matches.
     */
    public boolean isDependencyTag(String groupId, String artifactId) {
        if (!isDependencyTag()) {
            if (isTag("dependency") && PROFILE_DEPENDENCY_MATCHER.matches(getCursor())) {
                Xml.Tag tag = getCursor().getValue();
                return matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) &&
                        matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId);
            }
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
                        if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
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

    public boolean isPluginDependencyTag(String groupId, String artifactId) {
        if (!isTag("dependency") ||
                !PLUGIN_DEPENDENCY_MATCHER.matches(getCursor()) &&
                        !PROFILE_PLUGIN_DEPENDENCY_MATCHER.matches(getCursor())) {
            return false;
        }
        Xml.Tag tag = getCursor().getValue();
        return matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) &&
                matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId);
    }

    public boolean isManagedDependencyTag() {
        return isTag("dependency") && MANAGED_DEPENDENCY_MATCHER.matches(getCursor());
    }

    /**
     * Is a tag a managed dependency that matches the group and artifact?
     *
     * @param groupId    The group ID glob expression to compare the tag against.
     * @param artifactId The artifact ID glob expression to compare the tag against.
     * @return true if the tag matches.
     */
    public boolean isManagedDependencyTag(String groupId, String artifactId) {
        if (!isManagedDependencyTag()) {
            if (isTag("dependency") && PROFILE_MANAGED_DEPENDENCY_MATCHER.matches(getCursor())) {
                Xml.Tag tag = getCursor().getValue();
                return matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) &&
                        matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId);
            }
            return false;
        }
        Xml.Tag tag = getCursor().getValue();
        for (ResolvedManagedDependency dm : getResolutionResult().getPom().getDependencyManagement()) {
            if (matchesGlob(dm.getGroupId(), groupId) && matchesGlob(dm.getArtifactId(), artifactId)) {
                ManagedDependency req = dm.getRequested();
                String reqGroup = req.getGroupId();
                if (reqGroup.equals(tag.getChildValue("groupId").orElse(null)) &&
                        req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                        dm.getScope() == tag.getChildValue("scope").map(Scope::fromName).orElse(null)) {
                    return true;
                }
            }
            if (dm.getBomGav() != null) {
                if (matchesGlob(dm.getBomGav().getGroupId(), groupId) && matchesGlob(dm.getBomGav().getArtifactId(), artifactId)) {
                    ManagedDependency requestedBom = dm.getRequestedBom();
                    //noinspection ConstantConditions
                    if (requestedBom.getGroupId().equals(tag.getChildValue("groupId").orElse(null)) &&
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
        return tag.getChildValue("type").map("pom"::equalsIgnoreCase).orElse(false) &&
                tag.getChildValue("scope").map("import"::equalsIgnoreCase).orElse(false);
    }

    public void maybeUpdateModel() {
        for (TreeVisitor<?, P> afterVisit : getAfterVisit()) {
            if (afterVisit instanceof UpdateMavenModel) {
                return;
            }
        }
        doAfterVisit(new UpdateMavenModel<>());
    }

    public boolean isPluginTag() {
        return isTag("plugin") && PLUGIN_MATCHER.matches(getCursor());
    }

    public boolean isPluginTag(String groupId, @Nullable String artifactId) {
        return isPluginTag() && hasPluginGroupId(groupId) && hasPluginArtifactId(artifactId);
    }

    public boolean isManagedPluginTag() {
        return isTag("plugin") && MANAGED_PLUGIN_MATCHER.matches(getCursor());
    }

    private boolean hasPluginGroupId(String groupId) {
        Xml.Tag tag = getCursor().getValue();
        boolean isGroupIdFound = matchesGlob(tag.getChildValue("groupId").orElse("org.apache.maven.plugins"), groupId);
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
        return isTag("parent") && PARENT_MATCHER.matches(getCursor());
    }

    public boolean isProjectTag() {
        return isTag("project") && PROJECT_MATCHER.matches(getCursor());
    }

    protected boolean isTag(String name) {
        // `XPathMatcher` is still a bit expensive
        return getCursor().getValue() instanceof Xml.Tag && name.equals(getCursor().<Xml.Tag>getValue().getName());
    }

    protected Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, @Nullable String newValue, P p) {
        return changeChildTagValue(tag, childTagName, newValue, false, p);
    }

    protected Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, @Nullable String newValue, @Nullable Boolean addPropertyIfMissing, P p) {
        Optional<Xml.Tag> childTag = tag.getChild(childTagName);
        if (childTag.isPresent()) {
            String oldValue = childTag.get().getValue().orElse(null);
            if (newValue != null && !newValue.equals(oldValue)) {
                if (isProperty(oldValue)) {
                    MavenResolutionResult resolutionResult = getResolutionResult();
                    String propertyName = oldValue.substring(2, oldValue.length() - 1);
                    ResolvedPom pom = resolutionResult.getPom();
                    if (pom.getRequested().getProperties().containsKey(propertyName)) {
                        // The current REQUESTED pom already has a property, let's change the value of the property in the current file
                        doAfterVisit((TreeVisitor<?, P>) new ChangePropertyValue(propertyName, newValue, addPropertyIfMissing, false).getVisitor());
                    } else if (pom.getProperties().containsKey(propertyName)) {
                        // The combined pom knows about the property, but it is not originating from the current requested pom
                        boolean inheritsPropertyFromExternalParent = true;
                        while (resolutionResult.getParent() != null) {
                            resolutionResult = resolutionResult.getParent();
                            if (resolutionResult.getPom().getRequested().getProperties().containsKey(propertyName)) {
                                // We found an in-repo parent pom that creates this property, so we do not have to adapt this file as the other file will be adapted.
                                inheritsPropertyFromExternalParent = false;
                                break;
                            }
                        }
                        if (inheritsPropertyFromExternalParent) {
                            // We did not find a parent POM (external parents are not mapped on the resolution result's POM) in the repo,
                            // but the property exists so it must be a property that comes from an external parent pom,
                            // so we create the property in the current pom file in order to override its value
                            doAfterVisit((TreeVisitor<?, P>) new ChangePropertyValue(propertyName, newValue, true, false).getVisitor());
                        }
                    }
                } else {
                    tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag.get(), newValue).visitNonNull(tag, p);
                }
            }
        }
        return tag;
    }

    @Contract("null -> false")
    protected boolean isProperty(@Nullable String value) {
        return value != null && value.startsWith("${") && !IMPLICITLY_DEFINED_VERSION_PROPERTIES.contains(value);
    }

    public @Nullable ResolvedDependency findDependency(Xml.Tag tag) {
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

    public @Nullable ResolvedManagedDependency findManagedDependency(Xml.Tag tag) {
        String groupId = getResolutionResult().getPom().getValue(tag.getChildValue("groupId")
                .orElse(getResolutionResult().getPom().getGroupId()));
        String artifactId = getResolutionResult().getPom().getValue(tag.getChildValue("artifactId").orElse(""));
        String classifier = getResolutionResult().getPom().getValue(tag.getChildValue("classifier").orElse(null));
        String type = getResolutionResult().getPom().getValue(tag.getChildValue("type").orElse(null));
        if (groupId != null && artifactId != null) {
            return findManagedDependency(groupId, artifactId, classifier, type);
        }
        return null;
    }

    public @Nullable ResolvedManagedDependency findManagedDependency(String groupId, String artifactId) {
        return findManagedDependency(groupId, artifactId, null, null);
    }

    private @Nullable ResolvedManagedDependency findManagedDependency(String groupId, String artifactId, @Nullable String classifier, @Nullable String type) {
        for (ResolvedManagedDependency d : getResolutionResult().getPom().getDependencyManagement()) {
            if (groupId.equals(d.getGroupId()) &&
                    artifactId.equals(d.getArtifactId()) &&
                    (classifier == null || classifier.equals(d.getClassifier())) &&
                    (type == null || type.equals(d.getType()))) {
                return d;
            }
        }
        return null;
    }

    public @Nullable ResolvedManagedDependency findManagedDependency(Xml.Tag tag, @Nullable Scope inClasspathOf) {
        Scope tagScope = Scope.fromName(tag.getChildValue("scope").orElse(null));
        if (inClasspathOf != null && tagScope != inClasspathOf && !tagScope.isInClasspathOf(inClasspathOf)) {
            return null;
        }
        return findManagedDependency(tag);
    }

    public @Nullable ResolvedDependency findDependency(Xml.Tag tag, @Nullable Scope inClasspathOf) {
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
     * <p>
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
        return downloadMetadata(groupId, artifactId, null, ctx);
    }

    public MavenMetadata downloadMetadata(String groupId, String artifactId, @Nullable ResolvedPom containingPom, ExecutionContext ctx) throws MavenDownloadingException {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        Optional<MavenSettings> maybeSettings = Optional.ofNullable(mctx.effectiveSettings(getResolutionResult()));
        List<String> activeProfiles = maybeSettings.map(MavenSettings::getActiveProfiles)
                .map(MavenSettings.ActiveProfiles::getActiveProfiles)
                .orElse(null);
        return new MavenPomDownloader(getResolutionResult().getProjectPoms(), ctx, maybeSettings.orElse(null), activeProfiles)
                .downloadMetadata(new GroupArtifact(groupId, artifactId), containingPom, getResolutionResult().getPom().getRepositories());
    }

    public MavenMetadata downloadPluginMetadata(String groupId, String artifactId, ExecutionContext ctx) throws MavenDownloadingException {
        return downloadPluginMetadata(groupId, artifactId, null, ctx);
    }

    public MavenMetadata downloadPluginMetadata(String groupId, String artifactId, @Nullable ResolvedPom containingPom, ExecutionContext ctx) throws MavenDownloadingException {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        Optional<MavenSettings> maybeSettings = Optional.ofNullable(mctx.effectiveSettings(getResolutionResult()));
        List<String> activeProfiles = maybeSettings.map(MavenSettings::getActiveProfiles)
                .map(MavenSettings.ActiveProfiles::getActiveProfiles)
                .orElse(null);
        return new MavenPomDownloader(getResolutionResult().getProjectPoms(), ctx, maybeSettings.orElse(null), activeProfiles)
                .downloadMetadata(new GroupArtifact(groupId, artifactId), containingPom, getResolutionResult().getPom().getPluginRepositories());
    }

    /**
     * Does the current tag can contain groupId, artifactId and version?
     */
    @SuppressWarnings("unused")
    public boolean isDependencyLikeTag() {
        return isManagedDependencyTag() || isDependencyTag() || isPluginTag();
    }

    public @Nullable Plugin findPlugin(Xml.Tag tag) {
        return findPlugin(tag, getResolutionResult().getPom().getPlugins());
    }

    public @Nullable Plugin findManagedPlugin(Xml.Tag tag) {
        return findPlugin(tag, getResolutionResult().getPom().getPluginManagement());
    }

    private static @Nullable Plugin findPlugin(Xml.Tag tag, List<Plugin> plugins) {
        String tagGroupId = tag.getChildValue("groupId").orElse(PLUGIN_DEFAULT_GROUPID);
        String tagArtifactId = tag.getChildValue("artifactId").orElse(null);
        String tagVersion = tag.getChildValue("version").orElse(null);
        for (Plugin resolvedPlugin : plugins) {
            String resGroup = resolvedPlugin.getGroupId();
            String resArtifactId = resolvedPlugin.getArtifactId();
            String resVersion = resolvedPlugin.getVersion();
            if (resGroup.equals(tagGroupId) &&
                    resArtifactId.equals(tagArtifactId) &&
                    (resVersion == null || tagVersion == null || resVersion.equals(tagVersion))) {
                return resolvedPlugin;
            }
        }
        return null;
    }
}
