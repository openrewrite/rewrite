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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.*;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateDependencyLockFile extends PlainTextVisitor<ExecutionContext> {
    private static final String[] EMPTY = new String[0];

    GradleProjectDependencyState acc;

    @Value
    public static class GradleProjectDependencyState {
        Map<String, GradleProject> modules = new HashMap<>();
        Map<GroupArtifact, String> versionOverrides = new HashMap<>();

        <T extends Tree> @Nullable T addGradleModule(@Nullable T tree) {
            if (tree != null) {
                tree.getMarkers().findFirst(GradleProject.class).ifPresent(project -> {
                    String path = project.getPath();
                    if (path.startsWith(":")) {
                        path = path.substring(1);
                    }
                    if (!path.isEmpty()) {
                        path += "/";
                    }
                    modules.put(path.replaceAll(":", "/") + "gradle.lockfile", project);
                });
            }
            return tree;
        }

        private GroupArtifactVersion getVersion(ResolvedDependency dependency) {
            GroupArtifactVersion gav = dependency.getGav().asGroupArtifactVersion();
            if (versionOverrides.containsKey(gav.asGroupArtifact())) {
                return gav.withVersion(versionOverrides.get(gav.asGroupArtifact()));
            }
            return gav;
        }
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
        return sourceFile instanceof PlainText;
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
        Map<GroupArtifactVersion, SortedSet<String>> lockedVersions = new HashMap<>();
        SortedSet<String> empty = new TreeSet<>();
        List<String> comments = new ArrayList<>();

        if (tree == null) {
            return null;
        }
        PlainText plainText = (PlainText) tree;
        GradleProject gradleProject = acc.getModules().get(plainText.getSourcePath().toString());
        if (gradleProject == null) {
            return tree;
        }
        String text = plainText.getText();
        if (StringUtils.isBlank(text)) {
            return plainText;
        }

        gradleProject.getConfigurations().forEach(conf -> {
            if (conf.isCanBeResolved()) {
                if (conf.getResolved().isEmpty()) {
                    empty.add(conf.getName());
                } else {
                    for (ResolvedDependency resolved : conf.getDirectResolved()) {
                        lockedVersions.computeIfAbsent(acc.getVersion(resolved), k -> new TreeSet<>()).add(conf.getName());
                    }
                }
            }
        });

        Arrays.stream(text.split("\n")).forEach(lock -> {
            if (isComment(lock)) {
                comments.add(lock);
            }
        });

        SortedSet<String> locks = new TreeSet<>();
        lockedVersions.forEach((gav, configurations) -> {
            String lock = asLock(gav, configurations);
            if (lock != null) {
                locks.add(lock);
            }
        });

        StringBuilder sb = new StringBuilder();
        if (comments != null && !comments.isEmpty()) {
            sb.append(String.join("\n", comments));
        }
        if (comments != null && !comments.isEmpty() && locks != null && !locks.isEmpty()) {
            sb.append("\n");
        }
        if (locks != null && !locks.isEmpty()) {
            sb.append(String.join("\n", locks));
        }
        sb.append("\n").append(asLock(null, empty));
        if (plainText.getText().contentEquals(sb)) {
            return tree;
        }
        return plainText.withText(sb.toString());
    }

    private @Nullable String asLock(@Nullable GroupArtifactVersion gav, @Nullable Set<String> configurations) {
        TreeSet<String> sortedConfigurations = new TreeSet<>(configurations == null ? emptyList() : configurations);
        if (gav == null) {
            return "empty=" + String.join(",", sortedConfigurations);
        }
        if (sortedConfigurations.isEmpty()) {
            return null;
        }
        return String.format("%s=%s", gav, String.join(",", sortedConfigurations));
    }

    private static boolean isComment(String entry) {
        return entry.startsWith("# ");
    }
}
