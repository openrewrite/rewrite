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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasJavaVersion extends Recipe {
    @Option(displayName = "Java version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String version;

    @Override
    public String getDisplayName() {
        return "Find files compiled at a specific Java version";
    }

    @Override
    public String getDescription() {
        return "Finds Java source files matching a particular language level. This is useful especially as an " +
               "applicable test for other recipes.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                return cu.getMarkers().findFirst(JavaVersion.class)
                        .filter(version -> versionComparator.isValid(null, Integer.toString(version.getMajorVersion())))
                        .map(version -> SearchResult.found(cu))
                        .orElse(cu);
            }
        };
    }
}
