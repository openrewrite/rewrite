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
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.XPathMatcher;

import java.util.List;

/**
 * Make existing dependencies "dependency managed", moving the version to the dependencyManagement
 * section of the POM.
 * <p>
 * All dependencies that match {@link #groupPattern} and {@link #artifactPattern} should be
 * align-able to the same version (either the version provided to this visitor or the maximum matching
 * version if none is provided).
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ManageDependencies2 extends Recipe {
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                    "Group is the the first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.*")
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                    "Artifact is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Version",
            description = "Version to use for the dependency in dependency management. " +
                    "Defaults to the existing version found on the matching dependency, or the max version if multiple dependencies match the glob expression patterns.",
            example = "1.0.0",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Add to the root pom",
            description = "Add to the root pom where root is the eldest parent of the pom within the source set.",
            example = "true",
            required = false)
    @Nullable
    Boolean addToRootPom;

    @Override
    public String getDisplayName() {
        return "Manage dependencies";
    }

    @Override
    public String getDescription() {
        return "Make existing dependencies managed by moving their version to be specified in the dependencyManagement section of the POM.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return MavenReactorOrdering.visitPomsInReactorOrder(before, ctx, relation -> {
            if (relation.anyProjectMatches((pom, resolutionResult) -> {
                for (ResolvedDependency dependency : resolutionResult.findDependencies(groupPattern, artifactPattern == null ? "*" : artifactPattern, null)) {
                    if (dependency.isDirect()) {
                        return true;
                    }
                }
                return false;
            })) {
                // add managed dependency and return mutated relation.getPom()
            }
            return relation.getPom();
        });
    }
}
