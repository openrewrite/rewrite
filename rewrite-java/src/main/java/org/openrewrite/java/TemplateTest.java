package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

public class TemplateTest extends Recipe {

    @Override
    public String getDisplayName() {
        return "Test auto-templating";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                JavaTemplate template = JavaTemplate.compile("theAnswerToLifeTheUniverseAndEverything", () -> {
                    Integer.valueOf("42");
                }).build(this);

                return literal.withTemplate(template, literal.getCoordinates().replace());
            }
        };
    }
}
