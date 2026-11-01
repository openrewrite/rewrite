/*
 * Copyright 2021 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.xml.AddToTagVisitor.addToTag;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPluginDependency extends Recipe {

    @Option(displayName = "Plugin group",
            description = "Group ID of the plugin to which the dependency will be added. " +
                    "A group ID is the first part of a dependency coordinate `org.openrewrite.maven:rewrite-maven-plugin:VERSION`.",
            example = "org.openrewrite.maven")
    String pluginGroupId;

    @Option(displayName = "Plugin artifact",
            description = "Artifact ID of the plugin to which the dependency will be added." +
                    "The second part of a dependency coordinate `org.openrewrite.maven:rewrite-maven-plugin:VERSION`.",
            example = "rewrite-maven-plugin")
    String pluginArtifactId;

    @Option(displayName = "Group",
            description = "The group ID of the dependency to add.",
            example = "org.openrewrite.recipe")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The artifact ID of the dependency to add.",
            example = "org.openrewrite.recipe")
    String artifactId;

    @Option(displayName = "Version",
            description = "The version of the dependency to add.",
            example = "org.openrewrite.recipe")
    @Nullable
    String version;

    String displayName = "Add Maven plugin dependencies";

    String description = "Adds the specified dependencies to a Maven plugin. Will not add the plugin if it does not already exist in the pom. " +
            "When the given version resolves, an existing dependency on the same or a newer version is left as is.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            private final Map<String, ResolvedPom> scopedPoms = new HashMap<>();

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = (Xml.Tag) super.visitTag(tag, ctx);
                if (!isPluginTag(pluginGroupId, pluginArtifactId)) {
                    return plugin;
                }
                Optional<Xml.Tag> maybeDependencies = plugin.getChild("dependencies");
                boolean declaresDependency = maybeDependencies.map(dependencies -> dependencies.getChildren().stream()
                        .anyMatch(it -> groupId.equals(it.getChildValue("groupId").orElse(null)) &&
                                artifactId.equals(it.getChildValue("artifactId").orElse(null))))
                        .orElse(false);
                ResolvedPom pom;
                try {
                    pom = version == null ? getResolutionResult().getPom() : declarationPom(ctx);
                    if (!declaresDependency && inheritedDependencyIsSameOrNewer(pom)) {
                        return plugin;
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(plugin);
                }
                Xml.Tag dependencies;
                boolean formatAllDependencies = false;
                if (maybeDependencies.isPresent()) {
                    dependencies = maybeDependencies.get();
                } else {
                    formatAllDependencies = true;
                    dependencies = Xml.Tag.build("<dependencies />").withPrefix("\n");
                    plugin = addToTag(plugin, plugin, dependencies, getCursor().getParentOrThrow());
                }
                // The dependency being added may already exist and may or may not need its version updated
                Optional<Xml.Tag> maybeExistingDependency = dependencies.getChildren()
                        .stream()
                        .filter(it -> groupId.equals(it.getChildValue("groupId").orElse(null)) &&
                                artifactId.equals(it.getChildValue("artifactId").orElse(null)))
                        .findAny();
                if (maybeExistingDependency.isPresent()) {
                    return updateVersion(plugin, dependencies, maybeExistingDependency.get(), pom, ctx);
                }
                Xml.Tag newDependencyTag = Xml.Tag.build("<dependency>\n<groupId>" + groupId + "</groupId>\n<artifactId>" +
                                artifactId + "</artifactId>" + ((version == null) ? "\n" : "\n<version>" + version + "</version>\n") + "</dependency>")
                        .withPrefix("\n");
                plugin = addToTag(plugin, dependencies, newDependencyTag, getCursor().getParentOrThrow());
                if (formatAllDependencies) {
                    plugin = autoFormat(plugin, dependencies, ctx, getCursor().getParentOrThrow());
                } else {
                    plugin = autoFormat(plugin, newDependencyTag, ctx, getCursor().getParentOrThrow());
                }
                return plugin;
            }

            private Xml.Tag updateVersion(Xml.Tag plugin, Xml.Tag dependencies, Xml.Tag dependency,
                                          ResolvedPom pom, ExecutionContext ctx) {
                Optional<Xml.Tag> versionTag = dependency.getChild("version");
                if (version == null) {
                    return versionTag
                            .map(it -> (Xml.Tag) new RemoveContentVisitor<>(it, false, true)
                                    .visitNonNull(plugin, ctx, getCursor().getParentOrThrow()))
                            .orElse(plugin);
                }
                if (!versionTag.isPresent()) {
                    Xml.Tag artifactIdTag = dependency.getChild("artifactId").orElseThrow(IllegalStateException::new);
                    Xml.Tag newVersionTag = Xml.Tag.build("<version>" + version + "</version>")
                            .withPrefix(artifactIdTag.getPrefix());
                    List<Content> content = new ArrayList<>(requireNonNull(dependency.getContent()));
                    content.add(content.indexOf(artifactIdTag) + 1, newVersionTag);
                    Xml.Tag updated = dependency.withContent(content);
                    return mapTagChildren(plugin, dependencies, it -> it == dependency ? updated : it);
                }
                String existingValue = versionTag.get().getValue().orElse(null);
                if (isSameOrNewer(existingValue, pom)) {
                    return plugin;
                }
                // ChangePropertyValue writes every matching property, so do not use it from a profile declaration.
                // A requested property reference goes into the tag, not into the value of the existing property.
                Xml.Tag updated = !version.contains("${") && enclosingProfileId() == null && isPropertyOfThisPom(existingValue) ?
                        changeChildTagValue(dependency, "version", version, ctx) :
                        (Xml.Tag) new ChangeTagValueVisitor<>(versionTag.get(), version).visitNonNull(dependency, ctx);
                return mapTagChildren(plugin, dependencies, it -> it == dependency ? updated : it);
            }

            private boolean inheritedDependencyIsSameOrNewer(ResolvedPom pom) {
                if (version == null) {
                    return false;
                }
                Optional<Dependency> dependency = isManagedPluginTag() ?
                        findDependency(pom.getPluginManagement(), pom) : findDependency(pom.getPlugins(), pom);
                if (!dependency.isPresent() && !isManagedPluginTag()) {
                    dependency = findDependency(pom.getPluginManagement(), pom);
                }
                return dependency.isPresent() && isSameOrNewer(dependency.get().getVersion(), pom);
            }

            private ResolvedPom declarationPom(ExecutionContext ctx) throws MavenDownloadingException {
                MavenResolutionResult mrr = getResolutionResult();
                ResolvedPom pom = mrr.getPom();
                if (pom.getRequested().getProfiles().isEmpty()) {
                    return pom;
                }
                String profileId = enclosingProfileId();
                if (profileId == null) {
                    return pom;
                }
                String key = mrr.getId() + ":" + profileId;
                ResolvedPom scoped = scopedPoms.get(key);
                if (scoped == null) {
                    // Explicitly activating this profile suppresses default-only siblings, while retaining profiles
                    // independently activated by the caller, a JDK, or a property.
                    List<String> activeProfiles = new ArrayList<>(mrr.getActiveProfiles());
                    activeProfiles.removeIf(it -> {
                        String id = it.trim();
                        return (id.startsWith("!") || id.startsWith("-")) && profileId.equals(id.substring(1).trim());
                    });
                    if (!activeProfiles.contains(profileId)) {
                        activeProfiles.add(profileId);
                    }
                    MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), ctx,
                            MavenExecutionContextView.view(ctx).effectiveSettings(mrr), activeProfiles);
                    scoped = pom.getRequested().resolve(activeProfiles, downloader, ctx);
                    scopedPoms.put(key, scoped);
                }
                return scoped;
            }

            private @Nullable String enclosingProfileId() {
                return getCursor().getPathAsStream()
                        .filter(it -> it instanceof Xml.Tag && "profile".equals(((Xml.Tag) it).getName()))
                        .map(Xml.Tag.class::cast)
                        .findFirst().flatMap(it -> it.getChildValue("id")).orElse(null);
            }

            private Optional<Dependency> findDependency(List<Plugin> plugins, ResolvedPom pom) {
                return plugins.stream()
                        .filter(it -> pluginGroupId.equals(pom.getValue(it.getGroupId())) &&
                                pluginArtifactId.equals(pom.getValue(it.getArtifactId())))
                        .flatMap(it -> it.getDependencies().stream())
                        .filter(it -> groupId.equals(pom.getValue(it.getGroupId())) &&
                                artifactId.equals(pom.getValue(it.getArtifactId())))
                        .findFirst();
            }

            private boolean isSameOrNewer(@Nullable String existingValue, ResolvedPom pom) {
                String existingVersion = pom.getValue(existingValue);
                String requestedVersion = pom.getValue(version);
                return existingVersion != null && requestedVersion != null &&
                        !existingVersion.contains("${") && !requestedVersion.contains("${") &&
                        new Version(existingVersion).compareTo(new Version(requestedVersion)) >= 0;
            }

            /**
             * Whether the version is a property this pom declares or inherits from an external parent,
             * the only kind changeChildTagValue changes. A property an in-repo parent declares is changed
             * in that parent's pom, which this recipe visits only when it declares the plugin too.
             */
            private boolean isPropertyOfThisPom(@Nullable String value) {
                if (!isProperty(value)) {
                    return false;
                }
                String propertyName = value.substring(2, value.length() - 1);
                ResolvedPom pom = getResolutionResult().getPom();
                if (pom.getRequested().getProperties().containsKey(propertyName)) {
                    return true;
                }
                if (!pom.getProperties().containsKey(propertyName)) {
                    return false;
                }
                for (MavenResolutionResult parent = getResolutionResult().getParent(); parent != null; parent = parent.getParent()) {
                    if (parent.getPom().getRequested().getProperties().containsKey(propertyName)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
