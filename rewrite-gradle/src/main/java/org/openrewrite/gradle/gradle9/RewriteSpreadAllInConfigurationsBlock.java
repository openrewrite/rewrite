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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.marker.StarDot;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class RewriteSpreadAllInConfigurationsBlock extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace spread-`all*` calls in `configurations` blocks with `configurations.all { }`";
    }

    @Override
    public String getDescription() {
        return "Gradle 9 throws `Cannot mutate the dependencies of configuration ':all' after the configuration was resolved.` " +
                "when a `configurations { }` closure uses Groovy's spread-dot form `all*.<method>(args)`. " +
                "Rewrite each such call to the closure form `configurations.all { <method>(args) }`, which preserves " +
                "eager-`all` semantics but is accepted by Gradle 9. Only applied when every statement in the " +
                "`configurations { }` block uses the spread form; mixed blocks are left untouched for manual review.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (!(sourceFile instanceof G.CompilationUnit)) {
                    // Spread-dot is Groovy-only syntax; skip Kotlin DSL build files.
                    return m;
                }
                if (!isConfigurationsClosure(m)) {
                    return m;
                }
                J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                J.Block body = (J.Block) lambda.getBody();
                List<Statement> statements = body.getStatements();
                if (statements.isEmpty() || !statements.stream().allMatch(RewriteSpreadAllInConfigurationsBlock::isSpreadAll)) {
                    return m;
                }
                List<Statement> stripped = ListUtils.map(statements, RewriteSpreadAllInConfigurationsBlock::stripSpread);

                J.MethodInvocation template = parseConfigurationsAllTemplate(ctx);
                J.Lambda templateLambda = (J.Lambda) template.getArguments().get(0);
                J.Block templateBody = (J.Block) templateLambda.getBody();

                return autoFormat(template
                        .withPrefix(m.getPrefix())
                        .withArguments(singletonList(templateLambda.withBody(templateBody.withStatements(stripped)))),
                        ctx, getCursor().getParentOrThrow());
            }
        });
    }

    private static J.MethodInvocation parseConfigurationsAllTemplate(ExecutionContext ctx) {
        G.CompilationUnit parsed = (G.CompilationUnit) GradleParser.builder().build()
                .parse(ctx, "configurations.all {\n}\n")
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to parse `configurations.all { }` template"));
        Statement first = parsed.getStatements().get(0);
        if (!(first instanceof J.MethodInvocation)) {
            throw new IllegalStateException("Expected a method invocation, got " + first.getClass().getName());
        }
        return (J.MethodInvocation) first;
    }

    private static boolean isConfigurationsClosure(J.MethodInvocation m) {
        if (m.getSelect() != null || !"configurations".equals(m.getSimpleName()) || m.getArguments().size() != 1) {
            return false;
        }
        Expression arg = m.getArguments().get(0);
        return arg instanceof J.Lambda && ((J.Lambda) arg).getBody() instanceof J.Block;
    }

    private static boolean isSpreadAll(Statement statement) {
        J.MethodInvocation m = asInvocation(statement);
        if (m == null) {
            return false;
        }
        Expression select = m.getSelect();
        if (!(select instanceof J.Identifier) || !"all".equals(((J.Identifier) select).getSimpleName())) {
            return false;
        }
        return m.getName().getMarkers().findFirst(StarDot.class).isPresent();
    }

    private static J.@Nullable MethodInvocation asInvocation(Statement statement) {
        if (statement instanceof J.MethodInvocation) {
            return (J.MethodInvocation) statement;
        }
        if (statement instanceof J.Return) {
            Expression expression = ((J.Return) statement).getExpression();
            if (expression instanceof J.MethodInvocation) {
                return (J.MethodInvocation) expression;
            }
        }
        return null;
    }

    private static Statement stripSpread(Statement statement) {
        if (statement instanceof J.Return) {
            J.Return ret = (J.Return) statement;
            return ret.withExpression(stripSpreadFromInvocation((J.MethodInvocation) ret.getExpression()));
        }
        return stripSpreadFromInvocation((J.MethodInvocation) statement);
    }

    private static J.MethodInvocation stripSpreadFromInvocation(J.MethodInvocation spread) {
        J.Identifier name = spread.getName();
        return spread
                .withSelect(null)
                .withName(name.withMarkers(name.getMarkers().removeByType(StarDot.class)));
    }
}
