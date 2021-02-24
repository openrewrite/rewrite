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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.Semver;

import java.util.regex.Pattern;

@Incubating(since = "7.0.0")
@Data
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
    private String versionPattern;

    private boolean releasesOnly = true;

    @Nullable
    private String classifier;

    @Nullable
    private String scope;

    @Nullable
    private String type;

    /**
     * A glob expression used to identify other dependencies in the same family as the dependency to be added.
     */
    @Nullable
    private String familyPattern;

    /**
     * Allows the version selection to be extended beyond the original Node Semver semantics. So for example,
     * a {@link HyphenRange} of "25-29" can be paired with a version pattern of "-jre" to select
     * Guava 29.0-jre
     *
     * @param versionPattern The version pattern extending semver selection.
     */
    public void setVersionPattern(@Nullable String versionPattern) {
        this.versionPattern = versionPattern;
    }

    public void setClassifier(@Nullable String classifier) {
        this.classifier = classifier;
    }

    public void setScope(@Nullable String scope) {
        this.scope = scope;
    }

    public void setReleasesOnly(boolean releasesOnly) {
        this.releasesOnly = releasesOnly;
    }

    /**
     * @param familyPattern A glob expression used to identify other dependencies in the same
     *                      family as the dependency to be added.
     */
    public void setFamilyPattern(@Nullable String familyPattern) {
        this.familyPattern = familyPattern;
    }

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
