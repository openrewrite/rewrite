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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
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
            description = "A minimum version number or a node-style semver selector. Plain values like `17` or " +
                          "`17.0.1` match that version or higher. To match an exact version, use `HasJavaVersion` " +
                          "instead.",
            example = "17")
    String version;

    @Option(displayName = "Version check against target compatibility",
            description = "The source and target compatibility versions can be different. This option allows you to " +
                          "check against the target compatibility version instead of the source compatibility version.",
            example = "17.X",
            required = false)
    @Nullable
    Boolean checkTargetCompatibility;

    String displayName = "Has minimum Java version";

    String description = "Finds source files when the oldest Java version in use meets the " +
               "supplied minimum version. The oldest Java version in use is the lowest Java " +
               "version in use in any source set of any subproject of a repository. It is " +
               "possible that, for example, the main source set of a project uses Java 8, but " +
               "a test source set uses Java 17. In this case, the oldest Java version in use is " +
               "Java 8.";

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(canonicalizeVersion(version), null));
        }
        return validated;
    }

    /**
     * A plain version like "17" or "17.0.1" is treated as "N or higher" so the option
     * matches the recipe's "minimum" semantics. Any selector the user writes explicitly
     * (X-ranges, hyphen ranges, tildes, carets, set ranges) is left alone.
     */
    private static String canonicalizeVersion(String version) {
        if (version.isEmpty()) {
            return version;
        }
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c != '.' && !Character.isDigit(c)) {
                return version;
            }
        }
        return "[" + version + ",)";
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
                cu.getMarkers().findFirst(JavaVersion.class).ifPresent(javaVersion ->
                    acc.updateAndGet(current -> {
                        if (current == null || javaVersion.getMajorVersion() < current.getMajorVersion()) {
                            return javaVersion;
                        }
                        return current;
                    }));
                return cu;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicReference<JavaVersion> acc) {
        VersionComparator versionComparator = requireNonNull(Semver.validate(canonicalizeVersion(version), null).getValue());
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
