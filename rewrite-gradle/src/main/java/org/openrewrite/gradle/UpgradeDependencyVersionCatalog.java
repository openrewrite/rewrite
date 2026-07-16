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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;

import java.util.HashMap;
import java.util.Map;

final class UpgradeDependencyVersionCatalog extends TomlIsoVisitor<ExecutionContext> {
    private final String groupId;
    private final String artifactId;
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;
    private final DependencyMatcher dependencyMatcher;

    UpgradeDependencyVersionCatalog(
            String groupId,
            String artifactId,
            @Nullable String newVersion,
            @Nullable String versionPattern,
            MavenMetadataFailures metadataFailures,
            @Nullable GradleProject gradleProject) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
        this.dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        Toml.Table libraries = VersionCatalogToml.findTable(document, "libraries");
        Toml.Table versions = VersionCatalogToml.findTable(document, "versions");
        if (libraries == null) {
            return document;
        }

        Map<String, String> referencedVersions = new HashMap<>();
        for (Toml value : libraries.getValues()) {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getValue() instanceof Toml.Table)) {
                continue;
            }
            Toml.Table library = (Toml.Table) ((Toml.KeyValue) value).getValue();
            String versionRef = TomlTableValue.getString(library, "version.ref");
            if (versionRef == null || !matches(library)) {
                continue;
            }
            try {
                referencedVersions.put(versionRef, selectVersion(VersionCatalogToml.getVersion(versions, versionRef), ctx));
            } catch (MavenDownloadingException e) {
                return e.warn(document);
            }
        }

        Toml.Document updated = document.withValues(ListUtils.map(document.getValues(), value -> {
            if (!(value instanceof Toml.Table)) {
                return value;
            }
            Toml.Table table = (Toml.Table) value;
            if (table.getName() != null && "libraries".equals(table.getName().getName())) {
                return updateLibraries(table, ctx);
            }
            if (table.getName() != null && "versions".equals(table.getName().getName())) {
                return updateVersions(table, referencedVersions);
            }
            return value;
        }));
        return super.visitDocument(updated, ctx);
    }

    private Toml.Table updateLibraries(Toml.Table libraries, ExecutionContext ctx) {
        return libraries.withValues(ListUtils.map(libraries.getValues(), value -> {
            if (!(value instanceof Toml.KeyValue)) {
                return value;
            }
            Toml.KeyValue library = (Toml.KeyValue) value;
            if (library.getValue() instanceof Toml.Literal) {
                Toml.Literal literal = (Toml.Literal) library.getValue();
                if (!(literal.getValue() instanceof String)) {
                    return library;
                }
                Dependency dependency = DependencyNotation.parse((String) literal.getValue());
                if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                    return library;
                }
                try {
                    String selected = selectVersion(dependency.getVersion(), ctx);
                    if (selected != null) {
                        String notation = DependencyNotation.toStringNotation(dependency.withGav(dependency.getGav().withVersion(selected)));
                        return library.withValue(literal.withSource(VersionCatalogToml.quoted(literal, notation)).withValue(notation));
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(library);
                }
            } else if (library.getValue() instanceof Toml.Table) {
                Toml.Table inline = (Toml.Table) library.getValue();
                if (!matches(inline) || !TomlTableValue.has(inline, "version")) {
                    return library;
                }
                try {
                    String selected = selectVersion(TomlTableValue.getString(inline, "version"), ctx);
                    if (selected != null) {
                        return library.withValue(TomlTableValue.withString(inline, "version", selected));
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(library);
                }
            }
            return library;
        }));
    }

    private Toml.Table updateVersions(Toml.Table versions, Map<String, String> referencedVersions) {
        return versions.withValues(ListUtils.map(versions.getValues(), value -> {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier) ||
                    !(((Toml.KeyValue) value).getValue() instanceof Toml.Literal)) {
                return value;
            }
            String key = ((Toml.Identifier) ((Toml.KeyValue) value).getKey()).getName();
            String selected = referencedVersions.get(key);
            if (selected == null) {
                return value;
            }
            Toml.Literal literal = (Toml.Literal) ((Toml.KeyValue) value).getValue();
            return ((Toml.KeyValue) value).withValue(literal.withSource(VersionCatalogToml.quoted(literal, selected)).withValue(selected));
        }));
    }

    private boolean matches(Toml.Table library) {
        return dependencyMatcher.matches(TomlTableValue.getString(library, "group"), TomlTableValue.getString(library, "name"));
    }

    private @Nullable String selectVersion(@Nullable String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        if (currentVersion == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, null)
                .select(new GroupArtifactVersion(groupId, artifactId, currentVersion), null, newVersion, versionPattern, ctx);
    }
}
