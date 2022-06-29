package org.openrewrite.java.effects;

import org.openrewrite.java.tree.Dispatch1;
import org.openrewrite.java.tree.Dispatch2;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 *  Provides the reads() methods for nodes that can be on either side of an assignment.
 */
public class ReadSided implements Dispatch2<Boolean, JavaType.Variable, Side> {

    public static final Reads READS = new Reads();

    /**
     * @return Whether given expression, when in given position, may read variable v.
     */
    public boolean reads(J e, JavaType.Variable variable, Side side) {
        return dispatch(e, variable, side);
    }

    /**
     * @return Whether given expression, when in `RVALUE` position, may read variable v.
     */
    public boolean reads(J e, JavaType.Variable v) {
        return reads(e, v, Side.RVALUE);
    }

    @Override
    public Boolean defaultDispatch(J ignoredC, JavaType.Variable variable, Side side) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitArrayAccess(J.ArrayAccess arrayAccess, JavaType.Variable variable, Side side) {
        return reads(arrayAccess.getIndexed(), variable, side) || reads(arrayAccess.getDimension().getIndex(), variable, Side.RVALUE);
    }

    @Override
    public Boolean visitAssignment(J.Assignment assignment, JavaType.Variable variable, Side side) {
        return reads(assignment.getVariable(), variable, Side.LVALUE) || READS.reads(assignment.getAssignment(), variable);
    }

    @Override
    public Boolean visitBinary(J.Binary binary, JavaType.Variable variable, Side side) {
        return READS.reads(binary.getLeft(), variable) || READS.reads(binary.getRight(), variable);
    }

    @Override
    public Boolean visitBlock(J.Block block, JavaType.Variable variable, Side side) {
        return block.getStatements().stream().map(s -> READS.reads(s, variable)).reduce(false, (a, b) -> a | b);
    }

    @Override
    public Boolean visitBreak(J.Break breakStatement, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitCase(J.Case caze, JavaType.Variable variable, Side side) {
        return caze.getStatements().stream().map(s -> reads(s, variable)).reduce(false, (a, b) -> a | b);
    }

    // ClassDeclaration

    @Override
    public Boolean visitContinue(J.Continue continueStatement, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitDoWhileLoop(J.DoWhileLoop doWhileLoop, JavaType.Variable variable, Side side) {
        return reads(doWhileLoop.getWhileCondition(), variable)
                || reads(doWhileLoop.getBody(), variable);
    }

    // Empty

    @Override
    public Boolean visitEnumValueSet(J.EnumValueSet enums, JavaType.Variable variable, Side side) {
        return enums.getEnums().stream().map(n ->
                n.getInitializer() != null && reads(n.getInitializer(), variable)).reduce(false, (a, b) -> a | b);
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess fieldAccess, JavaType.Variable variable, Side side) {
        return (side == Side.RVALUE && fieldAccess.getType() != null && fieldAccess.getType().equals(variable))
                || reads(fieldAccess.getTarget(), variable, side);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier ident, JavaType.Variable variable, Side side) {
        return (side == Side.RVALUE) && ident.getFieldType() != null && ident.getFieldType().equals(variable);
    }

    @Override
    public Boolean visitLiteral(J.Literal pp, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation pp, JavaType.Variable variable, Side side) {
        assert side == Side.RVALUE;
        return READS.reads(pp, variable);
    }

    @Override
    public Boolean visitTypeCast(J.TypeCast typeCast, JavaType.Variable variable, Side side) {
        assert side == Side.RVALUE;
        return reads(typeCast.getExpression(), variable);
    }
}
