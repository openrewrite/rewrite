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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Gradle version catalog, declared either in {@code settings.gradle(.kts)} or in a
 * {@code gradle/libs.versions.toml} file. {@link Matcher} finds either.
 */
public interface VersionCatalog extends Trait<Tree> {

    /**
     * An entry's version, either written out at the entry or referring to a named declaration.
     */
    interface EntryVersion {
        @Nullable String getVersion();

        @Nullable String getVersionRef();

        /**
         * @return the version this comes to given the catalog's declarations, or {@code null} if
         * it has none or refers to a declaration that is not there.
         */
        default @Nullable String getResolvedVersion(Map<String, String> declarations) {
            String version = getVersion();
            if (version != null) {
                return version;
            }
            String versionRef = getVersionRef();
            return versionRef == null ? null : declarations.get(versionRef);
        }
    }

    /**
     * @return every library whose coordinates parse, in declaration order.
     */
    Map<GroupArtifact, ? extends EntryVersion> getLibraryVersions();

    /**
     * @return every plugin, by plugin id, in declaration order.
     */
    Map<String, ? extends EntryVersion> getPluginVersions();

    /**
     * @return the value of each named version declaration, by alias.
     */
    Map<String, String> getVersionDeclarations();

    VersionCatalog withLibraryVersion(GroupArtifact ga, String newVersion);

    /**
     * Replaces a library's version reference with a version written out at the library itself.
     */
    VersionCatalog withDetachedLibraryVersion(GroupArtifact ga, String newVersion);

    VersionCatalog withVersionDeclarationValue(String alias, String newVersion);

    default @Nullable String getVersion(GroupArtifact ga) {
        EntryVersion library = getLibraryVersions().get(ga);
        return library == null ? null : library.getResolvedVersion(getVersionDeclarations());
    }

    /**
     * Moves each library to its new version. A shared version declaration is changed in place when
     * every entry referring to it is a library moving to the same version, so the catalog goes on
     * saying that those libraries are versioned together. Otherwise only the libraries that are
     * moving get a version of their own; a declaration a plugin refers to is never moved, since
     * this moves no plugins.
     */
    default VersionCatalog withVersions(Map<GroupArtifact, String> newVersions) {
        if (newVersions.isEmpty()) {
            return this;
        }
        Map<GroupArtifact, ? extends EntryVersion> libraries = getLibraryVersions();
        Map<String, String> declarations = getVersionDeclarations();

        Map<String, List<GroupArtifact>> referrersByAlias = new LinkedHashMap<>();
        for (Map.Entry<GroupArtifact, ? extends EntryVersion> library : libraries.entrySet()) {
            String alias = library.getValue().getVersionRef();
            if (alias != null) {
                referrersByAlias.computeIfAbsent(alias, k -> new ArrayList<>()).add(library.getKey());
            }
        }
        Set<String> pluginAliases = new HashSet<>();
        for (EntryVersion plugin : getPluginVersions().values()) {
            String alias = plugin.getVersionRef();
            if (alias != null) {
                pluginAliases.add(alias);
            }
        }

        VersionCatalog catalog = this;
        Set<String> changedAliases = new HashSet<>();
        for (Map.Entry<GroupArtifact, String> entry : newVersions.entrySet()) {
            GroupArtifact ga = entry.getKey();
            String newVersion = entry.getValue();
            EntryVersion library = libraries.get(ga);
            if (library == null) {
                continue;
            }
            String alias = library.getVersionRef();
            if (alias == null) {
                if (!newVersion.equals(library.getVersion())) {
                    catalog = catalog.withLibraryVersion(ga, newVersion);
                }
            } else if (!newVersion.equals(declarations.get(alias)) && !changedAliases.contains(alias)) {
                boolean movingTogether = !pluginAliases.contains(alias);
                for (GroupArtifact referrer : referrersByAlias.get(alias)) {
                    if (!newVersion.equals(newVersions.get(referrer))) {
                        movingTogether = false;
                        break;
                    }
                }
                if (movingTogether) {
                    catalog = catalog.withVersionDeclarationValue(alias, newVersion);
                    changedAliases.add(alias);
                } else {
                    catalog = catalog.withDetachedLibraryVersion(ga, newVersion);
                }
            }
        }
        return catalog;
    }

    /**
     * Matches a {@link SettingsVersionCatalog} at its {@code libs { ... } } call or a
     * {@link TomlVersionCatalog} at the document of a {@code *.versions.toml} file.
     */
    class Matcher extends SimpleTraitMatcher<VersionCatalog> {
        private final SettingsVersionCatalog.Matcher settings = new SettingsVersionCatalog.Matcher();
        private final TomlVersionCatalog.Matcher toml = new TomlVersionCatalog.Matcher();

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<VersionCatalog, P> visitor) {
            return new TreeVisitor<Tree, P>() {
                @Override
                public boolean isAcceptable(SourceFile sourceFile, P p) {
                    String path = sourceFile.getSourcePath().toString();
                    return (sourceFile instanceof G.CompilationUnit && path.endsWith(".gradle")) ||
                           (sourceFile instanceof K.CompilationUnit && path.endsWith(".gradle.kts")) ||
                           TomlVersionCatalog.isVersionCatalog(sourceFile);
                }

                @Override
                public Tree preVisit(Tree tree, P p) {
                    VersionCatalog catalog = test(getCursor());
                    return catalog == null ? tree : visitor.visit(catalog, p);
                }
            };
        }

        @Override
        protected @Nullable VersionCatalog test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Toml.Document) {
                return toml.test(cursor);
            }
            if (value instanceof J.MethodInvocation) {
                return settings.test(cursor);
            }
            return null;
        }
    }
}
