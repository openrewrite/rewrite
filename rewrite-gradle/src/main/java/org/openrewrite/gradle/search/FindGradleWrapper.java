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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.table.GradleWrappersInUse;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_PROPERTIES_LOCATION;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindGradleWrapper extends Recipe {
    transient GradleWrappersInUse wrappersInUse = new GradleWrappersInUse(this);

    private static final Pattern GRADLE_VERSION = Pattern.compile("gradle-(.*?)-(all|bin).zip");

    @Option(displayName = "Version expression",
            description = "A version expression representing the versions to search for",
            example = "7.x",
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

    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to find. \"bin\" includes Gradle binaries. " +
                    "\"all\" includes Gradle binaries, source code, and documentation.",
            valid = {"bin", "all"},
            required = false
    )
    @Nullable
    String distribution;

    @Override
    public String getDisplayName() {
        return "Find Gradle wrappers";
    }

    @Override
    public String getDescription() {
        return "Find Gradle wrappers.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                return !equalIgnoringSeparators(file.getSourcePath(), WRAPPER_PROPERTIES_LOCATION) ? file :
                        super.visitFile(file, ctx);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                if (!"distributionUrl".equals(entry.getKey())) {
                    return entry;
                }

                String currentDistributionUrl = entry.getValue().getText();
                Matcher matcher = GRADLE_VERSION.matcher(currentDistributionUrl);
                if (matcher.find()) {
                    String currentVersion = matcher.group(1);
                    boolean requireVersion = !StringUtils.isNullOrEmpty(version);
                    String currentDistribution = matcher.group(2);
                    boolean requireMeta = !StringUtils.isNullOrEmpty(distribution);

                    wrappersInUse.insertRow(ctx, new GradleWrappersInUse.Row(
                            currentVersion,
                            currentDistribution
                    ));

                    if (requireVersion) {
                        VersionComparator versionComparator = Semver.validate(version, versionPattern).getValue();
                        if (versionComparator == null || versionComparator.isValid(null, currentVersion)) {
                            if (requireMeta) {
                                if (currentDistribution.matches(distribution)) {
                                    return SearchResult.found(entry);
                                }
                            } else {
                                return SearchResult.found(entry);
                            }
                        }
                    } else if (requireMeta) {
                        if (currentDistribution.matches(distribution)) {
                            return SearchResult.found(entry);
                        }
                    } else {
                        return SearchResult.found(entry);
                    }
                }
                return entry;
            }
        };
    }
}
