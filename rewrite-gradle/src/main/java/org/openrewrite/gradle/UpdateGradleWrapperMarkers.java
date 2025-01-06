/*
 * Copyright 2022 the original author or authors.
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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.time.Duration;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.isBlank;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class UpdateGradleWrapperMarkers extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper marker versions";
    }

    @Override
    public String getDescription() {
        return "Update the version of Gradle used in an existing Gradle wrapper. " +
                "Queries services.gradle.org to determine the available releases, but prefers the artifact repository URL " +
                "which already exists within the wrapper properties file. " +
                "If your artifact repository does not contain the same Gradle distributions as services.gradle.org, " +
                "then the recipe may suggest a version which is not available in your artifact repository.";
    }

    @Getter
    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                    "Defaults to the latest release available from services.gradle.org if not specified.",
            example = "7.x",
            required = false)
    @Nullable
    final String version;

    @Getter
    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to use. \"bin\" includes Gradle binaries. " +
                    "\"all\" includes Gradle binaries, source code, and documentation. " +
                    "Defaults to \"bin\".",
            valid = {"bin", "all"},
            required = false
    )
    @Nullable
    final String distribution;

    @Getter
    @Option(displayName = "Add if missing",
            description = "Add a Gradle wrapper, if it's missing. Defaults to `true`.",
            required = false)
    @Nullable
    final Boolean addIfMissing;

    @Getter
    @Option(example = "https://services.gradle.org/distributions/gradle-${version}-${distribution}.zip",
            displayName = "Wrapper URI",
            description = "The URI of the Gradle wrapper distribution. " +
                    "Lookup of available versions still requires access to https://services.gradle.org " +
                    "When this is specified the exact literal values supplied for `version` and `distribution` " +
                    "will be interpolated into this string wherever `${version}` and `${distribution}` appear respectively. " +
                    "Defaults to https://services.gradle.org/distributions/gradle-${version}-${distribution}.zip.",
            required = false)
    @Nullable
    final String wrapperUri;

    @Getter
    @Option(example = "29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda",
            displayName = "SHA-256 checksum",
            description = "The SHA-256 checksum of the Gradle distribution. " +
                    "If specified, the recipe will add the checksum along with the custom distribution URL.",
            required = false)
    @Nullable
    final String distributionChecksum;

    @NonFinal
    @Nullable
    transient GradleWrapper gradleWrapper;

    private GradleWrapper getGradleWrapper(ExecutionContext ctx) {
        gradleWrapper = UpdateGradleWrapper.getGradleWrapper(ctx, wrapperUri, distribution, version, gradleWrapper);
        return gradleWrapper;
    }

    /**
     * This recipe only updates markers, so it does not correspond to human manual effort.
     *
     * @return Zero estimated time.
     */
    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Optional<BuildTool> maybeCurrentMarker = tree.getMarkers().findFirst(BuildTool.class);
                if (maybeCurrentMarker.isPresent()) {
                    BuildTool currentMarker = maybeCurrentMarker.get();
                    if (currentMarker.getType() != BuildTool.Type.Gradle) {
                        return tree;
                    }
                    String gradleWrapperVersion = getGradleWrapper(ctx).getVersion();
                    BuildTool currentBuildTool = currentMarker.withVersion(gradleWrapperVersion);
                    VersionComparator versionComparator = requireNonNull(Semver.validate(isBlank(version) ? "latest.release" : version, null).getValue());
                    int compare = versionComparator.compare(null, currentMarker.getVersion(), currentBuildTool.getVersion());
                    if (compare < 0) {
                        return tree.withMarkers(tree.getMarkers().setByType(currentBuildTool));
                    } else {
                        return tree;
                    }
                }

                return tree;
            }
        };
    }
}