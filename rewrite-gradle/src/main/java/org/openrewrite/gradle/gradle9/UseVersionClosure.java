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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVersionClosure extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `version { }` closure instead of `version = { }` assignment";
    }

    @Override
    public String getDescription() {
        return "Converts `version = { ... }` assignment syntax to `version { ... }` closure call syntax " +
                "in Gradle dependency declarations. The assignment form is not valid Gradle DSL; " +
                "the closure form invokes the version spec method directly.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile && !(tree instanceof G.CompilationUnit)) {
                    return (J) tree;
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                return b.withStatements(ListUtils.map(b.getStatements(), s -> {
                    J.Assignment assignment = asVersionClosureAssignment(s);
                    if (assignment == null) {
                        return s;
                    }
                    J.Lambda lambda = (J.Lambda) assignment.getAssignment();
                    J.MethodInvocation template = parseVersionTemplate(ctx);
                    J.Lambda templateLambda = (J.Lambda) template.getArguments().get(0);
                    J.Block templateBody = (J.Block) templateLambda.getBody();
                    J.Block originalBody = (J.Block) lambda.getBody();
                    // Use the outer statement's prefix to preserve indentation (in Groovy, assignments
                    // inside closures may be wrapped in J.Return, so the indentation is on the wrapper)
                    return template
                            .withPrefix(((J) s).getPrefix())
                            .withArguments(Collections.singletonList(
                                    templateLambda.withBody(templateBody.withStatements(originalBody.getStatements()).withEnd(originalBody.getEnd()))));
                }));
            }
        });
    }

    private static J.MethodInvocation parseVersionTemplate(ExecutionContext ctx) {
        G.CompilationUnit parsed = (G.CompilationUnit) GradleParser.builder().build()
                .parse(ctx, "version {\n}\n")
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to parse `version { }` template"));
        Statement first = parsed.getStatements().get(0);
        if (!(first instanceof J.MethodInvocation)) {
            throw new IllegalStateException("Expected a method invocation, got " + first.getClass().getName());
        }
        return (J.MethodInvocation) first;
    }

    private static J.Assignment asVersionClosureAssignment(Statement s) {
        J.Assignment a = null;
        if (s instanceof J.Assignment) {
            a = (J.Assignment) s;
        } else if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.Assignment) {
            a = (J.Assignment) ((J.Return) s).getExpression();
        }
        if (a == null) {
            return null;
        }
        Expression variable = a.getVariable();
        if (!(variable instanceof J.Identifier)) {
            return null;
        }
        if (!"version".equals(((J.Identifier) variable).getSimpleName())) {
            return null;
        }
        Expression value = a.getAssignment();
        if (!(value instanceof J.Lambda) || !(((J.Lambda) value).getBody() instanceof J.Block)) {
            return null;
        }
        return a;
    }
}
