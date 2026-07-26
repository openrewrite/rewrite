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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.Preconditions.or;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveEmptyBuildscriptBlock extends Recipe {

    String displayName = "Remove empty `buildscript` block";

    String description = "Removes a `buildscript` block from `build.gradle(.kts)` or `settings.gradle(.kts)` when it " +
                         "contributes nothing to the build. A block containing only other empty blocks, such as an " +
                         "empty `dependencies` or `repositories` block, is also considered empty. A block containing a " +
                         "comment is left alone, so that no comment is silently deleted.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                J j = super.visit(tree, ctx);
                // A Groovy script holds its statements on the compilation unit, so they are unreachable from
                // visitBlock. A Kotlin script instead wraps them all in a single block, which visitBlock handles.
                if (j instanceof G.CompilationUnit) {
                    G.CompilationUnit c = (G.CompilationUnit) j;
                    return c.withStatements(withoutEmptyBuildscriptBlocks(c.getStatements()));
                }
                return j;
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                return b.withStatements(withoutEmptyBuildscriptBlocks(b.getStatements()));
            }
        });
    }

    /**
     * Drops every empty {@code buildscript} block from a list of statements. Removal happens here, rather than by
     * returning {@code null} from a method invocation visit, so that the statement taking the removed block's place
     * can inherit its prefix. Without that, removing the first statement in a file leaves its leading blank lines
     * behind on whatever follows it.
     */
    private static List<Statement> withoutEmptyBuildscriptBlocks(List<Statement> statements) {
        AtomicReference<@Nullable Space> orphanedPrefix = new AtomicReference<>(null);
        return ListUtils.map(statements, statement -> {
            if (isEmptyBuildscriptBlock(statement)) {
                orphanedPrefix.compareAndSet(null, statement.getPrefix());
                return null;
            }
            Space inherited = orphanedPrefix.getAndSet(null);
            return inherited == null ? statement : statement.withPrefix(inherited);
        });
    }

    private static boolean isEmptyBuildscriptBlock(Statement statement) {
        return statement instanceof J.MethodInvocation &&
               "buildscript".equals(((J.MethodInvocation) statement).getSimpleName()) &&
               isEmptyBlock((J.MethodInvocation) statement);
    }

    /**
     * A configuration block is empty when its closure body contains nothing but other empty configuration blocks.
     * A comment anywhere inside the block makes it non-empty, since removing the block would discard the comment.
     */
    private static boolean isEmptyBlock(J.MethodInvocation method) {
        if (method.getArguments().size() != 1) {
            return false;
        }
        Expression argument = method.getArguments().get(0);
        if (!(argument instanceof J.Lambda) || !(((J.Lambda) argument).getBody() instanceof J.Block)) {
            return false;
        }
        J.Block body = (J.Block) ((J.Lambda) argument).getBody();
        if (!body.getEnd().getComments().isEmpty()) {
            return false;
        }
        for (Statement statement : body.getStatements()) {
            if (!statement.getPrefix().getComments().isEmpty()) {
                return false;
            }
            // Groovy gives the last statement of a closure an implicit `return`
            Statement unwrapped = statement;
            if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof Statement) {
                unwrapped = (Statement) ((J.Return) statement).getExpression();
            }
            if (!(unwrapped instanceof J.MethodInvocation) || !isEmptyBlock((J.MethodInvocation) unwrapped)) {
                return false;
            }
        }
        return true;
    }
}
