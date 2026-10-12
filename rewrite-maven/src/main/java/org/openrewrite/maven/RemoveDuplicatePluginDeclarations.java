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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.style.Style;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.format.TabsAndIndentsVisitor;
import org.openrewrite.xml.style.Autodetect;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.openrewrite.maven.tree.Plugin.PLUGIN_DEFAULT_GROUPID;

public class RemoveDuplicatePluginDeclarations extends Recipe {

    private static final XPathMatcher BUILD_PLUGINS = new XPathMatcher("/project/build/plugins");
    private static final XPathMatcher PROFILE_BUILD_PLUGINS = new XPathMatcher("/project/profiles/profile/build/plugins");
    private static final XPathMatcher MANAGED_PLUGINS = new XPathMatcher("/project/build/pluginManagement/plugins");
    private static final XPathMatcher PROFILE_MANAGED_PLUGINS = new XPathMatcher("/project/profiles/profile/build/pluginManagement/plugins");
    private static final XPathMatcher REPORTING_PLUGINS = new XPathMatcher("/project/reporting/plugins");
    private static final XPathMatcher PROFILE_REPORTING_PLUGINS = new XPathMatcher("/project/profiles/profile/reporting/plugins");

    /// Plugins the Maven 3.9 super POM manages, which every POM's plugin management inherits.
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
            InheritedKeys inherited;

            @Nullable
            TabsAndIndentsStyle indentation;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                inherited = null;
                indentation = null;
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!"plugins".equals(t.getName())) {
                    return t;
                }

                if (BUILD_PLUGINS.matches(getCursor())) {
                    return collapseDuplicates(t, key -> Collapse.MERGE, this::format);
                } else if (PROFILE_BUILD_PLUGINS.matches(getCursor())) {
                    return collapseDuplicates(t, key -> mainBuildKeys(false).contains(key) ?
                            Collapse.MERGE_INTO_MAIN_BUILD :
                            inheritedKeys(ctx).plugins.contains(key) ? Collapse.MERGE : Collapse.KEEP_LAST,
                            this::format);
                } else if (MANAGED_PLUGINS.matches(getCursor())) {
                    return collapseDuplicates(t, key -> managedPluginCollapse(key, ctx), this::format);
                } else if (PROFILE_MANAGED_PLUGINS.matches(getCursor())) {
                    return collapseDuplicates(t, key -> mainBuildKeys(true).contains(key) ?
                            Collapse.MERGE_INTO_MAIN_BUILD : managedPluginCollapse(key, ctx), this::format);
                } else if (REPORTING_PLUGINS.matches(getCursor()) || PROFILE_REPORTING_PLUGINS.matches(getCursor())) {
                    return collapseDuplicates(t, key -> Collapse.KEEP_FIRST, this::format);
                }
                return t;
            }

            /// Indents content placed in the `<plugins>` element being visited the way the document is, then shifts it
            /// to the indentation of the declaration it replaces when that slot uses the same indent character but
            /// sits at a different depth, so it lines up with its neighbours.
            private Content format(Content content, Content slot) {
                if (indentation == null) {
                    Xml.Document document = getCursor().firstEnclosingOrThrow(Xml.Document.class);
                    indentation = Style.from(TabsAndIndentsStyle.class, document, () ->
                            Autodetect.detector().sample(document).build().getStyle(TabsAndIndentsStyle.class));
                }
                Content formatted = (Content) new TabsAndIndentsVisitor<Integer>(indentation)
                        .visitNonNull(content, 0, getCursor());
                String from = indentOf(formatted);
                String to = indentOf(slot);
                boolean sameCharacter = !to.contains(indentation.getUseTabCharacter() ? " " : "\t");
                return sameCharacter && !from.equals(to) && formatted.getPrefix().contains("\n") ?
                        shift(formatted, from, to) : formatted;
            }

            private Collapse managedPluginCollapse(String key, ExecutionContext ctx) {
                return SUPER_POM_MANAGED_PLUGINS.contains(key) || inheritedKeys(ctx).pluginManagement.contains(key) ?
                        Collapse.MERGE : Collapse.KEEP_LAST;
            }

            private Set<String> mainBuildKeys(boolean management) {
                Pom requested = getResolutionResult().getPom().getRequested();
                return pluginKeys((management ? requested.getPluginManagement() : requested.getPlugins()).stream());
            }

            /// Plugins the parent's effective model declares, which Maven 3 merged a child's duplicates into while
            /// assembling inheritance, so the child's duplicates were merged rather than the last one winning.
            private InheritedKeys inheritedKeys(ExecutionContext ctx) {
                if (inherited == null) {
                    ResolvedPom parent = parentPom(ctx);
                    inherited = parent == null ? new InheritedKeys(Collections.emptySet(), Collections.emptySet()) :
                            new InheritedKeys(inheritedPluginKeys(parent.getPlugins()),
                                    inheritedPluginKeys(parent.getPluginManagement()));
                }
                return inherited;
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
                    return downloader.download(parent.getGav(), parent.getRelativePath(), mrr.getPom(),
                                    mrr.getPom().getRepositories())
                            .resolve(mrr.getActiveProfiles(), downloader, ctx);
                } catch (MavenDownloadingException e) {
                    return null;
                }
            }

        };
    }

    private static final List<String> COORDINATES_FIRST = Arrays.asList(
            "groupId", "artifactId", "version", "extensions", "id", "phase");

    private enum Collapse {
        MERGE,
        /// Profile injection merges each duplicate into the main build's declaration in turn, and unlike
        /// inheritance and duplicate normalization it keeps every execution, whether inherited or not.
        MERGE_INTO_MAIN_BUILD,
        KEEP_FIRST,
        KEEP_LAST
    }

    @Value
    private static class InheritedKeys {
        Set<String> plugins;
        Set<String> pluginManagement;
    }

    private static Set<String> inheritedPluginKeys(List<Plugin> plugins) {
        return pluginKeys(plugins.stream()
                .filter(p -> p.getInherited() == null || Boolean.parseBoolean(p.getInherited().trim()) ||
                             !p.getExecutions().isEmpty()));
    }

    private static Set<String> pluginKeys(Stream<Plugin> plugins) {
        return plugins.map(p -> p.getGroupId() + ":" + p.getArtifactId()).collect(toSet());
    }

    private static Xml.Tag collapseDuplicates(Xml.Tag plugins, Function<String, Collapse> collapseFor,
                                              BinaryOperator<Content> format) {
        if (plugins.getContent() == null) {
            return plugins;
        }
        Map<String, List<Xml.Tag>> occurrences = new HashMap<>();
        boolean duplicated = false;
        for (Content content : plugins.getContent()) {
            String key = pluginKey(content);
            if (key != null) {
                List<Xml.Tag> declarations = occurrences.computeIfAbsent(key, k -> new ArrayList<>(1));
                declarations.add((Xml.Tag) content);
                duplicated |= declarations.size() > 1;
            }
        }
        if (!duplicated) {
            return plugins;
        }

        Map<Content, List<Content>> comments = leadingComments(plugins.getContent());
        Map<Content, @Nullable Object> replacements = new IdentityHashMap<>();
        occurrences.forEach((key, duplicates) -> {
            if (duplicates.size() == 1) {
                return;
            }
            Xml.Tag first = duplicates.get(0);
            List<Xml.Tag> later = duplicates.subList(1, duplicates.size());
            List<Content> moved = new ArrayList<>();
            for (Xml.Tag duplicate : later) {
                replacements.put(duplicate, null);
                for (Content comment : comments.get(duplicate)) {
                    replacements.put(comment, null);
                    moved.add(comment);
                }
            }
            Xml.Tag result = withGroupIdOf(collapse(duplicates, collapseFor.apply(key)), first);
            List<Content> placed = afterMovedComments(moved, !comments.get(first).isEmpty(), first.getPrefix(), result);
            boolean unchanged = result == first;
            replacements.put(first, ListUtils.map(placed, c -> unchanged && c == result ? c : format.apply(c, first)));
        });
        return plugins.withContent(ListUtils.flatMap(children(plugins),
                c -> replacements.containsKey(c) ? replacements.get(c) : c));
    }

    private static Xml.Tag collapse(List<Xml.Tag> duplicates, Collapse collapse) {
        if (collapse == Collapse.KEEP_FIRST || collapse == Collapse.KEEP_LAST) {
            return duplicates.get(collapse == Collapse.KEEP_FIRST ? 0 : duplicates.size() - 1);
        }
        Xml.Tag result = duplicates.get(0);
        for (Xml.Tag later : duplicates.subList(1, duplicates.size())) {
            result = mergePlugin(later, result, collapse == Collapse.MERGE);
        }
        return result;
    }

    /// Places `element` after the comments that moved along with it. With no comments of its own above it, the
    /// first moved comment takes over the element's prefix, so blank lines stay above the whole group.
    private static List<Content> afterMovedComments(List<Content> moved, boolean ownComments, String prefix,
                                                    Xml.Tag element) {
        List<Content> content = new ArrayList<>(moved.size() + 1);
        for (Content comment : moved) {
            content.add((Content) comment.withPrefix(ownComments ? singleLine(comment.getPrefix()) : prefix));
            ownComments = true;
            prefix = singleLine(prefix);
        }
        content.add(element.withPrefix(prefix));
        return content;
    }

    /// Keeps whether `<groupId>` is spelled out the way the first declaration had it, which Maven treats the
    /// same, so the diff doesn't churn on it.
    private static Xml.Tag withGroupIdOf(Xml.Tag result, Xml.Tag first) {
        boolean firstHasGroupId = first.getChild("groupId").isPresent();
        Xml.Tag resultGroupId = result.getChild("groupId").orElse(null);
        if (firstHasGroupId && resultGroupId == null) {
            Xml.Tag firstGroupId = first.getChild("groupId").get();
            return result.withContent(ListUtils.insert(children(result), firstGroupId,
                    insertionIndex(children(result), first, firstGroupId)));
        } else if (!firstHasGroupId && resultGroupId != null &&
                   PLUGIN_DEFAULT_GROUPID.equals(trimmedChildValue(result, "groupId", null))) {
            return result.withContent(ListUtils.map(children(result), c -> c == resultGroupId ? null : c));
        }
        return result;
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
        return trimmedChildValue(plugin, "groupId", PLUGIN_DEFAULT_GROUPID) + ":" + artifactId;
    }

    /// Mirrors `DuplicateMerger` in Maven 3's `DefaultModelNormalizer`, which merges each duplicate
    /// build plugin into the one before it with the later declaration dominant.
    private static Xml.Tag mergePlugin(Xml.Tag dominant, Xml.Tag recessive, boolean onlyInheritedExecutions) {
        Xml.Tag merged = inheritChild(dominant, recessive, "version");
        merged = inheritChild(merged, recessive, "extensions");
        merged = inheritChild(merged, recessive, "inherited");
        merged = mergeChild(merged, recessive, "configuration", RemoveDuplicatePluginDeclarations::mergeConfiguration);
        merged = mergeChild(merged, recessive, "dependencies",
                (d, r) -> mergeByKey(d, r, "dependency", RemoveDuplicatePluginDeclarations::dependencyKey));
        Xml.Tag executionSource = onlyInheritedExecutions ? withoutUninheritedExecutions(recessive) : recessive;
        return mergeChild(merged, executionSource, "executions", RemoveDuplicatePluginDeclarations::mergeExecutions);
    }

    /// Maven's merge only carries over the earlier declaration's executions that are inherited, so one marked
    /// `<inherited>false</inherited>`, or belonging to a plugin marked so, never ran.
    private static Xml.Tag withoutUninheritedExecutions(Xml.Tag plugin) {
        Xml.Tag executions = plugin.getChild("executions").orElse(null);
        if (executions == null) {
            return plugin;
        }
        boolean pluginInherited = isInherited(plugin, true);
        Map<Content, List<Content>> comments = leadingComments(children(executions));
        Set<Content> dropped = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Xml.Tag execution : executions.getChildren("execution")) {
            if (!isInherited(execution, pluginInherited)) {
                dropped.add(execution);
                dropped.addAll(comments.get(execution));
            }
        }
        if (dropped.isEmpty()) {
            return plugin;
        }
        Xml.Tag remaining = executions.withContent(ListUtils.filter(children(executions), c -> !dropped.contains(c)));
        boolean anyLeft = remaining.getChild("execution").isPresent();
        Xml.Tag replacement = anyLeft ? remaining : null;
        return plugin.withContent(ListUtils.map(children(plugin), c -> c == executions ? replacement : c));
    }

    private static boolean isInherited(Xml.Tag tag, boolean otherwise) {
        String inherited = trimmedChildValue(tag, "inherited", null);
        return inherited == null ? otherwise : Boolean.parseBoolean(inherited);
    }

    /// Keeps the dominant children and adds the recessive ones whose key isn't among them yet.
    private static Xml.Tag mergeByKey(Xml.Tag dominant, Xml.Tag recessive, String childName,
                                      Function<Xml.Tag, String> key) {
        Set<String> keys = new HashSet<>();
        for (Xml.Tag child : dominant.getChildren(childName)) {
            keys.add(key.apply(child));
        }
        Map<Content, List<Content>> comments = leadingComments(children(recessive));
        List<Content> added = new ArrayList<>();
        for (Xml.Tag child : recessive.getChildren(childName)) {
            if (keys.add(key.apply(child))) {
                added.addAll(comments.get(child));
                added.add(child);
            }
        }
        return dominant.withContent(ListUtils.concatAll(children(dominant), added));
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
        Map<Content, List<Content>> dominantComments = leadingComments(children(dominant));
        Map<Content, List<Content>> recessiveComments = leadingComments(children(recessive));
        Set<Content> moved = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Content> recessiveFirst = ListUtils.flatMap(children(recessive), c -> {
            Xml.Tag sameId = c instanceof Xml.Tag && "execution".equals(((Xml.Tag) c).getName()) ?
                    dominantById.get(executionId((Xml.Tag) c)) : null;
            if (sameId == null) {
                return c;
            }
            moved.add(sameId);
            moved.addAll(dominantComments.get(sameId));
            return afterMovedComments(dominantComments.get(sameId), !recessiveComments.get(c).isEmpty(),
                    c.getPrefix(), mergeExecution(sameId, (Xml.Tag) c));
        });
        return dominant.withContent(ListUtils.concatAll(recessiveFirst,
                ListUtils.filter(children(dominant), c -> !moved.contains(c))));
    }

    private static String executionId(Xml.Tag execution) {
        return trimmedChildValue(execution, "id", "default");
    }

    private static Xml.Tag mergeExecution(Xml.Tag dominant, Xml.Tag recessive) {
        Xml.Tag merged = inheritChild(dominant, recessive, "phase");
        merged = inheritChild(merged, recessive, "inherited");
        merged = mergeChild(merged, recessive, "configuration", RemoveDuplicatePluginDeclarations::mergeConfiguration);
        return mergeChild(merged, recessive, "goals",
                (d, r) -> mergeByKey(d, r, "goal", goal -> goal.getValue().map(String::trim).orElse("")));
    }

    /// Mirrors `Xpp3Dom.mergeXpp3Dom(dominant, recessive)`, including its `combine.self` and
    /// `combine.children` attributes, which is how Maven merges plugin and execution configuration.
    /// Directly inside `<configuration>` the elements are plugin parameters, which Maven looks up by name, so
    /// there a parameter only the earlier declaration had keeps its place instead of being appended.
    private static Xml.Tag mergeConfiguration(Xml.Tag dominant, Xml.Tag recessive) {
        return mergeDom(dominant, recessive, true);
    }

    private static Xml.Tag mergeDom(Xml.Tag dominant, Xml.Tag recessive, boolean parameters) {
        if ("override".equals(attribute(dominant, "combine.self"))) {
            return dominant;
        }

        Xml.Tag merged = dominant;
        if (dominant.getChildren().isEmpty() && recessive.getChildren().isEmpty() &&
            StringUtils.isBlank(dominant.getValue().orElse(null)) &&
            !StringUtils.isBlank(recessive.getValue().orElse(null))) {
            merged = merged.withContent(recessive.getContent());
        }

        Xml.Tag withValue = merged;
        List<Xml.Attribute> inherited = ListUtils.filter(recessive.getAttributes(),
                a -> StringUtils.isBlank(attribute(withValue, a.getKeyAsString())));
        Set<String> inheritedKeys = inherited.stream().map(Xml.Attribute::getKeyAsString).collect(toSet());
        merged = merged.withAttributes(ListUtils.concatAll(
                ListUtils.filter(merged.getAttributes(), a -> !inheritedKeys.contains(a.getKeyAsString())), inherited));

        if (recessive.getChildren().isEmpty()) {
            return merged;
        }
        if ("append".equals(attribute(merged, "combine.children"))) {
            return merged.withContent(ListUtils.concatAll(children(recessive), children(merged)));
        }

        Map<String, Iterator<Xml.Tag>> sameName = new HashMap<>();
        for (Xml.Tag child : recessive.getChildren()) {
            List<Xml.Tag> dominantChildren = merged.getChildren(child.getName());
            if (!dominantChildren.isEmpty()) {
                sameName.putIfAbsent(child.getName(), dominantChildren.iterator());
            }
        }
        Map<Content, List<Content>> dominantComments = leadingComments(children(merged));
        Map<Content, List<Content>> recessiveComments = leadingComments(children(recessive));
        Map<Content, @Nullable Object> replacements = new IdentityHashMap<>();
        List<Xml.Tag> unmatched = new ArrayList<>();
        for (Xml.Tag recessiveChild : recessive.getChildren()) {
            Iterator<Xml.Tag> candidates = sameName.get(recessiveChild.getName());
            if (candidates == null) {
                unmatched.add(recessiveChild);
            } else if (candidates.hasNext()) {
                Xml.Tag dominantChild = candidates.next();
                if ("remove".equals(attribute(dominantChild, "combine.self"))) {
                    replacements.put(dominantChild, null);
                    dominantComments.get(dominantChild).forEach(comment -> replacements.put(comment, null));
                } else {
                    Xml.Tag mergedChild = mergeDom(dominantChild, recessiveChild, false);
                    replacements.put(dominantChild, dominantComments.get(dominantChild).isEmpty() ?
                            ListUtils.concat(recessiveComments.get(recessiveChild), mergedChild) : mergedChild);
                }
            }
        }
        List<Content> content = ListUtils.flatMap(children(merged),
                c -> replacements.containsKey(c) ? replacements.get(c) : c);
        for (Xml.Tag child : unmatched) {
            int index = parameters ? insertionIndex(content, recessive, child) : content == null ? 0 : content.size();
            content = ListUtils.insertAll(content, index, ListUtils.concat(recessiveComments.get(child), child));
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
        Xml.Tag dominantChild = dominant.getChild(name).orElse(null);
        if (dominantChild != null) {
            Xml.Tag merged = merge.apply(dominantChild, recessiveChild);
            return dominant.withContent(ListUtils.map(children(dominant), c -> c == dominantChild ? merged : c));
        }
        List<Content> withComments = ListUtils.concat(leadingComments(children(recessive)).get(recessiveChild),
                recessiveChild);
        return dominant.withContent(ListUtils.insertAll(children(dominant),
                insertionIndex(children(dominant), recessive, recessiveChild), withComments));
    }

    /// Places a child copied over from the earlier declaration next to the same neighbours it had there, keeping
    /// the author's element order.
    private static int insertionIndex(@Nullable List<Content> content, Xml.Tag recessive, Xml.Tag recessiveChild) {
        if (content == null) {
            return 0;
        }
        List<Xml.Tag> siblings = recessive.getChildren();
        int at = siblings.indexOf(recessiveChild);
        for (int i = at - 1; i >= 0; i--) {
            int index = lastIndexOfTag(content, siblings.get(i).getName());
            if (index >= 0) {
                int rank = COORDINATES_FIRST.indexOf(recessiveChild.getName());
                while (index + 1 < content.size()) {
                    Content next = content.get(index + 1);
                    boolean trailingComment = next instanceof Xml.Comment && !next.getPrefix().contains("\n");
                    String nextName = next instanceof Xml.Tag ? ((Xml.Tag) next).getName() : null;
                    boolean conventionallyEarlier = nextName != null && !recessive.getChild(nextName).isPresent() &&
                                                    COORDINATES_FIRST.contains(nextName) &&
                                                    (rank < 0 || COORDINATES_FIRST.indexOf(nextName) < rank);
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

    /// Comments on their own lines directly above a tag describe it, so they travel with it when it is merged.
    private static Map<Content, List<Content>> leadingComments(@Nullable List<? extends Content> content) {
        Map<Content, List<Content>> comments = new IdentityHashMap<>();
        if (content == null) {
            return comments;
        }
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

    private static String singleLine(String prefix) {
        int newline = prefix.lastIndexOf('\n');
        return newline < 0 ? prefix : prefix.substring(newline);
    }

    private static <X extends Xml> X shift(X xml, String from, String to) {
        //noinspection unchecked
        return (X) new XmlVisitor<Integer>() {
            @Override
            public Xml preVisit(Xml tree, Integer p) {
                String prefix = tree.getPrefix();
                int lineStart = prefix.lastIndexOf('\n') + 1;
                if (lineStart == 0 || !prefix.startsWith(from, lineStart)) {
                    return tree;
                }
                String rest = prefix.substring(lineStart + from.length());
                return tree.withPrefix(prefix.substring(0, lineStart) + to + rest);
            }
        }.visitNonNull(xml, 0);
    }

    private static String indentOf(Content content) {
        String prefix = content.getPrefix();
        return prefix.substring(prefix.lastIndexOf('\n') + 1);
    }

    /// The tag's own content list, uncopied, so `ListUtils` can hand it back unchanged and `withContent` can
    /// recognize that nothing changed.
    @SuppressWarnings("unchecked")
    private static @Nullable List<Content> children(Xml.Tag tag) {
        return (List<Content>) tag.getContent();
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
}
