package org.openrewrite.gradle;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveEnableFeaturePreview extends Recipe {

  private static final String ENABLE_FEATURE_PREVIEW_METHOD_NAME = "enableFeaturePreview";

  @Option(displayName = "The feature preview name",
      description = "The name of the feature preview to remove.",
      example = "ONE_LOCKFILE_PER_PROJECT"
  )
  public String previewFeatureName;

  @Override
  public @NotNull String getDisplayName() {
    return "Remove a Gradle enableFeaturePreview method";
  }

  @Override
  public @NotNull String getDescription() {
    return "Remove a Gradle enableFeaturePreview method from settings.gradle / settings.gradle.kts.";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return Preconditions.check(
        new IsSettingsGradle<>(),
        new RemoveEnableFeaturePreviewVisitor());
  }

  public class RemoveEnableFeaturePreviewVisitor extends GroovyIsoVisitor<ExecutionContext> {

    @Override
    public MethodInvocation visitMethodInvocation(MethodInvocation method,
        @NotNull ExecutionContext executionContext) {
      if (ENABLE_FEATURE_PREVIEW_METHOD_NAME.equals(method.getSimpleName())) {
        List<Expression> arguments = method.getArguments();
        for (Expression argument : arguments) {
          if (argument instanceof J.Literal) {
            String candidatePreviewFeatureName = ((J.Literal) argument).getValue().toString();
            // Remove leading and trailing single or double quotes.
            candidatePreviewFeatureName = candidatePreviewFeatureName.substring(0,
                candidatePreviewFeatureName.length());
            if (candidatePreviewFeatureName.equals(previewFeatureName)) {
              return null;
            }
          }
        }
      }
      return method;
    }
  }

}
