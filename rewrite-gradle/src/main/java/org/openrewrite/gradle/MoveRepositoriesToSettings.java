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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Parser;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveRepositoriesToSettings extends ScanningRecipe<MoveRepositoriesToSettings.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Declare dependency repositories in the settings file";
    }

    @Override
    public String getDescription() {
        return "Move a build script's top-level `repositories` block into `dependencyResolutionManagement` in the " +
               "settings file, so the build has one place that says where dependencies come from and a project " +
               "cannot quietly resolve from somewhere the rest of the build does not. Deliberately narrow, because " +
               "the risk is all in the cases it declines: a build with more than one project is left alone, since " +
               "hoisting one project's repositories applies them to every project and can change which repository a " +
               "coordinate resolves from; `buildscript` repositories resolve the build's own classpath, which " +
               "`dependencyResolutionManagement` does not serve; `publishing` repositories are where artifacts go " +
               "rather than where dependencies come from; and a block inside `allprojects` or `subprojects` is " +
               "multi-project configuration whatever the build looks like today. A settings file that already " +
               "declares `dependencyResolutionManagement` is left for a person to merge. See the " +
               "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings).";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    public static class Accumulator {
        boolean multiProject;
        boolean settingsManagesResolution;

        @Nullable
        String repositoriesSource;

        boolean eligible() {
            return !multiProject && !settingsManagesResolution && repositoriesSource != null;
        }
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
                if (!(tree instanceof SourceFile) || ((SourceFile) tree).getSourcePath().getParent() != null) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;

                if (isSettings(s) && s instanceof JavaSourceFile) {
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                            if ("include".equals(method.getSimpleName())) {
                                acc.multiProject = true;
                            } else if ("dependencyResolutionManagement".equals(method.getSimpleName())) {
                                acc.settingsManagesResolution = true;
                            }
                            return super.visitMethodInvocation(method, p);
                        }
                    }.visit(s, 0);
                    return tree;
                }

                if (isBuildScript(s) && s instanceof JavaSourceFile) {
                    for (Statement statement : topLevelStatements((JavaSourceFile) s)) {
                        if (isRepositories(statement)) {
                            // Printed here, where the cursor still gives the block its indentation context
                            acc.repositoriesSource = statement.printTrimmed(new Cursor(null, s));
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!acc.eligible() || !(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                if (cu.getSourcePath().getParent() != null) {
                    return cu;
                }

                if (isSettings(cu)) {
                    Statement managed = parseStatement(cu.getSourcePath(),
                            "dependencyResolutionManagement {\n" + indent(acc.repositoriesSource) + "\n}", ctx);
                    return managed == null ? cu : append(cu, managed);
                }
                if (isBuildScript(cu)) {
                    return withoutTopLevelRepositories(cu);
                }
                return cu;
            }
        };
    }

    private static String indent(@Nullable String source) {
        if (source == null) {
            return "";
        }
        StringBuilder indented = new StringBuilder();
        for (String line : source.split("\n", -1)) {
            if (indented.length() > 0) {
                indented.append('\n');
            }
            indented.append(line.isEmpty() ? line : "    " + line);
        }
        return indented.toString();
    }

    private static boolean isSettings(SourceFile s) {
        String path = s.getSourcePath().toString();
        return path.endsWith("settings.gradle") || path.endsWith("settings.gradle.kts");
    }

    private static boolean isBuildScript(SourceFile s) {
        String path = s.getSourcePath().toString();
        return path.endsWith("build.gradle") || path.endsWith("build.gradle.kts");
    }

    /**
     * Only a block at the top level of the script; one nested in `buildscript`, `publishing`, `allprojects` or
     * `subprojects` is deliberately not reached.
     */
    private static boolean isRepositories(Statement statement) {
        return statement instanceof J.MethodInvocation &&
               "repositories".equals(((J.MethodInvocation) statement).getSimpleName());
    }

    private static JavaSourceFile withoutTopLevelRepositories(JavaSourceFile cu) {
        return mapStatements(cu, statements -> ListUtils.filter(statements, s -> !isRepositories(s)));
    }

    private static @Nullable Statement parseStatement(Path path, String source, ExecutionContext ctx) {
        return GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(path, source)), null, ctx)
                .findFirst()
                .map(parsed -> {
                    if (parsed instanceof ParseError) {
                        throw ((ParseError) parsed).toException();
                    }
                    List<Statement> statements = parsed instanceof G.CompilationUnit ?
                            ((G.CompilationUnit) parsed).getStatements() :
                            ((K.CompilationUnit) parsed).getStatements();
                    Statement first = statements.get(0);
                    // A Kotlin script wraps its statements in a block; a Groovy one does not
                    return first instanceof J.Block ? ((J.Block) first).getStatements().get(0) : first;
                })
                .orElse(null);
    }

    private static JavaSourceFile append(JavaSourceFile cu, Statement statement) {
        return mapStatements(cu, statements ->
                ListUtils.<Statement>concat(statements, statement.withPrefix(Space.format("\n\n"))));
    }

    private static List<Statement> topLevelStatements(JavaSourceFile cu) {
        if (cu instanceof G.CompilationUnit) {
            return ((G.CompilationUnit) cu).getStatements();
        }
        List<Statement> statements = ((K.CompilationUnit) cu).getStatements();
        // A Kotlin script's statements live in the block that is its first statement
        return !statements.isEmpty() && statements.get(0) instanceof J.Block ?
                ((J.Block) statements.get(0)).getStatements() :
                statements;
    }

    private static JavaSourceFile mapStatements(JavaSourceFile cu, UnaryOperator<List<Statement>> mapper) {
        if (cu instanceof G.CompilationUnit) {
            G.CompilationUnit g = (G.CompilationUnit) cu;
            return g.withStatements(mapper.apply(g.getStatements()));
        }
        K.CompilationUnit k = (K.CompilationUnit) cu;
        if (k.getStatements().isEmpty() || !(k.getStatements().get(0) instanceof J.Block)) {
            return cu;
        }
        return k.withStatements(ListUtils.mapFirst(k.getStatements(), b -> {
            J.Block block = (J.Block) b;
            return block.withStatements(mapper.apply(block.getStatements()));
        }));
    }
}
