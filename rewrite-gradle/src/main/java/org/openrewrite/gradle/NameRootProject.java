/*
 * Copyright 2026 the original author or authors.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class NameRootProject extends ScanningRecipe<NameRootProject.Accumulator> {

    private static final Set<String> SETTINGS_PREAMBLE = new HashSet<>(Arrays.asList("pluginManagement", "buildscript", "plugins"));

    @Getter
    final String displayName = "Name the root project";

    @Getter
    final String description = "Sets `rootProject.name` in Gradle settings files that don't set it. Without it, the " +
            "root project is named after the directory the build is checked out into, which differs between " +
            "machines and CI agents. The name Gradle currently uses is kept: the root project name recorded on the " +
            "build, or else the settings file's directory, or for a build at the repository root the repository " +
            "name. A settings file is created for a build at the repository root that has none. " +
            "See [name your root project](https://docs.gradle.org/current/userguide/best_practices_general.html#name_your_root_project).";

    public static class Accumulator {
        final Map<String, String> rootProjectNames = new HashMap<>();
        final Set<String> settingsDirectories = new HashSet<>();
        @Nullable Path rootBuildFile;
        @Nullable String repositoryName;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                Path sourcePath = sourceFile.getSourcePath();
                if (acc.repositoryName == null) {
                    acc.repositoryName = sourceFile.getMarkers().findFirst(GitProvenance.class)
                            .map(GitProvenance::getRepositoryName)
                            .orElse(null);
                }
                if (isSettingsFile(sourcePath)) {
                    acc.settingsDirectories.add(directoryOf(sourcePath));
                } else if (sourceFile instanceof JavaSourceFile && isBuildFile(sourcePath)) {
                    if (sourcePath.getParent() == null) {
                        acc.rootBuildFile = sourcePath;
                    }
                    sourceFile.getMarkers().findFirst(GradleProject.class)
                            .filter(gp -> ":".equals(gp.getPath()) && !gp.getName().isEmpty())
                            .ifPresent(gp -> acc.rootProjectNames.put(directoryOf(sourcePath), gp.getName()));
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.rootBuildFile == null || acc.settingsDirectories.contains("")) {
            return emptyList();
        }
        String name = rootProjectName(acc, "");
        if (name == null) {
            return emptyList();
        }
        boolean kotlin = acc.rootBuildFile.toString().endsWith(".kts");
        Path settings = Paths.get(kotlin ? "settings.gradle.kts" : "settings.gradle");
        return GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(settings, nameAssignment(name, kotlin) + "\n")), null, ctx)
                .filter(sourceFile -> !(sourceFile instanceof ParseError))
                .collect(toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new IsSettingsGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile) || namesRootProject((JavaSourceFile) tree)) {
                    return (J) tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                String name = rootProjectName(acc, directoryOf(cu.getSourcePath()));
                if (name == null) {
                    return cu;
                }
                Statement assignment = parseAssignment(name, cu instanceof K.CompilationUnit, ctx);
                if (assignment == null) {
                    return cu;
                }
                if (cu instanceof G.CompilationUnit) {
                    G.CompilationUnit g = (G.CompilationUnit) cu;
                    return g.withStatements(insert(g.getStatements(), assignment));
                }
                if (cu instanceof K.CompilationUnit) {
                    K.CompilationUnit k = (K.CompilationUnit) cu;
                    return k.withStatements(ListUtils.mapFirst(k.getStatements(), s -> s instanceof J.Block ?
                            ((J.Block) s).withStatements(insert(((J.Block) s).getStatements(), assignment)) : s));
                }
                return cu;
            }
        });
    }

    private static List<Statement> insert(List<Statement> statements, Statement assignment) {
        int index = 0;
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (statement instanceof J.MethodInvocation &&
                SETTINGS_PREAMBLE.contains(((J.MethodInvocation) statement).getSimpleName())) {
                index = i + 1;
            }
        }
        if (statements.isEmpty()) {
            return singletonList(assignment);
        }
        if (index == 0) {
            Statement first = statements.get(0);
            return ListUtils.insert(
                    ListUtils.mapFirst(statements, s -> s.withPrefix(Space.format("\n\n"))),
                    assignment.withPrefix(first.getPrefix()),
                    0);
        }
        return ListUtils.insert(statements, assignment.withPrefix(Space.format("\n\n")), index);
    }

    private static boolean namesRootProject(JavaSourceFile cu) {
        AtomicBoolean named = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean n) {
                if ("name".equals(fieldAccess.getSimpleName()) && isRootProject(fieldAccess.getTarget())) {
                    n.set(true);
                }
                return super.visitFieldAccess(fieldAccess, n);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean n) {
                if ("setName".equals(method.getSimpleName()) && isRootProject(method.getSelect())) {
                    n.set(true);
                }
                return super.visitMethodInvocation(method, n);
            }

            private boolean isRootProject(@Nullable Expression expression) {
                return expression instanceof J.Identifier && "rootProject".equals(((J.Identifier) expression).getSimpleName());
            }
        }.visit(cu, named);
        return named.get();
    }

    private static @Nullable String rootProjectName(Accumulator acc, String settingsDirectory) {
        String name = acc.rootProjectNames.get(settingsDirectory);
        if (name == null) {
            name = settingsDirectory.isEmpty() ?
                    acc.repositoryName :
                    settingsDirectory.substring(settingsDirectory.lastIndexOf('/') + 1);
        }
        return name == null || name.isEmpty() || !name.matches("[\\w.\\- ]+") ? null : name;
    }

    private static String nameAssignment(String name, boolean kotlin) {
        return kotlin ? "rootProject.name = \"" + name + "\"" : "rootProject.name = '" + name + "'";
    }

    private static @Nullable Statement parseAssignment(String name, boolean kotlin, ExecutionContext ctx) {
        SourceFile parsed = GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(
                        Paths.get(kotlin ? "settings.gradle.kts" : "settings.gradle"), nameAssignment(name, kotlin))), null, ctx)
                .findFirst()
                .orElse(null);
        if (parsed instanceof G.CompilationUnit) {
            return ((G.CompilationUnit) parsed).getStatements().get(0);
        }
        if (parsed instanceof K.CompilationUnit && ((K.CompilationUnit) parsed).getStatements().get(0) instanceof J.Block) {
            return ((J.Block) ((K.CompilationUnit) parsed).getStatements().get(0)).getStatements().get(0);
        }
        return null;
    }

    private static boolean isSettingsFile(Path path) {
        String fileName = String.valueOf(path.getFileName());
        return "settings.gradle".equals(fileName) || "settings.gradle.kts".equals(fileName);
    }

    private static boolean isBuildFile(Path path) {
        String fileName = String.valueOf(path.getFileName());
        return "build.gradle".equals(fileName) || "build.gradle.kts".equals(fileName);
    }

    private static String directoryOf(Path path) {
        Path parent = path.getParent();
        return parent == null ? "" : parent.toString().replace('\\', '/');
    }
}
