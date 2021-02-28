/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.Semver;

import java.util.regex.Pattern;

@Incubating(since = "7.0.0")
@Getter
@RequiredArgsConstructor
@AllArgsConstructor(onConstructor_=@JsonCreator)
@EqualsAndHashCode(callSuper = true)
public class AddDependency extends Recipe {

    @Option(displayName = "Group ID")
    private final String groupId;

    @Option(displayName = "Artifact ID")
    private final String artifactId;

    /**
     * When other modules exist from the same dependency family, defined as those dependencies whose groupId matches
     * {@link #familyPattern}, this recipe will ignore the version attribute and attempt to align the new dependency
     * with the highest version already in use.
     * <p>
     * To pull the whole family up to a later version, use {@link UpgradeDependencyVersion}.
     */
    @Option(displayName = "Version", description = "An exact version number, or node-style semver selector used to select the version number.")
    private final String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            required = false)
    @Nullable
    @With
    private String versionPattern;

    @Option(displayName = "Releases only",
            description = "When set to 'true' snapshots are excluded from consideration. Defaults to 'true'",
            required = false)
    @With
    private boolean releasesOnly = true;

    @Option(displayName = "Classifier", required = false)
    @Nullable
    @With
    private String classifier;

    @Option(displayName = "Scope", required = false)
    @Nullable
    @With
    private String scope;

    @Option(displayName = "Type", required = false)
    @Nullable
    @With
    private String type;

    /**
     * A glob expression used to identify other dependencies in the same family as the dependency to be added.
     */
    @Option(displayName = "Family pattern", required = false,
            description = "A pattern, applied to groupIds, used to determine which other dependencies should have aligned version numbers. " +
                    "Accepts '*' as a wildcard character.")
    @Nullable
    @With
    private String familyPattern;

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.or(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Add Maven dependency";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddDependencyVisitor(
                groupId,
                artifactId,
                version,
                versionPattern,
                releasesOnly,
                classifier,
                scope,
                type,
                familyPattern == null ? null : Pattern.compile(familyPattern.replace("*", ".*"))
        );
    }
}
