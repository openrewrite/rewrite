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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<AddManagedDependency.Scanned> {

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Override
    public String getDisplayName() {
        return "Upgrade transitive Maven dependencies";
    }

    @Override
    public String getDescription() {
        return "Upgrades the version of a transitive dependency in a Maven pom file. " +
               "Leaves direct dependencies unmodified. " +
               "Can be paired with the regular Upgrade Dependency Version recipe to upgrade a dependency everywhere, " +
               "regardless of whether it is direct or transitive.";
    }

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.apache.logging.log4j:ARTIFACT_ID:VERSION'.",
            example = "org.apache.logging.log4j")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
            example = "log4j-bom")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "latest.release")
    String version;

    @Option(displayName = "Scope",
            description = "An optional scope to use for the dependency management tag.",
            example = "import",
            valid = {"import", "runtime", "provided", "test"},
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Type",
            description = "An optional type to use for the dependency management tag.",
            valid = {"jar", "pom", "war"},
            example = "pom",
            required = false)
    @Nullable
    String type;

    @Option(displayName = "Classifier",
            description = "An optional classifier to use for the dependency management tag",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            required = false)
    @Nullable
    Boolean releasesOnly;

    @Option(displayName = "Only if using glob expression for group:artifact",
            description = "Only add managed dependencies to projects having a dependency matching the expression.",
            example = "org.apache.logging.log4j:log4j*",
            required = false)
    @Nullable
    String onlyIfUsing;

    @Option(displayName = "Add to the root pom",
            description = "Add to the root pom where root is the eldest parent of the pom within the source set.",
            required = false)
    @Nullable
    Boolean addToRootPom;

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Semver.validate(version, versionPattern));
    }

    @Override
    public AddManagedDependency.Scanned getInitialValue(ExecutionContext ctx) {
        return addManagedDependency().getInitialValue(ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AddManagedDependency.Scanned acc) {
        return addManagedDependency().getScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AddManagedDependency.Scanned acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Set<ResolvedDependency> matchingDependencies = getResolutionResult().findDependencies(groupId, artifactId, null)
                        .stream()
                        .filter(dep -> dep.getDepth() > 0)
                        .collect(toCollection(LinkedHashSet::new));
                if(matchingDependencies.isEmpty()) {
                    return document;
                }
                Xml.Document d = document;
                for (ResolvedDependency matchingDependency : matchingDependencies) {
                    d = (Xml.Document) addManagedDependency(matchingDependency.getGroupId(), matchingDependency.getArtifactId())
                            .getVisitor(acc)
                            .visitNonNull(document, ctx);
                }
                return d;
            }
        };
    }

    private AddManagedDependency addManagedDependency() {
        return addManagedDependency(groupId, artifactId);
    }

    private AddManagedDependency addManagedDependency(String groupId, String artifactId) {
        return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier, versionPattern, releasesOnly, onlyIfUsing, addToRootPom);
    }
}
