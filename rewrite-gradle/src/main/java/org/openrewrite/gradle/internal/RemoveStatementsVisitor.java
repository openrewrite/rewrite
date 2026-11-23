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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Removes the statements of a Gradle build script that a subclass marks with {@link #remove(Statement)} while
 * visiting them, once the block or script holding them has been visited. The comments about a removed statement
 * go with it, while those about what surrounds it stay.
 */
public abstract class RemoveStatementsVisitor<P> extends JavaIsoVisitor<P> {
    private final Set<UUID> removed = new HashSet<>();

    /**
     * Mark a statement for removal from the block or script holding it.
     */
    protected void remove(Statement statement) {
        removed.add(statement.getId());
    }

    /**
     * Whether a statement is marked for removal, the last statement of a closure being its implicit return.
     */
    protected boolean isRemoved(Statement statement) {
        Tree tree = statement instanceof J.Return && ((J.Return) statement).getExpression() != null ?
                ((J.Return) statement).getExpression() : statement;
        return removed.contains(tree.getId());
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (tree instanceof J.Block) {
            J.Block block = (J.Block) tree;
            List<Statement> statements = block.getStatements();
            // A Kotlin script wraps its statements in a block, and what follows the last one is the end of the file
            boolean script = getCursor().getParentTreeCursor().getValue() instanceof JavaSourceFile;
            return block.withStatements(withoutRemoved(statements, script))
                    .withEnd(script ? block.getEnd() : afterRemoved(statements, statements.size(), block.getEnd(), false));
        }
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            // The statements as they were, since those of a Kotlin script have lost the removed ones by now
            List<Statement> statements = SpringBomProperty.topLevelStatements(getCursor().getValue());
            if (cu instanceof G.CompilationUnit) {
                cu = ((G.CompilationUnit) cu).withStatements(withoutRemoved(((G.CompilationUnit) cu).getStatements(), true));
            }
            if (!statements.isEmpty() && isRemoved(statements.get(0))) {
                cu = SpringBomProperty.withTopLevelStatements(cu, ListUtils.mapFirst(SpringBomProperty.topLevelStatements(cu),
                        first -> first.withPrefix(first.getPrefix().withWhitespace(""))));
            }
            return cu.withEof(afterRemoved(statements, statements.size(), cu.getEof(), true));
        }
        return tree;
    }

    private List<Statement> withoutRemoved(List<Statement> statements, boolean script) {
        return ListUtils.map(statements, (i, statement) -> isRemoved(statement) ? null :
                statement.withPrefix(afterRemoved(statements, i, statement.getPrefix(), script)));
    }

    /**
     * The space of what follows the statement before {@code index}. After a run of removed statements it
     * loses the comment on the line of the last one, and takes what the first one's prefix holds that is
     * not about the statement: the comment on the line before it, and the comments a blank line sets apart.
     */
    private Space afterRemoved(List<Statement> statements, int index, Space space, boolean script) {
        int first = index;
        while (first > 0 && isRemoved(statements.get(first - 1))) {
            first--;
        }
        if (first == index) {
            return space;
        }
        Space after = withoutTrailingComment(space);
        Space before = statements.get(first).getPrefix();
        List<Comment> comments = before.getComments();
        // Nothing precedes the first statement of a script, and what starts the file is its header
        boolean header = script && first == 0;
        int kept = header ? 0 : comments.size() - withoutTrailingComment(before).getComments().size();
        for (int i = comments.size(); i > kept; i--) {
            if (blankLines(comments.get(i - 1).getSuffix()) > 0) {
                kept = i;
            }
        }
        if (header && kept == 0) {
            kept = comments.size();
        }
        if (kept == 0) {
            return after;
        }
        String suffix = comments.get(kept - 1).getSuffix();
        String indent = after.getWhitespace().substring(after.getWhitespace().lastIndexOf('\n') + 1);
        String separator = blankLines(suffix) > blankLines(after.getWhitespace()) ?
                suffix.substring(0, suffix.lastIndexOf('\n') + 1) + indent : after.getWhitespace();
        return Space.build(before.getWhitespace(), ListUtils.concatAll(
                ListUtils.mapLast(comments.subList(0, kept), comment -> comment.withSuffix(separator)), after.getComments()));
    }

    private static int blankLines(String whitespace) {
        return Math.max(0, StringUtils.countOccurrences(whitespace, "\n") - 1);
    }

    // A comment on the line of a removed statement is held by whatever follows the statement
    private static Space withoutTrailingComment(Space space) {
        while (!space.getComments().isEmpty() && !space.getWhitespace().contains("\n")) {
            List<Comment> comments = space.getComments();
            space = space.withWhitespace(comments.get(0).getSuffix()).withComments(comments.subList(1, comments.size()));
        }
        return space;
    }
}
