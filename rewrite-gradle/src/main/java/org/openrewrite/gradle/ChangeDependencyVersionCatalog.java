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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;

import java.util.Objects;

final class ChangeDependencyVersionCatalog extends TomlIsoVisitor<ExecutionContext> {
    private final String oldGroupId;
    private final String oldArtifactId;
    private final @Nullable String newGroupId;
    private final @Nullable String newArtifactId;
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final @Nullable Boolean overrideManagedVersion;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;

    ChangeDependencyVersionCatalog(
            String oldGroupId,
            String oldArtifactId,
            @Nullable String newGroupId,
            @Nullable String newArtifactId,
            @Nullable String newVersion,
            @Nullable String versionPattern,
            @Nullable Boolean overrideManagedVersion,
            MavenMetadataFailures metadataFailures,
            @Nullable GradleProject gradleProject) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        Toml.Table libraries = VersionCatalogToml.findTable(document, "libraries");
        Toml.Document updated = updateVersionReferences(document, libraries, ctx);
        return super.visitDocument(updated, ctx);
    }

    @Override
    public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
        Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);
        if (!(kv.getValue() instanceof Toml.Table)) {
            return kv;
        }

        Toml.Table library = (Toml.Table) kv.getValue();
        if (!matchesGroupAndName(library)) {
            return kv;
        }

        String replacementGroupId = StringUtils.isBlank(newGroupId) ? oldGroupId : newGroupId;
        String replacementArtifactId = StringUtils.isBlank(newArtifactId) ? oldArtifactId : newArtifactId;
        String selectedVersion = null;
        if (!StringUtils.isBlank(newVersion)) {
            try {
                selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                        .select(new GroupArtifact(replacementGroupId, replacementArtifactId), null, newVersion, versionPattern, ctx);
            } catch (MavenDownloadingException e) {
                return e.warn(kv);
            }
        }
        library = TomlTableValue.withString(library, "group", replacementGroupId);
        library = TomlTableValue.withString(library, "name", replacementArtifactId);
        if (selectedVersion != null) {
            if (TomlTableValue.has(library, "version")) {
                library = TomlTableValue.withString(library, "version", selectedVersion);
            } else if (Boolean.TRUE.equals(overrideManagedVersion) && !TomlTableValue.has(library, "version.ref")) {
                library = TomlTableValue.withStringOrAdd(library, "version", selectedVersion);
            }
        }
        return kv.withValue(library);
    }

    private boolean matchesGroupAndName(Toml.Table library) {
        return Objects.equals(oldGroupId, TomlTableValue.getString(library, "group")) &&
                Objects.equals(oldArtifactId, TomlTableValue.getString(library, "name"));
    }

    private Toml.Document updateVersionReferences(Toml.Document document, Toml.@Nullable Table libraries, ExecutionContext ctx) {
        if (libraries == null || StringUtils.isBlank(newVersion)) {
            return document;
        }
        return document.withValues(ListUtils.map(document.getValues(), value -> {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                if (table.getName() != null && "versions".equals(table.getName().getName())) {
                    return updateVersionTable(table, libraries, ctx);
                }
            }
            return value;
        }));
    }

    private Toml.Table updateVersionTable(Toml.Table versions, Toml.Table libraries, ExecutionContext ctx) {
        return versions.withValues(ListUtils.map(versions.getValues(), value -> {
            if (!(value instanceof Toml.KeyValue) ||
                    !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier) ||
                    !(((Toml.KeyValue) value).getValue() instanceof Toml.Literal)) {
                return value;
            }
            Toml.KeyValue version = (Toml.KeyValue) value;
            String versionName = ((Toml.Identifier) version.getKey()).getName();
            if (!isReferencedByMatchingLibrary(libraries, versionName)) {
                return version;
            }
            try {
                String selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                        .select(new GroupArtifact(
                                StringUtils.isBlank(newGroupId) ? oldGroupId : newGroupId,
                                StringUtils.isBlank(newArtifactId) ? oldArtifactId : newArtifactId
                        ), null, newVersion, versionPattern, ctx);
                if (selectedVersion != null) {
                    Toml.Literal literal = (Toml.Literal) version.getValue();
                    return version.withValue(literal.withSource("\"" + selectedVersion + "\"").withValue(selectedVersion));
                }
            } catch (MavenDownloadingException e) {
                return e.warn(version);
            }
            return version;
        }));
    }

    private boolean isReferencedByMatchingLibrary(Toml.Table libraries, String versionName) {
        for (Toml value : libraries.getValues()) {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getValue() instanceof Toml.Table)) {
                continue;
            }
            Toml.Table library = (Toml.Table) ((Toml.KeyValue) value).getValue();
            if (matchesGroupAndName(library) &&
                    Objects.equals(versionName, TomlTableValue.getString(library, "version.ref"))) {
                return true;
            }
        }
        return false;
    }
}
