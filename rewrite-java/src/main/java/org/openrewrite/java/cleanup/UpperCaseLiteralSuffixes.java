package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

public class UpperCaseLiteralSuffixes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upper case literal suffixes";
    }

    @Override
    public String getDescription() {
        return "Using upper case literal suffixes for declaring literals is less ambiguous, e.g., `1l` versus `1L`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-818");
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.of(2, ChronoUnit.MINUTES);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                J.VariableDeclarations.NamedVariable nv = super.visitVariable(variable, executionContext);
                if (nv.getInitializer() instanceof J.Literal && nv.getInitializer().getType() != null) {
                    J.Literal initializer = (J.Literal)nv.getInitializer();
                    if (initializer.getType() == JavaType.Primitive.Double
                            || initializer.getType() == JavaType.Primitive.Float
                            || initializer.getType() == JavaType.Primitive.Long) {
                        String upperValueSource = upperCaseSuffix(initializer.getValueSource());
                        if (upperValueSource != null && !upperValueSource.equals(initializer.getValueSource())) {
                            nv = nv.withInitializer(initializer.withValueSource(upperValueSource));
                        }
                    }
                }
                return nv;
            }

            @Nullable
            private String upperCaseSuffix(@Nullable String valueSource) {
                if (valueSource == null || valueSource.length() < 2) {
                    return valueSource;
                }
                return valueSource.substring(0, valueSource.length() - 1) + valueSource.substring(valueSource.length()-1).toUpperCase();
            }

        };
    }
}
