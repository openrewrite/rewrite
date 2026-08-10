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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginGoal extends Recipe {
    private static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("//plugins/plugin");

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
            description = "The goal to remove. Goal names are case-sensitive. Supports glob.",
            example = "compile")
    String goal;

    String displayName = "Remove Maven plugin goal";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` from `%s:%s`", goal, pluginGroupId, pluginArtifactId);
    }

    @Override
    public String getDescription() {
        return "Removes a goal from a Maven plugin wherever it is declared: directly under a `<plugin>`, " +
                "inside `<executions>`, and within `<build>`, `<pluginManagement>`, or `<profiles>`. " +
                "If removing the goal leaves an `<execution>` with no remaining goals, the execution is removed. " +
                "If all executions are removed, the `<executions>` element is also removed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveGoalVisitor();
    }

    private class RemoveGoalVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag plugin = super.visitTag(tag, ctx);
            if (!PLUGIN_MATCHER.matches(getCursor())) {
                return plugin;
            }
            if (!pluginMatches(plugin)) {
                return plugin;
            }
            plugin = removeDirectGoals(plugin);
            plugin = removeExecutionGoals(plugin);

            return plugin;
        }

        private Xml.Tag removeDirectGoals(Xml.Tag plugin) {
            Xml.Tag goals = plugin.getChild("goals").orElse(null);
            if (goals == null) {
                return plugin;
            }
            plugin = filterMatchingGoals(plugin, goals);

            return removeChildIfEmpty(plugin, "goals");
        }

        private Xml.Tag removeExecutionGoals(Xml.Tag plugin) {
            Xml.Tag executions = plugin.getChild("executions").orElse(null);
            if (executions == null) {
                return plugin;
            }
            for (Xml.Tag execution : executions.getChildren("execution")) {
                plugin = removeGoalFromExecution(plugin, execution);
            }

            return removeExecutionsIfEmpty(plugin);
        }

        private Xml.Tag removeGoalFromExecution(Xml.Tag plugin, Xml.Tag executionIdentity) {
            Xml.Tag execution = findCurrentExecution(plugin, executionIdentity).orElse(null);
            if (execution == null) {
                return plugin;
            }
            Xml.Tag goals = execution.getChild("goals").orElse(null);
            if (goals == null) {
                return plugin;
            }
            plugin = filterMatchingGoals(plugin, goals);

            return removeExecutionIfGoalsEmpty(plugin, executionIdentity);
        }

        private Xml.Tag filterMatchingGoals(Xml.Tag plugin, Xml.Tag goals) {
            Predicate<Xml.Tag> isNotMatchingGoal = goalTag ->
                    !goalTag.getValue().map(v -> matchesGlob(v, goal)).orElse(false);

            return filterTagChildren(plugin, goals, isNotMatchingGoal);
        }

        private Xml.Tag removeExecutionIfGoalsEmpty(Xml.Tag plugin, Xml.Tag executionIdentity) {
            Xml.Tag execution = findCurrentExecution(plugin, executionIdentity).orElse(null);
            if (execution == null) {
                return plugin;
            }
            if (isGoalsEmpty(execution)) {
                Xml.Tag executions = plugin.getChild("executions").orElse(null);
                if (executions != null) {
                    Predicate<Xml.Tag> isNotThisExecution = e -> !e.isScope(executionIdentity);
                    plugin = filterTagChildren(plugin, executions, isNotThisExecution);
                }
            }

            return plugin;
        }

        private Xml.Tag removeExecutionsIfEmpty(Xml.Tag plugin) {
            return removeChildIfEmpty(plugin, "executions");
        }

        private Xml.Tag removeChildIfEmpty(Xml.Tag plugin, String childName) {
            Xml.Tag child = plugin.getChild(childName).orElse(null);
            if (child != null && child.getChildren().isEmpty()) {
                Predicate<Xml.Tag> isNotChild = c -> !childName.equals(c.getName());
                plugin = filterTagChildren(plugin, plugin, isNotChild);
            }

            return plugin;
        }

        private boolean isGoalsEmpty(Xml.Tag execution) {
            Optional<Xml.Tag> goals = execution.getChild("goals");

            return goals.map(g -> g.getChildren().isEmpty()).orElse(false);
        }

        private Optional<Xml.Tag> findCurrentExecution(Xml.Tag plugin, Xml.Tag executionIdentity) {
            Optional<Xml.Tag> executions = plugin.getChild("executions");

            return executions.flatMap(e -> e.getChildren("execution").stream()
                    .filter(ex -> ex.isScope(executionIdentity))
                    .findFirst());
        }

        private boolean pluginMatches(Xml.Tag plugin) {
            String groupId = plugin.getChildValue("groupId").orElse("org.apache.maven.plugins");
            boolean groupIdMatches = matchesGlob(groupId, pluginGroupId);
            boolean artifactIdMatches = plugin.getChildValue("artifactId")
                    .map(v -> matchesGlob(v, pluginArtifactId))
                    .orElse(false);

            return groupIdMatches && artifactIdMatches;
        }
    }
}
