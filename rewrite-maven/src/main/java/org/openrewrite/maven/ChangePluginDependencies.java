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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginDependencies extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "Dependencies",
            description = "Plugin dependencies provided as dependency coordinates of format \"groupId:artifactId:version\". " +
                          "When supplying multiple coordinates separate them with \",\". " +
                          "Supplying `null` will remove any existing plugin dependencies.",
            example = "org.openrewrite.recipe:rewrite-spring:1.0.0, org.openrewrite.recipe:rewrite-testing-frameworks:1.0.0",
            required = false)
    @Nullable
    String dependencies;

    @Override
    public String getDisplayName() {
        return "Change Maven plugin dependencies";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Applies the specified dependencies to a Maven plugin. Will not add the plugin if it does not already exist in the pom.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Xml.Tag dependenciesTag;
        if (dependencies == null) {
            dependenciesTag = null;
        } else {
            dependenciesTag = Xml.Tag.build(
                    "<dependencies>\n" +
                    Arrays.stream(dependencies.split(","))
                            .map(String::trim)
                            .map(gav -> {
                                String[] gavs = gav.split(":");
                                return "<dependency>\n<groupId>" + gavs[0] + "</groupId>\n<artifactId>" + gavs[1] +
                                       "</artifactId>\n<version>" + gavs[2] + "</version>\n</dependency>";
                            }).collect(Collectors.joining("\n")) +
                    "\n</dependencies>\n");
        }
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugins = (Xml.Tag) super.visitTag(tag, ctx);
                if (PLUGINS_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybePlugin = plugins.getChildren().stream()
                            .filter(plugin ->
                                    "plugin".equals(plugin.getName()) &&
                                    groupId.equals(plugin.getChildValue("groupId").orElse(null)) &&
                                    artifactId.equals(plugin.getChildValue("artifactId").orElse(null))
                            )
                            .findAny();
                    if (maybePlugin.isPresent()) {
                        Xml.Tag plugin = maybePlugin.get();
                        if (dependencies == null) {
                            plugins = filterChildren(plugins, plugin, child -> !(child instanceof Xml.Tag && "dependencies".equals(((Xml.Tag) child).getName())));
                        } else {
                            plugins = addOrUpdateChild(plugins, plugin, dependenciesTag, getCursor().getParentOrThrow());
                        }
                    }
                }
                return plugins;
            }
        };
    }
}
