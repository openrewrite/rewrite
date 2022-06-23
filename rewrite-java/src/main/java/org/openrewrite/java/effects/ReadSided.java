package org.openrewrite.java.effects;

import org.openrewrite.java.JavaVisitableAdapter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class ReadSided extends JavaVisitableAdapter<Boolean, VariableSide> {

    public static final Read read = new Read();

    @Override
    public Boolean defaultValue(J tree, VariableSide vs) {
        throw new UnsupportedOperationException();
    }

    // Expression

    /** @return True if this expression, when evaluated, may read variable v. */
    //boolean reads(JavaType.Variable v);


    /**
     * @return True if this expression, when in `side` position, may read variable v.
     */
    public boolean reads(J e, VariableSide vs) {
        if (vs.side == Side.LVALUE) throw new NodeCannotBeAnLValueException();
        return e.acceptJava(this, vs);
    }

    public boolean reads(J e, JavaType.Variable v, Side s) {
        return reads(e, new VariableSide(v, s));
    }

    public boolean reads(J e, JavaType.Variable v) {
        return reads(e, new VariableSide(v, Side.RVALUE));
    }

    // Statement


    // AnnotatedType
    // Annotation
    // ArrayAccess
    @Override
    public Boolean visitArrayAccess(J.ArrayAccess arrayAccess, VariableSide vs) {
        return reads(arrayAccess.getIndexed(), vs) || reads(arrayAccess.getDimension().getIndex(), new VariableSide(vs.variable, Side.RVALUE));
    }

    @Override
    public Boolean visitAssignment(J.Assignment assignment, VariableSide vs) {
        return reads(assignment.getVariable(), vs.variable, Side.LVALUE)
                || read.reads(assignment.getAssignment(), vs.variable);
    }

    @Override
    public Boolean visitBinary(J.Binary binary, VariableSide vs) {
        return read.reads(binary.getLeft(), vs.variable) || read.reads(binary.getRight(), vs.variable);
    }

    // Block


    @Override
    public Boolean visitBlock(J.Block block, VariableSide vs) {
        return block.getStatements().stream().map(s -> read.reads(s, vs.variable)).reduce(false, (a, b) -> a | b);
    }

    // Break


    @Override
    public Boolean visitBreak(J.Break breakStatement, VariableSide variableSide) {
        return false;
    }


    // Case


    @Override
    public Boolean visitCase(J.Case caze, VariableSide vs) {
        return caze.getStatements().stream().map(s -> reads(s, vs.variable)).reduce(false, (a, b) -> a | b);
    }

    // ClassDeclaration


    // Continue


    @Override
    public Boolean visitContinue(J.Continue continueStatement, VariableSide variableSide) {
        return false;
    }


    // DoWhileLoop


    @Override
    public Boolean visitDoWhileLoop(J.DoWhileLoop doWhileLoop, VariableSide vs) {
        return reads(doWhileLoop.getWhileCondition(), vs.variable)
                || reads(doWhileLoop.getBody(), vs.variable);
    }

    // Empty


    // EnumValueSet


    @Override
    public Boolean visitEnumValueSet(J.EnumValueSet enums, VariableSide vs) {
        return enums.getEnums().stream().map(n ->
                n.getInitializer() != null && reads(n.getInitializer(), vs.variable)).reduce(false, (a, b) -> a | b);
    }


    // FieldAccess


    @Override
    public Boolean visitFieldAccess(J.FieldAccess fieldAccess, VariableSide vs) {
        return (vs.side == Side.RVALUE && fieldAccess.getType() != null && fieldAccess.getType().equals(vs.variable))
                || reads(fieldAccess.getTarget(), vs);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier ident, VariableSide vs) {
        return (vs.side == Side.RVALUE) && ident.getFieldType() != null && ident.getFieldType().equals(vs.variable);
    }

    @Override
    public Boolean visitTypeCast(J.TypeCast typeCast, VariableSide vs) {
        if (vs.side == Side.LVALUE) throw new NodeCannotBeAnLValueException();
        return reads(typeCast.getExpression(), vs.variable);
    }
}
