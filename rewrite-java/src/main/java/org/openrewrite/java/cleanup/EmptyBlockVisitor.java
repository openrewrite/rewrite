package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.format.ShiftFormat;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class EmptyBlockVisitor<P> extends JavaIsoVisitor<P> {
    EmptyBlockStyle emptyBlockStyle;
    JavaTemplate throwException = template("throw new #{}(#{});")
            .imports("java.io.UncheckedIOException")
            .build();
    JavaTemplate continueStatement = template("continue;").build();

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);

        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralWhile()) && isEmptyBlock(w.getBody())) {
            J.Block body = (J.Block) w.getBody();
            w = w.withTemplate(continueStatement, body.getCoordinates().lastStatement());
        }

        return w;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop w = super.visitDoWhileLoop(doWhileLoop, p);

        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralWhile()) && isEmptyBlock(w.getBody())) {
            J.Block body = (J.Block) w.getBody();
            w = w.withTemplate(continueStatement, body.getCoordinates().lastStatement());
        }

        return w;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);

        AtomicBoolean filtered = new AtomicBoolean(false);
        List<Statement> statements = ListUtils.map(b.getStatements(), s -> {
            if (!(s instanceof J.Block)) {
                return s;
            }

            J.Block nestedBlock = (J.Block) s;
            if (isEmptyBlock(nestedBlock) && ((Boolean.TRUE.equals(emptyBlockStyle.getStaticInit()) && nestedBlock.isStatic()) ||
                    (Boolean.TRUE.equals(emptyBlockStyle.getInstanceInit()) && !nestedBlock.isStatic()))) {
                filtered.set(true);
                return null;
            }

            return nestedBlock;
        });

        return filtered.get() ? b.withStatements(statements) : b;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch catzh, P p) {
        J.Try.Catch c = super.visitCatch(catzh, p);

        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralCatch()) && isEmptyBlock(c.getBody())) {
            String throwName;
            if (TypeUtils.isOfClassType(c.getParameter().getTree().getTypeAsClass(), "java.io.IOException")) {
                throwName = "UncheckedIOException";
                maybeAddImport("java.io.UncheckedIOException");
            } else {
                throwName = "RuntimeException";
            }

            c = c.withTemplate(throwException, c.getBody().getCoordinates().lastStatement(), throwName,
                    c.getParameter().getTree().getVariables().get(0).getName());
        }

        return c;
    }

    @Override
    public J.Try visitTry(J.Try tryable, P p) {
        J.Try t = super.visitTry(tryable, p);

        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralTry()) && isEmptyBlock(t.getBody())) {
            doAfterVisit(new DeleteStatement<>(tryable));
        } else if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralFinally()) && t.getFinally() != null && isEmptyBlock(t.getFinally())) {
            t = t.withFinally(null);
        }

        return t;
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);

        if (Boolean.FALSE.equals(emptyBlockStyle.getLiteralIf()) || !isEmptyBlock(i.getThenPart())) {
            return i;
        }

        if (i.getElsePart() == null) {
            // extract side effects from condition (if there are any).
            J.Block enclosingBlock = getCursor().firstEnclosing(J.Block.class);
            if (enclosingBlock != null) {
                doAfterVisit(new ExtractSideEffectsOfIfCondition<>(
                        enclosingBlock, i));
            }
            return i;
        }

        // invert top-level if
        J.ControlParentheses<Expression> cond = i.getIfCondition();
        if (cond.getTree() instanceof J.Binary) {
            J.Binary binary = (J.Binary) cond.getTree();

            // only boolean operators are valid for if conditions
            switch (binary.getOperator()) {
                case Equal:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.NotEqual));
                    break;
                case NotEqual:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.Equal));
                    break;
                case LessThan:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.GreaterThanOrEqual));
                    break;
                case LessThanOrEqual:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.GreaterThan));
                    break;
                case GreaterThan:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.LessThanOrEqual));
                    break;
                case GreaterThanOrEqual:
                    cond = cond.withTree(binary.withOperator(J.Binary.Type.LessThan));
                    break;
                default:
                    break;
            }

            i = i.withIfCondition(cond);
        }

        if (i.getElsePart() == null) {
            return i.withThenPart(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                    .withElsePart(null);
        }

        // NOTE: then part MUST be a J.Block, because otherwise impossible to have an empty if condition followed by else-if/else chain
        J.Block thenPart = (J.Block) i.getThenPart();

        Statement elseStatement = i.getElsePart().getBody();
        List<Statement> elseStatementBody;
        if (elseStatement instanceof J.Block) {
            // any else statements should already be at the correct indentation level
            elseStatementBody = ((J.Block) elseStatement).getStatements();
        } else if (elseStatement instanceof J.If) {
            // J.If will typically just have a format of one space (the space between "else" and "if" in "else if")
            // we want this to be on its own line now inside its containing if block
            elseStatementBody = singletonList(ShiftFormat.indent(
                    elseStatement.withPrefix(Space.format("\n" + i.getPrefix().getIndent())),
                    getCursor(), 1));
        } else {
            elseStatementBody = singletonList(ShiftFormat.indent(elseStatement, getCursor(), 1));
        }

        return i.withThenPart(thenPart.withStatements(elseStatementBody))
                .withElsePart(null);
    }

    @Override
    public J.Synchronized visitSynchronized(J.Synchronized synch, P p) {
        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralSynchronized()) && isEmptyBlock(synch.getBody())) {
            doAfterVisit(new DeleteStatement<>(synch));
        }

        return super.visitSynchronized(synch, p);
    }

    @Override
    public J.Switch visitSwitch(J.Switch switzh, P p) {
        if (Boolean.TRUE.equals(emptyBlockStyle.getLiteralSwitch()) && isEmptyBlock(switzh.getCases())) {
            doAfterVisit(new DeleteStatement<>(switzh));
        }

        return super.visitSwitch(switzh, p);
    }

    private boolean isEmptyBlock(Statement blockNode) {
        return EmptyBlockStyle.BlockPolicy.Statement.equals(emptyBlockStyle.getBlockPolicy()) &&
                blockNode instanceof J.Block &&
                ((J.Block) blockNode).getStatements().isEmpty();
    }

    private static class ExtractSideEffectsOfIfCondition<P> extends JavaVisitor<P> {
        private final J.Block enclosingBlock;
        private final J.If toExtract;

        public ExtractSideEffectsOfIfCondition(J.Block enclosingBlock, J.If toExtract) {
            this.enclosingBlock = enclosingBlock;
            this.toExtract = toExtract;
        }

        @Override
        public J visitBlock(J.Block block, P p) {
            J.Block b = (J.Block) super.visitBlock(block, p);
            if (enclosingBlock.isScope(block)) {
                List<Statement> statements = new ArrayList<>(b.getStatements().size());
                for (Statement statement : b.getStatements()) {
                    if (statement == toExtract) {
                        for (J sideEffect : toExtract.getIfCondition().getTree().getSideEffects()) {
                            sideEffect = autoFormat(sideEffect, p, getCursor());
                            statements.add((Statement) sideEffect);
                        }
                    } else {
                        statements.add(statement);
                    }
                }
                b = b.withStatements(statements);
            }
            return b;
        }
    }
}
