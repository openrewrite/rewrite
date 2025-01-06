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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class UpdateGradleWrapper extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper";
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

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new UpdateGradleWrapperFiles(version, distribution, addIfMissing, wrapperUri, distributionChecksum),
                new UpdateGradleWrapperMarkers(version, distribution, addIfMissing, wrapperUri, distributionChecksum)
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                return tree;
            }
        };
    }

    public static GradleWrapper getGradleWrapper(ExecutionContext ctx, String wrapperUri, String distribution, String version, @Nullable GradleWrapper gradleWrapper) {
        if (gradleWrapper == null) {
            if (wrapperUri != null) {
                return GradleWrapper.create(URI.create(wrapperUri), ctx);
            }
            try {
                return GradleWrapper.create(distribution, version, null, ctx);
            } catch (Exception e) {
                // services.gradle.org is unreachable
                // If the user didn't specify a wrapperUri, but they did provide a specific version we assume they know this version
                // is available from whichever distribution url they were previously using and update the version
                if (!StringUtils.isBlank(version) && Semver.validate(version, null).getValue() instanceof ExactVersion) {
                    return new GradleWrapper(version, new DistributionInfos("", null, null));
                }
                throw new IllegalArgumentException(
                        "Could not reach services.gradle.org. " +
                                "To use this recipe in environments where services.gradle.org is unavailable specify a wrapperUri or exact version.", e);
            }
        }
        return gradleWrapper;
    }
}
