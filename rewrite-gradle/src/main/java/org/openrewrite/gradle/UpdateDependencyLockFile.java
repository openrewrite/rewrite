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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.*;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateDependencyLockFile extends ScanningRecipe<UpdateDependencyLockFile.Accumulator> {
    private static final String[] EMPTY = new String[0];

    @Override
    public String getDisplayName() {
        return "Update gradle dependency lock file";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Update the version of a dependency in a gradle lockfile.";
    }

    public static class Accumulator {
        Map<String, GradleProject> modules = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        //noinspection BooleanMethodIsAlwaysInverted
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                        (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts"));
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    tree.getMarkers().findFirst(GradleProject.class).ifPresent(project -> {
                        String path = project.getPath();
                        if (path.startsWith(":")) {
                            path = path.substring(1);
                        }
                        if (!path.isEmpty()) {
                            path += "/";
                        }
                        acc.modules.put(path.replaceAll(":", "/") + "gradle.lockfile", project);
                    });
                }
                return super.visit(tree, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.modules.isEmpty()) {
            return TreeVisitor.noop();
        }
        return Preconditions.check(new FindSourceFiles("**/gradle.lockfile"), new PlainTextVisitor<ExecutionContext>() {
            private final Map<GroupArtifactVersion, SortedSet<String>> lockedVersions = new HashMap<>();
            private final Map<GroupArtifact, List<GroupArtifactVersion>> lockedArtifacts = new HashMap<>();
            private final SortedSet<String> empty = new TreeSet<>();
            private final List<String> comments = new ArrayList<>();

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                return sourceFile instanceof PlainText;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if (tree == null) {
                    return null;
                }
                PlainText plainText = (PlainText) tree;
                GradleProject gradleProject = acc.modules.get(plainText.getSourcePath().toString());
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
                            conf.getResolved().forEach(resolved -> {
                                GroupArtifactVersion gav = resolved.getGav().asGroupArtifactVersion();
                                lockedVersions.computeIfAbsent(gav, k -> new TreeSet<>()).add(conf.getName());
                                lockedArtifacts.computeIfAbsent(gav.asGroupArtifact(), k -> new ArrayList<>()).add(gav);
                            });
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
                sb.append("\n")
                        .append(asLock(null, empty));

                PlainText newLockFileContent = plainText.withText(sb.toString());
                if (newLockFileContent == plainText) {
                    return tree;
                }
                return newLockFileContent;
            }
        });
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