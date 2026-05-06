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
package org.openrewrite.gradle.gradle9;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMainClassPropertyForApplication extends Recipe {

    private static final String IN_APPLICATION = "IN_APPLICATION";

    String displayName = "Use `application { mainClass }` instead of `mainClassName`";

    String description = "The `mainClassName` property on the `application` extension was deprecated in Gradle 6.4 and removed in Gradle 9.0. " +
            "Use `application { mainClass = ... }` instead. Top-level `mainClassName` assignments are wrapped in an `application` block. " +
            "See the [Gradle upgrade guide](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html) for more information.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ("application".equals(method.getSimpleName())) {
                    getCursor().putMessage(IN_APPLICATION, true);
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                if (getCursor().getNearestMessage(IN_APPLICATION) != null) {
                    Expression variable = assignment.getVariable();
                    if (variable instanceof J.Identifier) {
                        J.Identifier id = (J.Identifier) variable;
                        if ("mainClassName".equals(id.getSimpleName())) {
                            return assignment.withVariable(id.withSimpleName("mainClass"));
                        }
                    } else if (variable instanceof J.FieldAccess) {
                        J.FieldAccess fieldAccess = (J.FieldAccess) variable;
                        if ("mainClassName".equals(fieldAccess.getSimpleName())) {
                            return assignment.withVariable(
                                    fieldAccess.withName(fieldAccess.getName().withSimpleName("mainClass"))
                            );
                        }
                    }
                    return assignment;
                }

                if (getCursor().firstEnclosing(J.Lambda.class) == null && isMainClassName(assignment.getVariable())) {
                    Expression rhs = assignment.getAssignment();
                    if (rhs instanceof J.Literal && ((J.Literal) rhs).getValueSource() != null) {
                        String valueSource = ((J.Literal) rhs).getValueSource();
                        return parseApplicationBlock(ctx, valueSource, assignment.getPrefix());
                    }
                }
                return assignment;
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                if (getCursor().getNearestMessage(IN_APPLICATION) != null ||
                        getCursor().firstEnclosing(J.Lambda.class) != null) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                List<J.VariableDeclarations.NamedVariable> variables = multiVariable.getVariables();
                if (variables.size() != 1) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                J.VariableDeclarations.NamedVariable variable = variables.get(0);
                if (!"mainClassName".equals(variable.getSimpleName())) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                Expression initializer = variable.getInitializer();
                if (!(initializer instanceof J.Literal) || ((J.Literal) initializer).getValueSource() == null) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                String valueSource = ((J.Literal) initializer).getValueSource();
                return parseApplicationBlock(ctx, valueSource, multiVariable.getPrefix());
            }

            private J parseApplicationBlock(ExecutionContext ctx, String valueSource, Space prefix) {
                String snippet = "application {\n    mainClass = " + valueSource + "\n}";
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                boolean isKotlinDsl = sourceFile != null &&
                        sourceFile.getSourcePath().toString().endsWith(".gradle.kts");
                if (isKotlinDsl) {
                    Statement statement = GradleParser.builder().build()
                            .parseInputs(Collections.singletonList(
                                    Parser.Input.fromString(Paths.get("build.gradle.kts"), snippet)), null, ctx)
                            .map(K.CompilationUnit.class::cast)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Could not parse application block"))
                            .getStatements()
                            .get(0);
                    return ((J) statement).withPrefix(prefix);
                }
                return ((J) GradleParser.builder().build()
                        .parse(ctx, snippet)
                        .map(G.CompilationUnit.class::cast)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not parse application block"))
                        .getStatements()
                        .get(0))
                        .withPrefix(prefix);
            }

            private boolean isMainClassName(Expression variable) {
                if (variable instanceof J.Identifier) {
                    return "mainClassName".equals(((J.Identifier) variable).getSimpleName());
                } else if (variable instanceof J.FieldAccess) {
                    return "mainClassName".equals(((J.FieldAccess) variable).getSimpleName());
                }
                return false;
            }
        });
    }
}
