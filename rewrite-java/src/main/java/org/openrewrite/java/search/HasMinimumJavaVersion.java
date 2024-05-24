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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasMinimumJavaVersion extends ScanningRecipe<AtomicReference<JavaVersion>> {
    @Option(displayName = "Java version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "17.X")
    String version;

    @Option(displayName = "Version check against target compatibility",
            description = "The source and target compatibility versions can be different. This option allows you to " +
                          "check against the target compatibility version instead of the source compatibility version.",
            example = "17.X",
            required = false)
    @Nullable
    Boolean checkTargetCompatibility;

    @Override
    public String getDisplayName() {
        return "Find the oldest Java version in use";
    }

    @Override
    public String getDescription() {
        return "The oldest Java version in use is the lowest Java " +
               "version in use in any source set of any subproject of " +
               "a repository. It is possible that, for example, the main " +
               "source set of a project uses Java 8, but a test source set " +
               "uses Java 17. In this case, the oldest Java version in use is " +
               "Java 8.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public AtomicReference<JavaVersion> getInitialValue(ExecutionContext ctx) {
        return new AtomicReference<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicReference<JavaVersion> acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                cu.getMarkers().findFirst(JavaVersion.class).ifPresent(javaVersion -> {
                    acc.updateAndGet(current -> {
                        if (current == null || javaVersion.getMajorVersion() < current.getMajorVersion()) {
                            return javaVersion;
                        }
                        return current;
                    });
                });
                return cu;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicReference<JavaVersion> acc) {
        VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());
        return Preconditions.check(minimumVersionInRange(acc, versionComparator), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                return cu.getMarkers().findFirst(JavaVersion.class)
                        .filter(javaVersion -> acc.get() != null && javaVersion.getMajorVersion() == acc.get().getMajorVersion())
                        .map(javaVersion -> SearchResult.found(cu, "Java version " + javaVersion.getMajorVersion()))
                        .orElse(cu);
            }
        });
    }

    private boolean minimumVersionInRange(AtomicReference<JavaVersion> acc, VersionComparator versionComparator) {
        return acc.get() != null && versionComparator.isValid(null, Integer.toString(
                Boolean.TRUE.equals(checkTargetCompatibility) ?
                        acc.get().getMajorReleaseVersion() :
                        acc.get().getMajorVersion()));
    }
}
