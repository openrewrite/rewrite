package org.openrewrite.java.effects;

import org.openrewrite.java.tree.Dispatch2;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class WriteSided  implements Dispatch2<Boolean, JavaType.Variable, Side> {

    public static final Writes WRITES = new Writes();

    /**
     * @return Whether given expression, when in given position, may read variable v.
     */
    public boolean writes(J e, JavaType.Variable variable, Side side) {
        return dispatch(e, variable, side);
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess pp, JavaType.Variable v, Side s) {
        return (s == Side.LVALUE && pp.getType() != null && pp.getType().equals(v)) || writes(pp.getTarget(), v, s);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier pp, JavaType.Variable v, Side s) {
        return (s == Side.LVALUE) && pp.getFieldType() != null && pp.getFieldType().equals(v);
    }

    @Override
    public Boolean visitLiteral(J.Literal pp, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation pp, JavaType.Variable variable, Side side) {
        assert side == Side.RVALUE;
        return WRITES.writes(pp, variable);
    }

}
