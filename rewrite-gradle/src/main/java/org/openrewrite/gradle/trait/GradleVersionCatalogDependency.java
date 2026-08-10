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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.ChangeValue;
import org.openrewrite.toml.DeleteKey;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

/**
 * Represents a library entry in a Gradle version catalog TOML file ({@code [libraries]} table).
 * <p>
 * Matches string-notation entries ({@code guava = "com.google.guava:guava:29.0-jre"}) and
 * inline-table entries using either separate coordinates or a {@code module} coordinate
 * ({@code guava = { module = "com.google.guava:guava", version = "29.0-jre" }}).
 * <p>
 * Use the inner {@link Matcher} to locate and filter library entries during a recipe traversal.
 */
@Value
public class GradleVersionCatalogDependency implements Trait<Toml.KeyValue> {

    Cursor cursor;
    String groupId;
    String artifactId;
    @Nullable String module;
    /**
     * Present for entries that carry an explicit {@code version} value.
     */
    @Nullable String version;
    /**
     * Present for entries that carry a {@code version.ref} key.
     */
    @Nullable String versionRef;

    /**
     * Returns a new {@link Toml.KeyValue} with the version updated to {@code newVersion}.
     * <p>
     * String notation is rebuilt while preserving the original quote style; inline-table notation
     * updates or adds a direct {@code version} key. Entries using {@code version.ref} are unchanged.
     */
    public GradleVersionCatalogDependency withGroup(String newGroupId) {
        if (newGroupId.equals(groupId)) {
            return this;
        }
        return withUpdatedValue(updateValue(newGroupId, artifactId, version, module),
                newGroupId, artifactId, module, version, versionRef);
    }

    public GradleVersionCatalogDependency withName(String newArtifactId) {
        if (newArtifactId.equals(artifactId)) {
            return this;
        }
        return withUpdatedValue(updateValue(groupId, newArtifactId, version, module),
                groupId, newArtifactId, module, version, versionRef);
    }

    public GradleVersionCatalogDependency withModule(String newModule) {
        if (newModule.indexOf(':') < 0 || newModule.indexOf(':') != newModule.lastIndexOf(':')) {
            return this;
        }
        Dependency dependency = DependencyNotation.parse(newModule);
        if (dependency == null) {
            return this;
        }
        String newGroupId = dependency.getGroupId();
        String newArtifactId = dependency.getArtifactId();
        if (newGroupId == null) {
            return this;
        }
        if (newModule.equals(module)) {
            return this;
        }
        return withUpdatedValue(updateValue(newGroupId, newArtifactId, version, newModule),
                newGroupId, newArtifactId, newModule, version, versionRef);
    }

    public GradleVersionCatalogDependency withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return this;
        }
        return withUpdatedValue(updateValue(groupId, artifactId, newVersion, module),
                groupId, artifactId, module, newVersion, null);
    }

    private GradleVersionCatalogDependency withUpdatedValue(
            Toml.KeyValue value, String newGroupId, String newArtifactId,
            @Nullable String newModule, @Nullable String newVersion, @Nullable String newVersionRef) {
        return new GradleVersionCatalogDependency(new Cursor(cursor.getParent(), value),
                newGroupId, newArtifactId, newModule, newVersion, newVersionRef);
    }

    private Toml.KeyValue updateValue(
            String newGroupId, String newArtifactId, @Nullable String newVersion, @Nullable String coordinateModule) {
        if (isUnchanged(newGroupId, newArtifactId, newVersion, coordinateModule)) {
            return getTree();
        }
        if (getTree().getValue() instanceof Toml.Literal) {
            return updateLiteralValue((Toml.Literal) getTree().getValue(), newGroupId, newArtifactId, newVersion);
        }
        if (!(getTree().getValue() instanceof Toml.Table)) {
            return getTree();
        }
        Toml.KeyValue kv = getTree();
        Toml.Table inline = (Toml.Table) kv.getValue();
        return coordinateModule == null ?
                updateGroupNameTable(kv, inline, newGroupId, newArtifactId, newVersion) :
                updateModuleTable(kv, inline, newGroupId, newArtifactId, newVersion);
    }

    private Toml.KeyValue updateLiteralValue(
            Toml.Literal literal, String newGroupId, String newArtifactId, @Nullable String newVersion) {
        if (!(literal.getValue() instanceof String)) {
            return getTree();
        }
        Dependency dependency = DependencyNotation.parse((String) literal.getValue());
        if (dependency == null) {
            return getTree();
        }
        String dependencyVersion = newVersion == null ? dependency.getVersion() : newVersion;
        String notation = DependencyNotation.toStringNotation(
                dependency.withGav(new org.openrewrite.maven.tree.GroupArtifactVersion(
                        newGroupId, newArtifactId, dependencyVersion)));
        return (Toml.KeyValue) new ChangeValue(keyName(getTree()), TomlTableValue.quoted(literal, notation))
                .getVisitor()
                .visitNonNull(getTree(), new InMemoryExecutionContext());
    }

    private Toml.KeyValue updateModuleTable(
            Toml.KeyValue kv, Toml.Table inline, String newGroupId, String newArtifactId, @Nullable String newVersion) {
        if (TomlTableValue.find(inline, "module") == null) {
            inline = (Toml.Table) new DeleteKey("group")
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
            inline = (Toml.Table) new DeleteKey("name")
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
            inline = TomlTableValue.withStringOrAdd(inline, "module", newGroupId + ":" + newArtifactId);
        } else if (TomlTableValue.getString(inline, "module") != null) {
            inline = (Toml.Table) new ChangeValue("module", quotedValue(inline, "module", newGroupId + ":" + newArtifactId))
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
        }
        return updateTableVersion(kv, inline, newVersion);
    }

    private Toml.KeyValue updateGroupNameTable(
            Toml.KeyValue kv, Toml.Table inline, String newGroupId, String newArtifactId, @Nullable String newVersion) {
        if (TomlTableValue.getString(inline, "group") != null) {
            inline = (Toml.Table) new ChangeValue("group", quotedValue(inline, "group", newGroupId))
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
        }
        if (TomlTableValue.getString(inline, "name") != null) {
            inline = (Toml.Table) new ChangeValue("name", quotedValue(inline, "name", newArtifactId))
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
        }
        return updateTableVersion(kv, inline, newVersion);
    }

    private Toml.KeyValue updateTableVersion(Toml.KeyValue kv, Toml.Table inline, @Nullable String newVersion) {
        if (newVersion == null) {
            return kv.withValue(inline);
        }
        if (TomlTableValue.find(inline, "version.ref") != null) {
            return kv.withValue(inline);
        }
        if (TomlTableValue.find(inline, "version") == null) {
            inline = TomlTableValue.withStringOrAdd(inline, "version", newVersion);
        } else if (TomlTableValue.getString(inline, "version") != null) {
            inline = (Toml.Table) new ChangeValue("version", quotedValue(inline, "version", newVersion))
                    .getVisitor()
                    .visitNonNull(inline, new InMemoryExecutionContext());
        }
        return kv.withValue(inline);
    }

    private static String keyName(Toml.KeyValue keyValue) {
        return ((Toml.Identifier) keyValue.getKey()).getName();
    }

    private static String quotedValue(Toml.Table table, String key, String value) {
        Toml.KeyValue keyValue = TomlTableValue.find(table, key);
        return keyValue != null && keyValue.getValue() instanceof Toml.Literal ?
                TomlTableValue.quoted((Toml.Literal) keyValue.getValue(), value) :
                "\"" + value + "\"";
    }

    private boolean isUnchanged(
            String newGroupId, String newArtifactId, @Nullable String newVersion, @Nullable String coordinateModule) {
        if (getTree().getValue() instanceof Toml.Literal) {
            Object value = ((Toml.Literal) getTree().getValue()).getValue();
            if (!(value instanceof String)) {
                return true;
            }
            Dependency dependency = DependencyNotation.parse((String) value);
            return dependency != null &&
                    newGroupId.equals(dependency.getGroupId()) &&
                    newArtifactId.equals(dependency.getArtifactId()) &&
                    (newVersion == null || newVersion.equals(dependency.getVersion()));
        }
        if (!(getTree().getValue() instanceof Toml.Table)) {
            return true;
        }
        Toml.Table table = (Toml.Table) getTree().getValue();
        if (coordinateModule != null) {
            return coordinateModule.equals(TomlTableValue.getString(table, "module")) &&
                    (newVersion == null || newVersion.equals(TomlTableValue.getString(table, "version")));
        }
        return newGroupId.equals(TomlTableValue.getString(table, "group")) &&
                newArtifactId.equals(TomlTableValue.getString(table, "name")) &&
                (newVersion == null || newVersion.equals(TomlTableValue.getString(table, "version")));
    }

    /**
     * Locates {@link GradleVersionCatalogDependency} instances in version-catalog TOML files.
     * <p>
     * Only {@link Toml.KeyValue} nodes that are direct children of a {@code [libraries]} table are matched.
     * Use {@link #groupPattern} and {@link #artifactPattern} to restrict the match to specific coordinates
     * (glob patterns are supported).
     */
    public static class Matcher extends SimpleTraitMatcher<GradleVersionCatalogDependency> {

        @Nullable
        private String groupPattern;

        @Nullable
        private String artifactPattern;

        /**
         * Restricts matching to entries whose {@code groupId} matches the given glob pattern.
         */
        public Matcher groupPattern(@Nullable String groupPattern) {
            this.groupPattern = groupPattern;
            return this;
        }

        /**
         * Restricts matching to entries whose {@code artifactId} matches the given glob pattern.
         */
        public Matcher artifactPattern(@Nullable String artifactPattern) {
            this.artifactPattern = artifactPattern;
            return this;
        }

        /**
         * Provides a {@link TomlIsoVisitor}-based visitor that only invokes the callback for
         * {@link Toml.KeyValue} nodes that match this matcher.
         */
        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleVersionCatalogDependency, P> visitor) {
            return new TomlIsoVisitor<P>() {
                @Override
                public Toml.KeyValue visitKeyValue(Toml.KeyValue kv, P p) {
                    GradleVersionCatalogDependency dep = test(getCursor());
                    return dep != null
                            ? (Toml.KeyValue) visitor.visit(dep, p)
                            : super.visitKeyValue(kv, p);
                }
            };
        }

        @Override
        protected @Nullable GradleVersionCatalogDependency test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Toml.KeyValue)) {
                return null;
            }
            Toml.KeyValue kv = cursor.getValue();

            Cursor parent = cursor.getParent();
            if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Table parentTable = parent.getValue();
            Toml.Identifier tableName = parentTable.getName();
            if (tableName == null || !"libraries".equals(tableName.getName())) {
                return null;
            }
            DependencyMatcher dependencyMatcher = new DependencyMatcher(groupPattern, artifactPattern, null);

            if (kv.getValue() instanceof Toml.Literal) {
                return testLiteral(cursor, kv, dependencyMatcher);
            }
            if (kv.getValue() instanceof Toml.Table) {
                return testInlineTable(cursor, kv, dependencyMatcher);
            }
            return null;
        }

        private static @Nullable GradleVersionCatalogDependency testLiteral(
                Cursor cursor, Toml.KeyValue kv, DependencyMatcher dependencyMatcher) {
            Toml.Literal literal = (Toml.Literal) kv.getValue();
            if (!(literal.getValue() instanceof String)) {
                return null;
            }
            Dependency dep = DependencyNotation.parse((String) literal.getValue());
            if (dep == null) {
                return null;
            }
            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            if (groupId == null || !dependencyMatcher.matches(groupId, artifactId)) {
                return null;
            }
            return new GradleVersionCatalogDependency(cursor, groupId, artifactId, null,
                    dep.getVersion(), null);
        }

        private static @Nullable GradleVersionCatalogDependency testInlineTable(
                Cursor cursor, Toml.KeyValue kv, DependencyMatcher dependencyMatcher) {
            Toml.Table inline = (Toml.Table) kv.getValue();
            String groupId = TomlTableValue.getString(inline, "group");
            String artifactId = TomlTableValue.getString(inline, "name");
            String module = TomlTableValue.getString(inline, "module");
            if (module != null && (groupId != null || artifactId != null)) {
                return null;
            }
            if (groupId != null && artifactId != null) {
                return dependencyMatcher.matches(groupId, artifactId) ?
                        new GradleVersionCatalogDependency(cursor, groupId, artifactId, null,
                                TomlTableValue.getString(inline, "version"),
                                TomlTableValue.getString(inline, "version.ref")) :
                        null;
            }
            return testModule(cursor, inline, module, dependencyMatcher);
        }

        private static @Nullable GradleVersionCatalogDependency testModule(
                Cursor cursor, Toml.Table inline, @Nullable String module, DependencyMatcher dependencyMatcher) {
            if (module == null || module.indexOf(':') < 0 || module.indexOf(':') != module.lastIndexOf(':')) {
                return null;
            }
            Dependency dep = DependencyNotation.parse(module);
            if (dep == null) {
                return null;
            }
            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            if (groupId == null || groupId.isEmpty() || artifactId.isEmpty() ||
                    !dependencyMatcher.matches(groupId, artifactId)) {
                return null;
            }
            return new GradleVersionCatalogDependency(cursor, groupId, artifactId, module,
                    TomlTableValue.getString(inline, "version"),
                    TomlTableValue.getString(inline, "version.ref"));
        }
    }
}
