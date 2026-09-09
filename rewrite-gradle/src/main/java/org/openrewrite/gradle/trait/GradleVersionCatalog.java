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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.marker.GradleVersionCatalogVersionReferences;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

/**
 * A single named catalog declared inside a Gradle
 * {@code dependencyResolutionManagement { versionCatalogs { ... } } } block, e.g. the
 * Groovy {@code libs { ... } } closure or the Kotlin {@code create("libs") { ... } } call.
 * <p>
 * {@link #getVersion(GroupArtifact)} looks up a library's current version by group:artifact,
 * following a {@code versionRef(...)} to the shared {@code version(...)} declaration it points
 * at, if any. {@link #withVersion(GroupArtifact, String)} changes a library's version, always
 * trying to preserve as much of the catalog's existing version-sharing structure as possible,
 * regardless of the order in which changes are made. A library is moved off a shared symbolic
 * version onto its own inline version only when keeping it would make the version assignment
 * inconsistent -- and moved back onto the shared symbolic version again as soon as it becomes
 * consistent, even if that only happens later, as a side effect of some other, unrelated change.
 * <p>
 * For example, given:
 * <pre>
 * version('springBootVersion', '3.5.15')
 * library('springBootStarterWeb', 'org.springframework.boot', 'spring-boot-starter-web').versionRef('springBootVersion')
 * library('springBootStarterWebflux', 'org.springframework.boot', 'spring-boot-starter-webflux').versionRef('springBootVersion')
 * </pre>
 * bumping just {@code spring-boot-starter-web} to {@code 3.5.16} can't reuse
 * {@code springBootVersion} (since {@code spring-boot-starter-webflux} still needs
 * {@code 3.5.15}), so it's given its own {@code version('3.5.16')} and {@code springBootVersion}
 * is left alone. If {@code spring-boot-starter-webflux} is later also bumped to {@code 3.5.16} --
 * even by a wholly separate call -- the two agree again, and this catalog puts them back onto
 * the shared reference: {@code springBootVersion} itself becomes {@code version('3.5.16')}, and
 * both libraries go back to {@code versionRef('springBootVersion')}.
 */
@EqualsAndHashCode(of = {"cursor", "catalogName"})
@ToString(of = {"cursor", "catalogName"})
public class GradleVersionCatalog implements Trait<J.MethodInvocation> {
    @Getter
    Cursor cursor;
    @Getter
    String catalogName;

    private @Nullable Map<GroupArtifact, Library> cachedLibrariesByGroupArtifact;
    private @Nullable Map<String, String> cachedVersionValuesByAlias;

    public GradleVersionCatalog(Cursor cursor, String catalogName) {
        this.cursor = cursor;
        this.catalogName = catalogName;
    }

    /**
     * @return the group:artifact of every {@code library(...)} declaration in this catalog with
     * a resolvable group:artifact, in declaration order. A library whose group:artifact can't be
     * resolved (malformed coordinates) is never included.
     */
    public Set<GroupArtifact> getGroupArtifacts() {
        return getLibraries().keySet();
    }

    private Map<GroupArtifact, Library> getLibraries() {
        collectLibrariesAndVersions();
        return cachedLibrariesByGroupArtifact;
    }

    private Map<String, String> getVersionDeclarations() {
        collectLibrariesAndVersions();
        return cachedVersionValuesByAlias;
    }

    private void collectLibrariesAndVersions() {
        if (cachedLibrariesByGroupArtifact == null) {
            Map<GroupArtifact, Library> librariesByGroupArtifact = new LinkedHashMap<>();
            Map<String, String> versionValuesByAlias = new LinkedHashMap<>();
            Library.Matcher libraryMatcher = new Library.Matcher();
            Version.Matcher versionMatcher = new Version.Matcher();
            new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                    libraryMatcher.get(getCursor()).ifPresent(library -> {
                        GroupArtifact ga = library.getGroupArtifact();
                        if (ga != null) {
                            librariesByGroupArtifact.putIfAbsent(ga, library);
                        }
                    });
                    versionMatcher.get(getCursor()).ifPresent(version -> versionValuesByAlias.put(version.getAlias(), version.getVersion()));
                    return m;
                }
            }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());

            cachedLibrariesByGroupArtifact = librariesByGroupArtifact;
            cachedVersionValuesByAlias = versionValuesByAlias;
        }
    }

    /**
     * Snapshots which libraries share each {@code versionRef(...)} declaration onto this
     * catalog's root AST node as a {@link GradleVersionCatalogVersionReferences} marker.
     */
    GradleVersionCatalog withOriginalVersionReferencesMarker() {
        if (getTree().getMarkers().findFirst(GradleVersionCatalogVersionReferences.class).isPresent()) {
            // Never recompute: a later call would only see the post-detachment structure, losing
            // the "these used to share a ref" fact the detach/re-attach algorithm depends on.
            return this;
        }
        Map<String, String> versionValuesByAlias = getVersionDeclarations();

        Map<String, List<GroupArtifact>> groupArtifactsByRefAlias = new LinkedHashMap<>();
        for (Library library : getLibraries().values()) {
            String versionRefAlias = library.getVersionRefAlias();
            if (versionRefAlias != null) {
                groupArtifactsByRefAlias.computeIfAbsent(versionRefAlias, k -> new ArrayList<>()).add(library.getGroupArtifact());
            }
        }

        Map<String, GradleVersionCatalogVersionReferences.SharedReference> sharedReferencesByAlias = new LinkedHashMap<>();
        for (Map.Entry<String, List<GroupArtifact>> entry : groupArtifactsByRefAlias.entrySet()) {
            String version = versionValuesByAlias.get(entry.getKey());
            if (version != null) {
                sharedReferencesByAlias.put(entry.getKey(),
                        new GradleVersionCatalogVersionReferences.SharedReference(version, entry.getValue()));
            }
        }

        GradleVersionCatalogVersionReferences marker = new GradleVersionCatalogVersionReferences(randomId(), sharedReferencesByAlias);
        J.MethodInvocation newTree = getTree().withMarkers(getTree().getMarkers().add(marker));
        Cursor newCursor = new Cursor(cursor.getParent(), newTree);
        return new GradleVersionCatalog(newCursor, catalogName);
    }

    /**
     * @return the version of the library matching {@code ga}, following {@code versionRef(...)}
     * indirection, or {@code null} if there is no such library or it has no resolvable version.
     */
    public @Nullable String getVersion(GroupArtifact ga) {
        Library library = getLibraries().get(ga);
        if (library != null) {
            String inlineVersion = library.getInlineVersion();
            if (inlineVersion != null) {
                return inlineVersion;
            }
            String versionRefAlias = library.getVersionRefAlias();
            if (versionRefAlias != null) {
                return getVersionDeclarations().get(versionRefAlias);
            }
        }
        return null;
    }

    /**
     * Rewrites the version of the library matching {@code ga} to {@code newVersion}. An inline
     * version has its literal rewritten directly; a {@code versionRef(...)} is tentatively
     * detached to an inline literal, then checked for convergence with the rest of its original
     * sharing group -- see {@link #reconciledAfterDetaching(String)}.
     * <p>
     * The library is re-located by {@code ga} against the current tree on every call, so calls
     * can be chained by threading the returned catalog from one to the next.
     */
    public GradleVersionCatalog withVersion(GroupArtifact ga, String newVersion) {
        Library library = getLibraries().get(ga);
        if (library != null) {
            String inlineVersion = library.getInlineVersion();
            if (inlineVersion != null) {
                if (!inlineVersion.equals(newVersion)) {
                    return withLibraryVersion(ga, newVersion);
                }
            } else {
                String versionRefAlias = library.getVersionRefAlias();
                if (versionRefAlias != null) {
                    String currentValue = getVersionDeclarations().get(versionRefAlias);
                    if (currentValue == null || !currentValue.equals(newVersion)) {
                        return withOriginalVersionReferencesMarker()
                                .withDetachedLibraryVersion(ga, newVersion)
                                .reconciledAfterDetaching(versionRefAlias);
                    }
                }
            }
        }
        return this;
    }

    private GradleVersionCatalog withLibraryVersion(GroupArtifact ga, String newVersion) {
        Library.Matcher libraryMatcher = new Library.Matcher();
        J newTree = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return libraryMatcher.get(getCursor())
                        .filter(library -> ga.equals(library.getGroupArtifact()))
                        .map(library -> library.withVersion(newVersion).getTree())
                        .orElse(m);
            }
        }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree(newTree);
    }

    private GradleVersionCatalog withDetachedLibraryVersion(GroupArtifact ga, String newVersion) {
        Library.Matcher libraryMatcher = new Library.Matcher();
        J newTree = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return libraryMatcher.get(getCursor())
                        .filter(library -> ga.equals(library.getGroupArtifact()))
                        .map(library -> library.detachToVersion(newVersion).getTree())
                        .orElse(m);
            }
        }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree(newTree);
    }

    /**
     * Collapses a sharing group back onto {@code refAlias} when every library that originally
     * shared it (per the {@link GradleVersionCatalogVersionReferences} marker) has since
     * converged on one version. Otherwise the tentative detach stands, leaving {@code refAlias}
     * and any members still pointing at it alone.
     */
    private GradleVersionCatalog reconciledAfterDetaching(String refAlias) {
        GradleVersionCatalogVersionReferences marker = getTree().getMarkers()
                .findFirst(GradleVersionCatalogVersionReferences.class)
                .orElse(null);
        GradleVersionCatalogVersionReferences.SharedReference sharedReference =
                marker == null ? null : marker.getSharedReferencesByAlias().get(refAlias);
        if (sharedReference != null) {
            List<GroupArtifact> groupMembers = sharedReference.getGroupArtifacts();
            Map<GroupArtifact, Library> librariesByGroupArtifact = getLibraries();
            Map<String, String> versionValuesByAlias = getVersionDeclarations();

            Set<@Nullable String> resolvedVersions = new LinkedHashSet<>();
            for (GroupArtifact groupMember : groupMembers) {
                Library library = librariesByGroupArtifact.get(groupMember);
                String resolvedVersion = library == null ? null : library.getInlineVersion();
                if (resolvedVersion == null && library != null) {
                    String currentRefAlias = library.getVersionRefAlias();
                    resolvedVersion = currentRefAlias == null ? null : versionValuesByAlias.get(currentRefAlias);
                }
                resolvedVersions.add(resolvedVersion);
            }

            if (resolvedVersions.size() == 1) {
                String commonVersion = resolvedVersions.iterator().next();
                if (commonVersion != null) {
                    GradleVersionCatalog collapsed = withVersionDeclarationValue(refAlias, commonVersion);
                    for (GroupArtifact groupMember : groupMembers) {
                        collapsed = collapsed.withLibraryReattachedToVersionRef(groupMember, refAlias);
                    }
                    return collapsed;
                }
            }
        }
        return this;
    }

    private GradleVersionCatalog withVersionDeclarationValue(String alias, String newVersion) {
        Version.Matcher versionMatcher = new Version.Matcher();
        J newTree = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return versionMatcher.get(getCursor())
                        .filter(version -> alias.equals(version.getAlias()))
                        .map(version -> version.withVersion(newVersion).getTree())
                        .orElse(m);
            }
        }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree(newTree);
    }

    private GradleVersionCatalog withLibraryReattachedToVersionRef(GroupArtifact ga, String refAlias) {
        Library.Matcher libraryMatcher = new Library.Matcher();
        J newTree = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return libraryMatcher.get(getCursor())
                        .filter(library -> ga.equals(library.getGroupArtifact()))
                        .map(library -> library.reattachToVersionRef(refAlias).getTree())
                        .orElse(m);
            }
        }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree(newTree);
    }

    private GradleVersionCatalog withTree(J newTree) {
        if (newTree != getTree()) {
            return new GradleVersionCatalog(new Cursor(cursor.getParent(), newTree), catalogName);
        }
        return this;
    }

    /**
     * @return {@code true} if the cursor's tree is itself a statement in an enclosing block,
     * rather than a nested expression such as the receiver of a chained method call.
     */
    private static boolean isTopLevelStatement(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        if (parent.getValue() instanceof J.Return) {
            // Groovy closures implicitly return their last expression through a synthetic Return
            parent = parent.getParentTreeCursor();
        }
        return !parent.isRoot() && parent.getValue() instanceof J.Block;
    }

    private static J.@Nullable MethodInvocation asChainedInvocation(J.MethodInvocation m) {
        return m.getSelect() instanceof J.MethodInvocation ? (J.MethodInvocation) m.getSelect() : null;
    }

    /**
     * @return the string value of {@code m}'s argument at {@code index}, or {@code null} if
     * there's no such argument or it isn't a string literal.
     */
    private static @Nullable String literalArgument(J.MethodInvocation m, int index) {
        if (index < m.getArguments().size()) {
            Expression argument = m.getArguments().get(index);
            if (argument instanceof J.Literal && ((J.Literal) argument).getValue() instanceof String) {
                return (String) ((J.Literal) argument).getValue();
            }
        }
        return null;
    }

    /**
     * A single {@code library(...)} declaration, in any of its forms: three-argument
     * {@code library(alias, group, artifact)}, optionally terminated by {@code .version(...)},
     * {@code .versionRef(...)}, or {@code .withoutVersion()}; or the single coordinate-string
     * {@code library(alias, "group:artifact:version")} form.
     */
    @Value
    private static class Library implements Trait<J.MethodInvocation> {
        Cursor cursor;

        private @Nullable String getAlias() {
            return literalArgument(libraryCall(), 0);
        }

        private @Nullable GroupArtifact getGroupArtifact() {
            J.MethodInvocation library = libraryCall();
            if (library.getArguments().size() == 3) {
                String groupId = literalArgument(library, 1);
                String artifactId = literalArgument(library, 2);
                return groupId == null || artifactId == null ? null : new GroupArtifact(groupId, artifactId);
            }
            if (library.getArguments().size() == 2) {
                String groupArtifactVersion = literalArgument(library, 1);
                String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
                return parts != null && parts.length == 3 ? new GroupArtifact(parts[0], parts[1]) : null;
            }
            return null;
        }

        /**
         * @return the alias of the shared {@code version(...)} declaration this library resolves
         * its version through, or {@code null} if it's declared inline or not at all.
         */
        private @Nullable String getVersionRefAlias() {
            J.MethodInvocation outer = getTree();
            return "versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 ?
                    literalArgument(outer, 0) : null;
        }

        /**
         * @return the inline version literal, whether chained as {@code .version(...)} or
         * embedded in a single coordinate string, or {@code null} if the version comes via
         * {@code versionRef(...)} or isn't declared at all.
         */
        private @Nullable String getInlineVersion() {
            J.MethodInvocation outer = getTree();
            if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
                return literalArgument(outer, 0);
            }
            if ("library".equals(outer.getSimpleName()) && outer.getArguments().size() == 2) {
                String groupArtifactVersion = literalArgument(outer, 1);
                String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
                return parts != null && parts.length == 3 ? parts[2] : null;
            }
            return null;
        }

        /**
         * @return a copy with its inline version literal rewritten to {@code newVersion}, or this
         * library unchanged if it has no inline version to rewrite.
         */
        private Library withVersion(String newVersion) {
            J.MethodInvocation outer = getTree();
            if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
                return withArgumentLiteral(outer, 0, newVersion);
            }
            if ("library".equals(outer.getSimpleName()) && outer.getArguments().size() == 2) {
                String groupArtifactVersion = literalArgument(outer, 1);
                String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
                if (parts != null && parts.length == 3) {
                    return withArgumentLiteral(outer, 1, parts[0] + ":" + parts[1] + ":" + newVersion);
                }
            }
            return this;
        }

        private Library withArgumentLiteral(J.MethodInvocation outer, int argIndex, String newValue) {
            Expression argument = outer.getArguments().get(argIndex);
            if (argument instanceof J.Literal) {
                J.Literal oldLiteral = (J.Literal) argument;
                String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
                J.Literal newLiteral = oldLiteral.withValue(newValue).withValueSource(quote + newValue + quote);
                List<Expression> newArguments = new ArrayList<>(outer.getArguments());
                newArguments.set(argIndex, newLiteral);
                return new Library(new Cursor(cursor.getParent(), outer.withArguments(newArguments)));
            }
            return this;
        }

        /**
         * @return a copy with its chained {@code .versionRef(...)} call replaced by
         * {@code .version(newVersion)}, or this library unchanged if it isn't on a
         * {@code versionRef(...)} chain.
         */
        private Library detachToVersion(String newVersion) {
            J.MethodInvocation outer = getTree();
            if ("versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
                return withRenamedChainedCall(outer, "version", newVersion);
            }
            return this;
        }

        /**
         * @return a copy with its chained {@code .version(...)} call replaced by
         * {@code .versionRef(alias)}, or this library unchanged if it has no chained
         * {@code .version(...)} call to rewrite.
         */
        private Library reattachToVersionRef(String alias) {
            J.MethodInvocation outer = getTree();
            if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
                return withRenamedChainedCall(outer, "versionRef", alias);
            }
            return this;
        }

        private Library withRenamedChainedCall(J.MethodInvocation outer, String methodName, String newArgumentValue) {
            Expression argument = outer.getArguments().get(0);
            if (!(argument instanceof J.Literal)) {
                return this;
            }
            J.Literal oldLiteral = (J.Literal) argument;
            String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
            J.Literal newLiteral = oldLiteral.withValue(newArgumentValue).withValueSource(quote + newArgumentValue + quote);
            J.MethodInvocation newOuter = outer.withName(outer.getName().withSimpleName(methodName))
                    .withArguments(Collections.singletonList(newLiteral));
            return new Library(new Cursor(cursor.getParent(), newOuter));
        }

        private J.MethodInvocation libraryCall() {
            J.MethodInvocation outer = getTree();
            J.MethodInvocation chained = asChainedInvocation(outer);
            return chained != null && "library".equals(chained.getSimpleName()) ? chained : outer;
        }

        private static class Matcher extends GradleTraitMatcher<Library> {
            @Override
            protected @Nullable Library test(Cursor cursor) {
                Object value = cursor.getValue();
                if (value instanceof J.MethodInvocation && isTopLevelStatement(cursor) && withinBlock(cursor, "versionCatalogs")) {

                    J.MethodInvocation outer = (J.MethodInvocation) value;
                    String versionRefAlias = null;
                    String inlineVersion = null;
                    boolean withoutVersion = false;

                    if ("versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
                        versionRefAlias = literalArgument(outer, 0);
                        outer = asChainedInvocation(outer);
                    } else if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
                        inlineVersion = literalArgument(outer, 0);
                        outer = asChainedInvocation(outer);
                    } else if ("withoutVersion".equals(outer.getSimpleName()) && outer.getArguments().isEmpty()) {
                        withoutVersion = true;
                        outer = asChainedInvocation(outer);
                    }

                    if (outer != null && "library".equals(outer.getSimpleName()) && literalArgument(outer, 0) != null) {
                        if (outer.getArguments().size() == 3) {
                            if (literalArgument(outer, 1) != null && literalArgument(outer, 2) != null) {
                                return new Library(cursor);
                            }
                        } else if (outer.getArguments().size() == 2 && versionRefAlias == null && inlineVersion == null && !withoutVersion) {
                            String groupArtifactVersion = literalArgument(outer, 1);
                            String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
                            if (parts != null && parts.length == 3) {
                                return new Library(cursor);
                            }
                        }
                    }

                }
                return null;
            }
        }
    }

    /**
     * A single {@code version(alias, value)} declaration -- what a library's
     * {@code versionRef(...)} points at.
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
            J.MethodInvocation outer = getTree();
            Expression argument = outer.getArguments().get(1);
            if (!(argument instanceof J.Literal)) {
                return this;
            }
            J.Literal oldLiteral = (J.Literal) argument;
            String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
            J.Literal newLiteral = oldLiteral.withValue(newVersion).withValueSource(quote + newVersion + quote);
            List<Expression> newArguments = new ArrayList<>(outer.getArguments());
            newArguments.set(1, newLiteral);
            return new Version(new Cursor(cursor.getParent(), outer.withArguments(newArguments)));
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

    public static class Matcher extends GradleTraitMatcher<GradleVersionCatalog> {
        @Override
        protected @Nullable GradleVersionCatalog test(Cursor cursor) {
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
                        return new GradleVersionCatalog(cursor, catalogName);
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
