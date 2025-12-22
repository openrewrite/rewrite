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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginDependency extends Recipe {
    private static final XPathMatcher PLUGIN_WITH_DEPENDENCIES_MATCHER = new XPathMatcher("/project/build/plugins/plugin[dependencies]");

    @Option(displayName = "Plugin group ID",
            description = "Group ID of the plugin from which the dependency will be removed. Supports glob." +
                    "A Group ID is the first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String pluginGroupId;

    @Option(displayName = "Plugin artifact ID",
            description = "Artifact ID of the plugin from which the dependency will be removed. Supports glob." +
                    "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String pluginArtifactId;

    @Option(displayName = "Group",
            description = "The first part of a plugin dependency coordinate. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a plugin dependency coordinate. Supports glob.",
            example = "guava")
    String artifactId;

    @Override
    public String getDisplayName() {
        return "Remove Maven plugin dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("from `%s:%s`", pluginGroupId, pluginArtifactId);
    }

    @Override
    public String getDescription() {
        return "Removes a dependency from the <dependencies> section of a plugin in the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDependencyVisitor();
    }

    private class RemoveDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag plugin = super.visitTag(tag, ctx);
            if (!PLUGIN_WITH_DEPENDENCIES_MATCHER.matches(getCursor())) {
                return plugin;
            }
            if (!childValueMatches(plugin, "groupId", pluginGroupId) ||
                    !childValueMatches(plugin, "artifactId", pluginArtifactId)) {
                return plugin;
            }
            //noinspection OptionalGetWithoutIsPresent - XPath predicate [dependencies] guarantees this child exists
            Xml.Tag dependencies = plugin.getChild("dependencies").get();
            plugin = filterTagChildren(plugin, dependencies, dependencyTag ->
                    !(childValueMatches(dependencyTag, "groupId", groupId) &&
                            childValueMatches(dependencyTag, "artifactId", artifactId))
            );
            // Remove empty dependencies element
            //noinspection OptionalGetWithoutIsPresent
            Xml.Tag updatedDependencies = plugin.getChild("dependencies").get();
            if (updatedDependencies.getChildren().isEmpty()) {
                plugin = filterTagChildren(plugin, plugin, pluginChildTag ->
                        !"dependencies".equals(pluginChildTag.getName()));
            }
            return plugin;
        }

        private boolean childValueMatches(Xml.Tag tag, String childValueName, String globPattern) {
            return tag.getChildValue(childValueName).map(it -> matchesGlob(it, globPattern)).orElse(false);
        }
    }
}
