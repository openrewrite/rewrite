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
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.tree.K;

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
                if (!isMainClassName(assignment.getVariable())) {
                    return assignment;
                }
                if (getCursor().getNearestMessage(IN_APPLICATION) != null) {
                    Expression variable = assignment.getVariable();
                    if (variable instanceof J.Identifier) {
                        return assignment.withVariable(((J.Identifier) variable).withSimpleName("mainClass"));
                    } else if (variable instanceof J.FieldAccess) {
                        J.FieldAccess fieldAccess = (J.FieldAccess) variable;
                        return assignment.withVariable(
                                fieldAccess.withName(fieldAccess.getName().withSimpleName("mainClass"))
                        );
                    }
                    return assignment;
                }

                if (getCursor().firstEnclosing(J.Lambda.class) == null) {
                    Expression rhs = assignment.getAssignment();
                    if (rhs instanceof J.Literal && ((J.Literal) rhs).getValueSource() != null) {
                        return applicationBlock(assignment, rhs);
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
                if (!isMainClassName(variable)) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                Expression initializer = variable.getInitializer();
                if (!(initializer instanceof J.Literal) || ((J.Literal) initializer).getValueSource() == null) {
                    return super.visitVariableDeclarations(multiVariable, ctx);
                }
                return applicationBlock(multiVariable, initializer);
            }

            // The main class travels as a parameter rather than as text, so a `#{` inside the literal cannot be
            // mistaken for a template placeholder
            private J applicationBlock(Statement original, Expression mainClass) {
                String snippet = "application {\n    mainClass = #{any()}\n}";
                JavaTemplate template = getCursor().firstEnclosing(JavaSourceFile.class) instanceof K.CompilationUnit ?
                        KotlinTemplate.builder(snippet).build() :
                        GroovyTemplate.builder(snippet).build();
                return template.apply(getCursor(), original.getCoordinates().replace(), mainClass);
            }

            private boolean isMainClassName(Tree variable) {
                if (variable instanceof J.Identifier) {
                    return "mainClassName".equals(((J.Identifier) variable).getSimpleName());
                } else if (variable instanceof J.FieldAccess) {
                    return "mainClassName".equals(((J.FieldAccess) variable).getSimpleName());
                } else if (variable instanceof J.VariableDeclarations.NamedVariable) {
                    return "mainClassName".equals(((J.VariableDeclarations.NamedVariable) variable).getSimpleName());
                }
                return false;
            }
        });
    }
}
