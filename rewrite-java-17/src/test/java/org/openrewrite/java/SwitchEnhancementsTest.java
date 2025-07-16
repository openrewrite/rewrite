package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class SwitchEnhancementsTest implements RewriteTest {
    @DocumentExample
    @Test
    void yieldFromJavaTemplate() {
        rewriteRun(spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {

            @Override
            public J.Case visitCase(J.Case _case, ExecutionContext executionContext) {
                if (((J.Literal)((J.Yield)_case.getStatements().get(0)).getValue()).getValue().equals("replaced")) {
                    return _case;
                }
                String template = """
                        Object x = switch (o) {
                            default: yield #{any()};
                        };
                        """;

                J.VariableDeclarations vd = JavaTemplate.apply(
                        template,
                        new Cursor(getCursor(), _case), _case.getCoordinates().replace(),
                        new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "replaced", "\"replaced\"", null, JavaType.Primitive.String)
                );

                J.SwitchExpression switchExpression = (J.SwitchExpression) vd.getVariables().get(0).getInitializer();
                Statement updatedStatement = ((J.Case) switchExpression.getCases().getStatements().get(0)).getStatements().get(0);
                return super.visitCase(_case.withStatements(List.of(updatedStatement)), executionContext);
            }
        })), java("""
                class Test {
                    String yielded(int i) {
                        return switch (i) {
                            default: yield "value";
                        };
                    }
                }
                """, """
                class Test {
                    String yielded(int i) {
                        return switch (i) {
                            default: yield "replaced";
                        };
                    }
                }
                """));
    }

    @Test
    void yieldReformated() {
        rewriteRun(spec -> spec.recipe(new AutoFormat()),
                java("""
                class Test {
                    String yielded(int i) {
                        return switch (i) {
                            default: yield"value";
                        };
                    }
                }
                """, """
                class Test {
                    String yielded(int i) {
                        return switch (i) {
                            default: yield "value";
                        };
                    }
                }
                """));
    }
}
