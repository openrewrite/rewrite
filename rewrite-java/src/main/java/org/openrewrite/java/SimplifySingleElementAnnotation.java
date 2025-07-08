package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.List;

public class SimplifySingleElementAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Simplify single-element annotation";
    }

    @Override
    public String getDescription() {
        return "This recipe will remove the attribute `value` on single-element annotations. " +
                "According to JLS, a _single-element annotation_, is a shorthand designed for use with single-element annotation types.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                J.Annotation an = super.visitAnnotation(annotation, executionContext);

                if (an.getArguments() != null && an.getArguments().size() == 1) {
                    an = an.withArguments(ListUtils.mapFirst(an.getArguments(), v -> {
                        if (v instanceof J.Assignment &&
                                ((J.Assignment) v).getVariable() instanceof J.Identifier &&
                                "value".equals(((J.Identifier) ((J.Assignment) v).getVariable()).getSimpleName())) {
                            Expression assignment = ((J.Assignment) v).getAssignment();
                            if (assignment instanceof J.NewArray) {
                                J.NewArray na = (J.NewArray) assignment;
                                List<Expression> initializer = na.getInitializer();
                                if (initializer != null && initializer.size() == 1) {
                                    return initializer.get(0).withPrefix(Space.EMPTY);
                                }
                            }
                            return assignment.withPrefix(Space.EMPTY);
                        }
                        return v;
                    }));
                }

                return an;
            }
        };
    }

}
