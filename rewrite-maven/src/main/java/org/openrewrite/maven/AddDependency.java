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

    private final String groupId;

    private final String artifactId;

    /**
     * When other modules exist from the same dependency family, defined as those dependencies whose groupId matches
     * {@link #familyPattern}, this recipe will ignore the version attribute and attempt to align the new dependency
     * with the highest version already in use.
     * <p>
     * To pull the whole family up to a later version, use {@link UpgradeDependencyVersion}.
     */
    private final String version;

    /**
     * Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     * A {@link HyphenRange} of "25-29" can be paired with a metadata pattern of "-jre" to select
     * Guava 29.0-jre
     */
    @Nullable
    @With
    private String versionPattern;

    @With
    private boolean releasesOnly = true;

    @Nullable
    @With
    private String classifier;

    @Nullable
    @With
    private String scope;

    @Nullable
    @With
    private String type;

    /**
     * A glob expression used to identify other dependencies in the same family as the dependency to be added.
     */
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
        return "Add Dependency";
    }

    @Override
    public String getDescription() {
        return "Adds a Maven dependency";
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
