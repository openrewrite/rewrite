package org.openrewrite.java.effects;

import org.openrewrite.java.tree.JavaType;

public class VariableSide {
    public final JavaType.Variable variable;
    public final Side side;

    public VariableSide(JavaType.Variable variable, Side side) {
        this.variable = variable;
        this.side = side;
    }


}
