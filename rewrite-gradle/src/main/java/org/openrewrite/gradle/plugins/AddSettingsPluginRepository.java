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

    @Override
    public String getDisplayName() {
        return "Add a Gradle settings repository";
    }

    @Override
    public String getDescription() {
        return "Add a Gradle settings repository to `settings.gradle(.kts)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    if (tree == new FindRepository(type, url, FindRepository.Purpose.Plugin).getVisitor().visit(tree, ctx)) {
                        if (tree instanceof G.CompilationUnit) {
                            return visitCompilationUnit((G.CompilationUnit) tree, ctx);
                        }
                        if (tree instanceof K.CompilationUnit) {
                            return visitCompilationUnit((K.CompilationUnit) tree, ctx);
                        }
                    }
                }
                return super.visit(tree, ctx);
            }

            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit g, ExecutionContext ctx) {
                J.MethodInvocation pluginManagement = (J.MethodInvocation) generatePluginManagementBlock(G.CompilationUnit.class, cu -> cu.getStatements().get(0), ctx);

                List<Statement> statements = new ArrayList<>(g.getStatements());
                if (statements.isEmpty()) {
                    statements.add(pluginManagement);
                } else {
                    addPluginManagementRepos(statements, pluginManagement);
                }

                return (G.CompilationUnit) new AutoFormat().getVisitor().visitNonNull(g.withStatements(statements), ctx);
            }

            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit k, ExecutionContext ctx) {
                J.Block pluginManagement = (J.Block) generatePluginManagementBlock(K.CompilationUnit.class, cu -> cu.getStatements().get(0), ctx);

                List<Statement> statements = new ArrayList<>(k.getStatements());

                if (statements.isEmpty()) {
                    statements.addAll(pluginManagement.getStatements());
                } else {
                    Statement blockStatement = statements.get(0);
                    if (blockStatement instanceof J.Block) {
                        J.Block b = (J.Block) blockStatement;
                        List<Statement> blockStatements = new ArrayList<>(b.getStatements());
                        if (blockStatements.isEmpty()) {
                            statements.addAll(pluginManagement.getStatements());
                        } else {
                            statements.set(0, b.withStatements(addPluginManagementRepos(blockStatements, pluginManagement)));
                        }
                    }
                }

                return (K.CompilationUnit) new AutoFormat().getVisitor().visitNonNull(k.withStatements(statements), ctx);
            }

            private List<Statement> addPluginManagementRepos(List<Statement> statements, J pluginManagement) {
                Statement statement = statements.get(0);
                if (statement instanceof J.MethodInvocation &&
                        "pluginManagement".equals(((J.MethodInvocation) statement).getSimpleName())) {
                    J.MethodInvocation m = (J.MethodInvocation) statement;
                    m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                        if (arg instanceof J.Lambda && ((J.Lambda) arg).getBody() instanceof J.Block) {
                            J.Lambda lambda = (J.Lambda) arg;
                            J.Block block = (J.Block) lambda.getBody();
                            return lambda.withBody(block.withStatements(ListUtils.map(block.getStatements(), statement2 -> {
                                if ((statement2 instanceof J.MethodInvocation && "repositories".equals(((J.MethodInvocation) statement2).getSimpleName())) ||
                                        (statement2 instanceof J.Return && ((J.Return) statement2).getExpression() instanceof J.MethodInvocation && "repositories".equals(((J.MethodInvocation) ((J.Return) statement2).getExpression()).getSimpleName()))) {
                                    J.MethodInvocation m2 = (J.MethodInvocation) (statement2 instanceof J.Return ? ((J.Return) statement2).getExpression() : statement2);
                                    m2 = m2.withArguments(ListUtils.mapFirst(m2.getArguments(), arg2 -> {
                                        if (arg2 instanceof J.Lambda && ((J.Lambda) arg2).getBody() instanceof J.Block) {
                                            J.Lambda lambda2 = (J.Lambda) arg2;
                                            J.Block block2 = (J.Block) lambda2.getBody();
                                            Statement lastStatement = block2.getStatements().get(block2.getStatements().size() - 1);
                                            return lambda2.withBody(block2.withStatements(ListUtils.concat(ListUtils.mapLast(block2.getStatements(), s -> s instanceof J.Return ? ((J.Return) s).getExpression().withPrefix(lastStatement.getPrefix()) : s), (Statement) (lastStatement instanceof J.Return ? ((J.Return) lastStatement).withExpression(extractRepository(pluginManagement).withPrefix(Space.EMPTY)) : extractRepository(pluginManagement).withPrefix(lastStatement.getPrefix())).withComments(emptyList()))));
                                        }
                                        return arg2;
                                    }));
                                    if (statement2 instanceof J.Return) {
                                        return ((J.Return) statement2).withExpression(m2);
                                    }
                                    return m2;
                                }
                                return statement2;
                            })));
                        }
                        return arg;
                    }));
                    statements.set(0, m);
                } else {
                    statements.add(0, pluginManagement instanceof J.Block ? (J.Block) pluginManagement : (J.MethodInvocation) pluginManagement);
                    statements.set(1, statements.get(1).withPrefix(Space.format("\n\n")));
                }
                return statements;
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
