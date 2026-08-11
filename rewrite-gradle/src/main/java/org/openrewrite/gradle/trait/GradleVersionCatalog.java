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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.marker.GradleVersionCatalogVersionReferences;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Represents a single named catalog declared inside a Gradle
 * {@code dependencyResolutionManagement { versionCatalogs { ... } } } block, e.g. the
 * Groovy {@code libs { ... } } closure or the Kotlin {@code create("libs") { ... } } call.
 * <p>
 * Works against the raw {@code version(...)}/{@code library(...)} DSL calls rather than
 * type-attributed method signatures, since Gradle's Groovy/Kotlin DSL closures for
 * user-defined catalogs are not reliably type-attributed to
 * {@code org.gradle.api.initialization.dsl.VersionCatalogBuilder} during parsing.
 * <p>
 * Not a Lombok {@code @Value}: {@link #getLibraries()} and the (package-private)
 * {@code getVersionDeclarations()} are both derived from the same single AST walk, memoized
 * lazily on first access to either -- see {@link #collectLibrariesAndVersions()}. Equality and
 * {@code toString} are still based solely on {@code cursor}/{@code catalogName}, the same as a
 * {@code @Value} class would produce; the cache fields are purely an implementation detail.
 */
@EqualsAndHashCode(of = {"cursor", "catalogName"})
@ToString(of = {"cursor", "catalogName"})
public class GradleVersionCatalog implements Trait<J.MethodInvocation> {
    @Getter
    Cursor cursor;
    @Getter
    String catalogName;

    private @Nullable Map<GroupArtifact, VersionCatalogLibrary> cachedLibrariesByGroupArtifact;
    private @Nullable Map<String, String> cachedVersionValuesByAlias;

    public GradleVersionCatalog(Cursor cursor, String catalogName) {
        this.cursor = cursor;
        this.catalogName = catalogName;
    }

    /**
     * @return every {@code library(...)} declaration in this catalog with a resolvable
     * group:artifact, keyed by it, in declaration order. {@code UpgradeDependencyVersion} only
     * ever targets a library by its group:artifact, never its alias, so that's the natural key --
     * a library whose group:artifact can't be resolved (malformed coordinates) is never included,
     * same as it was always skipped by callers before. If more than one library declares the same
     * group:artifact under different aliases, the first one encountered wins, matching the
     * find-the-first-match behavior every lookup here had before this was a map.
     */
    public Map<GroupArtifact, VersionCatalogLibrary> getLibraries() {
        collectLibrariesAndVersions();
        return cachedLibrariesByGroupArtifact;
    }

    /**
     * @return every {@code version(alias, value)} declaration in this catalog, keyed by alias.
     */
    private Map<String, String> getVersionDeclarations() {
        collectLibrariesAndVersions();
        return cachedVersionValuesByAlias;
    }

    /**
     * Populates {@link #cachedLibrariesByGroupArtifact} and {@link #cachedVersionValuesByAlias}
     * together, in a single AST walk, the first time either is needed -- a no-op on every
     * subsequent call.
     */
    private void collectLibrariesAndVersions() {
        if (cachedLibrariesByGroupArtifact == null) {
            Map<GroupArtifact, VersionCatalogLibrary> librariesByGroupArtifact = new LinkedHashMap<>();
            Map<String, String> versionValuesByAlias = new LinkedHashMap<>();
            VersionCatalogLibrary.Matcher libraryMatcher = new VersionCatalogLibrary.Matcher();
            VersionCatalogVersion.Matcher versionMatcher = new VersionCatalogVersion.Matcher();
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
     * Captures a snapshot of which libraries currently declared in this catalog share each
     * {@code versionRef(...)} declaration -- keyed by the reference's own alias, valued by its
     * current version value and the group:artifact of every library currently resolving through
     * it -- and attaches it to this catalog's root AST node as a
     * {@link GradleVersionCatalogVersionReferences} marker. Downstream recipes can consult this
     * marker to tell whether two separately-requested version bumps actually target the same
     * underlying {@code version(...)} declaration.
     */
    GradleVersionCatalog withOriginalVersionReferencesMarker() {
        if (getTree().getMarkers().findFirst(GradleVersionCatalogVersionReferences.class).isPresent()) {
            // Already memorized the original sharing structure on an earlier call -- never recompute
            // it, or a later call would only see the post-detachment structure and lose the "these
            // used to share a ref" fact the detach/re-attach algorithm depends on.
            return this;
        }
        Map<String, String> versionValuesByAlias = getVersionDeclarations();

        Map<String, List<GroupArtifact>> groupArtifactsByRefAlias = new LinkedHashMap<>();
        for (VersionCatalogLibrary library : getLibraries().values()) {
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
     * Resolves the current version of the library matching {@code ga} in this catalog, following
     * {@code versionRef(...)} indirection to the referenced {@code version(...)} declaration when
     * necessary.
     *
     * @return the resolved version, or {@code null} if no library in this catalog matches
     * {@code ga}, or it has no version at all (declared via {@code withoutVersion()}).
     */
    public @Nullable String getVersion(GroupArtifact ga) {
        VersionCatalogLibrary library = getLibraries().get(ga);
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
     * Rewrites the version of the library matching {@code ga} in this catalog to
     * {@code newVersion}. Re-locates the library by {@code ga} against this catalog's own current
     * tree on every call, so it's safe to call repeatedly, threading the returned
     * {@code GradleVersionCatalog} from one call to the next.
     * <p>
     * If the library's version is inline, its literal is rewritten directly. If it comes via
     * {@code versionRef(...)}, the library is tentatively detached to its own inline literal and
     * the whole original sharing group (per the {@link GradleVersionCatalogVersionReferences}
     * marker) is checked for convergence -- see {@link #reconciledAfterDetaching(String)}.
     * No-op if no library in this catalog matches {@code ga}, or it's already at
     * {@code newVersion}.
     */
    public GradleVersionCatalog withVersion(GroupArtifact ga, String newVersion) {
        VersionCatalogLibrary library = getLibraries().get(ga);
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
        VersionCatalogLibrary.Matcher libraryMatcher = new VersionCatalogLibrary.Matcher();
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
        VersionCatalogLibrary.Matcher libraryMatcher = new VersionCatalogLibrary.Matcher();
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
     * After a library has just been tentatively detached from {@code refAlias}, checks every
     * library that *originally* shared that reference (per the
     * {@link GradleVersionCatalogVersionReferences} marker) against its current, live resolved
     * version. If they all now agree on the same value, the whole group collapses back onto a
     * shared reference: {@code refAlias}'s {@code version(...)} declaration is set to that common
     * value, and every group member (including the one just detached) is re-attached via
     * {@code versionRef(refAlias)}. Otherwise, this catalog is returned unchanged -- the tentative
     * detach stands, and {@code refAlias} plus any group members still pointing at it are left
     * alone.
     */
    private GradleVersionCatalog reconciledAfterDetaching(String refAlias) {
        GradleVersionCatalogVersionReferences marker = getTree().getMarkers()
                .findFirst(GradleVersionCatalogVersionReferences.class)
                .orElse(null);
        GradleVersionCatalogVersionReferences.SharedReference sharedReference =
                marker == null ? null : marker.getSharedReferencesByAlias().get(refAlias);
        if (sharedReference != null) {
            List<GroupArtifact> groupMembers = sharedReference.getGroupArtifacts();
            Map<GroupArtifact, VersionCatalogLibrary> librariesByGroupArtifact = getLibraries();
            Map<String, String> versionValuesByAlias = getVersionDeclarations();

            Set<@Nullable String> resolvedVersions = new LinkedHashSet<>();
            for (GroupArtifact groupMember : groupMembers) {
                VersionCatalogLibrary library = librariesByGroupArtifact.get(groupMember);
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
        VersionCatalogVersion.Matcher versionMatcher = new VersionCatalogVersion.Matcher();
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
        VersionCatalogLibrary.Matcher libraryMatcher = new VersionCatalogLibrary.Matcher();
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

    public static class Matcher extends GradleTraitMatcher<GradleVersionCatalog> {
        @Nullable
        private String catalogNamePattern;

        public Matcher catalogName(@Nullable String catalogNamePattern) {
            this.catalogNamePattern = catalogNamePattern;
            return this;
        }

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

                    if (catalogName != null && (catalogNamePattern == null || matchesGlob(catalogName, catalogNamePattern))) {
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
