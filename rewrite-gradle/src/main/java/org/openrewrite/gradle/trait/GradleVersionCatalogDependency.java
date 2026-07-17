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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Represents a library entry in a Gradle version catalog TOML file ({@code [libraries]} table).
 * <p>
 * Matches both string-notation entries ({@code guava = "com.google.guava:guava:29.0-jre"}) and
 * inline-table entries ({@code guava = { group = "com.google.guava", name = "guava", version = "29.0-jre" }}).
 * <p>
 * Use the inner {@link Matcher} to locate and filter library entries during a recipe traversal.
 */
@Value
public class GradleVersionCatalogDependency implements Trait<Toml.KeyValue> {

    Cursor cursor;
    String groupId;
    String artifactId;
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
     * updates the existing {@code version} key. Entries using {@code version.ref} are unchanged.
     */
    public Toml.KeyValue withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return getTree();
        }
        Toml.KeyValue kv = getTree();
        if (kv.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) kv.getValue();
            if (!(literal.getValue() instanceof String)) {
                return kv;
            }
            Dependency dependency = DependencyNotation.parse((String) literal.getValue());
            if (dependency == null) {
                return kv;
            }
            String notation = DependencyNotation.toStringNotation(dependency.withGav(dependency.getGav().withVersion(newVersion)));
            return kv.withValue(literal.withSource(VersionCatalogToml.quoted(literal, notation)).withValue(notation));
        }
        if (kv.getValue() instanceof Toml.Table) {
            Toml.Table inline = (Toml.Table) kv.getValue();
            if (!TomlTableValue.has(inline, "version")) {
                return kv;
            }
            return kv.withValue(TomlTableValue.withString(inline, "version", newVersion));
        }
        return kv;
    }

    /**
     * Updates an inline-table library entry's coordinates and, optionally, its direct {@code version} value.
     * <p>
     * This method is the primary mutation helper for coordinate-change recipes. It:
     * <ul>
     *   <li>Always updates {@code group} and {@code name}.</li>
     *   <li>If {@code newVersion} is non-null and the entry already has a {@code version} key, updates it.</li>
     *   <li>If {@code newVersion} is non-null, {@code overrideManagedVersion} is {@code true}, and the entry
     *       has neither a {@code version} nor a {@code version.ref} key, the {@code version} key is added.</li>
     * </ul>
     * Has no effect for string-notation entries.
     */
    public Toml.KeyValue withInlineCoordinatesAndVersion(
            String newGroupId, String newArtifactId,
            @Nullable String newVersion, boolean overrideManagedVersion) {
        if (!(getTree().getValue() instanceof Toml.Table)) {
            return getTree();
        }
        Toml.KeyValue kv = getTree();
        Toml.Table inline = (Toml.Table) kv.getValue();
        inline = TomlTableValue.withString(inline, "group", newGroupId);
        inline = TomlTableValue.withString(inline, "name", newArtifactId);
        if (newVersion == null) {
            return kv.withValue(inline);
        }
        if (TomlTableValue.has(inline, "version")) {
            return kv.withValue(TomlTableValue.withString(inline, "version", newVersion));
        }
        if (!overrideManagedVersion || TomlTableValue.has(inline, "version.ref")) {
            return kv.withValue(inline);
        }
        return kv.withValue(TomlTableValue.withStringOrAdd(inline, "version", newVersion));
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

            // Must be a direct child of the [libraries] table.
            Cursor parent = cursor.getParent();
            if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Table parentTable = parent.getValue();
            if (parentTable.getName() == null || !"libraries".equals(parentTable.getName().getName())) {
                return null;
            }

            return extractWithCursor(cursor, kv, groupPattern, artifactPattern);
        }

        /**
         * Extracts a {@link GradleVersionCatalogDependency} from a {@link Toml.KeyValue} that is already
         * known to reside inside the {@code [libraries]} table, without requiring a full cursor chain.
         * Useful when iterating table values directly (e.g. inside a {@code visitDocument} pre-scan).
         * The returned trait can be used for coordinate/version access and for mutation via its
         * {@code with*} methods; the updated {@link Toml.KeyValue} should be used as the replacement
         * value in the enclosing map operation.
         */
        public static @Nullable GradleVersionCatalogDependency extract(
                Toml.KeyValue kv,
                @Nullable String groupPattern,
                @Nullable String artifactPattern) {
            // Build a minimal synthetic cursor so that getTree() / mutation methods work correctly.
            Cursor syntheticCursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), kv);
            return extractWithCursor(syntheticCursor, kv, groupPattern, artifactPattern);
        }

        private static @Nullable GradleVersionCatalogDependency extractWithCursor(
                Cursor cursor,
                Toml.KeyValue kv,
                @Nullable String groupPattern,
                @Nullable String artifactPattern) {
            if (kv.getValue() instanceof Toml.Literal) {
                Toml.Literal literal = (Toml.Literal) kv.getValue();
                if (!(literal.getValue() instanceof String)) {
                    return null;
                }
                Dependency dep = DependencyNotation.parse((String) literal.getValue());
                if (dep == null || !matchesPatterns(dep.getGroupId(), dep.getArtifactId(), groupPattern, artifactPattern)) {
                    return null;
                }
                return new GradleVersionCatalogDependency(cursor, dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), null);
            }
            if (kv.getValue() instanceof Toml.Table) {
                Toml.Table inline = (Toml.Table) kv.getValue();
                String groupId = TomlTableValue.getString(inline, "group");
                String artifactId = TomlTableValue.getString(inline, "name");
                if (groupId == null || artifactId == null ||
                        !matchesPatterns(groupId, artifactId, groupPattern, artifactPattern)) {
                    return null;
                }
                return new GradleVersionCatalogDependency(cursor, groupId, artifactId,
                        TomlTableValue.getString(inline, "version"),
                        TomlTableValue.getString(inline, "version.ref"));
            }
            return null;
        }

        private static boolean matchesPatterns(
                @Nullable String groupId, @Nullable String artifactId,
                @Nullable String groupPattern, @Nullable String artifactPattern) {
            if (groupPattern != null && !matchesGlob(groupId, groupPattern)) {
                return false;
            }
            return artifactPattern == null || matchesGlob(artifactId, artifactPattern);
        }
    }
}
