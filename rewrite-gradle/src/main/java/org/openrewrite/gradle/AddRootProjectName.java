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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;


@Value
@EqualsAndHashCode(callSuper = false)
public class AddRootProjectName extends ScanningRecipe<AddRootProjectName.Accumulator> {

    /**
     * Narrower than Gradle accepts: a name outside this would have to be escaped to be written into a script.
     */
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._\\-]+");

    @Option(displayName = "Root project name",
            description = "The name to assign. When omitted the name Gradle already resolved for the root project is " +
                          "used, which is the one the root directory's name was supplying.",
            required = false,
            example = "my-service")
    @Nullable
    String rootProjectName;

    @Override
    public String getDisplayName() {
        return "Name the root project";
    }

    @Override
    public String getDescription() {
        return "Assign `rootProject.name` in the settings file. Left unset, the root project is named after the " +
               "directory it sits in, so moving or cloning the build into a differently named directory silently " +
               "renames the project, changing published artifact names and the paths of its subprojects. The name " +
               "written is the one Gradle already resolved, taken from the `GradleProject` marker, so this pins " +
               "current behaviour rather than changing it; where that is unavailable the recipe makes no change " +
               "rather than guess a name. See the " +
               "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_general.html#name_your_root_project).";
    }

    public static class Accumulator {
        @Nullable
        String resolvedName;
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
                if (!(tree instanceof SourceFile) || acc.resolvedName != null) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;
                // Only the root build script's marker names the root project; a subproject's names itself.
                if (s.getSourcePath().getParent() != null) {
                    return tree;
                }
                s.getMarkers().findFirst(GradleProject.class)
                        .map(GradleProject::getName)
                        .filter(name -> !StringUtils.isBlank(name))
                        .ifPresent(name -> acc.resolvedName = name);
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new IsSettingsGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                // A nested build has its own settings file and root project, which one resolved name cannot describe.
                if (cu.getSourcePath().getParent() != null) {
                    return cu;
                }

                String name = StringUtils.isBlank(rootProjectName) ? acc.resolvedName : rootProjectName;
                if (name == null || !SAFE_NAME.matcher(name).matches() || alreadyNamed(cu)) {
                    return cu;
                }

                String quote = cu instanceof K.CompilationUnit ? "\"" : "'";
                Statement assignment = parseStatement(cu.getSourcePath(),
                        "rootProject.name = " + quote + name + quote, ctx);
                return assignment == null ? cu : insertAfterPluginManagement(cu, assignment);
            }
        });
    }

    private static boolean alreadyNamed(JavaSourceFile cu) {
        return new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean found) {
                if ("name".equals(fieldAccess.getSimpleName()) &&
                    fieldAccess.getTarget() instanceof J.Identifier &&
                    "rootProject".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName())) {
                    found.set(true);
                }
                return super.visitFieldAccess(fieldAccess, found);
            }
        }.reduce(cu, new AtomicBoolean()).get();
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

    /**
     * After any `pluginManagement` or `buildscript` block, which Gradle requires to come first, and otherwise at the
     * top of the script.
     */
    private static JavaSourceFile insertAfterPluginManagement(JavaSourceFile cu, Statement statement) {
        if (cu instanceof G.CompilationUnit) {
            G.CompilationUnit g = (G.CompilationUnit) cu;
            return g.withStatements(insert(g.getStatements(), statement));
        }
        K.CompilationUnit k = (K.CompilationUnit) cu;
        // A Kotlin script's statements live in the block that is its first statement
        if (k.getStatements().isEmpty() || !(k.getStatements().get(0) instanceof J.Block)) {
            return cu;
        }
        return k.withStatements(ListUtils.mapFirst(k.getStatements(), b -> {
            J.Block block = (J.Block) b;
            return block.withStatements(insert(block.getStatements(), statement));
        }));
    }

    private static List<Statement> insert(List<Statement> statements, Statement statement) {
        int insertAtIdx = 0;
        for (int i = 0; i < statements.size(); i++) {
            Statement existing = statements.get(i);
            if (existing instanceof J.MethodInvocation) {
                String methodName = ((J.MethodInvocation) existing).getSimpleName();
                if ("pluginManagement".equals(methodName) || "buildscript".equals(methodName)) {
                    insertAtIdx = i + 1;
                }
            }
        }
        if (insertAtIdx == 0) {
            Space leadingSpace = Space.firstPrefix(statements);
            return ListUtils.insert(
                    Space.formatFirstPrefix(statements,
                            leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace())),
                    statement,
                    insertAtIdx);
        }
        return ListUtils.insert(statements, statement.withPrefix(Space.format("\n\n")), insertAtIdx);
    }
}
