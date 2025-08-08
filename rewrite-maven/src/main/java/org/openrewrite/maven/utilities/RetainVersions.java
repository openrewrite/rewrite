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
package org.openrewrite.maven.utilities;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.search.FindDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class RetainVersions {

    /**
     * Returns a list of visitors which can be applied to add explicit versions
     * for dependencies matching the GAVs in param `retainVersions`
     */
    public static List<TreeVisitor<?, ExecutionContext>> plan(MavenVisitor<?> visitor, @Nullable List<String> retainVersions) {
        List<TreeVisitor<?, ExecutionContext>> visitors = new ArrayList<>();
        if (retainVersions != null) {
            for (String gav : retainVersions) {
                String[] split = gav.split(":");
                String requestedRetainedGroupId = split[0];
                String requestedRetainedArtifactId = split[1];
                String requestedRetainedVersion = split.length == 3 ? split[2] : null;
                Set<Xml.Tag> existingDependencies = FindDependency.find(
                        visitor.getCursor().firstEnclosingOrThrow(Xml.Document.class),
                        requestedRetainedGroupId, requestedRetainedArtifactId);

                // optimization for glob GAVs: more efficient to use one CDGIAAI recipe if they all will have the same version anyway
                if (requestedRetainedVersion != null && noneMatch(existingDependencies, it -> it.getChild("version").isPresent())) {
                    visitors.add(changeDependencyGroupIdAndArtifactId(visitor, requestedRetainedGroupId, requestedRetainedArtifactId, requestedRetainedVersion));
                    continue;
                }

                for (Xml.Tag existingDependency : existingDependencies) {
                    String retainedGroupId = existingDependency.getChildValue("groupId")
                            .orElseThrow(() -> new IllegalStateException("Dependency tag must have groupId"));
                    String retainedArtifactId = existingDependency.getChildValue("artifactId")
                            .orElseThrow(() -> new IllegalStateException("Dependency tag must have artifactId"));
                    String retainedVersion = requestedRetainedVersion;

                    if (retainedVersion == null) {
                        if (existingDependency.getChildValue("version").isPresent()) {
                            continue;
                        } else {
                            ResolvedManagedDependency managedDependency = visitor.findManagedDependency(
                                    retainedGroupId, retainedArtifactId);
                            retainedVersion = Objects.requireNonNull(managedDependency, String.format(
                                    "'%s' from 'retainVersions' did not have a version specified and was not in the project's dependency management",
                                    gav)).getVersion();

                        }
                    }
                    visitors.add(changeDependencyGroupIdAndArtifactId(visitor, retainedGroupId, retainedArtifactId, retainedVersion));
                }
            }
        }
        return visitors;
    }

    private static <T> boolean noneMatch(Set<T> existingDependencies, Predicate<T> predicate) {
        for (T existingDependency : existingDependencies) {
            if (predicate.test(existingDependency)) {
                return false;
            }
        }
        return true;
    }

    private static TreeVisitor<?, ExecutionContext> changeDependencyGroupIdAndArtifactId(MavenVisitor<?> visitor, String oldGroupId, String oldArtifactId, String newVersion) {
        ChangeDependencyGroupIdAndArtifactId recipe =
                new ChangeDependencyGroupIdAndArtifactId(oldGroupId, oldArtifactId, null, null, newVersion, null, true, true);
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ChangeDependencyGroupIdAndArtifactId.Accumulator accumulator = recipe.getInitialValue(ctx);
        recipe.getScanner(accumulator).visit(visitor.getCursor().firstEnclosingOrThrow(Xml.Document.class), ctx);
        return recipe.getVisitor(accumulator);
    }
}
