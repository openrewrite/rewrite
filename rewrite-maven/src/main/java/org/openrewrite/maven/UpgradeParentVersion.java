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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.semver.Semver;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeParentVersion extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
            example = "org.springframework.boot")
    @Getter
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
            example = "spring-boot-parent")
    @Getter
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    @Getter
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Getter
    @Nullable
    String versionPattern;

    @Option(displayName = "Retain versions",
            description = "Accepts a list of GAVs. For each GAV, if it is a project direct dependency, and it is removed " +
                          "from dependency management in the new parent pom, then it will be retained with an explicit version. " +
                          "The version can be omitted from the GAV to use the old value from dependency management",
            example = "com.jcraft:jsch",
            required = false)
    @Getter
    @Nullable
    List<String> retainVersions;

    @Override
    public String getDisplayName() {
        return "Upgrade Maven parent project version";
    }

    @Override
    public String getDescription() {
        return "Set the parent pom version number according to a node-style semver selector or to a specific version number.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    protected MavenVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return changeParentPom().getSingleSourceApplicableTest();
    }

    @Override
    public MavenVisitor<ExecutionContext> getVisitor() {
        return changeParentPom().getVisitor();
    }

    private ChangeParentPom changeParentPom() {
        return new ChangeParentPom(groupId, null, artifactId, null, newVersion,
                versionPattern, false, retainVersions);
    }
}
