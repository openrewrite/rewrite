/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.search.FindRepository;
import org.openrewrite.groovy.format.AutoFormat;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddSettingsPluginRepository extends Recipe {

    @Option(displayName = "Type",
            description = "The type of the artifact repository",
            example = "maven")
    String type;

    @Option(displayName = "URL",
            description = "The url of the artifact repository",
            required = false,
            example = "https://repo.spring.io")
    @Nullable
    String url;

    String displayName = "Add a Gradle settings repository";

    String description = "Add a Gradle settings repository to `settings.gradle(.kts)`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile &&
                        tree == new FindRepository(type, url, FindRepository.Purpose.Plugin).getVisitor().visit(tree, ctx)) {
                    if (tree instanceof G.CompilationUnit) {
                        return visitCompilationUnit((G.CompilationUnit) tree, ctx);
                    }
                    if (tree instanceof K.CompilationUnit) {
                        return visitCompilationUnit((K.CompilationUnit) tree, ctx);
                    }
                }
                return super.visit(tree, ctx);
            }

            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit g, ExecutionContext ctx) {
                J.MethodInvocation pluginManagement = (J.MethodInvocation) generatePluginManagementBlock(G.CompilationUnit.class, cu -> cu.getStatements().get(0), ctx);

                List<Statement> statements;
                if (g.getStatements().isEmpty()) {
                    statements = singletonList(pluginManagement);
                } else {
                    statements = addPluginManagementRepos(g.getStatements(), pluginManagement);
                }

                return (G.CompilationUnit) new AutoFormat().getVisitor().visitNonNull(g.withStatements(statements), ctx);
            }

            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit k, ExecutionContext ctx) {
                J.Block pluginManagement = (J.Block) generatePluginManagementBlock(K.CompilationUnit.class, cu -> cu.getStatements().get(0), ctx);

                List<Statement> statements = k.getStatements();

                if (statements.isEmpty()) {
                    statements = new ArrayList<>(pluginManagement.getStatements());
                } else {
                    Statement blockStatement = statements.get(0);
                    if (blockStatement instanceof J.Block) {
                        J.Block b = (J.Block) blockStatement;
                        if (b.getStatements().isEmpty()) {
                            statements = new ArrayList<>(pluginManagement.getStatements());
                        } else {
                            List<Statement> updated = addPluginManagementRepos(b.getStatements(), pluginManagement);
                            if (updated != b.getStatements()) {
                                statements = singletonList(b.withStatements(updated));
                            }
                        }
                    }
                }

                return (K.CompilationUnit) new AutoFormat().getVisitor().visitNonNull(k.withStatements(statements), ctx);
            }

            private List<Statement> addPluginManagementRepos(List<Statement> statements, J pluginManagement) {
                List<Statement> mapped = ListUtils.map(statements, statement -> {
                    J.MethodInvocation existing = unwrapMethodCall(statement, "pluginManagement");
                    if (existing == null) {
                        return statement;
                    }
                    J.MethodInvocation m = existing.withArguments(ListUtils.mapFirst(existing.getArguments(), arg -> {
                        if (!(arg instanceof J.Lambda) || !(((J.Lambda) arg).getBody() instanceof J.Block)) {
                            return arg;
                        }
                        J.Lambda lambda = (J.Lambda) arg;
                        J.Block block = (J.Block) lambda.getBody();
                        return lambda.withBody(block.withStatements(ListUtils.map(block.getStatements(), stmt ->
                                addRepoToRepositoriesBlock(stmt, pluginManagement))));
                    }));
                    return rewrap(statement, m);
                });
                if (mapped != statements) {
                    return mapped;
                }
                // Check if pluginManagement exists but no change was needed (repo already present)
                for (Statement s : statements) {
                    if (unwrapMethodCall(s, "pluginManagement") != null) {
                        return statements;
                    }
                }
                // No existing pluginManagement found — insert after any leading imports
                Statement pluginManagementStatement = pluginManagement instanceof J.Block ?
                        ((J.Block) pluginManagement).getStatements().get(0) :
                        (J.MethodInvocation) pluginManagement;

                int insertIdx = 0;
                for (int i = 0; i < statements.size(); i++) {
                    if (statements.get(i) instanceof J.Import) {
                        insertIdx = i + 1;
                    } else {
                        break;
                    }
                }

                if (insertIdx == 0) {
                    List<Statement> result = ListUtils.concat(pluginManagementStatement, statements);
                    return ListUtils.map(result, (i, s) -> i == 1 ? s.withPrefix(Space.format("\n\n")) : s);
                } else {
                    List<Statement> result = ListUtils.insert(statements, pluginManagementStatement.withPrefix(Space.format("\n\n")), insertIdx);
                    if (insertIdx < result.size() - 1) {
                        int nextIdx = insertIdx + 1;
                        result = ListUtils.map(result, (i, s) -> i == nextIdx ? s.withPrefix(Space.format("\n\n")) : s);
                    }
                    return result;
                }
            }

            private J.@Nullable MethodInvocation unwrapMethodCall(Statement statement, String methodName) {
                if (statement instanceof J.MethodInvocation &&
                        methodName.equals(((J.MethodInvocation) statement).getSimpleName())) {
                    return (J.MethodInvocation) statement;
                }
                if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation &&
                        methodName.equals(((J.MethodInvocation) ((J.Return) statement).getExpression()).getSimpleName())) {
                    return (J.MethodInvocation) ((J.Return) statement).getExpression();
                }
                return null;
            }

            private Statement rewrap(Statement original, J.MethodInvocation updated) {
                if (original instanceof J.Return) {
                    return ((J.Return) original).withExpression(updated);
                }
                return updated;
            }

            private Statement addRepoToRepositoriesBlock(Statement statement, J pluginManagement) {
                J.MethodInvocation repos = unwrapMethodCall(statement, "repositories");
                if (repos == null) {
                    return statement;
                }
                J.MethodInvocation repoToAdd = extractRepository(pluginManagement);

                if (url == null && repoAlreadyExists(repos, repoToAdd.getSimpleName())) {
                    return statement;
                }

                J.MethodInvocation m2 = repos.withArguments(ListUtils.mapFirst(repos.getArguments(), arg2 -> {
                    if (!(arg2 instanceof J.Lambda) || !(((J.Lambda) arg2).getBody() instanceof J.Block)) {
                        return arg2;
                    }
                    J.Lambda lambda2 = (J.Lambda) arg2;
                    J.Block block2 = (J.Block) lambda2.getBody();
                    Statement lastStatement = block2.getStatements().get(block2.getStatements().size() - 1);
                    return lambda2.withBody(block2.withStatements(ListUtils.concat(
                            ListUtils.mapLast(block2.getStatements(), s ->
                                    s instanceof J.Return ? ((J.Return) s).getExpression().withPrefix(lastStatement.getPrefix()) : s),
                            (Statement) (lastStatement instanceof J.Return ?
                                    ((J.Return) lastStatement).withExpression(repoToAdd.withPrefix(Space.EMPTY)) :
                                    repoToAdd.withPrefix(lastStatement.getPrefix()))
                                    .withComments(emptyList()))));
                }));
                return rewrap(statement, m2);
            }

            // Name-based fallback for when MethodMatcher fails due to incorrect type attribution (e.g. rewrite-kotlin)
            private boolean repoAlreadyExists(J.MethodInvocation repos, String repoName) {
                if (repos.getArguments().isEmpty() || !(repos.getArguments().get(0) instanceof J.Lambda)) {
                    return false;
                }
                J.Lambda lambda = (J.Lambda) repos.getArguments().get(0);
                if (!(lambda.getBody() instanceof J.Block)) {
                    return false;
                }
                for (Statement s : ((J.Block) lambda.getBody()).getStatements()) {
                    if (unwrapMethodCall(s, repoName) != null) {
                        return true;
                    }
                }
                return false;
            }

            private <T extends JavaSourceFile> J generatePluginManagementBlock(Class<T> compilationUnitClass, Function<T, J> methodExtractor, ExecutionContext ctx) {
                String code;
                if (url == null) {
                    code = "pluginManagement {\n" +
                            "    repositories {\n" +
                            "        " + type + "()\n" +
                            "    }\n" +
                            "}";
                } else if (G.class.isAssignableFrom(compilationUnitClass)) {
                    code = "pluginManagement {\n" +
                            "    repositories {\n" +
                            "        " + type + " {\n" +
                            "            url = \"" + url + "\"\n" +
                            "        }\n" +
                            "    }\n" +
                            "}";
                } else {
                    code = "pluginManagement {\n" +
                            "    repositories {\n" +
                            "        " + type + " {\n" +
                            "            url = uri(\"" + url + "\")\n" +
                            "        }\n" +
                            "    }\n" +
                            "}";
                }

                Path path = Paths.get("settings" + (G.class.isAssignableFrom(compilationUnitClass) ? ".gradle" : ".gradle.kts"));
                return methodExtractor.apply(
                        GradleParser.builder().build().parseInputs(singletonList(Parser.Input.fromString(path, code)), null, ctx)
                                .map(compilationUnitClass::cast)
                                .collect(toList()).get(0)
                );
            }

            private J.MethodInvocation extractRepository(J j) {
                J.MethodInvocation pluginManagement;
                if (j instanceof J.MethodInvocation) {
                    pluginManagement = (J.MethodInvocation) j;
                } else {
                    pluginManagement = (J.MethodInvocation) ((J.Block) j).getStatements().get(0);
                }
                J mi = ((J.Block) ((J.Lambda) pluginManagement.getArguments().get(0)).getBody()).getStatements().get(0);
                if (mi instanceof J.Return) {
                    mi = ((J.Return) mi).getExpression().withPrefix(mi.getPrefix());
                }
                mi = ((J.Block)((J.Lambda) requireNonNull((J.MethodInvocation) mi).getArguments().get(0)).getBody()).getStatements().get(0);
                if (mi instanceof J.Return) {
                    mi = ((J.Return) mi).getExpression().withPrefix(mi.getPrefix());
                }
                return (J.MethodInvocation) mi;
            }
        });
    }
}
