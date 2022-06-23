//package org.openrewrite.java;
//
//import org.openrewrite.java.tree.Expression;
//import org.openrewrite.java.tree.J;
//import org.openrewrite.java.tree.JavaType;
//
//import java.util.Arrays;
//
//public class Writes {
//
//    // Expression
//
//
//    /** @return True if this expression, when evaluated, may read variable v. */
//    boolean reads(JavaType.Variable v);
//    /** @return True if this expression, when evaluated, may write variable v. */
//    boolean writes(JavaType.Variable v);
//
//    /** @return True if this expression, when in `side` position, may read variable v. */
//    default boolean reads(JavaType.Variable v, Side s) {
//        if(s == Side.LVALUE) throw new NodeCannotBeAnLValueException();
//        return reads(v);
//    }
//    /** @return True if this expression, when in `side` position, may write variable v. */
//    default boolean writes(JavaType.Variable v, Side s) {
//        if(s == Side.LVALUE) throw new NodeCannotBeAnLValueException();
//        return writes(v);
//    }
//
//    // Statement
//
//    /** @return True if this statement, when executed, may read variable v. */
//    boolean reads(JavaType.Variable v);
//    /** @return True if this statement, when executed, may write variable v. */
//    boolean writes(JavaType.Variable v);
//
//    // AnnotatedType
//
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Annotation
//
//
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperatzionException();
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // ArrayAccess
//
//
//    @Override
//    public  boolean reads(JavaType.Variable v) {
//        return reads(v, Side.RVALUE);
//    }
//
//    @Override
//    public  boolean writes(JavaType.Variable v) {
//        return writes(v, Side.RVALUE);
//    }
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        return getIndexed().reads(v, s) || getDimension().getIndex().reads(v, Side.RVALUE);
//    }
//
//    public boolean writes(JavaType.Variable v, Side s) {
//        return getIndexed().writes(v, s) || getDimension().getIndex().writes(v, Side.RVALUE);
//    }
//
//    // ArrayType
//
//
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Assert
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Assignment
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return variable.reads(v, Side.LVALUE) || getAssignment().reads(v, Side.RVALUE);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return variable.writes(v, Side.LVALUE) || getAssignment().writes(v, Side.RVALUE);
//    }
//
//    // AssignmentOperation
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return variable.reads(v, Side.LVALUE) || assignment.reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return variable.writes(v, Side.LVALUE) || assignment.writes(v);
//    }
//
//    // Binary
//
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getLeft().reads(v) || getRight().reads(v);
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return getLeft().writes(v) || getRight().writes(v);
//    }
//
//    // Block
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getStatements().stream().map(s -> s.reads(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return getStatements().stream().map(s -> s.writes(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    // Break
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return false;
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return false;
//    }
//
//    // Case
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getStatements().stream().map(s -> s.reads(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return getStatements().stream().map(s -> s.writes(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    // ClassDeclaration
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Continue
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return false;
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return false;
//    }
//
//    // DoWhileLoop
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getWhileCondition().reads(v) || getBody().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getWhileCondition().writes(v) || getBody().writes(v);
//    }
//
//    // Empty
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // EnumValueSet
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getEnums().stream().map(n -> n.getInitializer() != null && n.getInitializer().reads(v)).reduce(false, (a, b) -> a|b);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getEnums().stream().map(n -> n.getInitializer() != null && n.getInitializer().writes(v)).reduce(false, (a, b) -> a|b);
//    }
//
//    // FieldAccess
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return reads(v, Side.RVALUE);
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return writes(v, Side.RVALUE);
//    }
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        return (s == Side.RVALUE && type != null && type.equals(v)) || getTarget().reads(v, s);
//    }
//
//    public boolean writes(JavaType.Variable v, Side s) {
//        return (s == Side.LVALUE && type != null && type.equals(v)) || getTarget().reads(v, s);
//    }
//
//    // ForeachLoop
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getControl().reads(v) || getBody().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getControl().writes(v) || getBody().writes(v);
//    }
//
//    // Foreachlopp.Control
//
//
//    public boolean reads(JavaType.Variable v) {
//        return getVariable().reads(v) || getIterable().reads(v);
//    }
//    public boolean writes(JavaType.Variable v) {
//        return getVariable().writes(v) || getIterable().writes(v);
//    }
//
//    // ForLoop
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getControl().reads(v) || getBody().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getControl().writes(v) || getBody().writes(v);
//    }
//
//    // ForLoop.Control
//
//
//    public boolean reads(JavaType.Variable v) {
//        return getInit().stream().map(s -> s.reads(v)).reduce(false, (a,b) -> a|b)
//                || getUpdate().stream().map(s -> s.reads(v)).reduce(false, (a,b) -> a|b)
//                || getCondition().reads(v);
//    }
//    public boolean writes(JavaType.Variable v) {
//        return getInit().stream().map(s -> s.writes(v)).reduce(false, (a,b) -> a|b)
//                || getUpdate().stream().map(s -> s.writes(v)).reduce(false, (a,b) -> a|b)
//                || getCondition().writes(v);
//    }
//
//    // Identifier
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return reads(v, Side.RVALUE);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return writes(v, Side.RVALUE);
//    }
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        return (s == Side.RVALUE) && fieldType != null && fieldType.equals(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v, Side s) {
//        return (s == Side.LVALUE) && fieldType != null && fieldType.equals(v);
//    }
//
//    // If
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getIfCondition().reads(v) || getThenPart().reads(v) || (getElsePart() != null && getElsePart().getBody().reads(v));
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getIfCondition().writes(v) || getThenPart().writes(v) || (getElsePart() != null && getElsePart().getBody().writes(v));
//    }
//
//    // Import
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Instanceof
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getExpression().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getExpression().writes(v);
//    }
//
//    // Label
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return false;
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return false;
//    }
//
//    // Lambda
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getBody() instanceof Expression && ((Expression) getBody()).reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getBody() instanceof Expression && ((Expression) getBody()).writes(v);
//    }
//
//    // Literal
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return false;
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return false;
//    }
//
//    // MemberReference
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        // Here we assume that v is a local variable, so it cannot be referenced by a member reference.
//        // However there might be references to v in the expression.
//        return getContaining().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        // Here we assume that v is a local variable, so it cannot be referenced by a member reference.
//        // However there might be references to v in the expression.
//        return getContaining().writes(v);
//    }
//
//    // MethodDeclaration
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // MethodInvocation
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        // This does not take into account the effects inside the method body.
//        // As long as v is a local variable, we are guaranteed that it cannot be affected
//        // as a side-effect of the method invocation.
//        return (getSelect() != null && getSelect().reads(v))
//                || getArguments().stream().map(e -> e.reads(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        // This does not take into account the effects inside the method body.
//        // As long as v is a local variable, we are guaranteed that it cannot be affected
//        // as a side-effect of the method invocation.
//        return (getSelect() != null && getSelect().writes(v))
//                || getArguments().stream().map(e -> e.writes(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    // NewArray
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return (getInitializer() != null && getInitializer().stream().map(e -> e.reads(v)).reduce(false, (a,b) -> a|b))
//                || getDimensions().stream().map(e -> e.getIndex().reads(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return (getInitializer() != null && getInitializer().stream().map(e -> e.writes(v)).reduce(false, (a,b) -> a|b))
//                || getDimensions().stream().map(e -> e.getIndex().writes(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v, Side s) {
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    // NewClass
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return (getEnclosing() != null && getEnclosing().reads(v))
//                || (getArguments() != null && getArguments().stream().map(e -> e.reads(v)).reduce(false, (a,b) -> a|b))
//                || (getBody() != null && getBody().reads(v));
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return (getEnclosing() != null && getEnclosing().writes(v))
//                || (getArguments() != null && getArguments().stream().map(e -> e.writes(v)).reduce(false, (a,b) -> a|b))
//                || (getBody() != null && getBody().writes(v));
//    }
//
//    // Package
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // ParameterizedType
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // ParamerizedType.Padding
//
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v, Side s) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Parentheses
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return ((Expression)getTree()).reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return ((Expression)getTree()).reads(v);
//    }
//
//    // ControlParentheses
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getTree() instanceof Expression && ((Expression) getTree()).reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getTree() instanceof Expression && ((Expression) getTree()).writes(v);
//    }
//
//    // Primitive
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    // Return
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getExpression() != null && getExpression().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getExpression() != null && getExpression().writes(v);
//    }
//
//    // Switch
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getSelector().reads(v) || getCases().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getSelector().writes(v) || getCases().writes(v);
//    }
//
//    // Synchronized
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getLock().reads(v) || getBody().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getLock().writes(v) || getBody().writes(v);
//    }
//
//    // Ternary
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getCondition().reads(v) || getTruePart().reads(v) || getFalsePart().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getCondition().writes(v) || getTruePart().writes(v) || getFalsePart().writes(v);
//    }
//
//    // Throw
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getException().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getException().writes(v);
//    }
//
//    // Try
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return (getResources() != null && getResources().stream().map(c -> c.reads(v)).reduce(false, (a,b) -> a|b))
//                || getBody().reads(v)
//                || getCatches().stream().map(c -> c.getBody().reads(v)).reduce(false, (a,b) -> a|b)
//                || (getFinally() != null && getFinally().reads(v));
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return (getResources() != null && getResources().stream().map(c -> c.writes(v)).reduce(false, (a,b) -> a|b))
//                || getBody().reads(v)
//                || getCatches().stream().map(c -> c.getBody().writes(v)).reduce(false, (a,b) -> a|b)
//                || (getFinally() != null && getFinally().writes(v));
//    }
//
//    // Try.Ressource
//
//
//    public boolean reads(JavaType.Variable v) {
//        return variableDeclarations instanceof J.VariableDeclarations && ((J.VariableDeclarations) variableDeclarations).reads(v);
//    }
//
//    public boolean writes(JavaType.Variable v) {
//        return variableDeclarations instanceof J.VariableDeclarations && ((J.VariableDeclarations) variableDeclarations).writes(v);
//    }
//
//    // TypeCast
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getExpression().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getExpression().writes(v);
//    }
//
//    @Override
//    public boolean reads(JavaType.Variable v, Side s) {
//        if(s == Side.LVALUE) throw new NodeCannotBeAnLValueException();
//        return expression.reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v, Side s) {
//        if(s == Side.LVALUE) throw new NodeCannotBeAnLValueException();
//        return expression.writes(v);
//    }
//
//    // Unary
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return expression.reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        if(Arrays.asList(new J.Unary.Type[]{ J.Unary.Type.PreIncrement, J.Unary.Type.PreDecrement, J.Unary.Type. PostIncrement, J.Unary.Type.PostDecrement })
//                .contains(getOperator())) {
//            // expr = expr + 1, expr = 1 + expr, ...: expr appears on both sides
//            return expression.writes(v, Side.LVALUE) || expression.writes(v, Side.RVALUE);
//        }
//        return expression.writes(v);
//    }
//
//    // VariableDeclarations
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getVariables().stream().map(n -> n.getInitializer() != null && n.getInitializer().reads(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getVariables().stream().map(n -> n.getInitializer() != null && n.getInitializer().writes(v)).reduce(false, (a,b) -> a|b);
//    }
//
//    // WhileLoop
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        return getCondition().reads(v) || getBody().reads(v);
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        return getCondition().writes(v) || getBody().writes(v);
//    }
//
//    // Wildcard
//
//
//    @Override
//    public boolean reads(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean writes(JavaType.Variable v) {
//        throw new UnsupportedOperationException();
//    }
//
//}
