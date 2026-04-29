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
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Accumulator populated during the scan phase and resolved into final
     * parent/orphan path sets via {@link #resolve()} before the visitor runs.
     */
    public static class Scanned {
        Set<Path> aggregatorPaths = new HashSet<>();
        Map<Path, Set<Path>> aggregatorSubmodulePaths = new HashMap<>();

        /**
         * Child-to-parent links recorded whenever
         * {@code MavenResolutionResult.parentPomIsProjectPom()} is true. The
         * check is GAV-based, so the link is upheld in {@link #resolve()} only
         * when the child is reachable via some aggregator's &lt;modules&gt;
         * chain; otherwise the child is treated as an orphan instead.
         */
        Map<Path, Path> tentativeChildToParent = new HashMap<>();

        Set<Path> noReactorParentPaths = new HashSet<>();
        Set<Path> packagingPomPaths = new HashSet<>();
        Set<Path> alreadyConfiguredInEffectivePomPaths = new HashSet<>();

        Set<Path> parentPomPaths = new HashSet<>();
        Set<Path> orphanPomPaths = new HashSet<>();

        void resolve() {
            Set<Path> reactorLinked = computeReactorLinkedPaths();
            Set<Path> orphans = new HashSet<>(noReactorParentPaths);

            for (Map.Entry<Path, Path> e : tentativeChildToParent.entrySet()) {
                Path child = e.getKey();
                Path parent = e.getValue();
                if (reactorLinked.contains(child)) {
                    parentPomPaths.add(parent);
                } else {
                    orphans.add(child);
                }
            }

            // Aggregator-only POMs are not modified.
            for (Path aggregator : aggregatorPaths) {
                if (!parentPomPaths.contains(aggregator)) {
                    orphans.remove(aggregator);
                }
            }

            // Dangling pom-packaging POMs (not claimed as parents, not aggregators)
            // have nothing to compile; leave them alone.
            for (Path packagingPom : packagingPomPaths) {
                if (!parentPomPaths.contains(packagingPom) && !aggregatorPaths.contains(packagingPom)) {
                    orphans.remove(packagingPom);
                }
            }

            orphanPomPaths.addAll(orphans);
        }

        private Set<Path> computeReactorLinkedPaths() {
            Set<Path> reachable = new HashSet<>();
            Deque<Path> queue = new ArrayDeque<>(aggregatorPaths);
            while (!queue.isEmpty()) {
                Path p = queue.poll();
                if (!reachable.add(p)) {
                    continue;
                }
                Set<Path> subs = aggregatorSubmodulePaths.get(p);
                if (subs != null) {
                    queue.addAll(subs);
                }
            }
            return reachable;
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

                if (sourcePath != null) {
                    if (mrr.parentPomIsProjectPom()) {
                        // Parent's GAV matches a project pom, but this is a tentative
                        // signal only — verified at resolution time by checking that
                        // the child is reactor-linked via an aggregator's <modules>
                        // chain. Two POMs co-ingested in the LST without a real
                        // aggregator linking them must not be treated as a reactor.
                        MavenResolutionResult parent = mrr.getParent();
                        Path parentPath = parent == null ? null :
                                parent.getPom().getRequested().getSourcePath();
                        if (parentPath != null) {
                            acc.tentativeChildToParent.put(sourcePath, parentPath);
                        } else {
                            acc.noReactorParentPaths.add(sourcePath);
                        }
                    } else {
                        // No project-pom parent — true single-module root or
                        // standalone pom-packaging shell.
                        acc.noReactorParentPaths.add(sourcePath);
                    }

                    if ("pom".equals(resolvedPom.getPackaging())) {
                        acc.packagingPomPaths.add(sourcePath);
                    }
                }

                // Treat this POM as a reactor aggregator only when its raw XML
                // declared <modules>/<subprojects>. We read that off the
                // requested (unresolved) Pom — `mrr.getModules()` is unsuitable
                // because it lists POMs that declare *this* one as their
                // <parent> (which can happen without any <modules>
                // declaration on this side).
                List<String> requestedSubs = mrr.getPom().getRequested().getSubprojects();
                if (sourcePath != null && requestedSubs != null && !requestedSubs.isEmpty()) {
                    acc.aggregatorPaths.add(sourcePath);
                    // Resolve each <module> string relative to the aggregator's
                    // directory. baseDir is null when the aggregator is at the
                    // root (e.g. "pom.xml"); fall back to the empty path so
                    // root-level reactors still reach their children.
                    Path baseDir = sourcePath.getParent();
                    Set<Path> resolvedSubmodules = new HashSet<>();
                    for (String sub : requestedSubs) {
                        Path resolved = baseDir == null ?
                                Paths.get(sub, "pom.xml").normalize() :
                                baseDir.resolve(sub).resolve("pom.xml").normalize();
                        resolvedSubmodules.add(resolved);
                    }
                    acc.aggregatorSubmodulePaths.put(sourcePath, resolvedSubmodules);
                }

                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        acc.resolve();
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
                if (!isParent && !acc.orphanPomPaths.contains(sourcePath)) {
                    return tree;
                }
                if (acc.alreadyConfiguredInEffectivePomPaths.contains(sourcePath)) {
                    return tree;
                }

                // GAV-coincident orphan: AddPluginVisitor.isAcceptable would
                // otherwise short-circuit via its parentPomIsProjectPom()
                // check and refuse to add the plugin. Targeting the visitor
                // at this exact source path bypasses that guard.
                boolean isGavCoincidentOrphan = !isParent && mrr.parentPomIsProjectPom();
                String pluginFilePattern = isGavCoincidentOrphan ? sourcePath.toString() : null;
                tree = new AddPluginVisitor(isParent,
                        MAVEN_COMPILER_PLUGIN_GROUP_ID, MAVEN_COMPILER_PLUGIN_ARTIFACT_ID, null,
                        "<configuration><annotationProcessorPaths/></configuration>", null, null,
                        pluginFilePattern
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

                                        if (!child.getChildValue("version").isPresent()) {
                                            // No explicit version: the path intentionally defers to
                                            // the effective POM's dependencyManagement. Leave it alone.
                                            return tg;
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

                                    // Not found, so we add it. Omit <version> when the effective POM's
                                    // dependencyManagement already manages this coordinate AND the
                                    // effective maven-compiler-plugin is 3.12+, which resolves
                                    // annotation processor path versions from dependencyManagement.
                                    // For older plugin versions the <version> is still required.
                                    boolean omitVersion =
                                            currentMrr.getPom().getManagedVersion(groupId, artifactId, null, null) != null &&
                                                    compilerPluginSupportsManagedVersions(currentMrr.getPom());
                                    String pathXml = omitVersion ?
                                            String.format("<path>\n<groupId>%s</groupId>\n<artifactId>%s</artifactId>\n</path>",
                                                    groupId, artifactId) :
                                            String.format("<path>\n<groupId>%s</groupId>\n<artifactId>%s</artifactId>\n<version>%s</version>\n</path>",
                                                    groupId, artifactId, version);
                                    return tg.withContent(ListUtils.concat(tg.getChildren(), Xml.Tag.build(pathXml)));
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

    /**
     * True when the effective maven-compiler-plugin version is 3.12.0 or newer.
     * From 3.12.0 onward the plugin resolves annotation processor path versions
     * from effective {@code <dependencyManagement>}, so callers can safely omit
     * {@code <version>} inside {@code <path>}. When the version cannot be
     * determined (e.g. no version anywhere in the effective POM), this is
     * conservatively false.
     */
    private static boolean compilerPluginSupportsManagedVersions(ResolvedPom resolvedPom) {
        for (Plugin p : ListUtils.concatAll(resolvedPom.getPlugins(), resolvedPom.getPluginManagement())) {
            if (!MAVEN_COMPILER_PLUGIN_GROUP_ID.equals(p.getGroupId()) ||
                    !MAVEN_COMPILER_PLUGIN_ARTIFACT_ID.equals(p.getArtifactId())) {
                continue;
            }
            String effectiveVersion = resolvedPom.getValue(p.getVersion());
            if (effectiveVersion == null) {
                continue;
            }
            return new LatestRelease(null).compare(null, effectiveVersion, "3.12.0") >= 0;
        }
        return false;
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
