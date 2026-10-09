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
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

public class RemoveDuplicatePluginDeclarations extends Recipe {

    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("//build/plugins");
    private static final XPathMatcher PLUGIN_MANAGEMENT_PLUGINS_MATCHER = new XPathMatcher("//build/pluginManagement/plugins");
    private static final XPathMatcher REPORTING_PLUGINS_MATCHER = new XPathMatcher("//reporting/plugins");

    /**
     * Plugins the Maven 3.9 super POM manages, which every POM's plugin management inherits.
     */
    private static final Set<String> SUPER_POM_MANAGED_PLUGINS = new HashSet<>(Arrays.asList(
            "org.apache.maven.plugins:maven-antrun-plugin",
            "org.apache.maven.plugins:maven-assembly-plugin",
            "org.apache.maven.plugins:maven-dependency-plugin",
            "org.apache.maven.plugins:maven-release-plugin"));

    @Getter
    final String displayName = "Remove duplicate plugin declarations";

    @Getter
    final String description = "Maven 3.10 and Maven 4 reject duplicate plugin declarations (same groupId and artifactId) " +
        "with an error, where Maven 3.9 and earlier only warned. This recipe collapses each set of duplicates into the " +
        "position of the first declaration, preserving the effective model Maven 3 built from them. Maven 3 merged " +
        "duplicates, with the later declaration taking precedence, whenever there was a declaration to merge them " +
        "into: always for build plugins, and for plugins in a profile or in plugin management when the main build or " +
        "a parent also declares the plugin. Otherwise it applied only the last declaration, which is then the one kept.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            Set<String> inheritedPlugins;

            @Nullable
            Set<String> inheritedPluginManagement;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                inheritedPlugins = null;
                inheritedPluginManagement = null;
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isInsideConfiguration()) {
                    return t;
                }

                if (PLUGINS_MATCHER.matches(getCursor())) {
                    boolean inProfile = isInsideProfile();
                    return collapseDuplicates(t, key -> !inProfile || mainBuildKeys(false).contains(key) ||
                                                        inheritedKeys(false, ctx).contains(key) ?
                            MERGE : KEEP_LAST);
                } else if (PLUGIN_MANAGEMENT_PLUGINS_MATCHER.matches(getCursor())) {
                    boolean inProfile = isInsideProfile();
                    return collapseDuplicates(t, key -> SUPER_POM_MANAGED_PLUGINS.contains(key) ||
                                                        inheritedKeys(true, ctx).contains(key) ||
                                                        inProfile && mainBuildKeys(true).contains(key) ?
                            MERGE : KEEP_LAST);
                } else if (REPORTING_PLUGINS_MATCHER.matches(getCursor())) {
                    return collapseDuplicates(t, key -> (earlier, later) -> earlier);
                }

                return t;
            }

            private boolean isInsideProfile() {
                return getCursor().getPathAsStream(o -> o instanceof Xml.Tag && "profile".equals(((Xml.Tag) o).getName()))
                        .findAny()
                        .isPresent();
            }

            private Set<String> mainBuildKeys(boolean management) {
                Pom requested = getResolutionResult().getPom().getRequested();
                return pluginKeys(management ? requested.getPluginManagement() : requested.getPlugins());
            }

            /**
             * Plugins the parent's effective model declares, which Maven 3 merged a child's duplicates into while
             * assembling inheritance, so the child's duplicates were merged rather than the last one winning.
             */
            private Set<String> inheritedKeys(boolean management, ExecutionContext ctx) {
                if (management && inheritedPluginManagement != null) {
                    return inheritedPluginManagement;
                } else if (!management && inheritedPlugins != null) {
                    return inheritedPlugins;
                }
                ResolvedPom parent = parentPom(ctx);
                Set<String> keys = parent == null ? emptySet() :
                        pluginKeys((management ? parent.getPluginManagement() : parent.getPlugins()).stream()
                                .filter(p -> !"false".equals(p.getInherited()) || !p.getExecutions().isEmpty())
                                .collect(toList()));
                if (management) {
                    inheritedPluginManagement = keys;
                } else {
                    inheritedPlugins = keys;
                }
                return keys;
            }

            private @Nullable ResolvedPom parentPom(ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                if (mrr.getParent() != null) {
                    return mrr.getParent().getPom();
                }
                Parent parent = mrr.getPom().getRequested().getParent();
                if (parent == null) {
                    return null;
                }
                try {
                    MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), ctx,
                            MavenExecutionContextView.view(ctx).effectiveSettings(mrr), mrr.getActiveProfiles());
                    return downloader.download(parent.getGav(), parent.getRelativePath(), mrr.getPom(), mrr.getPom().getRepositories())
                            .resolve(mrr.getActiveProfiles(), downloader, ctx);
                } catch (MavenDownloadingException e) {
                    return null;
                }
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

    private static final List<String> COORDINATES_FIRST = Arrays.asList(
            "groupId", "artifactId", "version", "extensions", "id", "phase");

    private static final BinaryOperator<Xml.Tag> MERGE = (earlier, later) -> mergePlugin(later, earlier);
    private static final BinaryOperator<Xml.Tag> KEEP_LAST = (earlier, later) -> later;

    private static Set<String> pluginKeys(List<Plugin> plugins) {
        Set<String> keys = new HashSet<>();
        for (Plugin plugin : plugins) {
            keys.add(plugin.getGroupId() + ":" + plugin.getArtifactId());
        }
        return keys;
    }

    private static Xml.Tag collapseDuplicates(Xml.Tag plugins, Function<String, BinaryOperator<Xml.Tag>> combineFor) {
        if (plugins.getContent() == null) {
            return plugins;
        }
        Map<String, List<Xml.Tag>> occurrences = new HashMap<>();
        for (Content content : plugins.getContent()) {
            String key = pluginKey(content);
            if (key != null) {
                occurrences.computeIfAbsent(key, k -> new ArrayList<>()).add((Xml.Tag) content);
            }
        }
        if (occurrences.values().stream().allMatch(o -> o.size() == 1)) {
            return plugins;
        }

        Map<Content, List<Content>> comments = leadingComments(plugins.getContent());
        Set<Content> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<Xml.Tag> duplicates : occurrences.values()) {
            for (Xml.Tag duplicate : duplicates.subList(1, duplicates.size())) {
                removed.add(duplicate);
                removed.addAll(comments.get(duplicate));
            }
        }

        List<Content> collapsed = new ArrayList<>(plugins.getContent().size());
        for (Content content : plugins.getContent()) {
            if (removed.contains(content)) {
                continue;
            }
            String key = pluginKey(content);
            List<Xml.Tag> duplicates = key == null ? null : occurrences.get(key);
            if (duplicates == null || duplicates.size() == 1) {
                collapsed.add(content);
                continue;
            }
            BinaryOperator<Xml.Tag> combine = combineFor.apply(key);
            Xml.Tag result = duplicates.get(0);
            for (Xml.Tag later : duplicates.subList(1, duplicates.size())) {
                result = combine.apply(result, later);
            }
            String prefix = withIndent(content.getPrefix(), indentOf(result));
            boolean ownComments = !comments.get(content).isEmpty();
            for (Xml.Tag later : duplicates.subList(1, duplicates.size())) {
                for (Content comment : comments.get(later)) {
                    Content moved = reindent(comment, later, result);
                    collapsed.add((Content) moved.withPrefix(ownComments ? singleLine(moved.getPrefix()) : prefix));
                    ownComments = true;
                    prefix = singleLine(prefix);
                }
            }
            collapsed.add(result.withPrefix(prefix));
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
    private static Xml.Tag mergePlugin(Xml.Tag dominant, Xml.Tag earlier) {
        Xml.Tag recessive = reindent(earlier, earlier, dominant);
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
        Map<Content, List<Content>> dominantComments = leadingComments(contentOf(dominant));
        Set<Content> moved = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Content> content = new ArrayList<>();
        for (Content c : contentOf(recessive)) {
            Xml.Tag sameId = c instanceof Xml.Tag && "execution".equals(((Xml.Tag) c).getName()) ?
                    dominantById.get(executionId((Xml.Tag) c)) : null;
            if (sameId == null) {
                content.add(c);
            } else {
                moved.add(sameId);
                String prefix = withIndent(c.getPrefix(), indentOf(sameId));
                for (Content comment : dominantComments.get(sameId)) {
                    moved.add(comment);
                    content.add((Content) comment.withPrefix(singleLine(comment.getPrefix())));
                    prefix = singleLine(prefix);
                }
                content.add(mergeExecution(sameId, (Xml.Tag) c).withPrefix(prefix));
            }
        }
        for (Content c : contentOf(dominant)) {
            if (!moved.contains(c)) {
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
            content.add(insertionIndex(content, recessive, recessiveChild), recessiveChild);
        }
        return dominant.withContent(content);
    }

    /**
     * Places a child copied over from the earlier declaration next to the same neighbours it had there, keeping
     * the author's element order.
     */
    private static int insertionIndex(List<Content> content, Xml.Tag recessive, Xml.Tag recessiveChild) {
        List<Xml.Tag> siblings = recessive.getChildren();
        int at = siblings.indexOf(recessiveChild);
        for (int i = at - 1; i >= 0; i--) {
            int index = lastIndexOfTag(content, siblings.get(i).getName());
            if (index >= 0) {
                int rank = COORDINATES_FIRST.indexOf(recessiveChild.getName());
                while (index + 1 < content.size()) {
                    Content next = content.get(index + 1);
                    boolean trailingComment = next instanceof Xml.Comment && !next.getPrefix().contains("\n");
                    boolean conventionallyEarlier = next instanceof Xml.Tag &&
                                                    !recessive.getChild(((Xml.Tag) next).getName()).isPresent() &&
                                                    COORDINATES_FIRST.contains(((Xml.Tag) next).getName()) &&
                                                    (rank < 0 || COORDINATES_FIRST.indexOf(((Xml.Tag) next).getName()) < rank);
                    if (!trailingComment && !conventionallyEarlier) {
                        break;
                    }
                    index++;
                }
                return index + 1;
            }
        }
        Map<Content, List<Content>> comments = leadingComments(content);
        for (int i = at + 1; i < siblings.size(); i++) {
            int index = lastIndexOfTag(content, siblings.get(i).getName());
            if (index >= 0) {
                return index - comments.get(content.get(index)).size();
            }
        }
        return content.size();
    }

    private static int lastIndexOfTag(List<Content> content, String name) {
        for (int i = content.size() - 1; i >= 0; i--) {
            if (content.get(i) instanceof Xml.Tag && name.equals(((Xml.Tag) content.get(i)).getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Comments on their own lines directly above a tag describe it, so they travel with it when it is merged.
     */
    private static Map<Content, List<Content>> leadingComments(List<? extends Content> content) {
        Map<Content, List<Content>> comments = new IdentityHashMap<>();
        List<Content> pending = new ArrayList<>();
        for (Content c : content) {
            if (c instanceof Xml.Comment) {
                if (!pending.isEmpty() || c.getPrefix().contains("\n")) {
                    pending.add(c);
                }
            } else {
                comments.put(c, pending);
                pending = new ArrayList<>();
            }
        }
        return comments;
    }

    private static String indentOf(Content content) {
        String prefix = content.getPrefix();
        return prefix.substring(prefix.lastIndexOf('\n') + 1);
    }

    private static String singleLine(String prefix) {
        int newline = prefix.lastIndexOf('\n');
        return newline < 0 ? prefix : prefix.substring(newline);
    }

    private static String withIndent(String prefix, String indent) {
        int newline = prefix.lastIndexOf('\n');
        return newline < 0 ? prefix : prefix.substring(0, newline + 1) + indent;
    }

    /**
     * Converts the indentation of {@code xml} from the style {@code from} is written in to the style of {@code to},
     * level by level, so content moved between two declarations doesn't mix tabs and spaces.
     */
    private static <X extends Xml> X reindent(X xml, Xml.Tag from, Xml.Tag to) {
        String fromBase = indentOf(from);
        String toBase = indentOf(to);
        String fromUnit = indentUnit(from);
        String toUnit = fromUnit.isEmpty() || indentUnit(to).isEmpty() ? fromUnit : indentUnit(to);
        if (fromBase.equals(toBase) && fromUnit.equals(toUnit)) {
            return xml;
        }
        //noinspection unchecked
        return (X) new XmlVisitor<Integer>() {
            @Override
            public Xml preVisit(Xml tree, Integer p) {
                String prefix = tree.getPrefix();
                int lineStart = prefix.lastIndexOf('\n') + 1;
                if (lineStart == 0 || !prefix.startsWith(fromBase, lineStart)) {
                    return tree;
                }
                String rest = prefix.substring(lineStart + fromBase.length());
                StringBuilder indent = new StringBuilder(toBase);
                while (!fromUnit.isEmpty() && rest.startsWith(fromUnit)) {
                    indent.append(toUnit);
                    rest = rest.substring(fromUnit.length());
                }
                return tree.withPrefix(prefix.substring(0, lineStart) + indent + rest);
            }
        }.visitNonNull(xml, 0);
    }

    private static String indentUnit(Xml.Tag tag) {
        String indent = indentOf(tag);
        for (Content child : contentOf(tag)) {
            if (child.getPrefix().contains("\n")) {
                String childIndent = indentOf(child);
                return childIndent.startsWith(indent) ? childIndent.substring(indent.length()) : "";
            }
        }
        return "";
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
