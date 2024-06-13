/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindManagedDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.0.0",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    public static Set<Xml.Tag> find(Xml.Document maven, String groupId, String artifactId) {
        return find(maven, groupId, artifactId, null, null);
    }

    public static Set<Xml.Tag> find(Xml.Document maven, String groupId, String artifactId,
                                    @Nullable String version, @Nullable String versionPattern) {
        Set<Xml.Tag> ds = new HashSet<>();
        new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isManagedDependencyTag(groupId, artifactId) &&
                    versionIsValid(version, tag.getChildValue("version").orElse(null), versionPattern)) {
                    ds.add(tag);
                }
                return super.visitTag(tag, ctx);
            }
        }.visit(maven, new InMemoryExecutionContext());
        return ds;
    }

    @Override
    public String getDisplayName() {
        return "Find Maven dependency management entry";
    }

    @Override
    public String getInstanceNameSuffix() {
        String maybeVersionSuffix = version == null ? "" : String.format(":%s%s", version, versionPattern == null ? "" : versionPattern);
        return String.format("`%s:%s%s`", groupId, artifactId, maybeVersionSuffix);
    }

    @Override
    public String getDescription() {
        return "Finds first-order dependency management entries, i.e. dependencies that are defined directly in a project.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isManagedDependencyTag(groupId, artifactId) &&
                    versionIsValid(version, tag.getChildValue("version").orElse(null), versionPattern)) {
                    return SearchResult.found(tag);
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    private static boolean versionIsValid(@Nullable String desiredVersion, @Nullable String actualVersion,
                                          @Nullable String versionPattern) {
        if (desiredVersion == null) {
            return true;
        }
        if (actualVersion == null) {
            // rare but technically valid for a dependencyManagement entry to have no version; bail if so
            return false;
        }
        Validated<VersionComparator> validate = Semver.validate(desiredVersion, versionPattern);
        if (validate.isInvalid()) {
            return false;
        }
        assert validate.getValue() != null;
        return validate.getValue().isValid(actualVersion, actualVersion);
    }
}
