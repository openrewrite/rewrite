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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.trait.MavenPlugin;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddAnnotationProcessor extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");
    private static final String MAVEN_COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

    @Option(displayName = "Group",
            description = "The first part of the coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the processor to add.",
            example = "org.projectlombok")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the processor to add.",
            example = "lombok-mapstruct-binding")
    String artifactId;

    @Option(displayName = "Version",
            description = "The third part of a coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the processor to add. Note that an exact version is expected",
            example = "0.2.0")
    String version;

    @Override
    public String getDisplayName() {
        return "Add an annotation processor to the maven compiler plugin";
    }

    @Override
    public String getDescription() {
        return "Add an annotation processor to the maven compiler plugin. Will not do anything if it already exists. Also doesn't add anything when no other annotation processors are defined yet (Perhaps `ChangePluginConfiguration` can be used).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugins = (Xml.Tag) super.visitTag(tag, ctx);
                plugins = (Xml.Tag) new MavenPlugin.Matcher().asVisitor(plugin -> {
                    if (MAVEN_COMPILER_PLUGIN_GROUP_ID.equals(plugin.getGroupId())
                        && MAVEN_COMPILER_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                        return new XmlIsoVisitor<ExecutionContext>() {
                            @Override
                            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                                Xml.Tag tg = super.visitTag(tag, ctx);
                                if ("annotationProcessorPaths".equals(tg.getName())) {
                                    boolean found = false;
                                    for (Xml.Tag child : tg.getChildren()) {
                                        if (groupId.equals(child.getChildValue("groupId").orElse(null))
                                            && artifactId.equals(child.getChildValue("artifactId").orElse(null))) {
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        return tg.withContent(
                                                ListUtils.concat(tg.getChildren(),
                                                        Xml.Tag.build(String.format("<path>\n<groupId>%s</groupId>\n<artifactId>%s</artifactId>\n<version>%s</version>\n</path>", groupId, artifactId, version))));
                                    }
                                }
                                return tg;
                            }
                        }.visitTag(plugin.getTree(), ctx);
                    }
                    return plugin.getTree();
                }).visit(plugins, 0);
                if (plugins != tag) {
                    plugins = autoFormat(plugins, ctx);
                }
                return plugins;
            }
        };
    }
}
