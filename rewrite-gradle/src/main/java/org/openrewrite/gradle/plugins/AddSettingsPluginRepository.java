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
import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.search.FindRepository;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
        return Preconditions.check(new IsSettingsGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                if (cu == new FindRepository(type, url, FindRepository.Purpose.Plugin).getVisitor().visit(cu, ctx)) {
                    G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);

                    J.MethodInvocation pluginManagement = generatePluginManagementBlock(ctx);

                    List<Statement> statements = new ArrayList<>(g.getStatements());
                    if (statements.isEmpty()) {
                        statements.add(pluginManagement);
                    } else {
                        Statement statement = statements.get(0);
                        if (statement instanceof J.MethodInvocation
                            && ((J.MethodInvocation) statement).getSimpleName().equals("pluginManagement")) {
                            J.MethodInvocation m = (J.MethodInvocation) statement;
                            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                if (arg instanceof J.Lambda && ((J.Lambda) arg).getBody() instanceof J.Block) {
                                    J.Lambda lambda = (J.Lambda) arg;
                                    J.Block block = (J.Block) lambda.getBody();
                                    return lambda.withBody(block.withStatements(ListUtils.map(block.getStatements(), statement2 -> {
                                        if ((statement2 instanceof J.MethodInvocation && ((J.MethodInvocation) statement2).getSimpleName().equals("repositories"))
                                            || (statement2 instanceof J.Return && ((J.Return) statement2).getExpression() instanceof J.MethodInvocation && ((J.MethodInvocation) ((J.Return) statement2).getExpression()).getSimpleName().equals("repositories"))) {
                                            J.MethodInvocation m2 = (J.MethodInvocation) (statement2 instanceof J.Return ? ((J.Return) statement2).getExpression() : statement2);
                                            return m2.withArguments(ListUtils.mapFirst(m2.getArguments(), arg2 -> {
                                                if (arg2 instanceof J.Lambda && ((J.Lambda) arg2).getBody() instanceof J.Block) {
                                                    J.Lambda lambda2 = (J.Lambda) arg2;
                                                    J.Block block2 = (J.Block) lambda2.getBody();
                                                    return lambda2.withBody(block2.withStatements(ListUtils.concat(block2.getStatements(), extractRepository(pluginManagement))));
                                                }
                                                return arg2;
                                            }));
                                        }
                                        return statement2;
                                    })));
                                }
                                return arg;
                            }));
                            statements.set(0, m);
                        } else {
                            statements.add(0, pluginManagement);
                            statements.set(1, statements.get(1).withPrefix(Space.format("\n\n")));
                        }
                    }

                    return autoFormat(g.withStatements(statements), ctx);
                }

                return cu;
            }

            private J.MethodInvocation generatePluginManagementBlock(ExecutionContext ctx) {
                String code;
                if (url == null) {
                    code = "pluginManagement {" +
                           "    repositories {" +
                           "        " + type + "()" +
                           "    }" +
                           "}";
                } else {
                    code = "pluginManagement {" +
                           "    repositories {" +
                           "        " + type + " {" +
                           "            url = \"" + url + "\"" +
                           "        }" +
                           "    }" +
                           "}";
                }

                return (J.MethodInvocation) GradleParser.builder().build().parseInputs(Collections.singletonList(Parser.Input.fromString(Paths.get("settings.gradle"), code)), null, ctx)
                        .map(G.CompilationUnit.class::cast)
                        .collect(Collectors.toList()).get(0).getStatements().get(0);
            }

            private J.MethodInvocation extractRepository(J.MethodInvocation pluginManagement) {
                J.MethodInvocation repositories = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) pluginManagement
                        .getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
                return (J.MethodInvocation) requireNonNull(((J.Return) ((J.Block) ((J.Lambda) requireNonNull(repositories)
                        .getArguments().get(0)).getBody()).getStatements().get(0)).getExpression());
            }
        });
    }
}
