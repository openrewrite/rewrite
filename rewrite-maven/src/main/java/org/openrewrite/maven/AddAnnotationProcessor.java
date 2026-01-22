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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.trait.MavenPlugin;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddAnnotationProcessor extends Recipe {
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

    String description = "Add an annotation processor to the maven compiler plugin. Will not do anything if it already exists. " +
            "Also doesn't add anything when no other annotation processors are defined yet. " +
            "(Perhaps `ChangePluginConfiguration` can be used).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                if (tree == null) {
                    return tree;
                }

                boolean isMultiModule = tree.getMarkers()
                        .findFirst(MavenResolutionResult.class)
                        .map(MavenResolutionResult::getProjectPoms)
                        .map(Map::size)
                        .map(size -> size > 1)
                        .orElseThrow(() -> new IllegalStateException("Unable to determine the number of project poms"));

                // maybe add the plugin to //build/pluginManagement/plugins or //build/plugins is not present yet
                tree = new AddPluginVisitor(isMultiModule, MAVEN_COMPILER_PLUGIN_GROUP_ID, MAVEN_COMPILER_PLUGIN_ARTIFACT_ID, null, "<configuration><annotationProcessorPaths/></configuration>", null, null, null).visit(tree, ctx);

                return new AddAnnotationProcessorPath(isMultiModule).visit(tree, ctx);
            }

            class AddAnnotationProcessorPath extends MavenIsoVisitor<ExecutionContext> {
                private final boolean addToManaged;

                AddAnnotationProcessorPath(boolean addToManaged) {
                    this.addToManaged = addToManaged;
                }

                @Override
                public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    Xml.Tag plugins = super.visitTag(tag, ctx);
                    plugins = (Xml.Tag)  new MavenPlugin.Matcher(addToManaged,MAVEN_COMPILER_PLUGIN_GROUP_ID, MAVEN_COMPILER_PLUGIN_ARTIFACT_ID).asVisitor(plugin -> {

                        MavenResolutionResult mrr = getResolutionResult();
                        AtomicReference<TreeVisitor<?, ExecutionContext>> afterVisitor = new AtomicReference<>();
                        Xml.Tag modifiedPlugin = new XmlIsoVisitor<ExecutionContext>() {
                            @Override
                            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                                Xml.Tag tg = super.visitTag(tag, ctx);
                                if (!"annotationProcessorPaths".equals(tg.getName())) {
                                    return tg;
                                }
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
                                                mrr.getPom().getValue(oldVersion.trim()) :
                                                oldVersion;
                                        VersionComparator comparator = Semver.validate(lookupVersion, null).getValue();
                                        if (comparator.compare(version, lookupVersion) > 0) {
                                            if (oldVersionUsesProperty) {
                                                afterVisitor.set(new ChangePropertyValue(oldVersion, version, null, null).getVisitor());
                                            } else {
                                                List<Xml.Tag> tags = tg.getChildren();
                                                tags.set(i, child.withChildValue("version", version));
                                                return tg.withContent(tags);
                                            }
                                        }
                                    }
                                    return tg;
                                }
                                return tg.withContent(ListUtils.concat(tg.getChildren(), Xml.Tag.build(String.format(
                                        "<path>\n<groupId>%s</groupId>\n<artifactId>%s</artifactId>\n<version>%s</version>\n</path>",
                                        groupId, artifactId, version))));
                            }
                        }.visitTag(plugin.getTree(), ctx);
                        if (afterVisitor.get() != null) {
                            doAfterVisit(afterVisitor.get());
                        }
                        return modifiedPlugin;
                    }).visitNonNull(plugins, 0);
                    if (plugins != tag) {
                        plugins = autoFormat(plugins, ctx);
                    }
                    return plugins;
                }
            }
        };
    }
}
