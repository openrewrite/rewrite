/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.trait.Trait;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A named catalog in a {@code dependencyResolutionManagement { versionCatalogs { ... } } } block,
 * either a Groovy {@code libs { ... } } closure or a Kotlin {@code create("libs") { ... } } call.
 */
@Value
class SettingsVersionCatalog implements VersionCatalog {
    Cursor cursor;
    String catalogName;

    @Override
    public J.MethodInvocation getTree() {
        return cursor.getValue();
    }

    @Override
    public Map<GroupArtifact, Library> getLibraryVersions() {
        Map<GroupArtifact, Library> libraries = new LinkedHashMap<>();
        new Library.Matcher().lower(cursor).forEach(library -> {
            GroupArtifact ga = library.getGroupArtifact();
            if (ga != null) {
                libraries.putIfAbsent(ga, library);
            }
        });
        return libraries;
    }

    @Override
    public Map<String, Plugin> getPluginVersions() {
        Map<String, Plugin> plugins = new LinkedHashMap<>();
        new Plugin.Matcher().lower(cursor).forEach(plugin -> {
            String pluginId = plugin.getPluginId();
            if (pluginId != null) {
                plugins.putIfAbsent(pluginId, plugin);
            }
        });
        return plugins;
    }

    @Override
    public Map<String, String> getVersionDeclarations() {
        Map<String, String> versions = new LinkedHashMap<>();
        new Version.Matcher().lower(cursor).forEach(version -> {
            String alias = version.getAlias();
            String value = version.getVersion();
            if (alias != null && value != null) {
                versions.put(alias, value);
            }
        });
        return versions;
    }

    @Override
    public SettingsVersionCatalog withLibraryVersion(GroupArtifact ga, String newVersion) {
        return with(new Library.Matcher(), library -> ga.equals(library.getGroupArtifact()), library -> library.withVersion(newVersion));
    }

    @Override
    public SettingsVersionCatalog withDetachedLibraryVersion(GroupArtifact ga, String newVersion) {
        return with(new Library.Matcher(), library -> ga.equals(library.getGroupArtifact()), library -> library.withDetachedVersion(newVersion));
    }

    @Override
    public SettingsVersionCatalog withVersionDeclarationValue(String alias, String newVersion) {
        return with(new Version.Matcher(), version -> alias.equals(version.getAlias()), version -> version.withVersion(newVersion));
    }

    private <U extends Trait<J.MethodInvocation>> SettingsVersionCatalog with(GradleTraitMatcher<U> matcher, Predicate<U> match, UnaryOperator<U> edit) {
        J newTree = (J) matcher.<ExecutionContext>asVisitor((trait, ctx) -> match.test(trait) ? edit.apply(trait).getTree() : trait.getTree())
                .visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return newTree == getTree() ? this : new SettingsVersionCatalog(new Cursor(cursor.getParent(), newTree), catalogName);
    }

    /**
     * A statement of the enclosing block, as opposed to the receiver of a chained call.
     */
    private static boolean isTopLevelStatement(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        if (parent.getValue() instanceof J.Return) {
            // Groovy closures implicitly return their last expression through a synthetic Return
            parent = parent.getParentTreeCursor();
        }
        return !parent.isRoot() && parent.getValue() instanceof J.Block;
    }

    private static boolean isChained(J.MethodInvocation m, String name) {
        return name.equals(m.getSimpleName()) && m.getArguments().size() == 1 && m.getSelect() instanceof J.MethodInvocation;
    }

    /**
     * The {@code library(...)} or {@code plugin(...)} call, whether {@code outer} is it or a call
     * chained onto it.
     */
    private static J.MethodInvocation entryCall(J.MethodInvocation outer, String name) {
        return outer.getSelect() instanceof J.MethodInvocation && name.equals(((J.MethodInvocation) outer.getSelect()).getSimpleName()) ?
                (J.MethodInvocation) outer.getSelect() : outer;
    }

    /**
     * The coordinates of a {@code library(alias, "group:artifact:version")} declaration.
     */
    private static @Nullable Dependency coordinates(J.MethodInvocation library) {
        if ("library".equals(library.getSimpleName()) && library.getArguments().size() == 2) {
            Dependency dependency = DependencyNotation.parse(literalArgument(library, 1));
            return dependency != null && dependency.getGroupId() != null && dependency.getVersion() != null ? dependency : null;
        }
        return null;
    }

    private static @Nullable String literalArgument(J.MethodInvocation m, int index) {
        if (index < m.getArguments().size()) {
            Expression argument = m.getArguments().get(index);
            if (argument instanceof J.Literal && ((J.Literal) argument).getValue() instanceof String) {
                return (String) ((J.Literal) argument).getValue();
            }
        }
        return null;
    }

    private static J.MethodInvocation withLiteralArgument(J.MethodInvocation m, int index, String value) {
        return m.withArguments(ListUtils.map(m.getArguments(), (i, argument) ->
                i == index && argument instanceof J.Literal ? ChangeStringLiteral.withStringValue((J.Literal) argument, value) : argument));
    }

    /**
     * A {@code library(alias, group, artifact)} declaration with its {@code .version(...)},
     * {@code .versionRef(...)} or {@code .withoutVersion()}, or a
     * {@code library(alias, "group:artifact:version")} one.
     */
    @Value
    private static class Library implements Trait<J.MethodInvocation>, VersionCatalog.Entry {
        Cursor cursor;

        private @Nullable GroupArtifact getGroupArtifact() {
            J.MethodInvocation library = entryCall(getTree(), "library");
            if (library.getArguments().size() == 3) {
                String groupId = literalArgument(library, 1);
                String artifactId = literalArgument(library, 2);
                return groupId == null || artifactId == null ? null : new GroupArtifact(groupId, artifactId);
            }
            Dependency dependency = coordinates(library);
            return dependency == null ? null : new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId());
        }

        @Override
        public @Nullable String getVersionRef() {
            return isChained(getTree(), "versionRef") ? literalArgument(getTree(), 0) : null;
        }

        @Override
        public @Nullable String getVersion() {
            if (isChained(getTree(), "version")) {
                return literalArgument(getTree(), 0);
            }
            Dependency dependency = coordinates(getTree());
            return dependency == null ? null : dependency.getVersion();
        }

        private Library withVersion(String newVersion) {
            if (isChained(getTree(), "version")) {
                return withTree(withLiteralArgument(getTree(), 0, newVersion));
            }
            Dependency dependency = coordinates(getTree());
            if (dependency == null) {
                return this;
            }
            String notation = DependencyNotation.toStringNotation(dependency.withGav(dependency.getGav().withVersion(newVersion)));
            return withTree(withLiteralArgument(getTree(), 1, notation));
        }

        /**
         * Replaces the chained {@code .versionRef(...)} with {@code .version(newVersion)}.
         */
        private Library withDetachedVersion(String newVersion) {
            if (!isChained(getTree(), "versionRef")) {
                return this;
            }
            J.MethodInvocation renamed = getTree().withName(getTree().getName().withSimpleName("version"));
            return withTree(withLiteralArgument(renamed, 0, newVersion));
        }

        private Library withTree(J.MethodInvocation tree) {
            return tree == getTree() ? this : new Library(new Cursor(cursor.getParent(), tree));
        }

        private static class Matcher extends GradleTraitMatcher<Library> {
            @Override
            protected @Nullable Library test(Cursor cursor) {
                Object value = cursor.getValue();
                if (!(value instanceof J.MethodInvocation) || !isTopLevelStatement(cursor) || !withinBlock(cursor, "versionCatalogs")) {
                    return null;
                }
                J.MethodInvocation outer = (J.MethodInvocation) value;
                J.MethodInvocation library = entryCall(outer, "library");
                if (!"library".equals(library.getSimpleName()) || literalArgument(library, 0) == null) {
                    return null;
                }
                if (library != outer) {
                    boolean versionChain = ("version".equals(outer.getSimpleName()) || "versionRef".equals(outer.getSimpleName())) && outer.getArguments().size() == 1;
                    if (!versionChain && !("withoutVersion".equals(outer.getSimpleName()) && outer.getArguments().isEmpty())) {
                        return null;
                    }
                }
                if (library.getArguments().size() == 3) {
                    return literalArgument(library, 1) != null && literalArgument(library, 2) != null ? new Library(cursor) : null;
                }
                return library == outer && coordinates(library) != null ? new Library(cursor) : null;
            }
        }
    }

    /**
     * A {@code plugin(alias, id)} declaration with its {@code .version(...)} or
     * {@code .versionRef(...)}.
     */
    @Value
    private static class Plugin implements Trait<J.MethodInvocation>, VersionCatalog.Entry {
        Cursor cursor;

        private @Nullable String getPluginId() {
            return literalArgument(entryCall(getTree(), "plugin"), 1);
        }

        @Override
        public @Nullable String getVersion() {
            return isChained(getTree(), "version") ? literalArgument(getTree(), 0) : null;
        }

        @Override
        public @Nullable String getVersionRef() {
            return isChained(getTree(), "versionRef") ? literalArgument(getTree(), 0) : null;
        }

        private static class Matcher extends GradleTraitMatcher<Plugin> {
            @Override
            protected @Nullable Plugin test(Cursor cursor) {
                Object value = cursor.getValue();
                if (!(value instanceof J.MethodInvocation) || !isTopLevelStatement(cursor) || !withinBlock(cursor, "versionCatalogs")) {
                    return null;
                }
                J.MethodInvocation plugin = entryCall((J.MethodInvocation) value, "plugin");
                return "plugin".equals(plugin.getSimpleName()) && plugin.getArguments().size() == 2 &&
                       literalArgument(plugin, 1) != null ? new Plugin(cursor) : null;
            }
        }
    }

    /**
     * A {@code version(alias, value)} declaration, what an entry's {@code versionRef(...)} points at.
     */
    @Value
    private static class Version implements Trait<J.MethodInvocation> {
        Cursor cursor;

        private @Nullable String getAlias() {
            return literalArgument(getTree(), 0);
        }

        private @Nullable String getVersion() {
            return literalArgument(getTree(), 1);
        }

        private Version withVersion(String newVersion) {
            J.MethodInvocation updated = withLiteralArgument(getTree(), 1, newVersion);
            return updated == getTree() ? this : new Version(new Cursor(cursor.getParent(), updated));
        }

        private static class Matcher extends GradleTraitMatcher<Version> {
            @Override
            protected @Nullable Version test(Cursor cursor) {
                Object value = cursor.getValue();
                if (value instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) value;
                    if ("version".equals(m.getSimpleName()) && m.getArguments().size() == 2 && m.getSelect() == null &&
                        isTopLevelStatement(cursor) && withinBlock(cursor, "versionCatalogs")) {
                        return new Version(cursor);
                    }
                }
                return null;
            }
        }
    }

    static class Matcher extends GradleTraitMatcher<SettingsVersionCatalog> {
        @Override
        protected @Nullable SettingsVersionCatalog test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) value;
                if (isDirectChildOfBlock(cursor, "versionCatalogs") && withinBlock(cursor, "dependencyResolutionManagement")) {
                    String catalogName;
                    if ("create".equals(m.getSimpleName())) {
                        // Kotlin DSL: versionCatalogs { create("libs") { ... } }
                        catalogName = literalArgument(m, 0);
                    } else {
                        // Groovy DSL sugar: versionCatalogs { libs { ... } } -- the method name IS the catalog name
                        catalogName = m.getSimpleName();
                    }

                    if (catalogName != null) {
                        return new SettingsVersionCatalog(cursor, catalogName);
                    }
                }
            }
            return null;
        }

        private boolean isDirectChildOfBlock(Cursor cursor, String name) {
            Cursor parent = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v == Cursor.ROOT_VALUE);
            return !parent.isRoot() && name.equals(((J.MethodInvocation) parent.getValue()).getSimpleName());
        }
    }
}
