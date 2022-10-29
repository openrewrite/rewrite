package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;

public class AvoidBoxedBooleanExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Avoid boxed boolean expressions";
    }

    @Override
    public String getDescription() {
        return "Under certain conditions the `java.lang.Boolean` type is used as an expression, " +
               "and it may throw a `NullPointerException` if the value is null.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-5411");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.lang.Boolean");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                Expression e = (Expression) super.visitExpression(expression, ctx);
                if (TypeUtils.isOfClassType(e.getType(), "java.lang.Boolean")) {
                    Object parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
                    if (parent instanceof J.ControlParentheses ||
                        parent instanceof J.Ternary) {
                        return e.withTemplate(
                                JavaTemplate.builder(this::getCursor,
                                        "Boolean.TRUE.equals(#{any(java.lang.Boolean)})").build(),
                                e.getCoordinates().replace(),
                                e
                        );
                    }
                }
                return e;
            }
        };
    }
}
