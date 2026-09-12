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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.DeleteKey;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

/**
 * An entry in the {@code [libraries]} table of a catalog TOML file, in string notation or as an
 * inline table using {@code group}/{@code name} or {@code module} coordinates.
 */
@Value
public class VersionCatalogLibrary implements Trait<Toml.KeyValue>, VersionCatalog.EntryVersion {

    Cursor cursor;
    String groupId;
    String artifactId;
    @Nullable String module;
    @Nullable String version;
    @Nullable String versionRef;

    public GroupArtifact getGroupArtifact() {
        return new GroupArtifact(groupId, artifactId);
    }

    public VersionCatalogLibrary withGroup(String newGroupId) {
        return withCoordinates(newGroupId, artifactId, module != null);
    }

    public VersionCatalogLibrary withName(String newArtifactId) {
        return withCoordinates(groupId, newArtifactId, module != null);
    }

    public VersionCatalogLibrary withModule(String newModule) {
        GroupArtifact ga = parseModule(newModule);
        return ga == null ? this : withCoordinates(ga.getGroupId(), ga.getArtifactId(), true);
    }

    /**
     * An entry using {@code version.ref} is left unchanged.
     */
    public VersionCatalogLibrary withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return this;
        }
        return with(groupId, artifactId, newVersion, module != null, null);
    }

    /**
     * Replaces the entry's {@code version.ref} with {@code newVersion} written at the entry itself.
     */
    public VersionCatalogLibrary withDetachedVersion(String newVersion) {
        if (versionRef == null || !(getTree().getValue() instanceof Toml.Table)) {
            return this;
        }
        Toml.Table inline = TomlTableValue.withKey((Toml.Table) getTree().getValue(), "version.ref", "version");
        inline = TomlTableValue.withString(inline, "version", newVersion);
        return new VersionCatalogLibrary(new Cursor(cursor.getParent(), getTree().withValue(inline)),
                groupId, artifactId, module, newVersion, null);
    }

    private VersionCatalogLibrary withCoordinates(String newGroupId, String newArtifactId, boolean moduleNotation) {
        if (newGroupId.equals(groupId) && newArtifactId.equals(artifactId) && moduleNotation == (module != null)) {
            return this;
        }
        return with(newGroupId, newArtifactId, version, moduleNotation, versionRef);
    }

    private VersionCatalogLibrary with(String newGroupId, String newArtifactId, @Nullable String newVersion,
                                       boolean moduleNotation, @Nullable String newVersionRef) {
        Toml.KeyValue keyValue = getTree();
        Toml.KeyValue updated;
        if (keyValue.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) keyValue.getValue();
            Dependency dependency = literal.getValue() instanceof String ? DependencyNotation.parse((String) literal.getValue()) : null;
            if (dependency == null) {
                return this;
            }
            String notation = DependencyNotation.toStringNotation(dependency.withGav(new GroupArtifactVersion(
                    newGroupId, newArtifactId, newVersion == null ? dependency.getVersion() : newVersion)));
            updated = keyValue.withValue(literal.withSource(TomlTableValue.quoted(literal, notation)).withValue(notation));
        } else if (keyValue.getValue() instanceof Toml.Table) {
            Toml.Table inline = (Toml.Table) keyValue.getValue();
            if (moduleNotation) {
                if (TomlTableValue.find(inline, "module") == null) {
                    inline = (Toml.Table) new DeleteKey("group").getVisitor().visitNonNull(inline, new InMemoryExecutionContext());
                    inline = (Toml.Table) new DeleteKey("name").getVisitor().visitNonNull(inline, new InMemoryExecutionContext());
                }
                inline = TomlTableValue.withStringOrAdd(inline, "module", newGroupId + ":" + newArtifactId);
            } else {
                inline = TomlTableValue.withString(inline, "group", newGroupId);
                inline = TomlTableValue.withString(inline, "name", newArtifactId);
            }
            if (newVersion != null && TomlTableValue.find(inline, "version.ref") == null) {
                inline = TomlTableValue.withStringOrAdd(inline, "version", newVersion);
            }
            updated = keyValue.withValue(inline);
        } else {
            return this;
        }
        return updated == keyValue ? this : new VersionCatalogLibrary(new Cursor(cursor.getParent(), updated),
                newGroupId, newArtifactId, moduleNotation ? newGroupId + ":" + newArtifactId : null, newVersion, newVersionRef);
    }

    private static @Nullable GroupArtifact parseModule(@Nullable String module) {
        if (module == null || module.indexOf(':') < 0 || module.indexOf(':') != module.lastIndexOf(':')) {
            return null;
        }
        Dependency dependency = DependencyNotation.parse(module);
        if (dependency == null || dependency.getGroupId() == null || dependency.getGroupId().isEmpty() || dependency.getArtifactId().isEmpty()) {
            return null;
        }
        return new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId());
    }

    public static class Matcher extends SimpleTraitMatcher<VersionCatalogLibrary> {

        @Nullable
        private String groupPattern;

        @Nullable
        private String artifactPattern;

        public Matcher groupPattern(@Nullable String groupPattern) {
            this.groupPattern = groupPattern;
            return this;
        }

        public Matcher artifactPattern(@Nullable String artifactPattern) {
            this.artifactPattern = artifactPattern;
            return this;
        }

        @Override
        protected @Nullable VersionCatalogLibrary test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Toml.KeyValue)) {
                return null;
            }
            Cursor parent = cursor.getParent();
            if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Identifier tableName = ((Toml.Table) parent.getValue()).getName();
            if (tableName == null || !"libraries".equals(tableName.getName())) {
                return null;
            }
            Toml.KeyValue keyValue = cursor.getValue();
            DependencyMatcher dependencyMatcher = new DependencyMatcher(groupPattern, artifactPattern, null);
            if (keyValue.getValue() instanceof Toml.Literal) {
                return testLiteral(cursor, (Toml.Literal) keyValue.getValue(), dependencyMatcher);
            }
            if (keyValue.getValue() instanceof Toml.Table) {
                return testInlineTable(cursor, (Toml.Table) keyValue.getValue(), dependencyMatcher);
            }
            return null;
        }

        private static @Nullable VersionCatalogLibrary testLiteral(Cursor cursor, Toml.Literal literal, DependencyMatcher dependencyMatcher) {
            Dependency dependency = literal.getValue() instanceof String ? DependencyNotation.parse((String) literal.getValue()) : null;
            if (dependency == null || dependency.getGroupId() == null ||
                !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                return null;
            }
            return new VersionCatalogLibrary(cursor, dependency.getGroupId(), dependency.getArtifactId(), null,
                    dependency.getVersion(), null);
        }

        private static @Nullable VersionCatalogLibrary testInlineTable(Cursor cursor, Toml.Table inline, DependencyMatcher dependencyMatcher) {
            String groupId = TomlTableValue.getString(inline, "group");
            String artifactId = TomlTableValue.getString(inline, "name");
            String module = TomlTableValue.getString(inline, "module");
            if (module != null && (groupId != null || artifactId != null)) {
                return null;
            }
            if (groupId == null || artifactId == null) {
                GroupArtifact ga = parseModule(module);
                if (ga == null) {
                    return null;
                }
                groupId = ga.getGroupId();
                artifactId = ga.getArtifactId();
            }
            if (!dependencyMatcher.matches(groupId, artifactId)) {
                return null;
            }
            return new VersionCatalogLibrary(cursor, groupId, artifactId, module,
                    TomlTableValue.getString(inline, "version"),
                    TomlTableValue.getString(inline, "version.ref"));
        }
    }
}
