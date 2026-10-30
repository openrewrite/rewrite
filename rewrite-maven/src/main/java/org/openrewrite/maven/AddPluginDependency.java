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
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

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
            "When a version is given, an existing dependency on the same or a newer version is left as is.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = (Xml.Tag) super.visitTag(tag, ctx);
                if (!isPluginTag(pluginGroupId, pluginArtifactId)) {
                    return plugin;
                }
                Optional<Xml.Tag> maybeDependencies = plugin.getChild("dependencies");
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
                    return updateVersion(plugin, dependencies, maybeExistingDependency.get(), ctx);
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
                                          ExecutionContext ctx) {
                Optional<Xml.Tag> versionTag = dependency.getChild("version");
                if (version == null) {
                    return versionTag
                            .map(it -> (Xml.Tag) new RemoveContentVisitor<>(it, false, true)
                                    .visitNonNull(plugin, ctx, getCursor().getParentOrThrow()))
                            .orElse(plugin);
                }
                if (!versionTag.isPresent()) {
                    Xml.Tag newVersionTag = Xml.Tag.build("<version>" + version + "</version>");
                    return (Xml.Tag) new AddToTagVisitor<>(dependency, newVersionTag,
                            new MavenTagInsertionComparator(dependency.getChildren()))
                            .visitNonNull(plugin, ctx, getCursor().getParentOrThrow());
                }
                String existingValue = versionTag.get().getValue().orElse(null);
                ResolvedPom pom = getResolutionResult().getPom();
                String existingVersion = pom.getValue(existingValue);
                String requestedVersion = pom.getValue(version);
                if (existingVersion != null && requestedVersion != null &&
                    !requestedVersion.contains("${") &&
                    new Version(existingVersion).compareTo(new Version(requestedVersion)) >= 0) {
                    return plugin;
                }
                // changeChildTagValue leaves a property owned by an in-repo parent to be changed in that parent.
                // This recipe edits only poms that declare the plugin, so it replaces the reference with the version.
                Xml.Tag updated = isPropertyOfInRepoParent(existingValue) ?
                        (Xml.Tag) new ChangeTagValueVisitor<>(versionTag.get(), version).visitNonNull(dependency, ctx) :
                        changeChildTagValue(dependency, "version", version, ctx);
                return mapTagChildren(plugin, dependencies, it -> it == dependency ? updated : it);
            }

            private boolean isPropertyOfInRepoParent(@Nullable String value) {
                if (!isProperty(value)) {
                    return false;
                }
                String propertyName = value.substring(2, value.length() - 1);
                if (getResolutionResult().getPom().getRequested().getProperties().containsKey(propertyName)) {
                    return false;
                }
                MavenResolutionResult parent = getResolutionResult().getParent();
                while (parent != null) {
                    if (parent.getPom().getRequested().getProperties().containsKey(propertyName)) {
                        return true;
                    }
                    parent = parent.getParent();
                }
                return false;
            }
        };
    }
}
