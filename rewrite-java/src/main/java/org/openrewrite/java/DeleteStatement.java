/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.FindReferencedTypes;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * Deletes standalone statements.
 * <p/>
 * Does not include deletion of:
 * <ul>
 *     <li>control statements present in for loops.</li>
 *     <li>control statements present in while loops.</li>
 *     <li>control statements present in do while loops.</li>
 *     <li>control statements present in if statements.</li>
 *     <li>control statements present in switch statements.</li>
 *     <li>statements that would render the closest parent non {@link J.Block} statement unable to be compiled</li>
 * </ul>
 * <p/>
 *
 * For example, the statement <code>isPotato()</code> would not be removed from any of the following code:
 * <pre>
 * {@code
 *     if (isPotato()) { }
 *     while (isPotato()) { }
 *     do { } while (isPotato());
 *     boolean potato = isPotato();
 *     boolean notPotato = !isPotato();
 * }
 * </pre>
 */
public class DeleteStatement<P> extends JavaIsoVisitor<P> {
    private final Statement statement;

    public DeleteStatement(Statement statement) {
        this.statement = statement;
    }

    @Override
    public @Nullable Statement visitStatement(Statement statement, P p) {
        Statement s = super.visitStatement(statement, p);

        // Only remove a statement directly if it appears as top-level statement in a script file
        // Otherwise, allow this statement to be removed by #visitBlock
        // This prevents the visitor from removing statements that are not top-level statements or direct children of a block.
        if (!(getCursor().getParentOrThrow().getValue() instanceof SourceFile)) {
            return s;
        }

        if (this.statement.isScope(s)) {
            return s instanceof J.Block ? emptyBlock() : null;
        }

        return s;
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);

        if (statement.isScope(i.getThenPart())) {
            i = i.withThenPart(emptyBlock());
        } else if (i.getElsePart() != null && statement.isScope(i.getElsePart().getBody())) {
            i = i.withElsePart(i.getElsePart().withBody(emptyBlock()));
        }

        return i;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        return statement.isScope(forLoop.getBody()) ?
                forLoop.withBody(emptyBlock()) :
                super.visitForLoop(forLoop, p);
    }

    @Override
    public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
        return control;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, P p) {
        return statement.isScope(forEachLoop.getBody()) ?
                forEachLoop.withBody(emptyBlock()) :
                super.visitForEachLoop(forEachLoop, p);
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        return control;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        return statement.isScope(whileLoop.getBody()) ? whileLoop.withBody(emptyBlock()) :
                super.visitWhileLoop(whileLoop, p);
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        return statement.isScope(doWhileLoop.getBody()) ? doWhileLoop.withBody(emptyBlock()) :
                super.visitDoWhileLoop(doWhileLoop, p);
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        return b.withStatements(ListUtils.map(b.getStatements(), s ->
                statement.isScope(s) ? null : s));
    }

    @Override
    public J preVisit(J tree, P p) {
        if (statement.isScope(tree)) {
            for (JavaType.FullyQualified referenced : FindReferencedTypes.find(tree)) {
                maybeRemoveImport(referenced);
            }
        }
        return super.preVisit(tree, p);
    }

    private Statement emptyBlock() {
        return J.Block.createEmptyBlock();
    }
}
