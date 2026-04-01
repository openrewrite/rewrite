/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.trait.MavenPlugin;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adds an annotation processor to the maven-compiler-plugin configuration.
 * <p>
 * The behavior differs based on project structure:
 * <ul>
 *   <li><b>Single module:</b> Adds to build/plugins</li>
 *   <li><b>Multi-module with parent in reactor:</b> Adds to parent's build/pluginManagement/plugins</li>
 *   <li><b>Orphan module (no parent in reactor):</b> Adds to build/plugins</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddAnnotationProcessor extends ScanningRecipe<AddAnnotationProcessor.Scanned> {
    private static final String MAVEN_COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

    @Option(displayName = "Group",
            description = "The first part of the coordinate 'org.projectlombok:lombok-mapstruct-binding:0.2.0' of the processor to add.",
            example = "org.projectlombok")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a coordinate 'org.projectlombok:lombok-mapstruct-binding:0.2.0' of the processor to add.",
            example = "lombok-mapstruct-binding")
    String artifactId;

    @Option(displayName = "Version",
            description = "The third part of a coordinate 'org.projectlombok:lombok-mapstruct-binding:0.2.0' of the processor to add. " +
                    "Note that an exact version is expected",
            example = "0.2.0")
    String version;

    String displayName = "Add an annotation processor to `maven-compiler-plugin`";

    String description = "Add an annotation processor path to the `maven-compiler-plugin` configuration. " +
            "For modules with an in-reactor parent, adds to the parent's `build/pluginManagement/plugins` section. " +
            "For modules without a parent or with a parent outside the reactor, adds directly to `build/plugins`. " +
            "Updates the annotation processor version if a newer version is specified.";

    /**
     * Accumulator to track which POMs need modifications and how.
     */
    public static class Scanned {
        /**
         * Source paths of POMs that are referenced as parents by at least one child within the reactor.
         * These should get pluginManagement updates.
         */
        Set<Path> parentPomPaths = new HashSet<>();

        /**
         * Source paths of POMs that have no parent within the reactor.
         * After scanning, aggregator-only POMs will be filtered out.
         */
        Set<Path> candidateOrphanPaths = new HashSet<>();

        /**
         * Source paths of POMs that have a &lt;modules&gt; section (aggregators).
         * Used to identify aggregator-only POMs that should not be modified.
         */
        Set<Path> aggregatorPaths = new HashSet<>();

        /**
         * Paths of POMs where the annotation processor is already present in the effective POM
         * (via a parent POM outside the reactor), but not in the POM's own XML.
         * These POMs should be skipped to avoid redundant configuration.
         */
        Set<Path> alreadyConfiguredInEffectivePomPaths = new HashSet<>();

        /**
         * Get the actual orphan paths (candidates minus aggregator-only POMs).
         * A true orphan has no parent in reactor and is not an aggregator-only POM.
         */
        Set<Path> getOrphanPomPaths() {
            Set<Path> result = new HashSet<>(candidateOrphanPaths);
            // Remove aggregator-only POMs (aggregators that are not also parents)
            for (Path aggregatorPath : aggregatorPaths) {
                if (!parentPomPaths.contains(aggregatorPath)) {
                    result.remove(aggregatorPath);
                }
            }
            return result;
        }
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                ResolvedPom resolvedPom = mrr.getPom();
                Path sourcePath = resolvedPom.getRequested().getSourcePath();

                // Check if the annotation processor is already in the effective POM (merged from parent POMs)
                // but NOT in the current POM's own XML. In that case, skip this POM to avoid adding
                // redundant configuration that duplicates what the parent already provides.
                boolean inEffectivePom = hasAnnotationProcessor(resolvedPom.getPlugins()) ||
                                        hasAnnotationProcessor(resolvedPom.getPluginManagement());
                Pom requestedPom = resolvedPom.getRequested();
                boolean inCurrentPomXml = hasAnnotationProcessor(requestedPom.getPlugins()) ||
                                          hasAnnotationProcessor(requestedPom.getPluginManagement());
                if (sourcePath != null && inEffectivePom && !inCurrentPomXml) {
                    acc.alreadyConfiguredInEffectivePomPaths.add(sourcePath);
                }

                if (mrr.parentPomIsProjectPom()) {
                    // This module has a parent within the reactor
                    // Mark the parent for pluginManagement update
                    MavenResolutionResult parent = mrr.getParent();
                    if (parent != null) {
                        Path parentPath = parent.getPom().getRequested().getSourcePath();
                        if (parentPath != null) {
                            acc.parentPomPaths.add(parentPath);
                        }
                    }
                } else {
                    // This module has no parent within the reactor
                    // Mark as candidate orphan (will be filtered later if it's aggregator-only)
                    if (sourcePath != null) {
                        acc.candidateOrphanPaths.add(sourcePath);
                    }
                }

                // Track aggregator POMs (those with <modules> section)
                List<String> subprojects = mrr.getPom().getSubprojects();
                if (sourcePath != null && subprojects != null && !subprojects.isEmpty()) {
                    acc.aggregatorPaths.add(sourcePath);
                }

                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }

                MavenResolutionResult mrr = tree.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (mrr == null) {
                    return tree;
                }

                Path sourcePath = mrr.getPom().getRequested().getSourcePath();
                if (sourcePath == null) {
                    return tree;
                }

                boolean isParent = acc.parentPomPaths.contains(sourcePath);
                // Skip POMs that are neither parents nor orphans (children with parents in reactor)
                if (!isParent && !acc.getOrphanPomPaths().contains(sourcePath)) {
                    return tree;
                }

                // Skip POMs where the annotation processor is already configured via a parent POM
                // outside the reactor (present in effective POM but not in own XML)
                if (acc.alreadyConfiguredInEffectivePomPaths.contains(sourcePath)) {
                    return tree;
                }

                // First, ensure the plugin exists - use the source path as file pattern
                tree = new AddPluginVisitor(isParent,
                        MAVEN_COMPILER_PLUGIN_GROUP_ID, MAVEN_COMPILER_PLUGIN_ARTIFACT_ID, null,
                        "<configuration><annotationProcessorPaths/></configuration>", null, null, null
                ).visit(tree, ctx);

                // Then, configure the annotation processor path
                return new MavenIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag plugins = super.visitTag(tag, ctx);
                        plugins = (Xml.Tag) new MavenPlugin.Matcher(isParent, MAVEN_COMPILER_PLUGIN_GROUP_ID, MAVEN_COMPILER_PLUGIN_ARTIFACT_ID).asVisitor(plugin -> {
                            MavenResolutionResult currentMrr = getResolutionResult();
                            AtomicReference<TreeVisitor<?, ExecutionContext>> maybePropertyUpdate = new AtomicReference<>();

                            Xml.Tag modifiedPlugin = new XmlIsoVisitor<ExecutionContext>() {
                                @Override
                                public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                                    Xml.Tag tg = super.visitTag(tag, ctx);

                                    if (!"annotationProcessorPaths".equals(tg.getName())) {
                                        return tg;
                                    }

                                    // Iterate the children (annotation processor paths) and try to update the version
                                    for (int i = 0; i < tg.getChildren().size(); i++) {
                                        Xml.Tag child = tg.getChildren().get(i);
                                        if (!groupId.equals(child.getChildValue("groupId").orElse(null)) ||
                                                !artifactId.equals(child.getChildValue("artifactId").orElse(null))) {
                                            continue;
                                        }

                                        if (!version.equals(child.getChildValue("version").orElse(null))) {
                                            String oldVersion = child.getChildValue("version").orElse("");
                                            boolean oldVersionUsesProperty = oldVersion.startsWith("${");
                                            String lookupVersion = oldVersionUsesProperty ?
                                                    currentMrr.getPom().getValue(oldVersion.trim()) : oldVersion;
                                            VersionComparator comparator = Semver.validate(lookupVersion, null).getValue();
                                            if (comparator.compare(version, lookupVersion) > 0) {
                                                if (oldVersionUsesProperty) {
                                                    // A maven property is used here, update in properties section later
                                                    maybePropertyUpdate.set(new ChangePropertyValue(oldVersion, version, null, null).getVisitor());
                                                } else {
                                                    // Update the path's version directly
                                                    List<Xml.Tag> tags = tg.getChildren();
                                                    tags.set(i, child.withChildValue("version", version));
                                                    return tg.withContent(tags);
                                                }
                                            }
                                        }

                                        return tg;
                                    }

                                    // Not found, so we add it
                                    return tg.withContent(ListUtils.concat(tg.getChildren(), Xml.Tag.build(String.format(
                                            "<path>\n<groupId>%s</groupId>\n<artifactId>%s</artifactId>\n<version>%s</version>\n</path>",
                                            groupId, artifactId, version))));
                                }
                            }.visitTag(plugin.getTree(), ctx);

                            if (maybePropertyUpdate.get() != null) {
                                doAfterVisit(maybePropertyUpdate.get());
                            }

                            return modifiedPlugin;
                        }).visitNonNull(plugins, 0);

                        if (plugins != tag) {
                            plugins = autoFormat(plugins, ctx);
                        }
                        return plugins;
                    }
                }.visit(tree, ctx);
            }
        };
    }

    private boolean hasAnnotationProcessor(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if (!MAVEN_COMPILER_PLUGIN_GROUP_ID.equals(plugin.getGroupId()) ||
                    !MAVEN_COMPILER_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                continue;
            }
            JsonNode config = plugin.getConfiguration();
            if (config == null || config.isMissingNode()) {
                continue;
            }
            JsonNode paths = config.path("annotationProcessorPaths").path("path");
            if (paths.isArray()) {
                for (JsonNode path : paths) {
                    if (isMatchingProcessor(path)) {
                        return true;
                    }
                }
            } else if (!paths.isMissingNode() && isMatchingProcessor(paths)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatchingProcessor(JsonNode path) {
        return groupId.equals(path.path("groupId").asText("")) &&
               artifactId.equals(path.path("artifactId").asText(""));
    }
}
