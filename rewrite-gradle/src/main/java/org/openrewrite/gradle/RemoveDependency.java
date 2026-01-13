/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.semver.DependencyMatcher;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.LINE_BREAK;
import static org.openrewrite.internal.StringUtils.hasLineBreak;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "The dependency configuration",
            description = "The dependency configuration to remove from.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Remove a Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Removes a single dependency from the dependencies section of the `build.gradle`. Comments before or after the removed dependencies will be removed as well.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            final GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher()
                    .configuration(configuration)
                    .groupId(groupId)
                    .artifactId(artifactId);
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());

            GradleProject gradleProject;

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof G.CompilationUnit || sourceFile instanceof K.CompilationUnit;
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile sourceFile = (JavaSourceFile) tree;
                    Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGp.isPresent()) {
                        return sourceFile;
                    }

                    gradleProject = maybeGp.get();

                    sourceFile = (JavaSourceFile) super.visit(sourceFile, ctx);
                    if (sourceFile != tree) {
                        sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(updateGradleModel(gradleProject)));
                    }
                    return sourceFile;
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                J.Block b = super.visitBlock(block, executionContext);

                if (withinDependenciesBlock(getCursor())) {
                    AtomicReference<String> whitespaceAfterPrevDep = new AtomicReference<>();
                    List<Comment> commentsAfterPrevDep = new ArrayList<>();
                    AtomicBoolean cleanupNextStatement = new AtomicBoolean(false);
                    b = b.withStatements(ListUtils.map(b.getStatements(), (i, stmt) -> {
                        if ((stmt instanceof J.MethodInvocation && gradleDependencyMatcher.get(stmt, getCursor()).isPresent()) ||
                                ((stmt instanceof J.Return && ((J.Return) stmt).getExpression() instanceof J.MethodInvocation && gradleDependencyMatcher.get(((J.Return) stmt).getExpression(), getCursor()).isPresent()))) {
                            if (i != 0 && !stmt.getPrefix().getComments().isEmpty() && !hasLineBreak(stmt.getPrefix().getWhitespace())) {
                                whitespaceAfterPrevDep.set(substringOfBeforeFirstLineBreak(stmt.getPrefix().getWhitespace()));
                                commentsAfterPrevDep.addAll(getCommentsUntilLineBreak(stmt.getComments()));
                            }
                            cleanupNextStatement.set(true);
                            return null;
                        }
                        if (cleanupNextStatement.get()) {
                            if (!commentsAfterPrevDep.isEmpty()) {
                                stmt = stmt.withPrefix(stmt.getPrefix().withWhitespace(whitespaceAfterPrevDep.get()))
                                        .withComments(ListUtils.concatAll(new ArrayList<>(commentsAfterPrevDep), stmt.getComments()));
                                whitespaceAfterPrevDep.set("");
                                commentsAfterPrevDep.clear();
                            } else if (!stmt.getPrefix().getComments().isEmpty() && !hasLineBreak(stmt.getPrefix().getWhitespace())) {
                                List<Comment> comments = getCommentsFromFirstLineBreak(stmt.getPrefix().getComments());
                                List<Comment> commentsAfterFirstLineBreak = comments.subList(1, comments.size());
                                String whitespace = comments.get(0).getSuffix() + substringOfAfterFirstLineBreak(stmt.getPrefix().getWhitespace());
                                stmt = stmt.withPrefix(Space.build(whitespace, commentsAfterFirstLineBreak));
                            }
                            cleanupNextStatement.set(false);
                        }
                        return stmt;
                    }));
                    if (!commentsAfterPrevDep.isEmpty()) {
                        List<Comment> commentsExceptLast = commentsAfterPrevDep.subList(0, commentsAfterPrevDep.size() - 1);
                        Comment lastComment = commentsAfterPrevDep.get(commentsAfterPrevDep.size() - 1).withSuffix("\n");
                        b = b.withEnd(Space.build(whitespaceAfterPrevDep.get(), ListUtils.concat(commentsExceptLast, lastComment)));
                    } else if (cleanupNextStatement.get() && !b.getEnd().getComments().isEmpty()) {
                        b = b.withEnd(Space.build("\n", emptyList()));
                    }
                }
                return b;
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    if (!StringUtils.isBlank(configuration) && configuration.equals(gdc.getName())) {
                        newNameToConfiguration.put(gdc.getName(), gdc);
                        continue;
                    }

                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            return null;
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            return null;
                        }
                        return resolved;
                    }));
                    anyChanged |= newGdc != gdc;
                    newNameToConfiguration.put(newGdc.getName(), newGdc);
                }
                if (anyChanged) {
                    gp = gp.withNameToConfiguration(newNameToConfiguration);
                }
                return gp;
            }
        });
    }

    private boolean withinDependenciesBlock(Cursor cursor) {
        Cursor parentCursor = cursor.getParent();
        while (parentCursor != null) {
            if (parentCursor.getValue() instanceof J.MethodInvocation) {
                J.MethodInvocation m = parentCursor.getValue();
                if (m.getSimpleName().equals("dependencies")) {
                    return true;
                }
            }
            parentCursor = parentCursor.getParent();
        }

        return false;
    }

    private static List<Comment> getCommentsUntilLineBreak(List<Comment> comments) {
        for (int i = 0; i < comments.size(); i ++) {
            if (hasLineBreak(comments.get(i).getSuffix())) {
                return comments.subList(0, i + 1);
            }
        }
        return comments;
    }

    private static List<Comment> getCommentsFromFirstLineBreak(List<Comment> comments) {
        for (int i = 0; i < comments.size(); i ++) {
            if (hasLineBreak(comments.get(i).getSuffix())) {
                return comments.subList(i, comments.size());
            }
        }
        return emptyList();
    }

    private String substringOfBeforeFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s);
        return lines.length > 0 ? lines[0] : "";
    }

    private String substringOfAfterFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s, -1);
        return lines.length > 1 ? String.join("\n", Arrays.copyOfRange(lines, 1, lines.length)) : "";
    }
}
