package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE;
import static org.openrewrite.java.Assertions.java;

class AddAnnotationInMethodParameterTest implements RewriteTest {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value={CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, MODULE, PARAMETER, TYPE})
    public @interface DeprecatedMethodParameter {

    }
    @Value
    @EqualsAndHashCode(callSuper = true)
    static class ExampleRecipe extends Recipe {

        @Override
        public String getDisplayName() {
            return "for format annotations";
        }

        @Override
        public String getDescription() {
            return "AddAnnotationInMethodParameterTest formatting not working.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {

                private final AnnotationMatcher annotationMatcher = new AnnotationMatcher("@org.openrewrite.java.DeprecatedMethodParameter");

                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                    boolean isMatched = multiVariable.getLeadingAnnotations()
                      .stream()
                      .filter(c -> c.getSimpleName().equals("DeprecatedMethodParameter"))
                      .findFirst()
                      .map(Objects::nonNull)
                      .orElse(false);

                    if (isMatched) {
                        return multiVariable;
                    }

                    maybeAddImport("org.openrewrite.java.DeprecatedMethodParameter");
                    return autoFormat(
                      JavaTemplate.builder("@DeprecatedMethodParameter")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().addAnnotation((o1, o2) -> -1)),
                      executionContext
                    );
                }
            };
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExampleRecipe());
    }

    @Test
    void testAddAnnotationFormatting() {

        rewriteRun(
          java(
            """
              package org.openrewrite.java;
                                  
              class Example {
                  
                  public void method(@Deprecated String method) {
                  }
              }
              """,
            """
              package org.openrewrite.java;
                          
              class Example {
                  
                  public void method(@DeprecatedMethodParameter @Deprecated String method) {
                  }
              }
              """
          )
        );
    }

}
