package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;

public class ReplaceValidateNotNullHavingVarargsWithObjectsRequireNonNull extends Recipe {

    private static final MethodMatcher VALIDATE_NOTNULL = new MethodMatcher("org.apache.commons.lang3.Validate notNull(Object, String, Object[])");

    @Override
    public String getDisplayName() {
        return "Replace `org.apache.commons.lang3.Validate#notNull` with `Objects#requireNonNull`";
    }

    @Override
    public String getDescription() {
        return "Replace `org.apache.commons.lang3.Validate.notNull(Object, String, Object[])` with `Objects.requireNonNull(Object, String)`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(VALIDATE_NOTNULL);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
                if (!VALIDATE_NOTNULL.matches(mi)) {
                    return mi;
                }

                List<Expression> arguments = mi.getArguments();
                String template = arguments.size()==2
                        ? "Objects.requireNonNull(#{any()}, #{any(java.lang.String)})"
                        : String.format("Objects.requireNonNull(#{any()}, String.format(#{any(java.lang.String)}, %s))",
                                String.join(", ", Collections.nCopies(arguments.size() - 2, "#{any()}")));


                maybeRemoveImport("org.apache.commons.lang3.Validate");
                maybeAddImport("java.util.Objects");

                mi = mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, template)
                                .imports("java.util.Objects")
                                .build(),
                        mi.getCoordinates().replace(),
                        arguments.toArray());

                return mi;
            }
        };
    }

}
