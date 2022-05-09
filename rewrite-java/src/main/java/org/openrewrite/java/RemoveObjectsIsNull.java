package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

public class RemoveObjectsIsNull extends Recipe {

    @JsonCreator
    public RemoveObjectsIsNull() {
    }

    @Override
    public String getDisplayName() {
        return "Transform calls to Objects.isNull() and Objects.nonNull()";
    }

    @Override
    public String getDescription() {
        return "Replace calls to Objects.isNull and Objects.nonNull with a simple null check. Using these methods outside of stream predicates is not idiomatic.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new TransformCallsToObjectsIsNullVisitor();
    }

    private static MethodMatcher isNullmatcher = new MethodMatcher("java.lang.Objects isNull()");
    private static MethodMatcher nonNullmatcher = new MethodMatcher("java.lang.Objects nonNull()");

    private class TransformCallsToObjectsIsNullVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public Expression visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            if(isNullmatcher.matches(method)) {
                // isNull(e) --> e == null
                Expression e = method.getArguments().get(0);
                J.Literal nullLiteral = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "null", "null", null, JavaType.Primitive.Null);
                return new J.Binary(Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        e,
                        new JLeftPadded<>(Space.EMPTY, J.Binary.Type.Equal, Markers.EMPTY),
                        nullLiteral,
                        JavaType.Primitive.Boolean);
            }
            return method;
        }
    }

}
