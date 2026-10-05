/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginGoal extends Recipe {

    @Option(displayName = "Plugin group ID",
            description = "Group ID of the plugin from which the goal will be removed. Supports glob. " +
                    "A Group ID is the first part of a plugin coordinate 'org.apache.maven.plugins:maven-compiler-plugin:VERSION'.",
            example = "org.apache.maven.plugins")
    String pluginGroupId;

    @Option(displayName = "Plugin artifact ID",
            description = "Artifact ID of the plugin from which the goal will be removed. Supports glob. " +
                    "The second part of a plugin coordinate 'org.apache.maven.plugins:maven-compiler-plugin:VERSION'.",
            example = "maven-compiler-plugin")
    String pluginArtifactId;

    @Option(displayName = "Goal",
            description = "The goal to remove. Matching is case-insensitive and supports glob.",
            example = "compile")
    String goal;

    String displayName = "Remove Maven plugin goal";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` from `%s:%s`", goal, pluginGroupId, pluginArtifactId);
    }

    String description = "Removes a goal from a Maven plugin wherever it is declared: directly under a `<plugin>`, " +
            "inside `<executions>`, and within `<build>`, `<pluginManagement>`, or `<profiles>`. " +
            "If removing the goal leaves an `<execution>` with no remaining goals, the execution is removed. " +
            "If all executions are removed, the `<executions>` element is also removed.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = super.visitTag(tag, ctx);
                if (!isPluginTag(pluginGroupId, pluginArtifactId) || isWithinConfiguration()) {
                    return plugin;
                }
                Xml.Tag updated = mapTagChildren(plugin, child -> {
                    if ("goals".equals(child.getName())) {
                        return filterGoals(child);
                    }
                    if ("executions".equals(child.getName())) {
                        return filterExecutions(child);
                    }
                    return child;
                });
                if (updated != plugin) {
                    maybeUpdateModel();
                }
                return updated;
            }

            private boolean isWithinConfiguration() {
                return getCursor().getPathAsStream(v -> v instanceof Xml.Tag && "configuration".equals(((Xml.Tag) v).getName()))
                        .findAny()
                        .isPresent();
            }

            private Xml.@Nullable Tag filterGoals(Xml.Tag goals) {
                Xml.Tag filtered = filterTagChildren(goals, goalTag ->
                        !goalTag.getValue().map(v -> matchesGlob(v, goal)).orElse(false));
                // Only cascade the cleanup of a now-empty element when a goal was actually removed
                return filtered != goals && filtered.getChildren().isEmpty() ? null : filtered;
            }

            private Xml.@Nullable Tag filterExecutions(Xml.Tag executions) {
                Xml.Tag mapped = mapTagChildren(executions, execution -> {
                    Xml.Tag goals = "execution".equals(execution.getName()) ?
                            execution.getChild("goals").orElse(null) : null;
                    if (goals == null) {
                        return execution;
                    }
                    Xml.Tag filtered = filterGoals(goals);
                    if (filtered == null) {
                        // the execution's last goal was removed, so the execution goes with it
                        return null;
                    }
                    if (filtered == goals) {
                        return execution;
                    }
                    return mapTagChildren(execution, child -> child == goals ? filtered : child);
                });
                return mapped != executions && mapped.getChildren().isEmpty() ? null : mapped;
            }
        };
    }
}
