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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.util.Arrays.asList;

public class RemoveDuplicatePluginDeclarations extends Recipe {

    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("//build/plugins");
    private static final XPathMatcher PLUGIN_MANAGEMENT_PLUGINS_MATCHER = new XPathMatcher("//build/pluginManagement/plugins");
    private static final XPathMatcher REPORTING_PLUGINS_MATCHER = new XPathMatcher("//reporting/plugins");

    private static final List<String> CHILD_ORDER = asList(
            "groupId", "artifactId", "version", "extensions", "id", "phase",
            "executions", "dependencies", "goals", "inherited", "configuration");

    @Getter
    final String displayName = "Remove duplicate plugin declarations";

    @Getter
    final String description = "Maven 3.10 and Maven 4 reject duplicate plugin declarations (same groupId and artifactId) " +
        "with an error, where Maven 3.9 and earlier only warned. This recipe collapses each set of duplicates into the " +
        "position of the first declaration, preserving the effective model Maven 3 built from them: duplicate build " +
        "plugins are merged with the later declaration taking precedence, the way Maven 3 merged them, and for plugin " +
        "management the last declaration is kept, as that is the one Maven 3 applied.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isInsideConfiguration()) {
                    return t;
                }

                if (PLUGINS_MATCHER.matches(getCursor())) {
                    return collapseDuplicates(t, (earlier, later) -> mergePlugin(later, earlier));
                } else if (PLUGIN_MANAGEMENT_PLUGINS_MATCHER.matches(getCursor())) {
                    return collapseDuplicates(t, (earlier, later) -> later);
                } else if (REPORTING_PLUGINS_MATCHER.matches(getCursor())) {
                    return collapseDuplicates(t, (earlier, later) -> earlier);
                }

                return t;
            }

            /**
             * Plugin {@code <configuration>} is free-form XML that a plugin interprets itself, so repeated
             * elements there are meaningful rather than duplicates.
             */
            private boolean isInsideConfiguration() {
                return getCursor().getPathAsStream(o -> o instanceof Xml.Tag &&
                                                       "configuration".equals(((Xml.Tag) o).getName()))
                        .findAny()
                        .isPresent();
            }
        };
    }

    private static Xml.Tag collapseDuplicates(Xml.Tag plugins, BinaryOperator<Xml.Tag> combine) {
        if (plugins.getContent() == null) {
            return plugins;
        }
        Map<String, Xml.Tag> combined = new HashMap<>();
        Map<String, Xml.Tag> first = new HashMap<>();
        boolean hasDuplicates = false;
        for (Content content : plugins.getContent()) {
            String key = pluginKey(content);
            if (key != null) {
                Xml.Tag plugin = (Xml.Tag) content;
                Xml.Tag earlier = combined.get(key);
                if (earlier == null) {
                    first.put(key, plugin);
                    combined.put(key, plugin);
                } else {
                    hasDuplicates = true;
                    combined.put(key, combine.apply(earlier, plugin));
                }
            }
        }
        if (!hasDuplicates) {
            return plugins;
        }

        List<Content> collapsed = new ArrayList<>(plugins.getContent().size());
        for (Content content : plugins.getContent()) {
            String key = pluginKey(content);
            if (key == null) {
                collapsed.add(content);
            } else if (first.get(key) == content) {
                collapsed.add(combined.get(key).withPrefix(content.getPrefix()));
            }
        }
        return plugins.withContent(collapsed);
    }

    private static @Nullable String pluginKey(Content content) {
        if (!(content instanceof Xml.Tag) || !"plugin".equals(((Xml.Tag) content).getName())) {
            return null;
        }
        Xml.Tag plugin = (Xml.Tag) content;
        String artifactId = trimmedChildValue(plugin, "artifactId", null);
        if (artifactId == null) {
            return null;
        }
        return trimmedChildValue(plugin, "groupId", "org.apache.maven.plugins") + ":" + artifactId;
    }

    /**
     * Mirrors {@code DuplicateMerger} in Maven 3's {@code DefaultModelNormalizer}, which merges each duplicate
     * build plugin into the one before it with the later declaration dominant.
     */
    private static Xml.Tag mergePlugin(Xml.Tag dominant, Xml.Tag recessive) {
        Xml.Tag merged = inheritChild(dominant, recessive, "version");
        merged = inheritChild(merged, recessive, "extensions");
        merged = inheritChild(merged, recessive, "inherited");
        merged = mergeChild(merged, recessive, "configuration", RemoveDuplicatePluginDeclarations::mergeDom);
        merged = mergeChild(merged, recessive, "dependencies", RemoveDuplicatePluginDeclarations::mergeDependencies);
        return mergeChild(merged, recessive, "executions", RemoveDuplicatePluginDeclarations::mergeExecutions);
    }

    private static Xml.Tag mergeDependencies(Xml.Tag dominant, Xml.Tag recessive) {
        Set<String> dominantKeys = new HashSet<>();
        for (Xml.Tag dependency : dominant.getChildren("dependency")) {
            dominantKeys.add(dependencyKey(dependency));
        }
        List<Content> content = contentOf(dominant);
        for (Xml.Tag dependency : recessive.getChildren("dependency")) {
            if (!dominantKeys.contains(dependencyKey(dependency))) {
                content.add(dependency);
            }
        }
        return dominant.withContent(content);
    }

    private static String dependencyKey(Xml.Tag dependency) {
        String classifier = trimmedChildValue(dependency, "classifier", null);
        return trimmedChildValue(dependency, "groupId", null) + ":" +
               trimmedChildValue(dependency, "artifactId", null) + ":" +
               trimmedChildValue(dependency, "type", "jar") +
               (classifier == null ? "" : ":" + classifier);
    }

    private static Xml.Tag mergeExecutions(Xml.Tag dominant, Xml.Tag recessive) {
        Map<String, Xml.Tag> dominantById = new HashMap<>();
        for (Xml.Tag execution : dominant.getChildren("execution")) {
            dominantById.put(executionId(execution), execution);
        }
        Set<Xml.Tag> mergedIntoRecessive = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Content> content = new ArrayList<>();
        for (Content c : contentOf(recessive)) {
            Xml.Tag sameId = c instanceof Xml.Tag && "execution".equals(((Xml.Tag) c).getName()) ?
                    dominantById.get(executionId((Xml.Tag) c)) : null;
            if (sameId == null) {
                content.add(c);
            } else {
                mergedIntoRecessive.add(sameId);
                content.add(mergeExecution(sameId, (Xml.Tag) c).withPrefix(c.getPrefix()));
            }
        }
        for (Content c : contentOf(dominant)) {
            if (!mergedIntoRecessive.contains(c)) {
                content.add(c);
            }
        }
        return dominant.withContent(content);
    }

    private static String executionId(Xml.Tag execution) {
        return trimmedChildValue(execution, "id", "default");
    }

    private static Xml.Tag mergeExecution(Xml.Tag dominant, Xml.Tag recessive) {
        Xml.Tag merged = inheritChild(dominant, recessive, "phase");
        merged = inheritChild(merged, recessive, "inherited");
        merged = mergeChild(merged, recessive, "configuration", RemoveDuplicatePluginDeclarations::mergeDom);
        return mergeChild(merged, recessive, "goals", RemoveDuplicatePluginDeclarations::mergeGoals);
    }

    private static Xml.Tag mergeGoals(Xml.Tag dominant, Xml.Tag recessive) {
        Set<String> dominantGoals = new HashSet<>();
        for (Xml.Tag goal : dominant.getChildren("goal")) {
            dominantGoals.add(goal.getValue().map(String::trim).orElse(""));
        }
        List<Content> content = contentOf(dominant);
        for (Xml.Tag goal : recessive.getChildren("goal")) {
            if (dominantGoals.add(goal.getValue().map(String::trim).orElse(""))) {
                content.add(goal);
            }
        }
        return dominant.withContent(content);
    }

    /**
     * Mirrors {@code Xpp3Dom.mergeXpp3Dom(dominant, recessive)}, including its {@code combine.self} and
     * {@code combine.children} attributes, which is how Maven merges plugin and execution configuration.
     */
    private static Xml.Tag mergeDom(Xml.Tag dominant, Xml.Tag recessive) {
        if ("override".equals(attribute(dominant, "combine.self"))) {
            return dominant;
        }

        Xml.Tag merged = dominant;
        if (dominant.getChildren().isEmpty() && recessive.getChildren().isEmpty() &&
            isBlank(dominant.getValue().orElse(null)) && !isBlank(recessive.getValue().orElse(null))) {
            merged = merged.withContent(recessive.getContent());
        }

        List<Xml.Attribute> attributes = new ArrayList<>(merged.getAttributes());
        for (Xml.Attribute attribute : recessive.getAttributes()) {
            if (isBlank(attribute(merged, attribute.getKeyAsString()))) {
                attributes.removeIf(a -> a.getKeyAsString().equals(attribute.getKeyAsString()));
                attributes.add(attribute);
            }
        }
        merged = merged.withAttributes(attributes);

        if (recessive.getChildren().isEmpty()) {
            return merged;
        }
        if ("append".equals(attribute(merged, "combine.children"))) {
            List<Content> content = new ArrayList<Content>(recessive.getChildren());
            content.addAll(contentOf(merged));
            return merged.withContent(content);
        }

        Map<String, Iterator<Xml.Tag>> sameName = new HashMap<>();
        for (Xml.Tag child : recessive.getChildren()) {
            List<Xml.Tag> dominantChildren = merged.getChildren(child.getName());
            if (!dominantChildren.isEmpty()) {
                sameName.putIfAbsent(child.getName(), dominantChildren.iterator());
            }
        }
        List<Content> content = contentOf(merged);
        for (Xml.Tag recessiveChild : recessive.getChildren()) {
            Iterator<Xml.Tag> candidates = sameName.get(recessiveChild.getName());
            if (candidates == null) {
                content.add(recessiveChild);
            } else if (candidates.hasNext()) {
                Xml.Tag dominantChild = candidates.next();
                int index = indexOf(content, dominantChild);
                if ("remove".equals(attribute(dominantChild, "combine.self"))) {
                    content.remove(index);
                } else {
                    content.set(index, mergeDom(dominantChild, recessiveChild));
                }
            }
        }
        return merged.withContent(content);
    }

    private static Xml.Tag inheritChild(Xml.Tag dominant, Xml.Tag recessive, String name) {
        return mergeChild(dominant, recessive, name, (d, r) -> d);
    }

    private static Xml.Tag mergeChild(Xml.Tag dominant, Xml.Tag recessive, String name, BinaryOperator<Xml.Tag> merge) {
        Xml.Tag recessiveChild = recessive.getChild(name).orElse(null);
        if (recessiveChild == null) {
            return dominant;
        }
        List<Content> content = contentOf(dominant);
        Xml.Tag dominantChild = dominant.getChild(name).orElse(null);
        if (dominantChild != null) {
            content.set(indexOf(content, dominantChild), merge.apply(dominantChild, recessiveChild));
        } else {
            content.add(insertionIndex(content, name), recessiveChild);
        }
        return dominant.withContent(content);
    }

    private static int insertionIndex(List<Content> content, String name) {
        int rank = CHILD_ORDER.indexOf(name);
        for (int i = 0; i < content.size(); i++) {
            Content c = content.get(i);
            if (c instanceof Xml.Tag && CHILD_ORDER.indexOf(((Xml.Tag) c).getName()) > rank) {
                return i;
            }
        }
        return content.size();
    }

    private static int indexOf(List<Content> content, Content child) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) == child) {
                return i;
            }
        }
        throw new IllegalStateException("Expected " + child + " among the content of its parent");
    }

    private static List<Content> contentOf(Xml.Tag tag) {
        return tag.getContent() == null ? new ArrayList<>() : new ArrayList<>(tag.getContent());
    }

    private static @Nullable String attribute(Xml.Tag tag, String key) {
        for (Xml.Attribute attribute : tag.getAttributes()) {
            if (key.equals(attribute.getKeyAsString())) {
                return attribute.getValueAsString();
            }
        }
        return null;
    }

    private static @Nullable String trimmedChildValue(Xml.Tag tag, String name, @Nullable String defaultValue) {
        return tag.getChildValue(name).map(String::trim).filter(v -> !v.isEmpty()).orElse(defaultValue);
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }
}
