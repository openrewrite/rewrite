package org.openrewrite.java.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

/**
 * This recipe modifies the invocation of a method so that it returns an unmodifiable list, if the
 * method returns a modifiable one.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ArraysAsListToImmutableRecipe extends Recipe {

  @JsonCreator
  public ArraysAsListToImmutableRecipe() {
  }

  @Override
  public String getDisplayName() {
    return "Wrap list to be unmodifiable";
  }

  @Override
  public String getDescription() {
    return "Return an unmodifiable list, if the method returns a modifiable list.";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new RenameFieldVisitor();
  }

  private static class RenameFieldVisitor extends JavaIsoVisitor<ExecutionContext> {

    final MethodMatcher asListMatcher = new MethodMatcher("java.util.Arrays asList(..)");
    final MethodMatcher unmodifiableListMatcher =
        new MethodMatcher("java.util.Collections unmodifiableList(..)");

    @Override
    public J.MethodInvocation visitMethodInvocation(
        final J.MethodInvocation method, final ExecutionContext executionContext) {
      if (asListMatcher.matches(method, true)) {
        final J.MethodInvocation parentInvocation =
            getCursor().getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
        if (unmodifiableListMatcher.matches(parentInvocation, true)) {
          return super.visitMethodInvocation(method, executionContext);
        }

        J.MethodInvocation result =
            JavaTemplate.builder("Collections.unmodifiableList(#{any()})")
                .imports("java.util.Collections", "java.util.Arrays")
                .contextSensitive()
                .build()
                .apply(getCursor(), method.getCoordinates().replace(), method);
        maybeAddImport("java.util.Arrays");
        maybeAddImport("java.util.Collections");

        return result;
      }
      return super.visitMethodInvocation(method, executionContext);
    }
  }
}
