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
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

import java.util.ArrayList;
import java.util.List;

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
        return Preconditions.check(or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof G.CompilationUnit) {
                    return new GroovyIsoVisitor<ExecutionContext>() {
                        @Override
                        public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                            G.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                            return c.withStatements(withoutEmptyBuildscriptBlocks(c.getStatements()));
                        }

                        @Override
                        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                            J.Block b = super.visitBlock(block, ctx);
                            return b.withStatements(withoutEmptyBuildscriptBlocks(b.getStatements()));
                        }
                    }.visit(tree, ctx);
                }
                if (tree instanceof K.CompilationUnit) {
                    return new KotlinIsoVisitor<ExecutionContext>() {
                        @Override
                        public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                            K.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                            return c.withStatements(withoutEmptyBuildscriptBlocks(c.getStatements()));
                        }

                        @Override
                        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                            J.Block b = super.visitBlock(block, ctx);
                            return b.withStatements(withoutEmptyBuildscriptBlocks(b.getStatements()));
                        }
                    }.visit(tree, ctx);
                }
                return tree;
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
        List<Statement> result = statements;
        for (int i = 0; i < result.size(); i++) {
            Statement statement = result.get(i);
            if (statement instanceof J.MethodInvocation &&
                "buildscript".equals(((J.MethodInvocation) statement).getSimpleName()) &&
                isEmptyBlock((J.MethodInvocation) statement)) {
                List<Statement> remaining = new ArrayList<>(result);
                Statement removed = remaining.remove(i);
                if (i < remaining.size()) {
                    remaining.set(i, remaining.get(i).withPrefix(removed.getPrefix()));
                }
                result = remaining;
                i--;
            }
        }
        return result;
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
