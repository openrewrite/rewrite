/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.effects;

import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaDispatcher2;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

/**
 * Provides the reads() methods for nodes that can be on either side of an assignment.
 */
@Incubating(since = "7.25.0")
class ReadSided implements JavaDispatcher2<Boolean, JavaType.Variable, Side> {

    private static final Reads READS = new Reads();

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
        for (Statement s : block.getStatements()) {
            if (READS.reads(s, variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitBreak(J.Break breakStatement, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitCase(J.Case caze, JavaType.Variable variable, Side side) {
        for (Statement s : caze.getStatements()) {
            if (reads(s, variable)) {
                return true;
            }
        }
        return false;
    }

    // ClassDeclaration

    @Override
    public Boolean visitContinue(J.Continue continueStatement, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitDoWhileLoop(J.DoWhileLoop doWhileLoop, JavaType.Variable variable, Side side) {
        return reads(doWhileLoop.getWhileCondition(), variable) || reads(doWhileLoop.getBody(), variable);
    }

    // Empty

    @Override
    public Boolean visitEnumValueSet(J.EnumValueSet enums, JavaType.Variable variable, Side side) {
        for (J.EnumValue n : enums.getEnums()) {
            if (n.getInitializer() != null && reads(n.getInitializer(), variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess fieldAccess, JavaType.Variable variable, Side side) {
        return (side == Side.RVALUE && fieldAccess.getType() != null && fieldAccess.getType().equals(variable)) ||
                reads(fieldAccess.getTarget(), variable, side);
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
